package com.github.molcikas.photon;

import com.github.molcikas.photon.blueprints.table.TableBlueprint;
import com.github.molcikas.photon.blueprints.table.TableBlueprintAndKey;
import com.github.molcikas.photon.blueprints.table.TableKey;
import com.github.molcikas.photon.query.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.blueprints.AggregateBlueprint;

import java.io.Closeable;
import java.sql.*;
import java.util.*;

/**
 * Contains a database transaction for Photon.
 */
@Slf4j
public class PhotonTransaction implements Closeable
{
    public class PhotonTransactionHandle
    {
        public void track(PopulatedEntityMap populatedEntityMap)
        {
            for(PopulatedEntity<?> populatedEntity : populatedEntityMap.getAllPopulatedEntities())
            {
                for(TableBlueprint tableBlueprint : populatedEntity.getEntityBlueprint().getTableBlueprintsForInsertOrUpdate())
                {
                    List<PhotonPreparedStatement.ParameterValue> parameterValues =
                        populatedEntity.getValuesForUpdate(tableBlueprint, populatedEntity.getParentPopulatedEntity());

                    trackedValues.putAll(new TableBlueprintAndKey(tableBlueprint, populatedEntity.getPrimaryKey()), parameterValues);
                }
            }
        }

        public List<PhotonPreparedStatement.ParameterValue> getTrackedValues(TableBlueprint tableBlueprint, TableKey primaryKey)
        {
            List<PhotonPreparedStatement.ParameterValue> values =
                trackedValues.get(new TableBlueprintAndKey(tableBlueprint, primaryKey));
            return values != null ? values : Collections.emptyList();
        }

        public void updateTrackedValues(TableBlueprintAndKey tableBlueprintAndKey, List<PhotonPreparedStatement.ParameterValue> values)
        {
            trackedValues.remove(tableBlueprintAndKey);
            trackedValues.putAll(tableBlueprintAndKey, values);
        }
    }

    private final Connection connection;
    private final Map<Class, AggregateBlueprint> registeredAggregates;
    private final Map<String, AggregateBlueprint> registeredViewModelAggregates;
    // TODO: Can't use this because it has a reference to the entity. We need to store a snapshot of the values!
    private final ListValuedMap<TableBlueprintAndKey, PhotonPreparedStatement.ParameterValue> trackedValues;
    private final Photon photon;
    private boolean hasUncommittedChanges = false;

    public PhotonTransaction(
        Connection connection,
        Map<Class, AggregateBlueprint> registeredAggregates,
        Map<String, AggregateBlueprint> registeredViewModelAggregates,
        Photon photon)
    {
        this.connection = connection;
        this.registeredAggregates = registeredAggregates;
        this.registeredViewModelAggregates = registeredViewModelAggregates;
        this.photon = photon;
        this.trackedValues = new ArrayListValuedHashMap<>();

        try
        {
            connection.setAutoCommit(false);
        }
        catch (Exception ex)
        {
            throw new PhotonException(ex, "Error starting transaction.");
        }
    }

    /**
     * Create a photon query. Can be used for ad-hoc queries that return custom read models, or to execute ad-hoc
     * SQL commands.
     *
     * @param sqlText - the plain parameterized SQL text
     * @return - the phton query
     */
    public PhotonQuery query(String sqlText)
    {
        verifyConnectionIsAvailable("query", false);
        return query(sqlText, false);
    }

    /**
     * Create a photon query. Can be used for ad-hoc queries that return custom read models, or to execute ad-hoc
     * SQL commands. Set populateGeneratedKeys to true if running an insert statement into a table with an auto
     * incrementing primary key and the generated key needs to be retrieved.
     *
     * @param sqlText - the plain parameterized SQL text
     * @param populateGeneratedKeys - whether to retrieve the generated keys from the query
     * @return - the photon query
     */
    public PhotonQuery query(String sqlText, boolean populateGeneratedKeys)
    {
        verifyConnectionIsAvailable("query", false);
        return new PhotonQuery(sqlText, populateGeneratedKeys, connection, photon);
    }

    /**
     * Create an aggregate query. Aggregates must be registered with Photon.
     *
     * @param aggregateClass - The aggregate root's class
     * @param <T> - The aggregate root's class
     * @return - The photon aggregate query
     */
    public <T> PhotonAggregateQuery<T> query(Class<T> aggregateClass)
    {
        verifyConnectionIsAvailable("query", false);
        AggregateBlueprint<T> aggregateBlueprint = getAggregateBlueprint(aggregateClass);
        return new PhotonAggregateQuery<>(aggregateBlueprint, connection, new PhotonTransactionHandle(), photon);
    }

