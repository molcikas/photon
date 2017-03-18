package photon;

import photon.blueprints.EntityBlueprintConstructorService;
import photon.exceptions.PhotonException;
import photon.blueprints.AggregateBlueprint;
import photon.query.*;

import java.io.Closeable;
import java.sql.*;
import java.util.List;
import java.util.Map;

public class PhotonConnection implements Closeable
{
    private final Connection connection;
    private final Map<Class, AggregateBlueprint> registeredAggregates;
    private final EntityBlueprintConstructorService entityBlueprintConstructorService;

    public PhotonConnection(
        Connection connection,
        boolean isTransaction,
        Map<Class, AggregateBlueprint> registeredAggregates,
        EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        this.connection = connection;
        this.registeredAggregates = registeredAggregates;
        this.entityBlueprintConstructorService = entityBlueprintConstructorService;

        if(isTransaction)
        {
            try
            {
                connection.setAutoCommit(false);
            }
            catch (Exception ex)
            {
                throw new PhotonException("Error starting query.", ex);
            }
        }
    }

    public PhotonQuery query(String sqlText)
    {
        return new PhotonQuery(sqlText, connection, entityBlueprintConstructorService);
    }

    public <T> PhotonAggregateQuery<T> query(Class<T> aggregateClass)
    {
        AggregateBlueprint<T> aggregateBlueprint = getAggregateBlueprint(aggregateClass);
        return new PhotonAggregateQuery<>(aggregateBlueprint, connection);
    }

    public void save(Object aggregate)
    {
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregate.getClass());
        new PhotonAggregateSave(aggregateBlueprint, connection).save(aggregate);
    }

    public void saveAll(List<?> aggregates)
    {
        if(aggregates == null || aggregates.isEmpty())
        {
            return;
        }
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregates.get(0).getClass());
        new PhotonAggregateSave(aggregateBlueprint, connection).saveAll(aggregates);
    }

    public void insert(Object aggregate)
    {
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregate.getClass());
        new PhotonAggregateSave(aggregateBlueprint, connection).insert(aggregate);
    }

    public void insertAll(List<?> aggregates)
    {
        if(aggregates == null || aggregates.isEmpty())
        {
            return;
        }
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregates.get(0).getClass());
        new PhotonAggregateSave(aggregateBlueprint, connection).insertAll(aggregates);
    }

    public void delete(Object aggregate)
    {
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregate.getClass());
        new PhotonAggregateDelete(aggregateBlueprint, connection).delete(aggregate);
    }

    public void deleteAll(List<?> aggregates)
    {
        if(aggregates == null || aggregates.isEmpty())
        {
            return;
        }
        AggregateBlueprint aggregateBlueprint = getAggregateBlueprint(aggregates.get(0).getClass());
        new PhotonAggregateDelete(aggregateBlueprint, connection).deleteAll(aggregates);
    }

    public void commit()
    {
        try
        {
            connection.commit();
        }
        catch(Exception ex)
        {
            throw new PhotonException("Error committing transaction.", ex);
        }
    }

    public void close()
    {
        try
        {
            connection.close();
        }
        catch(Exception ex)
        {
            throw new PhotonException("Error closing connection.", ex);
        }
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
}
