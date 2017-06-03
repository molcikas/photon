package com.github.molcikas.photon.query;

import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.blueprints.*;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.converters.Convert;
import com.github.molcikas.photon.exceptions.PhotonException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class PopulatedEntity<T>
{
    private final EntityBlueprint entityBlueprint;
    private final AggregateEntityBlueprint aggregateEntityBlueprint;
    private T existingEntityInstance;
    private Map<String, Object> entityValues;
    private Object primaryKeyValue;
    private Object foreignKeyToParentValue;

    public EntityBlueprint getEntityBlueprint()
    {
        return entityBlueprint;
    }

    public Object getPrimaryKeyValue()
    {
        return primaryKeyValue;
    }

    public Object getForeignKeyToParentValue()
    {
        return foreignKeyToParentValue;
    }

    public PopulatedEntity(EntityBlueprint entityBlueprint, PhotonQueryResultRow queryResultRow)
    {
        this.entityBlueprint = entityBlueprint;
        if(AggregateEntityBlueprint.class.isAssignableFrom(entityBlueprint.getClass()))
        {
            this.aggregateEntityBlueprint = (AggregateEntityBlueprint) entityBlueprint;
        }
        else
        {
            this.aggregateEntityBlueprint = null;
        }

        extractValues(queryResultRow);
    }

    public PopulatedEntity(EntityBlueprint entityBlueprint, T existingEntityInstance)
    {
        this.entityBlueprint = entityBlueprint;
        if(AggregateEntityBlueprint.class.isAssignableFrom(entityBlueprint.getClass()))
        {
            this.aggregateEntityBlueprint = (AggregateEntityBlueprint) entityBlueprint;
        }
        else
        {
            this.aggregateEntityBlueprint = null;
        }
        this.existingEntityInstance = existingEntityInstance;

        extractValues(existingEntityInstance);
    }

    public T constructInstance()
    {
        T entityInstance;
        Constructor<T> constructor = entityBlueprint.getEntityConstructor(entityValues);

        try
        {
            entityInstance = constructor.newInstance();
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }

        for (FieldBlueprint fieldBlueprint : entityBlueprint.getFields())
        {
            Object value = getEntityValue(fieldBlueprint);
            if(value != null)
            {
                setInstanceFieldToValue(entityInstance, fieldBlueprint, value);
            }
        }

        // TODO: Move this to extractValues()???
//        if(aggregateEntityBlueprint != null)
//        {
//            for (FieldBlueprint fieldBlueprint : aggregateEntityBlueprint.getForeignKeyListFields())
//            {
//                Collection fieldCollection = createCompatibleCollection(fieldBlueprint.getFieldClass());
//                setInstanceFieldToValue(entityInstance, fieldBlueprint, fieldCollection);
//            }
//        }

        return entityInstance;
    }

    public Object getEntityValue(FieldBlueprint fieldBlueprint)
    {
        if(fieldBlueprint == null)
        {
            return null;
        }

        return entityValues.get(fieldBlueprint.getFieldName());
    }

    public List<PopulatedEntity> getChildPopulatedEntitiesForField(FieldBlueprint fieldBlueprint)
    {
        Collection childEntityInstances;
        Object fieldValue = getEntityValue(fieldBlueprint);

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

        return Arrays
            .stream(childEntityInstances.toArray())
            .map(instance -> new PopulatedEntity(fieldBlueprint.getChildEntityBlueprint(), instance))
            .collect(Collectors.toList());
    }

    public void setPrimaryKeyValue(Object primaryKeyValue)
    {
        checkAndSetColumnValues(entityBlueprint.getPrimaryKeyColumnName(), primaryKeyValue);
        FieldBlueprint fieldBlueprint = entityBlueprint.getPrimaryKeyColumn().getMappedFieldBlueprint();
        entityValues.put(fieldBlueprint.getFieldName(), primaryKeyValue);
        if(existingEntityInstance != null)
        {
            Object value = extractValue(entityBlueprint.getPrimaryKeyColumnName(), primaryKeyValue);
            setInstanceFieldToValue(existingEntityInstance, fieldBlueprint, value);
        }
    }

    public void setForeignKeyToParentValue(Object foreignKeyToParentValue)
    {
        if(!AggregateEntityBlueprint.class.isAssignableFrom(entityBlueprint.getClass()))
        {
            throw new PhotonException("Cannot set foreign key to parent value because the entity is not an aggregate entity.");
        }
        AggregateEntityBlueprint aggregateEntityBlueprint = (AggregateEntityBlueprint) this.entityBlueprint;

        checkAndSetColumnValues(aggregateEntityBlueprint.getForeignKeyToParentColumnName(), foreignKeyToParentValue);
        FieldBlueprint fieldBlueprint = aggregateEntityBlueprint.getForeignKeyToParentColumn().getMappedFieldBlueprint();
        if(fieldBlueprint != null)
        {
            entityValues.put(fieldBlueprint.getFieldName(), foreignKeyToParentValue);
            if(existingEntityInstance != null)
            {
                Object value = extractValue(aggregateEntityBlueprint.getForeignKeyToParentColumnName(), foreignKeyToParentValue);
                setInstanceFieldToValue(existingEntityInstance, fieldBlueprint, value);
            }
        }
    }

    public void appendValueToForeignKeyListField(FieldBlueprint fieldBlueprint, Object value)
    {
        if(fieldBlueprint.getFieldType() != FieldType.ForeignKeyList)
        {
            throw new PhotonException(String.format("Field '%s' is not a foreign key list field.", fieldBlueprint.getFieldName()));
        }

        Object fieldCollection = getEntityValue(fieldBlueprint);

        Converter converter = Convert.getConverterIfExists(fieldBlueprint.getForeignKeyListBlueprint().getFieldListItemClass());
        Object fieldValue = converter.convert(value);
        ((Collection) fieldCollection).add(fieldValue);
    }

    public void mapEntityInstanceChildren(PopulatedEntityMap populatedEntityMap)
    {
        if(aggregateEntityBlueprint == null)
        {
            throw new PhotonException("Cannot map entity instance to children because the entity is not an aggregate entity.");
        }

        Object primaryKey = getPrimaryKeyValue();

        for(FieldBlueprint fieldBlueprint : aggregateEntityBlueprint.getFieldsWithChildEntities())
        {
            Class childEntityClass = fieldBlueprint.getChildEntityBlueprint().getEntityClass();

            if(fieldBlueprint.getFieldType() == FieldType.EntityList)
            {
                Collection collection = createCompatibleCollection(fieldBlueprint.getFieldClass());
                populatedEntityMap.addNextInstancesWithClassAndForeignKeyToParent(collection, childEntityClass, primaryKey);
                entityValues.put(fieldBlueprint.getFieldName(), collection);
            }
            else if(fieldBlueprint.getFieldClass().equals(fieldBlueprint.getChildEntityBlueprint().getEntityClass()))
            {
                Object childInstance = populatedEntityMap.getNextInstanceWithClassAndForeignKeyToParent(childEntityClass, primaryKey);
                if(childInstance != null)
                {
                    entityValues.put(fieldBlueprint.getFieldName(), childInstance);
                }
            }
        }
    }

    public boolean addUpdateToBatch(PhotonPreparedStatement updateStatement, PopulatedEntity parentPopulatedEntity)
    {
        if(primaryKeyValue == null)
        {
            return false;
        }

        if(entityBlueprint.getPrimaryKeyColumn().isAutoIncrementColumn() && primaryKeyValue.equals(0))
        {
            return false;
        }

        boolean canPerformUpdate = true;

        for (ColumnBlueprint columnBlueprint : entityBlueprint.getColumns())
        {
            Object fieldValue;
            FieldBlueprint fieldBlueprint = columnBlueprint.getMappedFieldBlueprint();
            Converter customToDatabaseValueConverter = null;

            if (fieldBlueprint != null)
            {
                fieldValue = getEntityValue(fieldBlueprint);
                customToDatabaseValueConverter = columnBlueprint.getCustomToDatabaseValueConverter();
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

            updateStatement.setNextParameter(fieldValue, columnBlueprint.getColumnDataType(), customToDatabaseValueConverter);
        }

        if(!canPerformUpdate)
        {
            return false;
        }

        updateStatement.addToBatch();

        return true;
    }

    public void addInsertToBatch(PhotonPreparedStatement insertStatement, PopulatedEntity parentPopulatedEntity)
    {
        addParametersToInsertStatement(insertStatement, parentPopulatedEntity);
        insertStatement.addToBatch();
    }

    public void addParametersToInsertStatement(PhotonPreparedStatement insertStatement, PopulatedEntity parentPopulatedEntity)
    {
        for (ColumnBlueprint columnBlueprint : entityBlueprint.getColumnsForInsertStatement())
        {
            Object fieldValue;
            FieldBlueprint fieldBlueprint = columnBlueprint.getMappedFieldBlueprint();
            Converter customToDatabaseValueConverter = null;

            if(fieldBlueprint != null)
            {
                fieldValue = getEntityValue(fieldBlueprint);
                customToDatabaseValueConverter = columnBlueprint.getCustomToDatabaseValueConverter();
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

            insertStatement.setNextParameter(fieldValue, columnBlueprint.getColumnDataType(), customToDatabaseValueConverter);
        }
    }

    private void extractValues(T entityInstance)
    {
        this.entityValues = new HashMap<>();

        for(FieldBlueprint fieldBlueprint : entityBlueprint.getFields())
        {
            Object value = extractValue(fieldBlueprint, entityInstance);

            if(fieldBlueprint.equals(entityBlueprint.getPrimaryKeyColumn().getMappedFieldBlueprint()))
            {
                primaryKeyValue = value;
            }
            if(aggregateEntityBlueprint != null &&
                aggregateEntityBlueprint.getForeignKeyToParentColumn() != null &&
                fieldBlueprint.equals(aggregateEntityBlueprint.getForeignKeyToParentColumn().getMappedFieldBlueprint()))
            {
                foreignKeyToParentValue = value;
            }
            this.entityValues.put(fieldBlueprint.getFieldName(), value);
        }
    }

    private void extractValues(PhotonQueryResultRow photonQueryResultRow)
    {
        this.entityValues = new HashMap<>();

        for(Map.Entry<String, Object> entry : photonQueryResultRow.getValues())
        {
            String columnName = entry.getKey();
            Object databaseValue = entry.getValue();
            checkAndSetColumnValues(columnName, databaseValue);

            FieldBlueprint fieldBlueprint = entityBlueprint.getFieldForColumnName(columnName);
            if(fieldBlueprint == null)
            {
                continue;
            }

            Object value = extractValue(columnName, databaseValue);
            this.entityValues.put(fieldBlueprint.getFieldName(), value);
        }
    }

    private void checkAndSetColumnValues(String columnName, Object databaseValue)
    {
        if(StringUtils.equals(columnName, entityBlueprint.getPrimaryKeyColumnName()))
        {
            primaryKeyValue = databaseValue;
        }
        if(aggregateEntityBlueprint != null && StringUtils.equals(columnName, aggregateEntityBlueprint.getForeignKeyToParentColumnName()))
        {
            foreignKeyToParentValue = databaseValue;
        }
    }

    private Object extractValue(FieldBlueprint fieldBlueprint, T entityInstance)
    {
        if(fieldBlueprint.getEntityFieldValueMapping() != null)
        {
            return fieldBlueprint.getEntityFieldValueMapping().getFieldValue(entityValues);
        }

        Exception thrownException;

        try
        {
            Field field = entityBlueprint.getReflectedField(fieldBlueprint.getFieldName());
            return field.get(entityInstance);
        }
        catch(IllegalArgumentException ex)
        {
            if(ex.getMessage().startsWith("Can not set"))
            {
                // If the field is not in the instance, it's probably because the entity blueprint contains multiple
                // sub-classes with different fields, and this field is not in this sub-class.
                return null;
            }
            thrownException = ex;
        }
        catch(Exception ex)
        {
            thrownException = ex;
        }

        throw new PhotonException(String.format("Error getting value for field '%s' on entity '%s'.", fieldBlueprint.getFieldName(), entityBlueprint.getEntityClassName()), thrownException);
    }

    private Object extractValue(String columnName, Object databaseValue)
    {
        FieldBlueprint fieldBlueprint = entityBlueprint.getFieldForColumnName(columnName);

        if(fieldBlueprint == null)
        {
            return null;
        }

        Converter converter = fieldBlueprint.getCustomToFieldValueConverter();
        if(converter == null && fieldBlueprint.getFieldClass() != null)
        {
            converter = Convert.getConverterIfExists(fieldBlueprint.getFieldClass());
        }

        if(converter == null && fieldBlueprint.getEntityFieldValueMapping() == null)
        {
            // If we don't know how to convert the database value to a field value, then don't try setting the field on the entity instance
            // unless the field has a custom field value mapping (in which case the mapping class will get the database value).
            return null;
        }

        return converter != null ? converter.convert(databaseValue) : databaseValue;
    }

    private void setInstanceFieldToValue(T entityInstance, FieldBlueprint fieldBlueprint, Object value)
    {
        try
        {
            Field field = entityBlueprint.getReflectedField(fieldBlueprint.getFieldName());
            field.set(entityInstance, value);
        }
        catch (Exception ex)
        {
            throw new PhotonException(String.format("Failed to set value for field '%s' to '%s' on entity '%s'.", fieldBlueprint.getFieldName(), value, entityBlueprint.getEntityClassName()), ex);
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
