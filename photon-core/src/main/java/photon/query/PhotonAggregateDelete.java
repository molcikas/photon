package photon.query;

import photon.blueprints.*;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

public class PhotonAggregateDelete
{
    private final AggregateBlueprint aggregateBlueprint;
    private final Connection connection;

    public PhotonAggregateDelete(
        AggregateBlueprint aggregateBlueprint,
        Connection connection)
    {
        this.aggregateBlueprint = aggregateBlueprint;
        this.connection = connection;
    }

    public void delete(Object aggregateRootInstance)
    {
        PopulatedEntity aggregateRootEntity = new PopulatedEntity(aggregateBlueprint.getAggregateRootEntityBlueprint(), aggregateRootInstance);
        deleteEntitiesRecursive(Collections.singletonList(aggregateRootEntity), null);
    }

    public void deleteAll(List<?> aggregateRootInstances)
    {
        List<PopulatedEntity> aggregateRootEntities = aggregateRootInstances
            .stream()
            .map(instance -> new PopulatedEntity(aggregateBlueprint.getAggregateRootEntityBlueprint(), instance))
            .collect(Collectors.toList());
        deleteEntitiesRecursive(aggregateRootEntities, null);
    }

    private void deleteEntitiesRecursive(List<PopulatedEntity> populatedEntities, PopulatedEntity parentPopulatedEntity)
    {
        if(populatedEntities == null || populatedEntities.isEmpty())
        {
            return;
        }

        AggregateEntityBlueprint entityBlueprint = (AggregateEntityBlueprint) populatedEntities.get(0).getEntityBlueprint();

        for(PopulatedEntity populatedEntity : populatedEntities)
        {
            for (FieldBlueprint fieldBlueprint : entityBlueprint.getFieldsWithChildEntities())
            {
                List<PopulatedEntity> fieldPopulatedEntities = populatedEntity.getChildPopulatedEntitiesForField(fieldBlueprint);
                deleteEntitiesRecursive(fieldPopulatedEntities, populatedEntity);
            }
        }

        List<Object> ids = populatedEntities
            .stream()
            .map(PopulatedEntity::getPrimaryKeyValue)
            .collect(Collectors.toList());
        Integer primaryKeyColumnDataType = entityBlueprint.getPrimaryKeyColumn().getColumnDataType();

        for (FieldBlueprint fieldBlueprint : entityBlueprint.getForeignKeyListFields())
        {
            try(PhotonPreparedStatement photonPreparedStatement = new PhotonPreparedStatement(fieldBlueprint.getForeignKeyListBlueprint().getDeleteSql(), connection))
            {
                photonPreparedStatement.setNextArrayParameter(ids, primaryKeyColumnDataType, entityBlueprint.getPrimaryKeyCustomToDatabaseValueConverter());
                photonPreparedStatement.executeUpdate();
            }
        }

        if(entityBlueprint.isPrimaryKeyMappedToField())
        {
            try (PhotonPreparedStatement photonPreparedStatement = new PhotonPreparedStatement(entityBlueprint.getDeleteSql(), connection))
            {
                photonPreparedStatement.setNextArrayParameter(ids, primaryKeyColumnDataType, entityBlueprint.getPrimaryKeyCustomToDatabaseValueConverter());
                photonPreparedStatement.executeUpdate();
            }
        }
        else
        {
            try (PhotonPreparedStatement photonPreparedStatement = new PhotonPreparedStatement(entityBlueprint.getDeleteChildrenExceptSql(), connection))
            {
                EntityBlueprint parentPopulatedEntityBlueprint = parentPopulatedEntity.getEntityBlueprint();
                photonPreparedStatement.setNextParameter(parentPopulatedEntity.getPrimaryKeyValue(), parentPopulatedEntityBlueprint.getPrimaryKeyColumn().getColumnDataType(), parentPopulatedEntityBlueprint.getPrimaryKeyCustomToDatabaseValueConverter());
                photonPreparedStatement.setNextArrayParameter(Collections.emptyList(), null, null);
                photonPreparedStatement.executeUpdate();
            }
        }
    }
}
