package photon.query;
import org.apache.commons.lang3.StringUtils;
import photon.converters.Convert;
import photon.converters.Converter;
import photon.exceptions.PhotonException;
import java.io.Closeable;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PhotonPreparedStatement implements Closeable
{
    private class ParameterValue
    {
        public final Object value;
        public final Integer dataType;

        public ParameterValue(Object value, Integer dataType)
        {
            this.value = value;
            this.dataType = dataType;
        }
    }

    private PreparedStatement preparedStatement;
    private final Connection connection;
    private final String originalSqlText;
    private String sqlText;
    private final List<ParameterValue> parameterValues;

    public PhotonPreparedStatement(String sqlText, Connection connection)
    {
        this.connection = connection;
        this.originalSqlText = sqlText;
        this.sqlText = sqlText;
        this.parameterValues = new ArrayList<>(StringUtils.countMatches(sqlText, "?"));
    }

    public void setNextArrayParameter(Collection values, Integer columnType)
    {
        int questionMarkIndex = StringUtils.ordinalIndexOf(sqlText, "?", parameterValues.size() + 1);
        String questionMarks = getQuestionMarks(values.size());
        StringBuilder newSqlText = new StringBuilder(sqlText.length() + questionMarks.length());

        if(questionMarkIndex > 0)
        {
            newSqlText.append(sqlText.substring(0, questionMarkIndex));
        }
        newSqlText.append(questionMarks);
        if(questionMarkIndex < sqlText.length() - 1)
        {
            newSqlText.append(sqlText.substring(questionMarkIndex + 1));
        }
        sqlText = newSqlText.toString();

        for(Object value : values)
        {
            setNextParameter(value, columnType);
        }
    }

    public void setNextParameter(Object value, Integer dataType)
    {
        parameterValues.add(new ParameterValue(value, dataType));
    }

    private void prepareStatement()
    {
        try
        {
            preparedStatement = connection.prepareStatement(sqlText);
        }
        catch(Exception ex)
        {
            throw new PhotonException(String.format("Error preparing statement for SQL: \n%s", sqlText), ex);
        }

        int parameterIndex = 0;
        for(ParameterValue parameterValue : parameterValues)
        {
            parameterIndex++;

            try
            {
                if (parameterValue.dataType == null)
                {
                    preparedStatement.setObject(parameterIndex, parameterValue.value);
                    continue;
                }

                switch (parameterValue.dataType)
                {
                    case Types.BIT:
                    case Types.BOOLEAN:
                        preparedStatement.setBoolean(parameterIndex, convertValue(parameterValue.value, Boolean.class));
                        continue;
                    case Types.TINYINT:
                    case Types.SMALLINT:
                    case Types.INTEGER:
                        preparedStatement.setInt(parameterIndex, convertValue(parameterValue.value, Integer.class));
                        continue;
                    case Types.BIGINT:
                        preparedStatement.setLong(parameterIndex, convertValue(parameterValue.value, Long.class));
                        continue;
                    case Types.FLOAT:
                        preparedStatement.setFloat(parameterIndex, convertValue(parameterValue.value, Float.class));
                        continue;
                    case Types.REAL:
                    case Types.DOUBLE:
                    case Types.NUMERIC:
                    case Types.DECIMAL:
                        preparedStatement.setDouble(parameterIndex, convertValue(parameterValue.value, Double.class));
                        continue;
                    case Types.CHAR:
                    case Types.VARCHAR:
                    case Types.LONGVARCHAR:
                        preparedStatement.setString(parameterIndex, convertValue(parameterValue.value, String.class));
                        continue;
                    case Types.DATE:
                    case Types.TIME:
                    case Types.TIMESTAMP:
                        preparedStatement.setDate(parameterIndex, convertValue(parameterValue.value, Date.class));
                        continue;
                    case Types.BINARY:
                    case Types.VARBINARY:
                    case Types.LONGVARBINARY:
                        preparedStatement.setBytes(parameterIndex, convertValue(parameterValue.value, byte[].class));
                        continue;
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
                        preparedStatement.setObject(parameterIndex, parameterValue.value, parameterValue.dataType);
                }
            }
            catch(Exception ex)
            {
                throw new PhotonException(String.format("Error setting parameter %s with type %s to '%s'.", parameterIndex, parameterValue.dataType, parameterValue.value), ex);
            }
        }
    }

    public List<PhotonQueryResultRow> executeQuery(List<String> columnNames)
    {
        List<PhotonQueryResultRow> resultRows = new ArrayList<>(100);

        prepareStatement();

        try(ResultSet resultSet = preparedStatement.executeQuery())
        {
            while (resultSet.next())
            {
                PhotonQueryResultRow row = new PhotonQueryResultRow();
                for (String columnName : columnNames)
                {
                    Object value = resultSet.getObject(columnName);
                    if (value != null)
                    {
                        row.addValue(columnName, value);
                    }
                }
                resultRows.add(row);
            }
        }
        catch(Exception ex)
        {
            throw new PhotonException(String.format("Error executing query for statement with SQL: \n%s", originalSqlText), ex);
        }

        return resultRows;
    }

    public int executeUpdate()
    {
        prepareStatement();

        try
        {
            return preparedStatement.executeUpdate();
        }
        catch(Exception ex)
        {
            throw new PhotonException(String.format("Error executing update for statement with SQL: \n%s", originalSqlText), ex);
        }
    }

    public Object executeInsert()
    {
        prepareStatement();

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
            throw new PhotonException(String.format("Error executing insert for statement with SQL: \n%s", originalSqlText), ex);
        }
    }

    public void resetParameterCounter()
    {
        this.parameterValues.clear();
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

    // TODO: Put this code in central place
    private String getQuestionMarks(int count)
    {
        StringBuilder questionMarks = new StringBuilder(count * 2 - 1);
        for(int i = 0; i < count; i++)
        {
            if(i < count - 1)
            {
                questionMarks.append("?,");
            }
            else
            {
                questionMarks.append("?");
            }
        }
        return questionMarks.toString();
    }
}
