package photon;

import photon.exceptions.PhotonException;
import photon.blueprints.AggregateBlueprint;
import photon.query.PhotonAggregateQuery;
import photon.query.PhotonQuery;

import java.io.Closeable;
import java.sql.*;
import java.util.Map;

public class PhotonConnection implements Closeable
{
    private final Connection connection;
    private final Map<Class, AggregateBlueprint> registeredAggregates;

    public PhotonConnection(Connection connection, boolean isTransaction, Map<Class, AggregateBlueprint> registeredAggregates)
    {
        this.connection = connection;
        this.registeredAggregates = registeredAggregates;

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

    public <T> PhotonAggregateQuery<T> aggregateQuery(Class<T> aggregateClass)
    {
        AggregateBlueprint aggregateBlueprint = registeredAggregates.get(aggregateClass);
        if(aggregateBlueprint == null)
        {
            throw new PhotonException(String.format("The aggregate root class '%s' is not registered with photon.", aggregateClass.getName()));
        }

        return new PhotonAggregateQuery<>(aggregateBlueprint, connection);
    }

    public PhotonQuery query(String sqlText)
    {
        return new PhotonQuery(connection, sqlText);
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
