package photon.query;

import photon.blueprints.EntityBlueprint;

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

    public void createPopulatedEntity(EntityBlueprint entityBlueprint, Map<String, Object> databaseValues)
    {
        PopulatedEntity populatedEntity = new PopulatedEntity(entityBlueprint, databaseValues);
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

    public Object getNextInstanceWithClassAndForeignKeyToParent(Class entityClass, Object key)
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
        if (index < populatedEntities.size() && keysAreEqual(key, populatedEntities.get(index).getForeignKeyToParentValue()))
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
