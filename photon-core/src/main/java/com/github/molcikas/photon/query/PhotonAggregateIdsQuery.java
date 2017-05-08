package com.github.molcikas.photon.query;

import com.github.molcikas.photon.options.PhotonOptions;
import com.github.molcikas.photon.sqlbuilders.SqlBuilderApplyOptionsService;
import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.blueprints.AggregateBlueprint;
import com.github.molcikas.photon.blueprints.AggregateEntityBlueprint;
import com.github.molcikas.photon.exceptions.PhotonException;

import java.sql.Connection;
import java.util.List;

public class PhotonAggregateIdsQuery<T>
{
    private final PhotonAggregateQuery<T> photonAggregateQuery;
    private final PhotonQuery photonQuery;

    public PhotonAggregateIdsQuery(
        AggregateBlueprint<T> aggregateBlueprint,
        String selectIdsSql,
        boolean isWhereClauseOnly,
        Connection connection,
        PhotonOptions photonOptions,
        PhotonAggregateQuery<T> photonAggregateQuery)
    {
        if(StringUtils.isBlank(selectIdsSql))
        {
            throw new PhotonException("Photon aggregate SELECT by ids SQL cannot be blank.");
        }

        this.photonAggregateQuery = photonAggregateQuery;

        AggregateEntityBlueprint aggregateEntityBlueprint = aggregateBlueprint.getAggregateRootEntityBlueprint();

        if(isWhereClauseOnly)
        {
            selectIdsSql = String.format(
                "SELECT [%s] FROM [%s] WHERE %s",
                aggregateEntityBlueprint.getPrimaryKeyColumnName(),
                aggregateEntityBlueprint.getTableName(),
                selectIdsSql
            );
            selectIdsSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(selectIdsSql, photonOptions);
        }

        this.photonQuery = new PhotonQuery(selectIdsSql, false, connection, photonOptions, null);
    }

    /**
     * Adds a parameter to the current query.
     *
     * @param parameter - The name of the parameter. Must match the name used in the SQL text for this query.
     * @param value - The parameter value
     * @return - The photon query (for chaining)
     */
    public PhotonAggregateIdsQuery<T> addParameter(String parameter, Object value)
    {
        photonQuery.addParameter(parameter, value);
        return this;
    }

    /**
     * Execute the query and use the first id in the result to query for the aggregate.
     *
     * @return - The aggregate instance
     */
    public T fetch()
    {
        return photonAggregateQuery.fetchByIdsQuery(photonQuery);
    }

    /**
     * Execute the query and use the ids in the result to query for aggregates.
     *
     * @return - The aggregate instances
     */
    public List<T> fetchList()
    {
        return photonAggregateQuery.fetchListByIdsQuery(photonQuery);
    }
}
