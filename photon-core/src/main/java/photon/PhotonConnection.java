package photon;

import photon.blueprints.EntityBlueprintConstructorService;
import photon.exceptions.PhotonException;
import photon.blueprints.AggregateBlueprint;
import photon.query.PhotonAggregateQuery;
import photon.query.PhotonAggregateSave;
import photon.query.PhotonQuery;

import java.io.Closeable;
import java.sql.*;
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
        AggregateBlueprint aggregateBlueprint = registeredAggregates.get(aggregateClass);
        if(aggregateBlueprint == null)
        {
            throw new PhotonException(String.format("The aggregate root class '%s' is not registered with photon.", aggregateClass.getName()));
        }

        return new PhotonAggregateQuery<>(aggregateBlueprint, connection);
    }

    public void save(Object aggregate)
    {
        Class aggregateClass = aggregate.getClass();
        AggregateBlueprint aggregateBlueprint = registeredAggregates.get(aggregateClass);
        if(aggregateBlueprint == null)
        {
            throw new PhotonException(String.format("The aggregate root class '%s' is not registered with photon.", aggregateClass.getName()));
        }

        new PhotonAggregateSave(aggregateBlueprint, connection).save(aggregate);
    }

    // TODO: public void save(List<Object> aggregates)

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
}
