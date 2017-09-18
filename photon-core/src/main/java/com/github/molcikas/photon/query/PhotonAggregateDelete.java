package com.github.molcikas.photon.query;

import com.github.molcikas.photon.blueprints.*;
import com.github.molcikas.photon.blueprints.entity.EntityBlueprint;
import com.github.molcikas.photon.blueprints.entity.FieldBlueprint;
import com.github.molcikas.photon.blueprints.table.ColumnDataType;
import com.github.molcikas.photon.blueprints.table.TableBlueprint;
import com.github.molcikas.photon.options.PhotonOptions;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

public class PhotonAggregateDelete
{
    private final AggregateBlueprint aggregateBlueprint;
    private final Connection connection;
    private final PhotonOptions photonOptions;

    public PhotonAggregateDelete(
        AggregateBlueprint aggregateBlueprint,
        Connection connection,
        PhotonOptions photonOptions)
    {
        this.aggregateBlueprint = aggregateBlueprint;
        this.connection = connection;
        this.photonOptions = photonOptions;
    }

    /**
     * Delete an aggregate instance.
     *
     * @param aggregateRootInstance - The aggregate instance to delete
     */
    public void delete(Object aggregateRootInstance)
    {
        PopulatedEntity aggregateRootEntity = new PopulatedEntity(aggregateBlueprint.getAggregateRootEntityBlueprint(), aggregateRootInstance);
        deleteEntitiesRecursive(Collections.singletonList(aggregateRootEntity), null);
    }

    /**
     * Deletes a list of aggregate instances.
     *
     * @param aggregateRootInstances - The aggregate instances to delete
     */
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

        EntityBlueprint entityBlueprint = populatedEntities.get(0).getEntityBlueprint();

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
        ColumnDataType primaryKeyColumnDataType = entityBlueprint.getTableBlueprint().getPrimaryKeyColumn().getColumnDataType();

        for (FieldBlueprint fieldBlueprint : entityBlueprint.getForeignKeyListFields())
        {
            try(PhotonPreparedStatement photonPreparedStatement = new PhotonPreparedStatement(
                fieldBlueprint.getForeignKeyListBlueprint().getDeleteSql(),
                false,
                connection,
                photonOptions))
            {
                photonPreparedStatement.setNextArrayParameter(
                    ids,
                    primaryKeyColumnDataType,
                    entityBlueprint.getTableBlueprint().getPrimaryKeyColumnSerializer()
                );
                photonPreparedStatement.executeUpdate();
            }
        }

        for(TableBlueprint tableBlueprint : entityBlueprint.getTableBlueprintsForDelete())
        {
            if (tableBlueprint.isPrimaryKeyMappedToField())
            {
                try (PhotonPreparedStatement photonPreparedStatement = new PhotonPreparedStatement(
                    tableBlueprint.getDeleteSql(),
                    false,
                    connection,
                    photonOptions))
                {
                    photonPreparedStatement.setNextArrayParameter(
                        ids,
                        primaryKeyColumnDataType,
                        tableBlueprint.getPrimaryKeyColumnSerializer()
                    );
                    photonPreparedStatement.executeUpdate();
                }
            }
            else
            {
                try (PhotonPreparedStatement photonPreparedStatement = new PhotonPreparedStatement(
                    tableBlueprint.getDeleteChildrenExceptSql(),
                    false,
                    connection,
                    photonOptions))
                {
                    EntityBlueprint parentPopulatedEntityBlueprint = parentPopulatedEntity.getEntityBlueprint();
                    photonPreparedStatement.setNextParameter(
                        parentPopulatedEntity.getPrimaryKeyValue(),
                        // TODO: This should use tableBlueprint.getParentBlueprint() instead.
                        parentPopulatedEntityBlueprint.getTableBlueprint().getPrimaryKeyColumn().getColumnDataType(),
                        parentPopulatedEntityBlueprint.getTableBlueprint().getPrimaryKeyColumnSerializer()
                    );
                    photonPreparedStatement.setNextArrayParameter(Collections.emptyList(), null, null);
                    photonPreparedStatement.executeUpdate();
                }
            }
        }
    }
}
