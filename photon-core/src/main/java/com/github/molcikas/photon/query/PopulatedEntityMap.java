package com.github.molcikas.photon.query;

import com.github.molcikas.photon.blueprints.entity.EntityBlueprint;
import com.github.molcikas.photon.blueprints.table.ColumnBlueprint;
import com.github.molcikas.photon.blueprints.entity.FieldBlueprint;

import java.util.*;
import java.util.stream.Collectors;

public class PopulatedEntityMap
{
    // TODO: Convert to multivalue map
    private final Map<EntityBlueprint, List<PopulatedEntity<?>>> populatedEntityMap;
    private final Map<EntityBlueprint, Integer> childIndexes;

    public PopulatedEntityMap()
    {
        this.populatedEntityMap = new HashMap<>();
        this.childIndexes = new HashMap<>();
    }

    public List<PopulatedEntity<?>> getAllPopulatedEntities()
    {
        return populatedEntityMap
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    public void createPopulatedEntity(EntityBlueprint entityBlueprint, PhotonQueryResultRow queryResultRow)
    {
        PopulatedEntity populatedEntity = new PopulatedEntity(entityBlueprint, queryResultRow);
        // 100 is the typical max length for an aggregate sub entity list.
        List<PopulatedEntity<?>> populatedEntities =
            populatedEntityMap.computeIfAbsent(entityBlueprint, k -> new ArrayList<>(100));
        populatedEntities.add(populatedEntity);
    }

    public List<PopulatedEntity<?>> getPopulatedEntitiesForBlueprint(EntityBlueprint entityBlueprint)
    {
        List<PopulatedEntity<?>> populatedEntities = populatedEntityMap.get(entityBlueprint);
        return populatedEntities != null ? Collections.unmodifiableList(populatedEntities) : Collections.emptyList();
    }

    public void setParentAndAddChildrenToCollection(Collection collection, EntityBlueprint entityBlueprint, PopulatedEntity<?> parentPopulatedEntity)
    {
        PopulatedEntity<?> populatedEntity = setParentAndGetNextChild(entityBlueprint, parentPopulatedEntity);

        while (populatedEntity != null)
        {
            collection.add(populatedEntity.getEntityInstance());
            populatedEntity = setParentAndGetNextChild(entityBlueprint, parentPopulatedEntity);
        }
    }

    public PopulatedEntity<?> setParentAndGetNextChild(EntityBlueprint entityBlueprint, PopulatedEntity<?> parentPopulatedEntity)
    {
        Integer index = childIndexes.get(entityBlueprint);
        List<PopulatedEntity<?>> populatedEntities = populatedEntityMap.get(entityBlueprint);
        if(populatedEntities == null)
        {
            return null;
        }
        if (index == null)
        {
            index = 0;
        }
        if (index < populatedEntities.size() && keysAreEqual(parentPopulatedEntity.getPrimaryKeyValue(), populatedEntities.get(index).getForeignKeyToParentValue()))
        {
            childIndexes.put(entityBlueprint, index + 1);
            PopulatedEntity<?> populatedEntity = populatedEntities.get(index);
            populatedEntity.setParentPopulatedEntity(parentPopulatedEntity);
            return populatedEntity;
        }
        return null;
    }

    public void mapAllEntityInstanceChildren()
    {
        populatedEntityMap
            .values()
            .forEach(populatedEntities -> populatedEntities.forEach(populatedEntity -> populatedEntity.mapEntityInstanceChildren(this)));
    }

    public void setFieldValuesOnEntityInstances(List<PhotonQueryResultRow> photonQueryResultRows, FieldBlueprint fieldBlueprint, EntityBlueprint entityBlueprint)
    {
        ColumnBlueprint primaryKeyColumn = entityBlueprint.getTableBlueprint().getPrimaryKeyColumn();
        List<PopulatedEntity<?>> populatedEntities = populatedEntityMap.get(entityBlueprint);
        String foreignTableKeyColumnName = fieldBlueprint.getFlattenedCollectionBlueprint().getColumnName();

        int entityIndex = 0;
        for(PhotonQueryResultRow photonQueryResultRow : photonQueryResultRows)
        {
            Object primaryKeyValue = photonQueryResultRow.getValue(primaryKeyColumn.getColumnName());
            while(entityIndex < populatedEntities.size() && !keysAreEqual(primaryKeyValue, populatedEntities.get(entityIndex).getPrimaryKeyValue()))
            {
                entityIndex++;
            }
            if(entityIndex >= populatedEntities.size())
            {
                break;
            }
            PopulatedEntity populatedEntity = populatedEntities.get(entityIndex);
            populatedEntity.appendValueToFlattenedCollectionField(fieldBlueprint, photonQueryResultRow.getValue(foreignTableKeyColumnName));
        }
    }

    private boolean keysAreEqual(Object primaryKey, Object foreignKey)
    {
        if(primaryKey.equals(foreignKey))
        {
            return true;
        }

        if (primaryKey instanceof byte[] && foreignKey instanceof byte[])
        {
            return Arrays.equals((byte[]) primaryKey, (byte[]) foreignKey);
        }

        return false;
    }
}
