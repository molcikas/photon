package photon.query;

import photon.blueprints.AggregateBlueprint;
import photon.blueprints.AggregateEntityBlueprint;

import java.sql.Connection;
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

        return populatedAggregateRoots
            .stream()
            .map(pe -> (T) pe.getEntityInstance())
            .collect(Collectors.toList());
    }

    private List<PopulatedEntity> getPopulatedAggregateRoots(List ids)
    {
        Map<AggregateEntityBlueprint, String> entitySelectSqlTemplates = aggregateBlueprint.getEntitySelectSqlTemplates();
        PopulatedEntityMap populatedEntityMap = new PopulatedEntityMap();

        for(Map.Entry<AggregateEntityBlueprint, String> entityAndSelectSql : entitySelectSqlTemplates.entrySet())
        {
            AggregateEntityBlueprint entityBlueprint = entityAndSelectSql.getKey();
            executeQueryAndCreateEntityOrphans(populatedEntityMap, entityBlueprint, entityAndSelectSql.getValue(), ids);
        }

        populatedEntityMap.mapAllEntityInstanceChildren();

        return populatedEntityMap.getPopulatedEntitiesForClass(aggregateBlueprint.getAggregateRootClass());
    }

    private void executeQueryAndCreateEntityOrphans(PopulatedEntityMap populatedEntityMap, AggregateEntityBlueprint entityBlueprint, String sqlTemplate, List ids)
    {
        try (PhotonPreparedStatement statement = new PhotonPreparedStatement(sqlTemplate, connection))
        {
            statement.setNextArrayParameter(ids, entityBlueprint.getPrimaryKeyColumn().getColumnDataType());
            List<String> columnNames = entityBlueprint.getColumnNames();
            List<PhotonQueryResultRow> queryResultRows = statement.executeQuery(columnNames);
            queryResultRows.forEach(queryResultRow -> populatedEntityMap.createPopulatedEntity(entityBlueprint, queryResultRow));
        }
    }
}
