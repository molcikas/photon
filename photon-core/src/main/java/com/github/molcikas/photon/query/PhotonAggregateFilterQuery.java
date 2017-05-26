package com.github.molcikas.photon.query;

import com.github.molcikas.photon.options.PhotonOptions;
import com.github.molcikas.photon.sqlbuilders.SqlBuilderApplyOptionsService;
import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.blueprints.AggregateBlueprint;
import com.github.molcikas.photon.exceptions.PhotonException;

import java.sql.Connection;
import java.util.List;

public class PhotonAggregateFilterQuery<T>
{
    private final PhotonAggregateQuery<T> photonAggregateQuery;
    private final PhotonQuery photonQuery;
    private final boolean isQueryIdsOnly;

    PhotonAggregateFilterQuery(
        AggregateBlueprint<T> aggregateBlueprint,
        String selectSql,
        boolean isWhereClauseOnly,
        Connection connection,
        PhotonOptions photonOptions,
        PhotonAggregateQuery<T> photonAggregateQuery)
    {
        if(StringUtils.isBlank(selectSql))
        {
            throw new PhotonException("Photon aggregate SELECT SQL cannot be blank.");
        }

        this.photonAggregateQuery = photonAggregateQuery;
        this.isQueryIdsOnly = !isWhereClauseOnly;

        if(isWhereClauseOnly)
        {
            selectSql = String.format(aggregateBlueprint.getAggregateRootEntityBlueprint().getSelectWhereSql(), selectSql);
            selectSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(selectSql, photonOptions);
        }

        this.photonQuery = new PhotonQuery(selectSql, false, connection, photonOptions, null);
    }

    /**
     * Adds a parameter to the current query.
     *
     * @param parameter - The name of the parameter. Must match the name used in the SQL text for this query.
     * @param value - The parameter value
     * @return - The photon query (for chaining)
     */
    public PhotonAggregateFilterQuery<T> addParameter(String parameter, Object value)
    {
        photonQuery.addParameter(parameter, value);
        return this;
    }

    /**
     * Execute the query and use the first id in the result set to query for the aggregate.
     *
     * @return - The aggregate instance
     */
    public T fetch()
    {
        if(isQueryIdsOnly)
        {
            return photonAggregateQuery.fetchByIdsQuery(photonQuery);
        }
        else
        {
            return photonAggregateQuery.fetchByQuery(photonQuery);
        }
    }

    /**
     * Execute the query and use the ids in the result set to query for aggregates.
     *
     * @return - The aggregate instances
     */
    public List<T> fetchList()
    {
        if(isQueryIdsOnly)
        {
            return photonAggregateQuery.fetchListByIdsQuery(photonQuery);
        }
        else
        {
            return photonAggregateQuery.fetchListByQuery(photonQuery);
        }
    }
}
