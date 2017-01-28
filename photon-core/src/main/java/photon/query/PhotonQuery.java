package photon.query;

import photon.exceptions.PhotonException;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class PhotonQuery
{
    private final Connection connection;
    private final String sqlText;

    public PhotonQuery(Connection connection, String sqlText)
    {
        this.connection = connection;
        this.sqlText = sqlText;
    }

    public int executeUpdate()
    {
        try
        {
            PreparedStatement preparedStatement = connection.prepareStatement(this.sqlText);
            return preparedStatement.executeUpdate();
        }
        catch(Exception ex)
        {
            throw new PhotonException("Error executing update.", ex);
        }
    }
}
