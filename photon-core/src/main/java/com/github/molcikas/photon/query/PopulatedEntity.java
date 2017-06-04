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
    private T entityInstance;
    private Object primaryKeyValue;
    private Object foreignKeyToParentValue;

    public EntityBlueprint getEntityBlueprint()
    {
        return entityBlueprint;
    }

    public T getEntityInstance()
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
        constructOrphanEntityInstance(queryResultRow);
    }

    public PopulatedEntity(EntityBlueprint entityBlueprint, T entityInstance)
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
        this.entityInstance = entityInstance;
        this.primaryKeyValue = getInstanceValue(entityBlueprint.getPrimaryKeyColumn());

        if(aggregateEntityBlueprint != null)
        {
            this.foreignKeyToParentValue = getInstanceValue(aggregateEntityBlueprint.getForeignKeyToParentColumn());
        }
    }

    public Object getInstanceValue(ColumnBlueprint columnBlueprint)
    {
        if(columnBlueprint == null)
        {
            return null;
        }

        return getInstanceValue(columnBlueprint.getMappedFieldBlueprint());
    }

    public Map<String, Object> getDatabaseValuesForCompoundField(FieldBlueprint fieldBlueprint)
    {
        return fieldBlueprint.getCompoundEntityFieldValueMapping().getDatabaseValues(entityInstance);
    }

    public Object getInstanceValue(FieldBlueprint fieldBlueprint)
    {
        if(fieldBlueprint == null)
        {
            return null;
        }

        if(fieldBlueprint.getEntityFieldValueMapping() != null)
        {
            return fieldBlueprint.getEntityFieldValueMapping().getFieldValue(entityInstance);
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

        return Arrays
            .stream(childEntityInstances.toArray())
            .map(instance -> new PopulatedEntity(fieldBlueprint.getChildEntityBlueprint(), instance))
            .collect(Collectors.toList());
    }

    public void setPrimaryKeyValue(Object primaryKeyValue)
    {
        setInstanceFieldToDatabaseValue(aggregateEntityBlueprint.getPrimaryKeyColumnName(), primaryKeyValue);
    }

    public void setForeignKeyToParentValue(Object foreignKeyToParentValue)
    {
        if(!AggregateEntityBlueprint.class.isAssignableFrom(entityBlueprint.getClass()))
        {
            throw new PhotonException("Cannot set foreign key to parent value because the entity is not an aggregate entity.");
        }
        AggregateEntityBlueprint aggregateEntityBlueprint = (AggregateEntityBlueprint) this.entityBlueprint;

        setInstanceFieldToDatabaseValue(aggregateEntityBlueprint.getForeignKeyToParentColumnName(), foreignKeyToParentValue);
    }

    public void appendValueToForeignKeyListField(FieldBlueprint fieldBlueprint, Object value)
    {
        if(fieldBlueprint.getFieldType() != FieldType.ForeignKeyList)
        {
            throw new PhotonException(String.format("Field '%s' is not a foreign key list field.", fieldBlueprint.getFieldName()));
        }

        Object fieldCollection = getInstanceValue(fieldBlueprint);

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
        Map<String, Object> values = new HashMap<>();

        for (ColumnBlueprint columnBlueprint : entityBlueprint.getColumns())
        {
            Object fieldValue;
            FieldBlueprint fieldBlueprint = columnBlueprint.getMappedFieldBlueprint();
            Converter customToDatabaseValueConverter = null;

            if (fieldBlueprint != null)
            {
                if(fieldBlueprint.getFieldType() == FieldType.CompoundCustomValueMapper)
                {
                    if(!values.containsKey(columnBlueprint.getColumnName()))
                    {
                        values.putAll(getDatabaseValuesForCompoundField(fieldBlueprint));
                    }
                    fieldValue = values.get(columnBlueprint.getColumnName());
                }
                else
                {
                    fieldValue = getInstanceValue(fieldBlueprint);
                    customToDatabaseValueConverter = columnBlueprint.getCustomToDatabaseValueConverter();
                }
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
        Map<String, Object> values = new HashMap<>();

        for (ColumnBlueprint columnBlueprint : entityBlueprint.getColumnsForInsertStatement())
        {
            Object fieldValue;
            FieldBlueprint fieldBlueprint = columnBlueprint.getMappedFieldBlueprint();
            Converter customToDatabaseValueConverter = null;

            if(fieldBlueprint != null)
            {
                if(fieldBlueprint.getFieldType() == FieldType.CompoundCustomValueMapper)
                {
                    if(!values.containsKey(columnBlueprint.getColumnName()))
                    {
                        values.putAll(getDatabaseValuesForCompoundField(fieldBlueprint));
                    }
                    fieldValue = values.get(columnBlueprint.getColumnName());
                }
                else
                {
                    fieldValue = getInstanceValue(fieldBlueprint);
                    customToDatabaseValueConverter = columnBlueprint.getCustomToDatabaseValueConverter();
                }
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

    private void constructOrphanEntityInstance(PhotonQueryResultRow queryResultRow)
    {
        Constructor<T> constructor = entityBlueprint.getEntityConstructor(queryResultRow.getValuesMap());

        try
        {
            entityInstance = constructor.newInstance();
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }

        for(Map.Entry<String, Object> entry : queryResultRow.getValues())
        {
            setInstanceFieldToDatabaseValue(entry.getKey(), entry.getValue());
        }

        for(FieldBlueprint fieldBlueprint : entityBlueprint.getCompoundCustomValueMapperFields())
        {
            Map<String, Object> databaseValues = queryResultRow
                .getValues()
                .stream()
                .filter(v -> fieldBlueprint.getMappedColumnNames().contains(v.getKey()))
                .collect(Collectors.toMap(v -> v.getKey(), v -> v.getValue()));

            Map<String, Object> valuesToSet = fieldBlueprint.getCompoundEntityFieldValueMapping().setFieldValues(entityInstance, databaseValues);
            setInstanceFieldsToValues(valuesToSet);
        }

        if(aggregateEntityBlueprint != null)
        {
            for (FieldBlueprint fieldBlueprint : aggregateEntityBlueprint.getForeignKeyListFields())
            {
                Collection fieldCollection = createCompatibleCollection(fieldBlueprint.getFieldClass());
                setInstanceFieldToValue(fieldBlueprint, fieldCollection);
            }
        }
    }

    private void setInstanceFieldToDatabaseValue(String columnName, Object databaseValue)
    {
        FieldBlueprint fieldBlueprint = entityBlueprint.getFieldForColumnName(columnName);

        if(StringUtils.equals(columnName, entityBlueprint.getPrimaryKeyColumnName()))
        {
            primaryKeyValue = databaseValue;
        }
        if(aggregateEntityBlueprint != null && StringUtils.equals(columnName, aggregateEntityBlueprint.getForeignKeyToParentColumnName()))
        {
            foreignKeyToParentValue = databaseValue;
        }

        if(fieldBlueprint == null || fieldBlueprint.getFieldType() == FieldType.CompoundCustomValueMapper)
        {
            return;
        }

        Object fieldValue = convertValue(databaseValue, fieldBlueprint);

        setInstanceFieldToValue(fieldBlueprint, fieldValue);
    }

    private Object convertValue(Object databaseValue, FieldBlueprint fieldBlueprint)
    {
        Converter converter = fieldBlueprint.getCustomToFieldValueConverter();
        if(converter == null && fieldBlueprint.getFieldClass() != null)
        {
            converter = Convert.getConverterIfExists(fieldBlueprint.getFieldClass());
        }

        return converter != null ? converter.convert(databaseValue) : databaseValue;
    }

    private void setInstanceFieldToValue(FieldBlueprint fieldBlueprint, Object value)
    {
        if(fieldBlueprint.getEntityFieldValueMapping() != null)
        {
            Map<String, Object> valuesToSet = fieldBlueprint.getEntityFieldValueMapping().setFieldValue(entityInstance, value);
            setInstanceFieldsToValues(valuesToSet);
        }
        else
        {
            Field field = entityBlueprint.getReflectedField(fieldBlueprint.getFieldName());
            setInstanceFieldToValue(field, value);
        }
    }

    private void setInstanceFieldToValue(Field field, Object value)
    {
        try
        {
            field.set(entityInstance, value);
        }
        catch (Exception ex)
        {
            throw new PhotonException(String.format("Failed to set value for field '%s' to '%s' on entity '%s'.", field.getName(), value, entityBlueprint.getEntityClassName()), ex);
        }
    }

    private void setInstanceFieldsToValues(Map<String, Object> valuesToSet)
    {
        if(valuesToSet == null)
        {
            return;
        }

        for(Map.Entry<String, Object> valueToSet : valuesToSet.entrySet())
        {
            Field field = entityBlueprint.getReflectedField(valueToSet.getKey());
            setInstanceFieldToValue(field, valueToSet.getValue());
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
