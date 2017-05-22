package com.github.molcikas.photon.query;

import com.github.molcikas.photon.blueprints.*;
import com.github.molcikas.photon.converters.Convert;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.options.PhotonOptions;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PhotonQuery
{
    private static final String parameterRegex = "'?\"?:[A-Za-z0-9_]+'?\"?";
    private static final Pattern parameterPattern = Pattern.compile(parameterRegex);

    private final String sqlText;
    private final boolean populateGeneratedKeys;
    private final Connection connection;
    private final PhotonOptions photonOptions;
    private final EntityBlueprintConstructorService entityBlueprintConstructorService;
    private final List<MappedClassBlueprint> mappedClasses;

    private List<PhotonSqlParameter> parameters;
    private EntityClassDiscriminator entityClassDiscriminator;
    private List<Long> generatedKeys;

    private final Map<String, Converter> customToFieldValueConverters;

    public PhotonQuery(String sqlText, boolean populateGeneratedKeys, Connection connection, PhotonOptions photonOptions, EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        this.sqlText = sqlText;
        this.populateGeneratedKeys = populateGeneratedKeys;
        this.connection = connection;
        this.photonOptions = photonOptions;
        this.entityBlueprintConstructorService = entityBlueprintConstructorService;
        this.parameters = new ArrayList<>();
        this.mappedClasses = new ArrayList<>();

        Matcher parameterMatcher = parameterPattern.matcher(sqlText);
        while(parameterMatcher.find())
        {
            String parameterName = sqlText.substring(parameterMatcher.start() + 1, parameterMatcher.end());
            parameters.add(new PhotonSqlParameter(parameterName));
        }

        this.customToFieldValueConverters = new HashMap<>();
    }

    /**
     * Retrieve the list of generated keys. This should only be called if populateGeneratedKeys is true for this query,
     * otherwise an exception will be thrown.
     *
     * @return - The list of generated keys.
     */
    public List<Long> getGeneratedKeys()
    {
        if(!populateGeneratedKeys)
        {
            throw new PhotonException("Cannot get generated keys because the query was created with populateGeneratedKeys set to false.");
        }
        return generatedKeys;
    }

    /**
     * Adds a parameter to the current query.
     *
     * @param parameter - The name of the parameter. Must match the name used in the SQL text for this query.
     * @param value - The parameter value
     * @return - The photon query (for chaining)
     */
    public PhotonQuery addParameter(String parameter, Object value)
    {
        boolean foundMatch = false;

        for(PhotonSqlParameter photonSqlParameter : parameters)
        {
            if(StringUtils.equals(photonSqlParameter.getName(), parameter))
            {
                photonSqlParameter.assignValue(value, photonOptions);
                foundMatch = true;
            }
        }
        if(!foundMatch)
        {
            throw new PhotonException(String.format("The parameter '%s' is not in the SQL query: \n%s", parameter, sqlText));
        }
        return this;
    }

    public PhotonQuery withMappedClass(Class mappedClass)
    {
        mappedClasses.add(new MappedClassBlueprint(mappedClass, true, null));
        return this;
    }

    public PhotonQuery withMappedClass(Class mappedClass, List<String> includedFields)
    {
        mappedClasses.add(new MappedClassBlueprint(mappedClass, false, includedFields));
        return this;
    }

    public PhotonQuery withClassDiscriminator(EntityClassDiscriminator entityClassDiscriminator)
    {
        this.entityClassDiscriminator = entityClassDiscriminator;
        return this;
    }

    /**
     * Adds a parameter to the current query.
     *
     * @param parameter - The name of the parameter. Must match the name used in the SQL text for this query.
     * @param value - The parameter value
     * @param dataType - the data type of parameter. See java.sql.Types.
     * @return - The photon query (for chaining)
     */
    public PhotonQuery addParameter(String parameter, Object value, Integer dataType)
    {
        boolean foundMatch = false;

        for(PhotonSqlParameter photonSqlParameter : parameters)
        {
            if(StringUtils.equals(photonSqlParameter.getName(), parameter))
            {
                photonSqlParameter.assignValue(value, dataType);
                foundMatch = true;
            }
        }
        if(!foundMatch)
        {
            throw new PhotonException(String.format("The parameter '%s' is not in the SQL query: \n%s", parameter, sqlText));
        }
        return this;
    }

    /**
     * Execute the query and return a scalar value. The value is the first value in the first row.
     *
     * @param scalarClass - The scalar value's class
     * @param <T> - The scalar value's class
     * @return - The scalar value, or null if there were no results
     */
    public <T> T fetchScalar(Class<T> scalarClass)
    {
        PhotonPreparedStatement photonPreparedStatement = prepareStatement();
        List<PhotonQueryResultRow> results = photonPreparedStatement.executeQuery();
        if(results.isEmpty())
        {
            return null;
        }
        Object firstValue = results.get(0).getFirstValue();
        Converter<T> converter = Convert.getConverterIfExists(scalarClass);
        if(converter == null)
        {
            return (T) firstValue;
        }
        return converter.convert(firstValue);
    }

    /**
     * Execute the query and return a scalar list. The list consists of the first value in each row.
     *
     * @param scalarClass - The scalar value's class
     * @param <T> - The scalar value's class
     * @return - The scalar value list
     */
    public <T> List<T> fetchScalarList(Class<T> scalarClass)
    {
        PhotonPreparedStatement photonPreparedStatement = prepareStatement();
        List<PhotonQueryResultRow> results = photonPreparedStatement.executeQuery();
        if(results.isEmpty())
        {
            return Collections.emptyList();
        }
        List databaseValues = results.stream().map(PhotonQueryResultRow::getFirstValue).collect(Collectors.toList());
        Converter<T> converter = Convert.getConverterIfExists(scalarClass);
        if(converter == null)
        {
            return databaseValues;
        }
        return (List<T>) databaseValues.stream().map(v -> converter.convert(v)).collect(Collectors.toList());
    }

    /**
     * Executes the query and returns the result mapped to the specified class. Only the first row is mapped. Columns
     * are mapped to fields by name.
     *
     * @param classToFetch - The class to map the results into
     * @param <T> - The class to map the results into
     * @return - An instance of the class with the values in the result set, or null if there were no results
     */
    public <T> T fetch(Class<T> classToFetch)
    {
        List<T> entities = fetchList(classToFetch);
        return entities.size() > 0 ? entities.get(0) : null;
    }

    /**
     * Executes the query and returns the result with each row mapped to the specified class. Columns are mapped
     * to fields by name.
     *
     * @param classToFetch - The class to map the results into
     * @param <T> - The class to map the results into
     * @return - A list of instances of the class with the values in the result set
     */
    public <T> List<T> fetchList(Class<T> classToFetch)
    {
        PhotonPreparedStatement photonPreparedStatement = prepareStatement();

        EntityBlueprint entityBlueprint = new EntityBlueprint(
            classToFetch,
            mappedClasses,
            entityClassDiscriminator,
            null,
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            customToFieldValueConverters,
            null,
            photonOptions,
            entityBlueprintConstructorService
        );

        List<PhotonQueryResultRow> rows = photonPreparedStatement.executeQuery(entityBlueprint.getColumnNames());
        List<PopulatedEntity<T>> populatedEntities = rows
            .stream()
            .map(r -> new PopulatedEntity<T>(entityBlueprint, r))
            .collect(Collectors.toList());

        return populatedEntities
            .stream()
            .map(PopulatedEntity::getEntityInstance)
            .collect(Collectors.toList());
    }

    /**
     * Executes the query and returns the number of rows updated.
     *
     * @return - The number of rows updated
     */
    public int executeUpdate()
    {
        PhotonPreparedStatement photonPreparedStatement = prepareStatement();
        return photonPreparedStatement.executeUpdate();
    }

    /**
     * Executes the query and returns the number of rows inserted.
     *
     * @return - The number of rows inserted
     */
    public int executeInsert()
    {
        PhotonPreparedStatement photonPreparedStatement = prepareStatement();
        int rowsUpdated = photonPreparedStatement.executeInsert();
        if(populateGeneratedKeys)
        {
            generatedKeys = photonPreparedStatement.getGeneratedKeys();
        }
        return rowsUpdated;
    }

    /**
     * Use a custom value converter for mapping a database value to a field value.
     *
     * @param fieldName - The field name
     * @param customToFieldValueConverter - The value converter
     * @return - The photon query (for chaining)
     */
    public PhotonQuery withCustomToFieldValueConverter(String fieldName, Converter customToFieldValueConverter)
    {
        customToFieldValueConverters.put(fieldName, customToFieldValueConverter);
        return this;
    }

    List<PhotonSqlParameter> getParameters()
    {
        return parameters;
    }

    String getSqlTextWithQuestionMarks()
    {
        return sqlText.replaceAll(parameterRegex, "?");
    }

    private PhotonPreparedStatement prepareStatement()
    {
        PhotonPreparedStatement photonPreparedStatement =
            new PhotonPreparedStatement(getSqlTextWithQuestionMarks(), populateGeneratedKeys, connection, photonOptions);

        for(PhotonSqlParameter photonSqlParameter : parameters)
        {
            photonPreparedStatement.setNextParameter(photonSqlParameter);
        }

        return photonPreparedStatement;
    }
}
