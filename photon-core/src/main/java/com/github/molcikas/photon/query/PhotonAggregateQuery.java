package com.github.molcikas.photon.query;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonEntityState;
import com.github.molcikas.photon.blueprints.AggregateBlueprint;
import com.github.molcikas.photon.blueprints.entity.EntityBlueprint;
import com.github.molcikas.photon.blueprints.entity.FieldBlueprint;
import com.github.molcikas.photon.blueprints.entity.FlattenedCollectionBlueprint;
import com.github.molcikas.photon.exceptions.PhotonException;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

public class PhotonAggregateQuery<T>
{
    private final AggregateBlueprint<T> aggregateBlueprint;
    private final Connection connection;
    private final PhotonEntityState photonEntityState;
    private final Photon photon;
    private final List<String> excludedFieldPaths;
    private boolean trackChanges = true;

    public PhotonAggregateQuery(
        AggregateBlueprint<T> aggregateBlueprint,
        Connection connection,
        PhotonEntityState photonEntityState,
        Photon photon)
    {
        this.aggregateBlueprint = aggregateBlueprint;
        this.connection = connection;
        this.photonEntityState = photonEntityState;
        this.photon = photon;
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

    public PhotonAggregateQuery<T> noTracking()
    {
        this.trackChanges = false;
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
        return new PhotonAggregateFilterQuery<>(aggregateBlueprint, selectSql, false, connection, photon, this);
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
        return new PhotonAggregateFilterQuery<>(aggregateBlueprint, whereClause, true, connection, photon, this);
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

        for(EntityBlueprint entityBlueprint : aggregateBlueprint.getEntityBlueprints(excludedFieldPaths))
        {
            ids = executeQueryAndCreateEntityOrphans(populatedEntityMap, entityBlueprint, ids, photonQuery, isQueryIdsOnly);
        }

        populatedEntityMap.mapAllEntityInstanceChildren();

        if(trackChanges)
        {
            photonEntityState.track(populatedEntityMap);
        }

        return populatedEntityMap
            .getPopulatedEntitiesForBlueprint(aggregateBlueprint.getAggregateRootEntityBlueprint())
            .stream()
            .map(pe -> (T) pe.getEntityInstance())
            .collect(Collectors.toList());
    }

    private List<?> executeQueryAndCreateEntityOrphans(
        PopulatedEntityMap populatedEntityMap,
        EntityBlueprint entityBlueprint,
        List<?> ids,
        PhotonQuery photonQuery,
        boolean isQueryIdsOnly)
    {
        List<PhotonQueryResultRow> queryResultRows;

        if(ids != null)
        {
            String selectSql = String.format(entityBlueprint.getTableBlueprint().getSelectSql(), "?");
            try (PhotonPreparedStatement statement = new PhotonPreparedStatement(selectSql, false, connection, photon.getOptions()))
            {
                statement.setNextArrayParameter(
                    ids,
                    entityBlueprint.getTableBlueprint().getPrimaryKeyColumn().getColumnDataType(),
                    entityBlueprint.getTableBlueprint().getPrimaryKeyColumnSerializer()
                );
                queryResultRows = statement.executeQuery(entityBlueprint.getAllColumnNamesQualified());
            }
        }
        else if(photonQuery != null)
        {
            String selectSql;
            if(isQueryIdsOnly)
            {
                selectSql = String.format(entityBlueprint.getTableBlueprint().getSelectSql(), photonQuery.getSqlTextWithQuestionMarks());
            }
            else
            {
                selectSql = photonQuery.getSqlTextWithQuestionMarks();
            }
            try (PhotonPreparedStatement statement = new PhotonPreparedStatement(selectSql, false, connection, photon.getOptions()))
            {
                for(PhotonSqlParameter photonSqlParameter : photonQuery.getParameters())
                {
                    statement.setNextParameter(photonSqlParameter);
                }
                queryResultRows = statement.executeQuery(entityBlueprint.getAllColumnNamesQualified());
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
                .getPopulatedEntitiesForBlueprint(entityBlueprint)
                .stream()
                .map(PopulatedEntity::getPrimaryKeyValue)
                .collect(Collectors.toList());
        }

        populateFlattenedCollectionFields(populatedEntityMap, entityBlueprint, ids);

        return ids;
    }

    private void populateFlattenedCollectionFields(PopulatedEntityMap populatedEntityMap, EntityBlueprint entityBlueprint, List<?> ids)
    {
        for(FieldBlueprint fieldBlueprint : entityBlueprint.getFlattenedCollectionFields())
        {
            FlattenedCollectionBlueprint flattenedCollectionBlueprint = fieldBlueprint.getFlattenedCollectionBlueprint();
            try (PhotonPreparedStatement statement = new PhotonPreparedStatement(flattenedCollectionBlueprint.getSelectSql(), false, connection, photon.getOptions()))
            {
                statement.setNextArrayParameter(ids, flattenedCollectionBlueprint.getColumnDataType(), null);
                List<PhotonQueryResultRow> queryResultRows = statement.executeQuery(flattenedCollectionBlueprint.getSelectColumnNames());
                populatedEntityMap.setFieldValuesOnEntityInstances(queryResultRows, fieldBlueprint, entityBlueprint);
            }
        }
    }
}
