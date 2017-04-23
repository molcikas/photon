package com.github.molcikas.photon.query;

import com.github.molcikas.photon.blueprints.EntityBlueprint;
import com.github.molcikas.photon.blueprints.EntityBlueprintConstructorService;
import com.github.molcikas.photon.converters.Convert;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.exceptions.PhotonException;

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
    private final EntityBlueprintConstructorService entityBlueprintConstructorService;

    private Map<String, PhotonSqlParameter> parameters;
    private List<Long> generatedKeys;

    private final Map<String, Converter> customToFieldValueConverters;

    public List<Long> getGeneratedKeys()
    {
        if(!populateGeneratedKeys)
        {
            throw new PhotonException("Cannot get generated keys because the query was created with populateGeneratedKeys set to false.");
        }
        return generatedKeys;
    }

    public PhotonQuery(String sqlText, boolean populateGeneratedKeys, Connection connection, EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        this.sqlText = sqlText;
        this.populateGeneratedKeys = populateGeneratedKeys;
        this.connection = connection;
        this.entityBlueprintConstructorService = entityBlueprintConstructorService;
        this.parameters = new HashMap<>();

        Matcher parameterMatcher = parameterPattern.matcher(sqlText);
        while(parameterMatcher.find())
        {
            String parameterName = sqlText.substring(parameterMatcher.start() + 1, parameterMatcher.end());
            parameters.put(parameterName, new PhotonSqlParameter(parameters.size(), parameterName));
        }

        this.customToFieldValueConverters = new HashMap<>();
    }

    public List<PhotonSqlParameter> getParametersInOrder()
    {
        return parameters
            .values()
            .stream()
            .sorted(Comparator.comparingInt(PhotonSqlParameter::getIndex))
            .collect(Collectors.toList());
    }

    public String getSqlTextWithQuestionMarks()
    {
        return sqlText.replaceAll(parameterRegex, "?");
    }

    public PhotonQuery addParameter(String parameter, Object value)
    {
        if(!parameters.containsKey(parameter))
        {
            throw new PhotonException(String.format("The parameter '%s' is not in the SQL query: \n%s", parameter, sqlText));
        }
        parameters.get(parameter).assignValue(value);
        return this;
    }

    public PhotonQuery addParameter(String parameter, Object value, Integer dataType)
    {
        if(!parameters.containsKey(parameter))
        {
            throw new PhotonException(String.format("The parameter '%s' is not in the SQL query: \n%s", parameter, sqlText));
        }
        parameters.get(parameter).assignValue(value, dataType);
        return this;
    }

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

    public <T> T fetch(Class<T> classToFetch)
    {
        List<T> entities = fetchList(classToFetch);
        return entities.size() > 0 ? entities.get(0) : null;
    }

    public <T> List<T> fetchList(Class<T> classToFetch)
    {
        PhotonPreparedStatement photonPreparedStatement = prepareStatement();

        EntityBlueprint entityBlueprint = new EntityBlueprint(
            classToFetch,
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

    public int executeUpdate()
    {
        PhotonPreparedStatement photonPreparedStatement = prepareStatement();
        return photonPreparedStatement.executeUpdate();
    }

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

    public PhotonQuery withCustomToFieldValueConverter(String fieldName, Converter customToFieldValueConverter)
    {
        customToFieldValueConverters.put(fieldName, customToFieldValueConverter);
        return this;
    }

    private PhotonPreparedStatement prepareStatement()
    {
        PhotonPreparedStatement photonPreparedStatement = new PhotonPreparedStatement(getSqlTextWithQuestionMarks(), populateGeneratedKeys, connection);

        for(PhotonSqlParameter photonSqlParameter : getParametersInOrder())
        {
            photonPreparedStatement.setNextParameter(photonSqlParameter);
        }

        return photonPreparedStatement;
    }
}