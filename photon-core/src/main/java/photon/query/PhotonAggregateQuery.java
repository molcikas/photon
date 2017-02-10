package photon.query;

import photon.blueprints.*;

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
        PopulatedEntityMap populatedEntityMap = new PopulatedEntityMap();

        for(AggregateEntityBlueprint aggregateEntityBlueprint : aggregateBlueprint.getEntityBlueprints())
        {
            executeQueryAndCreateEntityOrphans(populatedEntityMap, aggregateEntityBlueprint, ids);
        }

        populatedEntityMap.mapAllEntityInstanceChildren();

        return populatedEntityMap.getPopulatedEntitiesForClass(aggregateBlueprint.getAggregateRootClass());
    }

    private void executeQueryAndCreateEntityOrphans(PopulatedEntityMap populatedEntityMap, AggregateEntityBlueprint entityBlueprint, List ids)
    {
        try (PhotonPreparedStatement statement = new PhotonPreparedStatement(entityBlueprint.getSelectSql(), connection))
        {
            statement.setNextArrayParameter(ids, entityBlueprint.getPrimaryKeyColumn().getColumnDataType());
            List<PhotonQueryResultRow> queryResultRows = statement.executeQuery(entityBlueprint.getColumnNames());
            queryResultRows.forEach(queryResultRow -> populatedEntityMap.createPopulatedEntity(entityBlueprint, queryResultRow));
        }

        for(FieldBlueprint fieldBlueprint : entityBlueprint.getForeignKeyListFields())
        {
            ForeignKeyListBlueprint foreignKeyListBlueprint = fieldBlueprint.getForeignKeyListBlueprint();
            try (PhotonPreparedStatement statement = new PhotonPreparedStatement(foreignKeyListBlueprint.getSelectSql(), connection))
            {
                statement.setNextArrayParameter(ids, foreignKeyListBlueprint.getForeignTableKeyColumnType());
                List<PhotonQueryResultRow> queryResultRows = statement.executeQuery(foreignKeyListBlueprint.getSelectColumnNames());
                populatedEntityMap.setFieldValuesOnEntityInstances(queryResultRows, fieldBlueprint, entityBlueprint);
            }
        }
    }
}
