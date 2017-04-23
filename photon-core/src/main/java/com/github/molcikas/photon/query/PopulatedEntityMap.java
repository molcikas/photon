package com.github.molcikas.photon.query;

import com.github.molcikas.photon.blueprints.AggregateEntityBlueprint;
import com.github.molcikas.photon.blueprints.ColumnBlueprint;
import com.github.molcikas.photon.blueprints.EntityBlueprint;
import com.github.molcikas.photon.blueprints.FieldBlueprint;

import java.util.*;

public class PopulatedEntityMap
{
    private final Map<Class, List<PopulatedEntity>> populatedEntityMap;
    private final Map<Class, Integer> childIndexes;

    public PopulatedEntityMap()
    {
        this.populatedEntityMap = new HashMap<>();
        this.childIndexes = new HashMap<>();
    }

    public void createPopulatedEntity(AggregateEntityBlueprint entityBlueprint, PhotonQueryResultRow queryResultRow)
    {
        PopulatedEntity populatedEntity = new PopulatedEntity(entityBlueprint, queryResultRow);
        List<PopulatedEntity> populatedEntities = populatedEntityMap.get(entityBlueprint.getEntityClass());
        if(populatedEntities == null)
        {
            // 100 is the typical max length for an aggregate sub entity list.
            populatedEntities = new ArrayList<>(100);
            populatedEntityMap.put(entityBlueprint.getEntityClass(), populatedEntities);
        }
        populatedEntities.add(populatedEntity);
    }

    public List<PopulatedEntity> getPopulatedEntitiesForClass(Class entityClass)
    {
        List<PopulatedEntity> populatedEntities = populatedEntityMap.get(entityClass);
        return populatedEntities != null ? Collections.unmodifiableList(populatedEntities) : Collections.emptyList();
    }

    public void addNextInstancesWithClassAndForeignKeyToParent(Collection collection, Class entityClass, Object key)
    {
        Integer index = childIndexes.get(entityClass);
        List<PopulatedEntity> populatedEntities = populatedEntityMap.get(entityClass);
        if(populatedEntities == null)
        {
            return;
        }
        if (index == null)
        {
            index = 0;
        }
        while (index < populatedEntities.size() && keysAreEqual(key, populatedEntities.get(index).getForeignKeyToParentValue()))
        {
            collection.add(populatedEntities.get(index).getEntityInstance());
            index++;
        }
        childIndexes.put(entityClass, index);
    }

    public Object getNextInstanceWithClassAndForeignKeyToParent(Class entityClass, Object foreignKeyToParentValue)
    {
        Integer index = childIndexes.get(entityClass);
        List<PopulatedEntity> populatedEntities = populatedEntityMap.get(entityClass);
        if(populatedEntities == null)
        {
            return null;
        }
        if (index == null)
        {
            index = 0;
        }
        if (index < populatedEntities.size() && keysAreEqual(foreignKeyToParentValue, populatedEntities.get(index).getForeignKeyToParentValue()))
        {
            childIndexes.put(entityClass, index + 1);
            return populatedEntities.get(index).getEntityInstance();
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
        ColumnBlueprint primaryKeyColumn = entityBlueprint.getPrimaryKeyColumn();
        List<PopulatedEntity> populatedEntities = populatedEntityMap.get(entityBlueprint.getEntityClass());
        String foreignTableKeyColumnName = fieldBlueprint.getForeignKeyListBlueprint().getForeignTableKeyColumnName();

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
            populatedEntity.appendValueToForeignKeyListField(fieldBlueprint, photonQueryResultRow.getValue(foreignTableKeyColumnName));
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
