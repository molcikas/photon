package photon.query;

import org.apache.commons.lang3.StringUtils;
import photon.converters.Convert;
import photon.converters.Converter;
import photon.exceptions.PhotonException;
import photon.blueprints.EntityBlueprint;
import photon.blueprints.FieldBlueprint;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

public class PopulatedEntity
{
    private final EntityBlueprint entityBlueprint;
    private final Object constructedEntityInstance;
    private Object primaryKeyValue;
    private Object foreignKeyToParentValue;

    public EntityBlueprint getEntityBlueprint()
    {
        return entityBlueprint;
    }

    public Object getEntityInstance()
    {
        return constructedEntityInstance;
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
        this.constructedEntityInstance = constructOrphanEntityInstance(databaseValues);
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
                    Field field = entityBlueprint.getEntityClass().getDeclaredField(fieldBlueprint.getFieldName());
                    field.setAccessible(true);
                    field.set(constructedEntityInstance, collection);
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
                        Field field = entityBlueprint.getEntityClass().getDeclaredField(fieldBlueprint.getFieldName());
                        field.setAccessible(true);
                        field.set(constructedEntityInstance, childInstance);
                    } catch (Exception ex)
                    {
                        throw new PhotonException(String.format("Error setting one-to-one field '%s' on entity '%s'.", fieldBlueprint.getFieldName(), entityBlueprint.getEntityClassName()), ex);
                    }
                }
            }
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
            Field field = entityBlueprint.getEntityClass().getDeclaredField(fieldBlueprint.getFieldName());
            field.setAccessible(true);
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
