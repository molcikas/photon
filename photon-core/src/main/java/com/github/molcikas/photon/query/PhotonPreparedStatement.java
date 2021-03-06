package com.github.molcikas.photon.query;

import com.github.molcikas.photon.blueprints.table.ColumnDataType;
import com.github.molcikas.photon.converters.Convert;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.options.PhotonOptions;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class PhotonPreparedStatement implements Closeable
{
    private final Connection connection;
    private final String originalSqlText;
    private final boolean populateGeneratedKeys;
    private final PhotonOptions photonOptions;
    private String sqlText;
    private PreparedStatement preparedStatement;
    private final List<ParameterValue> parameterValues;
    private List<Long> generatedKeys;

    private boolean isBatched = false;

    public PhotonPreparedStatement(String sqlText, boolean populateGeneratedKeys, Connection connection, PhotonOptions photonOptions)
    {
        this.connection = connection;
        this.originalSqlText = sqlText;
        this.sqlText = sqlText;
        this.populateGeneratedKeys = populateGeneratedKeys;
        this.photonOptions = photonOptions;
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

    public void setNextArrayParameter(Collection values, ColumnDataType dataType, Converter customSerializer)
    {
        if(isBatched)
        {
            throw new PhotonException("Cannot call setNextArrayParameter() because this is a batched query. Sql: \n%s", originalSqlText);
        }

        String newTextForQuestionMark;
        int questionMarkIndex = StringUtils.ordinalIndexOf(sqlText, "?", parameterValues.size() + 1);

        if(values == null || values.size() == 0)
        {
            // Clever hack to get around SQL not liking empty IN() queries
            newTextForQuestionMark = "SELECT 1 FROM (SELECT 1) t WHERE 1=0";
        }
        else
        {
            newTextForQuestionMark = getQuestionMarks(values.size());

            for (Object value : values)
            {
                setNextParameter(value, dataType, customSerializer);
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

    public void setNextParameter(Object value, ColumnDataType dataType, Converter customSerializer)
    {
        parameterValues.add(new ParameterValue(value, dataType, customSerializer));
    }

    public void setNextParameters(List<ParameterValue> parameterValues)
    {
        this.parameterValues.addAll(parameterValues);
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
                            .map(ParameterValue::getRawValue)
                            .collect(Collectors.toList()), ','));

                }
            }
            preparedStatement.addBatch();
            isBatched = true;
            parameterValues.clear();
        }
        catch(Exception ex)
        {
            throw new PhotonException(
                ex,
                "Error preparing statement for SQL: \n%s",
                originalSqlText
            );
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
            log.debug("Photon batch query executing with SQL:\n" + sqlText);
            int[] resultCounts = preparedStatement.executeBatch();
            parameterValues.clear();
            sqlText = originalSqlText;
            updateGeneratedKeysIfRequested();
            return resultCounts;
        }
        catch(Exception ex)
        {
            throw new PhotonException(
                ex,
                "Error executing batch for SQL:\n%s",
                originalSqlText
            );
        }
    }

    public List<PhotonQueryResultRow> executeQuery()
    {
        List<PhotonQueryResultRow> resultRows = new ArrayList<>(100);

        prepareStatement();

        logQuery(null);

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
            throw new PhotonException(
                ex,
                "Error executing query for statement with SQL:\n%s",
                originalSqlText
            );
        }

        return resultRows;
    }

    public List<PhotonQueryResultRow> executeQuery(List<String> columnNames, List<String> columnNamesLowerCase)
    {
        List<PhotonQueryResultRow> resultRows = new ArrayList<>(100);

        prepareStatement();

        logQuery(null);

        try(ResultSet resultSet = preparedStatement.executeQuery())
        {
            ResultSetMetaData metaData = resultSet.getMetaData();
            List<String> resultColumnsLowerCase = new ArrayList<>(metaData.getColumnCount());
            for(int i = 1; i <= metaData.getColumnCount(); i++)
            {
                resultColumnsLowerCase.add(metaData.getColumnLabel(i).toLowerCase());
            }

            while (resultSet.next())
            {
                PhotonQueryResultRow row = new PhotonQueryResultRow();
                for(int i = 0; i < columnNames.size(); i++)
                {
                    String columnName = columnNames.get(i);
                    String columnNameLowerCase = columnNamesLowerCase.get(i);

                    if(!resultColumnsLowerCase.contains(columnNameLowerCase))
                    {
                        continue;
                    }
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
            throw new PhotonException(
                ex,
                "Error executing query for statement with SQL:\n%s",
                originalSqlText
            );
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
            throw new PhotonException(
                ex,
                "Error executing update for statement with SQL: \n%s",
                originalSqlText
            );
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
            throw new PhotonException(
                ex,
                "Error executing insert for statement with SQL: \n%s",
                originalSqlText
            );
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
            throw new PhotonException(
                ex,
                "Error getting generated keys from insert for SQL: \n%s",
                originalSqlText
            );
        }
    }

    public static Object convertValue(ParameterValue parameterValue)
    {
        if(parameterValue == null || parameterValue.getRawValue() == null)
        {
            return null;
        }

        if (parameterValue.getDataType() == null)
        {
            return parameterValue.getRawValue();
        }

        Class<?> toClass;

        switch (parameterValue.getDataType())
        {
            case BIT:
            case BOOLEAN:
                toClass = Boolean.class;
                break;
            case TINYINT:
            case SMALLINT:
            case INTEGER:
                toClass = Integer.class;
                break;
            case BIGINT:
                toClass = Long.class;
                break;
            case FLOAT:
                toClass = Float.class;
                break;
            case REAL:
            case DOUBLE:
            case NUMERIC:
            case DECIMAL:
                toClass = Double.class;
                break;
            case CHAR:
            case VARCHAR:
            case LONGVARCHAR:
                toClass = String.class;
                break;
            case DATE:
            case TIME:
            case TIMESTAMP:
                toClass = Timestamp.class;
                break;
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
                toClass = byte[].class;
                break;
            case NULL:
            case OTHER:
            case JAVA_OBJECT:
            case DISTINCT:
            case STRUCT:
            case ARRAY:
            case BLOB:
            case CLOB:
            case REF:
            case DATALINK:
            default:
                return parameterValue.getRawValue();
        }

        Converter converter = parameterValue.getCustomSerializer() != null ?
            parameterValue.getCustomSerializer() :
            Convert.getConverterIfExists(toClass);

        if(converter == null)
        {
            throw new PhotonException("No converter found for class '%s'.", toClass.getName());
        }

        return converter.convert(parameterValue.getRawValue());
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

    @SuppressWarnings("SQL_INJECTION_JDBC")
    private void prepareStatement()
    {
        try
        {
            if(preparedStatement == null)
            {
                if(populateGeneratedKeys)
                {
                    if(photonOptions.isEnableJdbcGetGeneratedKeys())
                    {
                        preparedStatement = connection.prepareStatement(sqlText, Statement.RETURN_GENERATED_KEYS);
                    }
                    else
                    {
                        preparedStatement = connection.prepareStatement(sqlText, new int[] {1});
                    }
                }
                else
                {
                    preparedStatement = connection.prepareStatement(sqlText);
                }
            }
        }
        catch(Exception ex)
        {
            throw new PhotonException(ex, "Error preparing statement for SQL: \n%s", sqlText);
        }

        int parameterIndex = 0;
        for(ParameterValue parameterValue : parameterValues)
        {
            parameterIndex++;

            try
            {
                if(parameterValue.getRawValue() == null)
                {
                    preparedStatement.setNull(parameterIndex,
                        parameterValue.getDataType() != null ? parameterValue.getDataType().getJdbcType() : ColumnDataType.VARCHAR.getJdbcType());
                    continue;
                }

                if (parameterValue.getDataType() == null)
                {
                    preparedStatement.setObject(parameterIndex, parameterValue.getRawValue());
                    continue;
                }

                switch (parameterValue.getDataType())
                {
                    case BIT:
                    case BOOLEAN:
                        preparedStatement.setBoolean(parameterIndex, (Boolean) convertValue(parameterValue));
                        continue;
                    case TINYINT:
                    case SMALLINT:
                    case INTEGER:
                        preparedStatement.setInt(parameterIndex, (Integer) convertValue(parameterValue));
                        continue;
                    case BIGINT:
                        preparedStatement.setLong(parameterIndex, (Long) convertValue(parameterValue));
                        continue;
                    case FLOAT:
                        preparedStatement.setFloat(parameterIndex, (Float) convertValue(parameterValue));
                        continue;
                    case REAL:
                    case DOUBLE:
                    case NUMERIC:
                    case DECIMAL:
                        preparedStatement.setDouble(parameterIndex, (Double) convertValue(parameterValue));
                        continue;
                    case CHAR:
                    case VARCHAR:
                    case LONGVARCHAR:
                        preparedStatement.setString(parameterIndex, (String) convertValue(parameterValue));
                        continue;
                    case DATE:
                    case TIME:
                    case TIMESTAMP:
                        preparedStatement.setTimestamp(parameterIndex, (Timestamp) convertValue(parameterValue));
                        continue;
                    case BINARY:
                    case VARBINARY:
                    case LONGVARBINARY:
                        preparedStatement.setBytes(parameterIndex, (byte[]) convertValue(parameterValue));
                        continue;
                    case NULL:
                    case OTHER:
                    case JAVA_OBJECT:
                    case DISTINCT:
                    case STRUCT:
                    case ARRAY:
                    case BLOB:
                    case CLOB:
                    case REF:
                    case DATALINK:
                    default:
                        preparedStatement.setObject(parameterIndex, parameterValue.getRawValue(), parameterValue.getDataType().getJdbcType());
                }
            }
            catch(Exception ex)
            {
                throw new PhotonException(
                    ex,
                    "Error setting parameter %s with type %s to '%s'.",
                    parameterIndex,
                    parameterValue.getDataType(),
                    parameterValue.getRawValue()
                );
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

            if(log.isDebugEnabled())
            {
                if (parameterValues.isEmpty())
                {
                    log.debug("Photon {}query executing with no params and SQL:\n{}",
                        StringUtils.isBlank(queryType) ? queryType : queryType + " ",
                        sqlText);
                }
                else
                {
                    log.debug(
                        "Photon {}query executing with params:\n{}\nSQL:\n{}",
                        StringUtils.isBlank(queryType) ? queryType : queryType + " ",
                        StringUtils.join(parameterValues.stream()
                            .map(ParameterValue::getRawValue)
                            .collect(Collectors.toList()), ','),
                        sqlText);
                }
            }
        }
    }
}
