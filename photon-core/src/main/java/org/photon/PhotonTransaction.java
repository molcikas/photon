package org.photon;

import org.photon.query.PhotonAggregateDelete;
import org.photon.query.PhotonAggregateQuery;
import org.photon.query.PhotonAggregateSave;
import org.photon.query.PhotonQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.photon.blueprints.EntityBlueprintConstructorService;
import org.photon.exceptions.PhotonException;
import org.photon.blueprints.AggregateBlueprint;

import java.io.Closeable;
import java.sql.*;
import java.util.List;
import java.util.Map;

public class PhotonTransaction implements Closeable
{
    private static final Logger log = LoggerFactory.getLogger(PhotonTransaction.class);

    private final Connection connection;
    private final Map<Class, AggregateBlueprint> registeredAggregates;
    private final EntityBlueprintConstructorService entityBlueprintConstructorService;
    private boolean committed = false;
    private boolean hasUncommittedChanges = false;

    public PhotonTransaction(
        Connection connection,
        Map<Class, AggregateBlueprint> registeredAggregates,
        EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        this.connection = connection;
        this.registeredAggregates = registeredAggregates;
        this.entityBlueprintConstructorService = entityBlueprintConstructorService;

        try
        {
            connection.setAutoCommit(false);
        }
        catch (Exception ex)
        {
            throw new PhotonException("Error starting transaction.", ex);
        }
    }

    public PhotonQuery query(String sqlText)
    {
        verifyConnectionIsAvailable("query", false);
        return query(sqlText, false);
    }

    public PhotonQuery query(String sqlText, boolean populateGeneratedKeys)
    {
        verifyConnectionIsAvailable("query", false);
        return new PhotonQuery(sqlText, populateGeneratedKeys, connection, entityBlueprintConstructorService);
    }

    public <T> PhotonAggregateQuery<T> query(Class<T> aggregateClass)
    {
        verifyConnectionIsAvailable("query", false);
        AggregateBlueprint<T> aggregateBlueprint = getAggregateBlueprint(aggregateClass);
        return new PhotonAggregateQuery<>(aggregateBlueprint, connection);
    }

    public void save(Object aggregate)
    {
        verifyConnectionIsAvailable("save", false);
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregate.getClass());
        new PhotonAggregateSave(aggregateBlueprint, connection).save(aggregate);
        hasUncommittedChanges = true;
    }

    public void saveAll(List<?> aggregates)
    {
        verifyConnectionIsAvailable("save", false);
        if(aggregates == null || aggregates.isEmpty())
        {
            return;
        }
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregates.get(0).getClass());
        new PhotonAggregateSave(aggregateBlueprint, connection).saveAll(aggregates);
        hasUncommittedChanges = true;
    }

    public void insert(Object aggregate)
    {
        verifyConnectionIsAvailable("insert", true);
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregate.getClass());
        new PhotonAggregateSave(aggregateBlueprint, connection).insert(aggregate);
        hasUncommittedChanges = true;
    }

    public void insertAll(List<?> aggregates)
    {
        verifyConnectionIsAvailable("insert", true);
        if(aggregates == null || aggregates.isEmpty())
        {
            return;
        }
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregates.get(0).getClass());
        new PhotonAggregateSave(aggregateBlueprint, connection).insertAll(aggregates);
        hasUncommittedChanges = true;
    }

    public void delete(Object aggregate)
    {
        verifyConnectionIsAvailable("delete", false);
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregate.getClass());
        new PhotonAggregateDelete(aggregateBlueprint, connection).delete(aggregate);
        hasUncommittedChanges = true;
    }

    public void deleteAll(List<?> aggregates)
    {
        verifyConnectionIsAvailable("delete", false);
        if(aggregates == null || aggregates.isEmpty())
        {
            return;
        }
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregates.get(0).getClass());
        new PhotonAggregateDelete(aggregateBlueprint, connection).deleteAll(aggregates);
        hasUncommittedChanges = true;
    }

    public void commit()
    {
        try
        {
            connection.commit();
            committed = true;
            hasUncommittedChanges = false;
            connection.close();
        }
        catch(Exception ex)
        {
            throw new PhotonException("Error committing transaction.", ex);
        }
    }

    public void close()
    {
        if(!committed && hasUncommittedChanges)
        {
            log.warn("Closing a transaction with uncommitted changes.");
        }

        try
        {
            connection.close();
        }
        catch(Exception ex)
        {
            throw new PhotonException("Error closing connection.", ex);
        }
    }

    public void rollbackTransaction()
    {
        if(committed)
        {
            throw new PhotonException("Cannot roll back the transaction because it has already been committed.");
        }

        try
        {
            connection.rollback();
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    public boolean isCommitted()
    {
        return committed;
    }

    private <T> AggregateBlueprint<T> getAggregateBlueprint(Class<T> aggregateClass)
    {
        AggregateBlueprint<T> aggregateBlueprint = registeredAggregates.get(aggregateClass);
        if(aggregateBlueprint == null)
        {
            throw new PhotonException(String.format("The aggregate root class '%s' is not registered with photon.", aggregateClass.getName()));
        }
        return aggregateBlueprint;
    }

    private void verifyConnectionIsAvailable(String operation, boolean useA)
    {
        if(committed)
        {
            throw new PhotonException(String.format("Cannot perform %s %s operation because the transaction has already been committed.", useA ? "a" : "an", operation));
        }
        try
        {
            if (connection.isClosed())
            {
                throw new PhotonException(String.format("Cannot perform %s %s operation because the connection is closed.", useA ? "a" : "an", operation));
            }
        }
        catch(SQLException ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