    /**
     * Create an aggregate query. Aggregates must be registered with Photon.
     *
     * @param aggregateClass - The aggregate root's class
     * @param viewModelAggregateBlueprintName - the name of the view model aggregate blueprint to use for querying
     * @param <T> - The aggregate root's class
     * @return - The photon aggregate query
     */
    public <T> PhotonAggregateQuery<T> query(Class<T> aggregateClass, String viewModelAggregateBlueprintName)
    {
        verifyConnectionIsAvailable("query", false);
        AggregateBlueprint<T> aggregateBlueprint = getViewModelAggregateBlueprint(aggregateClass, viewModelAggregateBlueprintName);
        return new PhotonAggregateQuery<>(aggregateBlueprint, connection, new PhotonTransactionHandle(), photon);
    }

    /**
     * Save the aggregate instance. This will perform an update first, then an insert if the entity is not in the
     * database.
     *
     * @param aggregate - The aggregate instance to save
     */
    public void save(Object aggregate)
    {
        saveWithExcludedFields(aggregate, Collections.emptyList());
    }

    /**
     * Save the aggregate instance. This will perform an update first, then an insert if the entity is not in the
     * database.
     *
     * @param aggregate - The aggregate instance to save
     * @param fieldsToExclude - A list of fields that will NOT be saved to the database. Orphaned rows will also
     *                        not be removed.
     */
    public void saveWithExcludedFields(Object aggregate, String... fieldsToExclude)
    {
        saveWithExcludedFields(aggregate, Arrays.asList(fieldsToExclude));
    }

