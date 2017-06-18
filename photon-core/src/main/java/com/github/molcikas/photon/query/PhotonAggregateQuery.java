package com.github.molcikas.photon.query;

import com.github.molcikas.photon.blueprints.AggregateBlueprint;
import com.github.molcikas.photon.blueprints.AggregateEntityBlueprint;
import com.github.molcikas.photon.blueprints.FieldBlueprint;
import com.github.molcikas.photon.blueprints.ForeignKeyListBlueprint;
import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.options.PhotonOptions;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

public class PhotonAggregateQuery<T>
{
    private final AggregateBlueprint<T> aggregateBlueprint;
    private final Connection connection;
    private final PhotonOptions photonOptions;
    private final List<String> excludedFieldPaths;

    public PhotonAggregateQuery(
        AggregateBlueprint<T> aggregateBlueprint,
        Connection connection,
        PhotonOptions photonOptions)
    {
        this.aggregateBlueprint = aggregateBlueprint;
        this.connection = connection;
        this.photonOptions = photonOptions;
        this.excludedFieldPaths = new ArrayList<>();
    }

    public PhotonAggregateQuery<T> exclude(String fieldName)
    {
        if(StringUtils.isBlank(fieldName))
        {
            throw new PhotonException("Excluded field name cannot be blank.");
        }
        excludedFieldPaths.add(fieldName);
        return this;
    }

    /**
     * Fetch an aggregate by its id.
     *
     * @param id - The id
     * @return - The aggregate instance, or null if the aggregate was not found
     */
    public T fetchById(Object id)
    {
        List<T> populatedAggregateRoots = getPopulatedAggregateRoots(Collections.singletonList(id), null, false);
        return populatedAggregateRoots.isEmpty() ? null : populatedAggregateRoots.get(0);
    }

    /**
     * Fetch a list of aggregates by ids.
     *
     * @param ids - The ids
     * @return - The aggregate instances
     */
    public List<T> fetchByIds(Object... ids)
    {
        return fetchByIds(Arrays.asList(ids));
    }

    /**
     * Fetch a list of aggregates by ids.
     *
     * @param ids - The ids
     * @return - The aggregate instances
     */
    public List<T> fetchByIds(List<?> ids)
    {
        return getPopulatedAggregateRoots(ids, null, false);
    }

    /**
     * Fetch a list of aggregates from a query that selects a list of ids.
     *
     * @param selectSql - The parameterized SQL query for selecting a list of ids
     * @return - A filter query object that can be executed to return a list of aggregate ids to fetch
     */
    public PhotonAggregateFilterQuery<T> whereIdIn(String selectSql)
    {
        return new PhotonAggregateFilterQuery<>(aggregateBlueprint, selectSql, false, connection, photonOptions, this);
    }

    /**
     * Fetch a list of aggregates that meet a specified set of conditions. Only the WHERE clause needs to be specified,
     * not including the WHERE keyword.
     *
     * @param whereClause - The where clause for a parameterized SQL query
     * @return - A filter query object that can be executed to return a list of aggregates
     */
    public PhotonAggregateFilterQuery<T> where(String whereClause)
    {
        return new PhotonAggregateFilterQuery<>(aggregateBlueprint, whereClause, true, connection, photonOptions, this);
    }

    T fetchByIdsQuery(PhotonQuery photonQuery)
    {
        List<T> populatedAggregateRoots =  getPopulatedAggregateRoots(null, photonQuery, true);
        return populatedAggregateRoots.isEmpty() ? null : populatedAggregateRoots.get(0);
    }

    T fetchByQuery(PhotonQuery photonQuery)
    {
        List<T> populatedAggregateRoots =  getPopulatedAggregateRoots(null, photonQuery, false);
        return populatedAggregateRoots.isEmpty() ? null : populatedAggregateRoots.get(0);
    }

    List<T> fetchListByIdsQuery(PhotonQuery photonQuery)
    {
        return getPopulatedAggregateRoots(null, photonQuery, true);
    }

    List<T> fetchListByQuery(PhotonQuery photonQuery)
    {
        return getPopulatedAggregateRoots(null, photonQuery, false);
    }

    private List<T> getPopulatedAggregateRoots(List<?> ids, PhotonQuery photonQuery, boolean isQueryIdsOnly)
    {
        PopulatedEntityMap populatedEntityMap = new PopulatedEntityMap();

        for(AggregateEntityBlueprint aggregateEntityBlueprint : aggregateBlueprint.getEntityBlueprints(excludedFieldPaths).values())
        {
            ids = executeQueryAndCreateEntityOrphans(populatedEntityMap, aggregateEntityBlueprint, ids, photonQuery, isQueryIdsOnly);
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
        PhotonQuery photonQuery,
        boolean isQueryIdsOnly)
    {
        List<PhotonQueryResultRow> queryResultRows;

        if(ids != null)
        {
            String selectSql = String.format(entityBlueprint.getSelectSql(), "?");
            try (PhotonPreparedStatement statement = new PhotonPreparedStatement(selectSql, false, connection, photonOptions))
            {
                statement.setNextArrayParameter(
                    ids,
                    entityBlueprint.getPrimaryKeyColumn().getColumnDataType(),
                    entityBlueprint.getPrimaryKeyColumnSerializer()
                );
                queryResultRows = statement.executeQuery(entityBlueprint.getColumnNames());
            }
        }
        else if(photonQuery != null)
        {
            String selectSql;
            if(isQueryIdsOnly)
            {
                selectSql = String.format(entityBlueprint.getSelectSql(), photonQuery.getSqlTextWithQuestionMarks());
            }
            else
            {
                selectSql = photonQuery.getSqlTextWithQuestionMarks();
            }
            try (PhotonPreparedStatement statement = new PhotonPreparedStatement(selectSql, false, connection, photonOptions))
            {
                for(PhotonSqlParameter photonSqlParameter : photonQuery.getParameters())
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
            try (PhotonPreparedStatement statement = new PhotonPreparedStatement(foreignKeyListBlueprint.getSelectSql(), false, connection, photonOptions))
            {
                statement.setNextArrayParameter(ids, foreignKeyListBlueprint.getForeignTableKeyColumnType(), null);
                List<PhotonQueryResultRow> queryResultRows = statement.executeQuery(foreignKeyListBlueprint.getSelectColumnNames());
                populatedEntityMap.setFieldValuesOnEntityInstances(queryResultRows, fieldBlueprint, entityBlueprint);
            }
        }
    }
}
