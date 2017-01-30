package photon.query;

import photon.exceptions.PhotonException;
import photon.blueprints.AggregateBlueprint;
import photon.blueprints.ColumnBlueprint;
import photon.blueprints.EntityBlueprint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

public class PhotonAggregateQuery<T>
{
    private final AggregateBlueprint aggregateBlueprint;
    private final Connection connection;

    public PhotonAggregateQuery(
        AggregateBlueprint aggregateBlueprint,
        Connection connection)
    {
        this.aggregateBlueprint = aggregateBlueprint;
        this.connection = connection;
    }

    public T fetchById(Object id)
    {
        List<PopulatedEntity> populatedAggregateRoots = getPopulatedAggregateRoots(Collections.singletonList(id));

        if(populatedAggregateRoots.size() == 0)
        {
            return null;
        }

        return (T) populatedAggregateRoots.get(0).getEntityInstance();
    }

    public List<T> fetchByIds(List ids)
    {
        List<PopulatedEntity> populatedAggregateRoots = getPopulatedAggregateRoots(ids);

        return (List<T>) populatedAggregateRoots
            .stream()
            .map(pe -> pe.getEntityInstance())
            .collect(Collectors.toList());
    }

    private List<PopulatedEntity> getPopulatedAggregateRoots(List ids)
    {
        Map<EntityBlueprint, String> entitySelectSqlTemplates = aggregateBlueprint.getEntitySelectSqlTemplates();
        PopulatedEntityMap populatedEntityMap = new PopulatedEntityMap();

        for(Map.Entry<EntityBlueprint, String> entityAndSelectSql : entitySelectSqlTemplates.entrySet())
        {
            EntityBlueprint entityBlueprint = entityAndSelectSql.getKey();
            executeQueryAndCreateEntityOrphans(populatedEntityMap, entityBlueprint, entityAndSelectSql.getValue(), ids);
        }

        populatedEntityMap.mapAllEntityInstanceChildren();

        return populatedEntityMap.getPopulatedEntitiesForClass(aggregateBlueprint.getAggregateRootClass());
    }

    private void executeQueryAndCreateEntityOrphans(PopulatedEntityMap populatedEntityMap, EntityBlueprint entityBlueprint, String sqlTemplate, List ids)
    {
        ColumnBlueprint primaryKeyColumnBlueprint = entityBlueprint.getPrimaryKeyColumn();

        try (PhotonPreparedStatement statement = prepareStatementAndSetParameters(sqlTemplate, primaryKeyColumnBlueprint, ids, entityBlueprint.getEntityClassName()))
        {
            try (ResultSet resultSet = statement.executeQuery())
            {
                createEntityOrphans(populatedEntityMap, resultSet, entityBlueprint);
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
        }
    }

    private void createEntityOrphans(
        PopulatedEntityMap populatedEntityMap,
        ResultSet resultSet,
        EntityBlueprint entityBlueprint)
    {
        try
        {
            while (resultSet.next())
            {
                Map<String, Object> databaseValues = new HashMap<>();
                for (ColumnBlueprint columnBlueprint : entityBlueprint.getColumns())
                {
                    Object databaseValue = resultSet.getObject(columnBlueprint.getColumnName());
                    if(databaseValue != null)
                    {
                        databaseValues.put(columnBlueprint.getColumnName(), databaseValue);
                    }
                }
                populatedEntityMap.createPopulatedEntity(entityBlueprint, databaseValues);
            }
        }
        catch (Exception ex)
        {
            throw new PhotonException(String.format("Error parsing SELECT results for entity %s.", entityBlueprint.getEntityClassName()), ex);
        }
    }

    private PhotonPreparedStatement prepareStatementAndSetParameters(String sqlTemplate, ColumnBlueprint primaryKeyColumnBlueprint, List ids, String entityClassName)
    {
        try
        {
            String sqlWithQuestionMarks = String.format(sqlTemplate, getQuestionMarks(ids.size()));
            PhotonPreparedStatement statement = new PhotonPreparedStatement(connection, sqlWithQuestionMarks);

            for(Object id : ids)
            {
                statement.setNextParameter(id, primaryKeyColumnBlueprint.getColumnDataType());
            }

            return statement;
        }
        catch (Exception ex)
        {
            throw new PhotonException(String.format("Error preparing SELECT for entity '%s'.", entityClassName), ex);
        }
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
