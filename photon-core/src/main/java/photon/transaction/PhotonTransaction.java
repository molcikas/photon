package photon.transaction;

import photon.exceptions.PhotonException;
import photon.blueprints.AggregateBlueprint;

import java.io.Closeable;
import java.sql.*;
import java.util.Map;

public class PhotonTransaction implements Closeable
{
    private final Connection connection;
    private final Map<Class, AggregateBlueprint> registeredAggregates;

    public PhotonTransaction(Connection connection, Map<Class, AggregateBlueprint> registeredAggregates)
    {
        this.connection = connection;
        this.registeredAggregates = registeredAggregates;

        try
        {
            connection.setAutoCommit(false);
        }
        catch(Exception ex)
        {
            throw new PhotonException("Error starting transaction.", ex);
        }
    }

    public <T> PhotonAggregateQuery<T> aggregateQuery(Class<T> aggregateClass)
    {
        AggregateBlueprint aggregateBlueprint = registeredAggregates.get(aggregateClass);
        if(aggregateBlueprint == null)
        {
            throw new PhotonException(String.format("The aggregateBlueprint class %s is not registered with photon.", aggregateBlueprint.getAggregateRootClassName()));
        }
        return new PhotonAggregateQuery<>(aggregateBlueprint, connection);
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
}
