package photon.query;
import photon.converters.Convert;
import photon.converters.Converter;
import photon.exceptions.PhotonException;
import java.io.Closeable;
import java.sql.*;

public class PhotonPreparedStatement implements Closeable
{
    private final PreparedStatement preparedStatement;
    private final String sql;

    private int parameterIndex;

    public PhotonPreparedStatement(Connection connection, String sql)
    {
        try
        {
            this.preparedStatement = connection.prepareStatement(sql);
            this.sql = sql;
            this.parameterIndex = 1;
        }
        catch(Exception ex)
        {
            throw new PhotonException(String.format("Error preparing statement for SQL: \n%s", sql), ex);
        }
    }

    public void setNextParameter(Object value, Integer columnType)
    {
        int currentParameterIndex = parameterIndex;
        parameterIndex++;

        try
        {
            if(columnType == null)
            {
                preparedStatement.setObject(currentParameterIndex, value);
                return;
            }
            
            switch(columnType)
            {
                case Types.BIT:
                case Types.BOOLEAN:
                    preparedStatement.setBoolean(currentParameterIndex, convertValue(value, Boolean.class));
                    return;
                case Types.TINYINT:
                case Types.SMALLINT:
                case Types.INTEGER:
                    preparedStatement.setInt(currentParameterIndex, convertValue(value, Integer.class));
                    return;
                case Types.BIGINT:
                    preparedStatement.setLong(currentParameterIndex, convertValue(value, Long.class));
                    return;
                case Types.FLOAT:
                    preparedStatement.setFloat(currentParameterIndex, convertValue(value, Float.class));
                    return;
                case Types.REAL:
                case Types.DOUBLE:
                case Types.NUMERIC:
                case Types.DECIMAL:
                    preparedStatement.setDouble(currentParameterIndex, convertValue(value, Double.class));
                    return;
                case Types.CHAR:
                case Types.VARCHAR:
                case Types.LONGVARCHAR:
                    preparedStatement.setString(currentParameterIndex, convertValue(value, String.class));
                    return;
                case Types.DATE:
                case Types.TIME:
                case Types.TIMESTAMP:
                    preparedStatement.setDate(currentParameterIndex, convertValue(value, Date.class));
                    return;
                case Types.BINARY:
                case Types.VARBINARY:
                case Types.LONGVARBINARY:
                    preparedStatement.setBytes(currentParameterIndex, convertValue(value, byte[].class));
                    return;
                case Types.NULL:
                case Types.OTHER:
                case Types.JAVA_OBJECT:
                case Types.DISTINCT:
                case Types.STRUCT:
                case Types.ARRAY:
                case Types.BLOB:
                case Types.CLOB:
                case Types.REF:
                case Types.DATALINK:
                default:
                    preparedStatement.setObject(currentParameterIndex, value, columnType);
            }
        }
        catch(Exception ex)
        {
            parameterIndex--;
            throw new PhotonException(String.format("Error setting parameter %s with type %s to '%s'.", parameterIndex, columnType, value), ex);
        }
    }

    public ResultSet executeQuery()
    {
        try
        {
            return preparedStatement.executeQuery();
        }
        catch(Exception ex)
        {
            throw new PhotonException(String.format("Error executing query for statement with SQL: \n%s", sql), ex);
        }
    }

    public int executeUpdate()
    {
        try
        {
            return preparedStatement.executeUpdate();
        }
        catch(Exception ex)
        {
            throw new PhotonException(String.format("Error executing update for statement with SQL: \n%s", sql), ex);
        }
    }

    public Object executeInsert()
    {
        try
        {
            preparedStatement.executeUpdate();

            try(ResultSet generatedKeys = preparedStatement.getGeneratedKeys())
            {
                if (generatedKeys.next())
                {
                    return generatedKeys.getLong(1);
                }
                return null;
            }
        }
        catch(Exception ex)
        {
            throw new PhotonException(String.format("Error executing insert for statement with SQL: \n%s", sql), ex);
        }
    }

    public void resetParameterCounter()
    {
        this.parameterIndex = 1;
    }

    @Override
    public void close()
    {
        if(preparedStatement != null)
        {
            try
            {
                preparedStatement.close();
            }
            catch(Exception ex)
            {
                // Suppress errors related to closing.
            }
        }
    }

    private <T> T convertValue(Object value, Class<T> toClass)
    {
        Converter<T> converter = Convert.getConverterIfExists(toClass);

        if(converter == null)
        {
            throw new PhotonException(String.format("No converter found for class '%s'.", toClass.getName()));
        }

        return converter.convert(value);
    }
}
