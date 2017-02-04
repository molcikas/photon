package photon.query;

import photon.blueprints.*;
import photon.exceptions.PhotonException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PhotonQuery
{
    private static final String parameterRegex = "'?\"?:[A-Za-z0-9_]+'?\"?";
    private static final Pattern parameterPattern = Pattern.compile(parameterRegex);

    private final String sqlText;
    private final Connection connection;
    private final EntityBlueprintConstructorService entityBlueprintConstructorService;

    private Map<String, PhotonSqlParameter> parameters;

    public PhotonQuery(String sqlText, Connection connection, EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        this.sqlText = sqlText;
        this.connection = connection;
        this.entityBlueprintConstructorService = entityBlueprintConstructorService;
        this.parameters = new HashMap<>();

        Matcher parameterMatcher = parameterPattern.matcher(sqlText);
        while(parameterMatcher.find())
        {
            String parameterName = sqlText.substring(parameterMatcher.start() + 1, parameterMatcher.end());
            parameters.put(parameterName, new PhotonSqlParameter(parameters.size(), parameterName));
        }
    }

    public PhotonQuery addParameter(String parameter, Object value)
    {
        if(!parameters.containsKey(parameter))
        {
            throw new PhotonException(String.format("The parameter '%s' is not in the SQL query: \n%s", parameter, sqlText));
        }
        if(value == null)
        {
            throw new PhotonException(String.format("The parameter '%s' was set to null. Use \"IS NULL\" to check for null.", parameter, sqlText));
        }
        parameters.get(parameter).assignValue(value);
        return this;
    }

    public <T> T fetch(Class<T> classToFetch)
    {
        List<T> entities = fetchList(classToFetch);
        return entities.size() > 0 ? entities.get(0) : null;
    }

    public <T> List<T> fetchList(Class<T> classToFetch)
    {
        String sqlTextWithQuestionMarks = sqlText.replaceAll(parameterRegex, "?");

        PhotonPreparedStatement photonPreparedStatement = new PhotonPreparedStatement(sqlTextWithQuestionMarks, connection);

        List<PhotonSqlParameter> parametersInOrder = parameters
            .values()
            .stream()
            .sorted((p1, p2) -> p1.getIndex() - p2.getIndex())
            .collect(Collectors.toList());

        for(PhotonSqlParameter parameter : parametersInOrder)
        {
            Integer dataType = parameter.getValue() != null ?
                entityBlueprintConstructorService.defaultColumnDataTypeForField(parameter.getValue().getClass()) :
                null;
            Object value = parameter.getValue();
            if(value != null && Collection.class.isAssignableFrom(value.getClass()))
            {
                photonPreparedStatement.setNextArrayParameter((Collection) value, dataType);
            }
            else
            {
                photonPreparedStatement.setNextParameter(parameter.getValue(), dataType);
            }
        }

        // TODO: Cache this and try to re-use it rather than building it for every query.
        EntityBlueprint entityBlueprint = new EntityBlueprint(
            classToFetch,
            null,
            false,
            null,
            null,
            null,
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
