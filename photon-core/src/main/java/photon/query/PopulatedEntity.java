package photon.query;

import org.apache.commons.lang3.StringUtils;
import photon.blueprints.ColumnBlueprint;
import photon.converters.Convert;
import photon.converters.Converter;
import photon.exceptions.PhotonException;
import photon.blueprints.EntityBlueprint;
import photon.blueprints.FieldBlueprint;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class PopulatedEntity
{
    private final EntityBlueprint entityBlueprint;
    private final Object entityInstance;
    private Object primaryKeyValue;
    private Object foreignKeyToParentValue;

    public EntityBlueprint getEntityBlueprint()
    {
        return entityBlueprint;
    }

    public Object getEntityInstance()
    {
        return entityInstance;
    }

    public Object getPrimaryKeyValue()
    {
        return primaryKeyValue;
    }

    public Object getForeignKeyToParentValue()
    {
        return foreignKeyToParentValue;
    }

    public PopulatedEntity(EntityBlueprint entityBlueprint, Map<String, Object> databaseValues)
    {
        this.entityBlueprint = entityBlueprint;
        this.entityInstance = constructOrphanEntityInstance(databaseValues);
    }

    public PopulatedEntity(EntityBlueprint entityBlueprint, Object entityInstance)
    {
        this.entityBlueprint = entityBlueprint;
        this.entityInstance = entityInstance;
        this.primaryKeyValue = getInstanceValue(entityBlueprint.getPrimaryKeyColumn());
        this.foreignKeyToParentValue = getInstanceValue(entityBlueprint.getForeignKeyToParentColumn());
    }

    public Object getInstanceValue(ColumnBlueprint columnBlueprint)
    {
        if(columnBlueprint == null)
        {
            return null;
        }

        return getInstanceValue(columnBlueprint.getMappedFieldBlueprint());
    }

    public Object getInstanceValue(FieldBlueprint fieldBlueprint)
    {
        if(fieldBlueprint == null)
        {
            return null;
        }

        return getInstanceValue(fieldBlueprint.getFieldName());
    }

    public Object getInstanceValue(String fieldName)
    {
        try
        {
            Field field = entityBlueprint.getReflectedField(fieldName);
            return field.get(entityInstance);
        }
        catch(Exception ex)
        {
            throw new PhotonException(String.format("Error getting value for field '%s' on entity '%s'.", fieldName, entityBlueprint.getEntityClassName()), ex);
        }
    }

    public List<PopulatedEntity> getChildPopulatedEntitiesForField(FieldBlueprint fieldBlueprint)
    {
        Collection childEntityInstances;
        Object fieldValue = getInstanceValue(fieldBlueprint);

        if(fieldValue == null)
        {
            childEntityInstances = Collections.emptyList();
        }
        else if(Collection.class.isAssignableFrom(fieldValue.getClass()))
        {
            childEntityInstances = (Collection) fieldValue;
        }
        else
        {
            childEntityInstances = Collections.singletonList(fieldValue);
        }

        return Arrays.stream(childEntityInstances.toArray())
            .map(i -> new PopulatedEntity(fieldBlueprint.getChildEntityBlueprint(), i))
            .collect(Collectors.toList());
    }

    public void setForeignKeyToParentValue(Object foreignKeyToParentValue)
    {
        this.foreignKeyToParentValue = foreignKeyToParentValue;
        setEntityInstanceFieldToDatabaseValue(entityInstance, this.entityBlueprint.getForeignKeyToParentColumnName(), foreignKeyToParentValue);
    }

    public void mapEntityInstanceChildren(PopulatedEntityMap populatedEntityMap)
    {
        Object primaryKey = getPrimaryKeyValue();

        for(FieldBlueprint fieldBlueprint : entityBlueprint.getFieldsWithChildEntities())
        {
            Class childEntityClass = fieldBlueprint.getChildEntityBlueprint().getEntityClass();

            if(Collection.class.isAssignableFrom(fieldBlueprint.getFieldClass()))
            {
                Collection collection = createCompatibleCollection(fieldBlueprint.getFieldClass());
                populatedEntityMap.addNextInstancesWithClassAndForeignKeyToParent(collection, childEntityClass, primaryKey);

                try
                {
                    Field field = entityBlueprint.getReflectedField(fieldBlueprint.getFieldName());
                    field.set(entityInstance, collection);
                }
                catch(Exception ex)
                {
                    throw new PhotonException(String.format("Error setting collection field '%s' on entity '%s'.", fieldBlueprint.getFieldName(), entityBlueprint.getEntityClassName()), ex);
                }
            }
            else if(fieldBlueprint.getFieldClass().equals(fieldBlueprint.getChildEntityBlueprint().getEntityClass()))
            {
                Object childInstance = populatedEntityMap.getNextInstanceWithClassAndForeignKeyToParent(childEntityClass, primaryKey);
                if(childInstance != null)
                {
                    try
                    {
                        Field field = entityBlueprint.getReflectedField(fieldBlueprint.getFieldName());
                        field.set(entityInstance, childInstance);
                    }
                    catch (Exception ex)
                    {
                        throw new PhotonException(String.format("Error setting one-to-one field '%s' on entity '%s'.", fieldBlueprint.getFieldName(), entityBlueprint.getEntityClassName()), ex);
                    }
                }
            }
        }
    }

    public int performUpdate(PhotonPreparedStatement updateStatement, PopulatedEntity parentPopulatedEntity)
    {
        if(primaryKeyValue == null)
        {
            return 0;
        }

        if(entityBlueprint.getPrimaryKeyColumn().isAutoIncrementColumn() && primaryKeyValue.equals(0))
        {
            return 0;
        }

        boolean canPerformUpdate = true;

        for (ColumnBlueprint columnBlueprint : entityBlueprint.getColumns())
        {
            Object fieldValue;
            FieldBlueprint fieldBlueprint = columnBlueprint.getMappedFieldBlueprint();

            if (fieldBlueprint != null)
            {
                fieldValue = getInstanceValue(fieldBlueprint);
            }
            else if (columnBlueprint.isForeignKeyToParentColumn())
            {
                fieldValue = parentPopulatedEntity.getPrimaryKeyValue();
            }
            else
            {
                canPerformUpdate = false;
                break;
            }

            updateStatement.setNextParameter(fieldValue, columnBlueprint.getColumnDataType());
        }

        if(!canPerformUpdate)
        {
            return 0;
        }

        return updateStatement.executeUpdate();
    }

    public void performInsert(PhotonPreparedStatement insertStatement, PopulatedEntity parentPopulatedEntity)
    {
        for (ColumnBlueprint columnBlueprint : entityBlueprint.getColumnsForInsertStatement())
        {
            Object fieldValue;
            FieldBlueprint fieldBlueprint = columnBlueprint.getMappedFieldBlueprint();

            if(fieldBlueprint != null)
            {
                fieldValue = getInstanceValue(fieldBlueprint);
            }
            else if(columnBlueprint.isForeignKeyToParentColumn())
            {
                fieldValue = parentPopulatedEntity.getPrimaryKeyValue();
            }
            else if(columnBlueprint.isPrimaryKeyColumn())
            {
                // If the primary key is not mapped to a field and is not auto increment, assume it's a UUID column.
                fieldValue = UUID.randomUUID();
            }
            else
            {
                throw new PhotonException(String.format("Cannot save entity '%s' because a value for column '%s' could not be determined.",
                    entityBlueprint.getEntityClassName(),
                    columnBlueprint.getColumnName()
                ));
            }

            insertStatement.setNextParameter(fieldValue, columnBlueprint.getColumnDataType());
        }

        Object generatedKey = insertStatement.executeInsert();

        if(generatedKey != null)
        {
            primaryKeyValue = generatedKey;
            setEntityInstanceFieldToDatabaseValue(entityInstance, entityBlueprint.getPrimaryKeyColumnName(), generatedKey);
        }
    }

    private Object constructOrphanEntityInstance(Map<String, Object> databaseValues)
    {
        Object entityInstance;

        try
        {
            Constructor constructor = entityBlueprint.getEntityClass().getDeclaredConstructor(new Class[0]);
            constructor.setAccessible(true);
            entityInstance = constructor.newInstance();
        }
        catch (Exception ex)
        {
            throw new PhotonException(
                String.format("Error constructing entity '%s'. Make sure the entity has a parameterless constructor (private is ok).",
                    entityBlueprint.getEntityClassName()),
                ex);
        }

        for(Map.Entry<String, Object> entry : databaseValues.entrySet())
        {
            setEntityInstanceFieldToDatabaseValue(entityInstance, entry.getKey(), entry.getValue());
        }

        return entityInstance;
    }

    private void setEntityInstanceFieldToDatabaseValue(Object entityInstance, String columnName, Object databaseValue)
    {
        FieldBlueprint fieldBlueprint = entityBlueprint.getFieldForColumnName(columnName);

        if(StringUtils.equals(columnName, entityBlueprint.getPrimaryKeyColumnName()))
        {
            primaryKeyValue = databaseValue;
        }
        if(StringUtils.equals(columnName, entityBlueprint.getForeignKeyToParentColumnName()))
        {
            foreignKeyToParentValue = databaseValue;
        }

        if(fieldBlueprint == null)
        {
            return;
        }

        Converter converter = Convert.getConverterIfExists(fieldBlueprint.getFieldClass());

        if(converter == null)
        {
            return;
        }

        Object fieldValue = converter.convert(databaseValue);

        try
        {
            Field field = entityBlueprint.getReflectedField(fieldBlueprint.getFieldName());
            field.set(entityInstance, fieldValue);
        }
        catch (Exception ex)
        {
            throw new PhotonException(String.format("Failed to set value for field '%s' to '%s' on entity '%s'.", fieldBlueprint.getFieldName(), fieldValue, entityBlueprint.getEntityClassName()), ex);
        }
    }

    private Collection createCompatibleCollection(Class<? extends Collection> collectionClass)
    {
        if(List.class.isAssignableFrom(collectionClass))
        {
            return new ArrayList();
        }
        else if(Set.class.isAssignableFrom(collectionClass))
        {
            return new HashSet();
        }

        throw new PhotonException(String.format("Unable to create instance of collection type '%s'.", collectionClass.getName()));
    }
}
