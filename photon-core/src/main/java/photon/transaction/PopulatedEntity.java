package photon.transaction;

import org.apache.commons.lang3.StringUtils;
import photon.converters.Convert;
import photon.converters.Converter;
import photon.exceptions.PhotonException;
import photon.blueprints.EntityBlueprint;
import photon.blueprints.EntityFieldBlueprint;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

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

    private Object constructOrphanEntityInstance(Map<String, Object> databaseValues)
    {
        Object entityInstance;

        try
        {
            Constructor constructor = entityBlueprint.getEntityClass().getConstructor();
            constructor.setAccessible(true);
            entityInstance = constructor.newInstance();
        }
        catch (Exception ex)
        {
            throw new PhotonException(String.format("Error constructing entity '%s'.", entityBlueprint.getEntityClassName()), ex);
        }

        for(Map.Entry<String, Object> entry : databaseValues.entrySet())
        {
            setDatabaseValueOnEntityInstance(entityInstance, entry.getKey(), entry.getValue());
        }

        return entityInstance;
    }

    private void setDatabaseValueOnEntityInstance(Object entityInstance, String columnName, Object databaseValue)
    {
        EntityFieldBlueprint entityFieldBlueprint = entityBlueprint.getFieldForColumnName(columnName);

        if(StringUtils.equals(columnName, entityBlueprint.getPrimaryKeyColumnName()))
        {
            primaryKeyValue = databaseValue;
        }
        else if(StringUtils.equals(columnName, entityBlueprint.getForeignKeyToParentColumnName()))
        {
            foreignKeyToParentValue = databaseValue;
        }

        if(entityFieldBlueprint == null)
        {
            return;
        }

        Converter converter = Convert.getConverterIfExists(entityFieldBlueprint.fieldClass);

        if(converter == null)
        {
            return;
        }

        Object fieldValue = converter.convert(databaseValue);

        try
        {
            Field field = entityBlueprint.getEntityClass().getDeclaredField(entityFieldBlueprint.fieldName);
            field.setAccessible(true);
            field.set(entityInstance, fieldValue);
        }
        catch (Exception ex)
        {
            throw new PhotonException(String.format("Failed to set value for field '%s' to '%s' on entity '%s'.", entityFieldBlueprint.fieldName, fieldValue, entityBlueprint.getEntityClassName()), ex);
        }
    }
}
