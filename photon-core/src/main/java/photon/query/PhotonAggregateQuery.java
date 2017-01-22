package photon.query;

import photon.IOUtils;
import photon.exceptions.PhotonException;
import photon.blueprints.AggregateBlueprint;
import photon.blueprints.ColumnBlueprint;
import photon.blueprints.EntityBlueprint;
import photon.blueprints.EntityFieldBlueprint;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

public class PhotonAggregateQuery<T>
{
    private final AggregateBlueprint aggregateBlueprint;
    private final Connection connection;

    public PhotonAggregateQuery(AggregateBlueprint aggregateBlueprint, Connection connection)
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

        populatedEntityMap.mapEntityInstanceChildren();

        return populatedEntityMap.getPopulatedEntitiesForClass(aggregateBlueprint.getAggregateRootClass());
    }

    private void executeQueryAndCreateEntityOrphans(PopulatedEntityMap populatedEntityMap, EntityBlueprint entityBlueprint, String sqlTemplate, List ids)
    {
        ColumnBlueprint primaryKeyColumnBlueprint = entityBlueprint.getPrimaryKeyColumn();

        try (PreparedStatement statement = prepareStatementAndSetParameters(sqlTemplate, primaryKeyColumnBlueprint, ids, entityBlueprint.getEntityClassName()))
        {
            try (ResultSet resultSet = executeJdbcQuery(statement, entityBlueprint.getEntityClassName()))
            {
                createEntityOrphans(populatedEntityMap, resultSet, entityBlueprint);
            }
        }
        catch(Exception ex)
        {
            throw new PhotonException("Error executing query.", ex);
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
                for (ColumnBlueprint columnBlueprint : entityBlueprint.getColumns().values())
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

    private PreparedStatement prepareStatementAndSetParameters(String sqlTemplate, ColumnBlueprint primaryKeyColumnBlueprint, List ids, String entityClassName)
    {
        try
        {
            String sqlWithQuestionMarks = String.format(sqlTemplate, getQuestionMarks(ids.size()));
            PreparedStatement statement = connection.prepareStatement(sqlWithQuestionMarks);

            int i = 1;
            for(Object id : ids)
            {
                if (primaryKeyColumnBlueprint.getColumnDataType() != null)
                {
                    if (primaryKeyColumnBlueprint.getColumnDataType() == Types.BINARY && id.getClass().equals(UUID.class))
                    {
                        statement.setObject(i, IOUtils.uuidToBytes((UUID) id), primaryKeyColumnBlueprint.getColumnDataType());
                    }
                    else
                    {
                        statement.setObject(i, id, primaryKeyColumnBlueprint.getColumnDataType());
                    }
                }
                else
                {
                    statement.setObject(i, id);
                }

                i++;
            }

            return statement;
        }
        catch (Exception ex)
        {
            throw new PhotonException(String.format("Error preparing SELECT for entityBlueprint '%s'.", entityClassName), ex);
        }
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

    private ResultSet executeJdbcQuery(PreparedStatement statement, String entityClassName)
    {
        try
        {
            return statement.executeQuery();
        }
        catch (Exception ex)
        {
            throw new PhotonException(String.format("Error executing SELECT for entityBlueprint '%s'.", entityClassName), ex);
        }
    }
}
