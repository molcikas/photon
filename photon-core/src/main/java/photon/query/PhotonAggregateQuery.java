package photon.query;

import photon.blueprints.*;
import photon.exceptions.PhotonException;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

public class PhotonAggregateQuery<T>
{
    private final AggregateBlueprint<T> aggregateBlueprint;
    private final Connection connection;

    public PhotonAggregateQuery(
        AggregateBlueprint<T> aggregateBlueprint,
        Connection connection)
    {
        this.aggregateBlueprint = aggregateBlueprint;
        this.connection = connection;
    }

    public T fetchById(Object id)
    {
        List<T> populatedAggregateRoots = getPopulatedAggregateRoots(Collections.singletonList(id), null);
        return populatedAggregateRoots.isEmpty() ? null : populatedAggregateRoots.get(0);
    }

    public List<T> fetchByIds(List<?> ids)
    {
        return getPopulatedAggregateRoots(ids, null);
    }

    public PhotonAggregateIdsQuery<T> fetchByIdsQuery(String selectIdsSql)
    {
        return new PhotonAggregateIdsQuery<>(aggregateBlueprint, selectIdsSql, false, connection, this);
    }

    public PhotonAggregateIdsQuery<T> where(String whereClause)
    {
        return new PhotonAggregateIdsQuery<>(aggregateBlueprint, whereClause, true, connection, this);
    }

    public T fetchByIdsQuery(PhotonQuery photonQuery)
    {
        List<T> populatedAggregateRoots =  getPopulatedAggregateRoots(null, photonQuery);
        return populatedAggregateRoots.isEmpty() ? null : populatedAggregateRoots.get(0);
    }

    public List<T> fetchListByIdsQuery(PhotonQuery photonQuery)
    {
        return getPopulatedAggregateRoots(null, photonQuery);
    }

    private List<T> getPopulatedAggregateRoots(List<?> ids, PhotonQuery photonQuery)
    {
        PopulatedEntityMap populatedEntityMap = new PopulatedEntityMap();

        for(AggregateEntityBlueprint aggregateEntityBlueprint : aggregateBlueprint.getEntityBlueprints())
        {
            ids = executeQueryAndCreateEntityOrphans(populatedEntityMap, aggregateEntityBlueprint, ids, photonQuery);
        }

        populatedEntityMap.mapAllEntityInstanceChildren();

        return populatedEntityMap
            .getPopulatedEntitiesForClass(aggregateBlueprint.getAggregateRootClass())
            .stream()
            .map(pe -> (T) pe.getEntityInstance())
            .collect(Collectors.toList());
    }

    private List<?> executeQueryAndCreateEntityOrphans(
        PopulatedEntityMap populatedEntityMap,
        AggregateEntityBlueprint entityBlueprint,
        List<?> ids,
        PhotonQuery photonQuery)
    {
        List<PhotonQueryResultRow> queryResultRows;

        if(ids != null)
        {
            String selectSql = String.format(entityBlueprint.getSelectSql(), "?");
            try (PhotonPreparedStatement statement = new PhotonPreparedStatement(selectSql, false, connection))
            {
                statement.setNextArrayParameter(ids, entityBlueprint.getPrimaryKeyColumn().getColumnDataType(), entityBlueprint.getPrimaryKeyCustomToDatabaseValueConverter());
                queryResultRows = statement.executeQuery(entityBlueprint.getColumnNames());
            }
        }
        else if(photonQuery != null)
        {
            String selectSql = String.format(entityBlueprint.getSelectSql(), photonQuery.getSqlTextWithQuestionMarks());
            try (PhotonPreparedStatement statement = new PhotonPreparedStatement(selectSql, false, connection))
            {
                for(PhotonSqlParameter photonSqlParameter : photonQuery.getParametersInOrder())
                {
                    statement.setNextParameter(photonSqlParameter);
                }
                queryResultRows = statement.executeQuery(entityBlueprint.getColumnNames());
            }
        }
        else
        {
            throw new PhotonException("Ids list and query were both null.");
        }

        queryResultRows.forEach(queryResultRow -> populatedEntityMap.createPopulatedEntity(entityBlueprint, queryResultRow));

        if(ids == null)
        {
            ids = populatedEntityMap
                .getPopulatedEntitiesForClass(entityBlueprint.getEntityClass())
                .stream()
                .map(PopulatedEntity::getPrimaryKeyValue)
                .collect(Collectors.toList());
        }

        populateForeignKeyListFields(populatedEntityMap, entityBlueprint, ids);

        return ids;
    }

    private void populateForeignKeyListFields(PopulatedEntityMap populatedEntityMap, AggregateEntityBlueprint entityBlueprint, List<?> ids)
    {
        for(FieldBlueprint fieldBlueprint : entityBlueprint.getForeignKeyListFields())
        {
            ForeignKeyListBlueprint foreignKeyListBlueprint = fieldBlueprint.getForeignKeyListBlueprint();
            try (PhotonPreparedStatement statement = new PhotonPreparedStatement(foreignKeyListBlueprint.getSelectSql(), false, connection))
            {
                statement.setNextArrayParameter(ids, foreignKeyListBlueprint.getForeignTableKeyColumnType(), null);
                List<PhotonQueryResultRow> queryResultRows = statement.executeQuery(foreignKeyListBlueprint.getSelectColumnNames());
                populatedEntityMap.setFieldValuesOnEntityInstances(queryResultRows, fieldBlueprint, entityBlueprint);
            }
        }
    }
}
