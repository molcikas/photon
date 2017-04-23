package com.github.molcikas.photon.query;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.molcikas.photon.converters.Convert;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.exceptions.PhotonException;
import java.io.Closeable;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PhotonPreparedStatement implements Closeable
{
    private static final Logger log = LoggerFactory.getLogger(PhotonPreparedStatement.class);

    private class ParameterValue
    {
        public final Object value;
        public final Integer dataType;
        public final Converter customToDatabaseValueConverter;

        public ParameterValue(Object value, Integer dataType, Converter customToDatabaseValueConverter)
        {
            this.value = value;
            this.dataType = dataType;
            this.customToDatabaseValueConverter = customToDatabaseValueConverter;
        }
    }

    private final Connection connection;
    private final String originalSqlText;
    private final boolean populateGeneratedKeys;
    private String sqlText;
    private PreparedStatement preparedStatement;
    private final List<ParameterValue> parameterValues;
    private List<Long> generatedKeys;

    private boolean isBatched = false;

    public PhotonPreparedStatement(String sqlText, boolean populateGeneratedKeys, Connection connection)
    {
        this.connection = connection;
        this.originalSqlText = sqlText;
        this.sqlText = sqlText;
        this.populateGeneratedKeys = populateGeneratedKeys;
        this.parameterValues = new ArrayList<>(StringUtils.countMatches(sqlText, "?"));
        this.generatedKeys = populateGeneratedKeys ? new ArrayList<>(100) : null;
    }

    public void setNextParameter(PhotonSqlParameter photonSqlParameter)
    {
        if(photonSqlParameter.isCollection())
        {
            setNextArrayParameter((Collection) photonSqlParameter.getValue(), photonSqlParameter.getDataType(), null);
        }
        else
        {
            setNextParameter(photonSqlParameter.getValue(), photonSqlParameter.getDataType(), null);
        }
    }

    public void setNextArrayParameter(Collection values, Integer dataType, Converter customToDatabaseValueConverter)
    {
        if(isBatched)
        {
            throw new PhotonException(String.format("Cannot call setNextArrayParameter() because this is a batched query. Sql: \n%s", originalSqlText));
        }

        String newTextForQuestionMark;
        int questionMarkIndex = StringUtils.ordinalIndexOf(sqlText, "?", parameterValues.size() + 1);

        if(values == null || values.size() == 0)
        {
            // Clever hack to get around SQL not liking empty IN() queries
            newTextForQuestionMark = "SELECT 1 WHERE 1=0";
        }
        else
        {
            newTextForQuestionMark = getQuestionMarks(values.size());

            for (Object value : values)
            {
                setNextParameter(value, dataType, customToDatabaseValueConverter);
            }
        }

        StringBuilder newSqlText = new StringBuilder(sqlText.length() + newTextForQuestionMark.length());
        if (questionMarkIndex > 0)
        {
            newSqlText.append(sqlText.substring(0, questionMarkIndex));
        }
        newSqlText.append(newTextForQuestionMark);
        if (questionMarkIndex < sqlText.length() - 1)
        {
            newSqlText.append(sqlText.substring(questionMarkIndex + 1));
        }
        sqlText = newSqlText.toString();
    }

    public void setNextParameter(Object value, Integer dataType, Converter customToDatabaseValueConverter)
    {
        parameterValues.add(new ParameterValue(value, dataType, customToDatabaseValueConverter));
    }

    public void addToBatch()
    {
        prepareStatement();

        try
        {
            if(log.isDebugEnabled())
            {
                if(parameterValues.isEmpty())
                {
                    log.debug("Photon query batch added with no params.");
                }
                else
                {
                    log.debug("Photon query batch added with params:\n" +
                        StringUtils.join(parameterValues.stream()
                            .map(p -> p.value)
                            .collect(Collectors.toList()), ','));

                }
            }
            preparedStatement.addBatch();
            isBatched = true;
            parameterValues.clear();
        }
        catch(Exception ex)
        {
            throw new PhotonException(String.format("Error preparing statement for SQL: \n%s", originalSqlText), ex);
        }
    }

    public int[] executeBatch()
    {
        if(preparedStatement == null)
        {
            // Executing empty batch, just reset and return that nothing was updated.
            parameterValues.clear();
            if(generatedKeys != null)
            {
                generatedKeys.clear();
            }
            return new int[0];
        }

        try
        {
            if(log.isDebugEnabled())
            {
                log.debug("Photon batch query executing with SQL:\n" + sqlText);
            }
            int[] resultCounts = preparedStatement.executeBatch();
            parameterValues.clear();
            sqlText = originalSqlText;
            updateGeneratedKeysIfRequested();
            return resultCounts;
        }
        catch(Exception ex)
        {
            throw new PhotonException(String.format("Error executing batch for SQL:\n%s", originalSqlText), ex);
        }
    }

    public List<PhotonQueryResultRow> executeQuery()
    {
        List<PhotonQueryResultRow> resultRows = new ArrayList<>(100);

        prepareStatement();

        try(ResultSet resultSet = preparedStatement.executeQuery())
        {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            while (resultSet.next())
            {
                PhotonQueryResultRow row = new PhotonQueryResultRow();
                for (int c = 1; c <= resultSetMetaData.getColumnCount(); c++)
                {
                    Object value = resultSet.getObject(c);
                    if (value != null)
                    {
                        row.addValue(resultSetMetaData.getColumnLabel(c), value);
                    }
                }
                resultRows.add(row);
            }
        }
        catch(Exception ex)
        {
            throw new PhotonException(String.format("Error executing query for statement with SQL:\n%s", originalSqlText), ex);
        }

        return resultRows;
    }

    public List<PhotonQueryResultRow> executeQuery(List<String> columnNames)
    {
        List<PhotonQueryResultRow> resultRows = new ArrayList<>(100);

        prepareStatement();

        logQuery(null);

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
            throw new PhotonException(String.format("Error executing query for statement with SQL:\n%s", originalSqlText), ex);
        }

        return resultRows;
    }

    public int executeUpdate()
    {
        prepareStatement();

        try
        {
            logQuery("update");
            return preparedStatement.executeUpdate();
        }
        catch(Exception ex)
        {
            throw new PhotonException(String.format("Error executing update for statement with SQL: \n%s", originalSqlText), ex);
        }
    }

    public int executeInsert()
    {
        prepareStatement();

        try
        {
            logQuery("insert");
            int rowsUpdated = preparedStatement.executeUpdate();
            updateGeneratedKeysIfRequested();
            return rowsUpdated;
        }
        catch(Exception ex)
        {
            throw new PhotonException(String.format("Error executing insert for statement with SQL: \n%s", originalSqlText), ex);
        }
    }

    public List<Long> getGeneratedKeys()
    {
        if(!populateGeneratedKeys)
        {
            throw new PhotonException("Cannot get generated keys because the statement was created with populateGeneratedKeys set to false.");
        }
        return Collections.unmodifiableList(generatedKeys);
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

    private void updateGeneratedKeysIfRequested()
    {
        if(!populateGeneratedKeys)
        {
            return;
        }

        generatedKeys.clear();

        try(ResultSet keysResult = preparedStatement.getGeneratedKeys())
        {
            while (keysResult.next())
            {
                generatedKeys.add(keysResult.getLong(1));
            }
        }
        catch(Exception ex)
        {
            throw new PhotonException(String.format("Error getting generated keys from insert for SQL: \n%s", originalSqlText), ex);
        }
    }

    private <T> T convertValue(ParameterValue parameterValue, Class<T> toClass)
    {
        Converter<T> converter = parameterValue.customToDatabaseValueConverter != null ?
            parameterValue.customToDatabaseValueConverter :
            Convert.getConverterIfExists(toClass);

        if(converter == null)
        {
            throw new PhotonException(String.format("No converter found for class '%s'.", toClass.getName()));
        }

        return converter.convert(parameterValue.value);
    }

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

    private void prepareStatement()
    {
        try
        {
            if(preparedStatement == null)
            {
                if(populateGeneratedKeys)
                {
                    preparedStatement = connection.prepareStatement(sqlText, Statement.RETURN_GENERATED_KEYS);
                }
                else
                {
                    preparedStatement = connection.prepareStatement(sqlText);
                }
            }
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
                if(parameterValue.value == null)
                {
                    preparedStatement.setNull(parameterIndex, parameterValue.dataType != null ? parameterValue.dataType : Types.VARCHAR);
                }

                if (parameterValue.dataType == null)
                {
                    preparedStatement.setObject(parameterIndex, parameterValue.value);
                    continue;
                }

                switch (parameterValue.dataType)
                {
                    case Types.BIT:
                    case Types.BOOLEAN:
                        preparedStatement.setBoolean(parameterIndex, convertValue(parameterValue, Boolean.class));
                        continue;
                    case Types.TINYINT:
                    case Types.SMALLINT:
                    case Types.INTEGER:
                        preparedStatement.setInt(parameterIndex, convertValue(parameterValue, Integer.class));
                        continue;
                    case Types.BIGINT:
                        preparedStatement.setLong(parameterIndex, convertValue(parameterValue, Long.class));
                        continue;
                    case Types.FLOAT:
                        preparedStatement.setFloat(parameterIndex, convertValue(parameterValue, Float.class));
                        continue;
                    case Types.REAL:
                    case Types.DOUBLE:
                    case Types.NUMERIC:
                    case Types.DECIMAL:
                        preparedStatement.setDouble(parameterIndex, convertValue(parameterValue, Double.class));
                        continue;
                    case Types.CHAR:
                    case Types.VARCHAR:
                    case Types.LONGVARCHAR:
                        preparedStatement.setString(parameterIndex, convertValue(parameterValue, String.class));
                        continue;
                    case Types.DATE:
                    case Types.TIME:
                    case Types.TIMESTAMP:
                        preparedStatement.setTimestamp(parameterIndex, convertValue(parameterValue, Timestamp.class));
                        continue;
                    case Types.BINARY:
                    case Types.VARBINARY:
                    case Types.LONGVARBINARY:
                        preparedStatement.setBytes(parameterIndex, convertValue(parameterValue, byte[].class));
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

    private void logQuery(String queryType)
    {
        if(log.isDebugEnabled())
        {
            if(queryType == null)
            {
                queryType = "";
            }

            if(parameterValues.isEmpty())
            {
                log.debug("Photon {}query executing with no params and SQL:\n{}", StringUtils.isBlank(queryType) ? queryType : queryType + " ", sqlText);
            }
            else
            {
                log.debug(
                    "Photon {}query executing with params:\n{}\nSQL:\n{}",
                    StringUtils.isBlank(queryType) ? queryType : queryType + " ",
                    StringUtils.join(parameterValues.stream()
                        .map(p -> p.value)
                        .collect(Collectors.toList()), ','),
                    sqlText);
            }
        }
    }
}