    /**
     * Save the aggregate instance. This will perform an update first, then an insert if the entity is not in the
     * database.
     *
     * @param aggregate - The aggregate instance to save
     * @param fieldsToExclude - A list of fields that will NOT be saved to the database. Orphaned rows will also
     *                        not be removed.
     */
    public void saveWithExcludedFields(Object aggregate, List<String> fieldsToExclude)
    {
        verifyConnectionIsAvailable("save", false);
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregate.getClass());
        new PhotonAggregateSave(aggregateBlueprint, connection, new PhotonTransactionHandle(), photon.getOptions())
            .save(aggregate, fieldsToExclude);
        hasUncommittedChanges = true;
    }

    /**
     * Save a list of aggregate instances. This will perform an update first, then an insert if the entity is not in the
     * database.
     *
     * @param aggregates - The aggregate instances to save
     * @param <T> - The aggregate root's class
     */
    public <T> void saveAll(T... aggregates)
    {
        saveAllAndExcludeFields(Arrays.asList(aggregates), null);
    }

    /**
     * Save a list of aggregate instances. This will perform an update first, then an insert if the entity is not in the
     * database.
     *
     * @param aggregates - The aggregate instances to save
     */
    public void saveAll(List<?> aggregates)
    {
        saveAllAndExcludeFields(aggregates, null);
    }

    /**
     * Save a list of aggregate instances. This will perform an update first, then an insert if the entity is not in the
     * database.
     *
     * @param aggregates - The aggregate instances to save
     * @param fieldsToExclude - A list of fields that will NOT be saved to the database. Orphaned rows will also
     *                        not be removed.
     */
    public void saveAllAndExcludeFields(List<?> aggregates, List<String> fieldsToExclude)
    {
        verifyConnectionIsAvailable("save", false);
        if(aggregates == null || aggregates.isEmpty())
        {
            return;
        }
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregates.get(0).getClass());
        new PhotonAggregateSave(aggregateBlueprint, connection, new PhotonTransactionHandle(), photon.getOptions())
            .saveAll(aggregates, fieldsToExclude);
        hasUncommittedChanges = true;
    }

    /**
     * Inserts an aggregate instance. Only call this method if the aggregate does not exist in the database, otherwise
     * an error will occur when trying to insert it.
     *
     * @param aggregate - The aggregate instance to insert
     */
    public void insert(Object aggregate)
    {
        verifyConnectionIsAvailable("insert", true);
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregate.getClass());
        new PhotonAggregateSave(aggregateBlueprint, connection, new PhotonTransactionHandle(), photon.getOptions())
            .insert(aggregate);
        hasUncommittedChanges = true;
    }

    /**
     * Inserts a list of aggregate instances. Only call this method if the aggregates do not exist in the database, otherwise
     * an error will occur when trying to insert them.
     *
     * @param aggregates - The aggregate instances to insert
     * @param <T> The aggregate root's class
     */
    public <T> void insertAll(T... aggregates)
    {
        insertAll(Arrays.asList(aggregates));
    }

    /**
     * Inserts a list of aggregate instances. Only call this method if the aggregates do not exist in the database, otherwise
     * an error will occur when trying to insert them.
     *
     * @param aggregates - The aggregate instances to insert
     */
    public void insertAll(List<?> aggregates)
    {
        verifyConnectionIsAvailable("insert", true);
        if(aggregates == null || aggregates.isEmpty())
        {
            return;
        }
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregates.get(0).getClass());
        new PhotonAggregateSave(aggregateBlueprint, connection, new PhotonTransactionHandle(), photon.getOptions())
            .insertAll(aggregates);
        hasUncommittedChanges = true;
    }

    /**
     * Delete an aggregate instance.
     *
     * @param aggregate - The aggregate instance to delete
     */
    public void delete(Object aggregate)
    {
        verifyConnectionIsAvailable("delete", false);
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregate.getClass());
        new PhotonAggregateDelete(aggregateBlueprint, connection, photon.getOptions()).delete(aggregate);
        hasUncommittedChanges = true;
    }

    /**
     * Delete a list of aggregate instances.
     *
     * @param aggregates - The aggregate instances to delete
     * @param <T> - The aggregate root's class
     */
    public <T> void deleteAll(T... aggregates)
    {
        deleteAll(Arrays.asList(aggregates));
    }

    /**
     * Delete a list of aggregate instances.
     *
     * @param aggregates - The aggregate instances to delete
     */
    public void deleteAll(List<?> aggregates)
    {
        verifyConnectionIsAvailable("delete", false);
        if(aggregates == null || aggregates.isEmpty())
        {
            return;
        }
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregates.get(0).getClass());
        new PhotonAggregateDelete(aggregateBlueprint, connection, photon.getOptions()).deleteAll(aggregates);
        hasUncommittedChanges = true;
    }

    /**
     * Commit the transaction and close the underlying connection.
     */
    public void commit()
    {
        try
        {
            connection.commit();
            hasUncommittedChanges = false;
        }
        catch(Exception ex)
        {
            throw new PhotonException(ex, "Error committing transaction.");
        }
    }

    /**
     * Close the connection without committing. If commit() was already called, calling this method will have
     * no effect.
     */
    public void close()
    {
        if(hasUncommittedChanges)
        {
            log.warn("Closing a transaction with uncommitted changes.");
        }

        try
        {
            connection.close();
        }
        catch(Exception ex)
        {
            throw new PhotonException(ex, "Error closing connection.");
        }
    }

    /**
     * Roll back the current transaction.
     */
    public void rollback()
    {
        try
        {
            connection.rollback();
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Returns whether the transaction has pending changes that haven't been committed yet.
     * @return - True if there are pending changes, otherwise false.
     */
    public boolean hasUncommittedChanges()
    {
        return hasUncommittedChanges;
    }

    private <T> AggregateBlueprint<T> getViewModelAggregateBlueprint(Class<T> aggregateClass, String viewModelAggregateBlueprintName)
    {
        AggregateBlueprint<T> aggregateBlueprint = registeredViewModelAggregates.get(viewModelAggregateBlueprintName);
        if(aggregateBlueprint == null)
        {
            throw new PhotonException(
                "The aggregate view model named '%s' is not registered with photon.",
                viewModelAggregateBlueprintName
            );
        }
        if(aggregateBlueprint.getAggregateRootClass().isAssignableFrom(aggregateClass.getClass()))
        {
            throw new PhotonException(
                "The aggregate blueprint view model '%s' is not compatible with class '%s'.",
                viewModelAggregateBlueprintName,
                aggregateClass.getName()
            );
        }
        return aggregateBlueprint;
    }

    private <T> AggregateBlueprint<T> getAggregateBlueprint(Class<T> aggregateClass)
    {
        AggregateBlueprint<T> aggregateBlueprint = registeredAggregates.get(aggregateClass);
        Class superClass = aggregateClass;

        while(aggregateBlueprint == null && superClass != null)
        {
            superClass = superClass.getSuperclass();
            if(superClass != null)
            {
                aggregateBlueprint = registeredAggregates.get(superClass);
            }
        }

        if(aggregateBlueprint == null)
        {
            throw new PhotonException("The aggregate root class '%s' is not registered with photon.", aggregateClass.getName());
        }
        return aggregateBlueprint;
    }

    private void verifyConnectionIsAvailable(String operation, boolean useA)
    {
        try
        {
            if (connection.isClosed())
            {
                throw new PhotonException("Cannot perform %s %s operation because the connection is closed.", useA ? "a" : "an", operation);
            }
        }
        catch(SQLException ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
