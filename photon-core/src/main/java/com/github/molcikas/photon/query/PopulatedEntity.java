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
        this(entityBlueprint, queryResultRow, true);
    }

    public PopulatedEntity(EntityBlueprint entityBlueprint, PhotonQueryResultRow queryResultRow, boolean columnsFullyQualified)
    {
        this.entityBlueprint = entityBlueprint;
        constructOrphanEntityInstance(queryResultRow, columnsFullyQualified);
    }

    public PopulatedEntity(EntityBlueprint entityBlueprint, T entityInstance)
    {
        this.entityBlueprint = entityBlueprint;
        this.entityInstance = entityInstance;
        this.primaryKeyValue = getInstanceValue(entityBlueprint.getTableBlueprint().getPrimaryKeyColumn());
        this.foreignKeyToParentValue = getInstanceValue(entityBlueprint.getTableBlueprint().getForeignKeyToParentColumn());
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

        throw new PhotonException(
            thrownException,
            "Error getting value for field '%s' on entity '%s'.",
            fieldBlueprint.getFieldName(),
            entityBlueprint.getEntityClassName()
        );
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
        setInstanceFieldToDatabaseValue(entityBlueprint.getTableBlueprint().getPrimaryKeyColumnNameQualified(), primaryKeyValue, true);
    }

    public void setForeignKeyToParentValue(Object foreignKeyToParentValue)
    {
        setInstanceFieldToDatabaseValue(entityBlueprint.getTableBlueprint().getForeignKeyToParentColumnNameQualified(), foreignKeyToParentValue, true);
    }

    public void appendValueToForeignKeyListField(FieldBlueprint fieldBlueprint, Object value)
    {
        if(fieldBlueprint.getFieldType() != FieldType.ForeignKeyList)
        {
            throw new PhotonException("Field '%s' is not a foreign key list field.", fieldBlueprint.getFieldName());
        }

        Object fieldCollection = getInstanceValue(fieldBlueprint);

        Converter converter = Convert.getConverterIfExists(fieldBlueprint.getForeignKeyListBlueprint().getFieldListItemClass());
        Object fieldValue = converter.convert(value);
        ((Collection) fieldCollection).add(fieldValue);
    }

    public void mapEntityInstanceChildren(PopulatedEntityMap populatedEntityMap)
    {
        Object primaryKey = getPrimaryKeyValue();

        for(FieldBlueprint fieldBlueprint : entityBlueprint.getFieldsWithChildEntities())
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
                    throw new PhotonException(
                        ex,
                        "Error setting collection field '%s' on entity '%s'.",
                        fieldBlueprint.getFieldName(),
                        entityBlueprint.getEntityClassName()
                    );
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
                        throw new PhotonException(
                            ex,
                            "Error setting one-to-one field '%s' on entity '%s'.",
                            fieldBlueprint.getFieldName(),
                            entityBlueprint.getEntityClassName()
                        );
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

        if(entityBlueprint.getTableBlueprint().getPrimaryKeyColumn().isAutoIncrementColumn() && primaryKeyValue.equals(0))
        {
            return false;
        }

        boolean canPerformUpdate = true;
        Map<String, Object> values = new HashMap<>();

        for (ColumnBlueprint columnBlueprint : entityBlueprint.getTableBlueprint().getColumns())
        {
            Object fieldValue;
            FieldBlueprint fieldBlueprint = columnBlueprint.getMappedFieldBlueprint();
            Converter customColumnSerializer = null;

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
                    customColumnSerializer = columnBlueprint.getCustomSerializer();
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

            updateStatement.setNextParameter(fieldValue, columnBlueprint.getColumnDataType(), customColumnSerializer);
        }

        if(!canPerformUpdate)
        {
            return false;
        }

        updateStatement.addToBatch();

        return true;
    }

    public void addInsertToBatch(PhotonPreparedStatement insertStatement, TableBlueprint tableBlueprint, PopulatedEntity parentPopulatedEntity)
    {
        addParametersToInsertStatement(insertStatement, tableBlueprint, parentPopulatedEntity);
        insertStatement.addToBatch();
    }

    public void addParametersToInsertStatement(PhotonPreparedStatement insertStatement, TableBlueprint tableBlueprint, PopulatedEntity parentPopulatedEntity)
    {
        Map<String, Object> values = new HashMap<>();

        for (ColumnBlueprint columnBlueprint : tableBlueprint.getColumnsForInsertStatement())
        {
            Object fieldValue;
            FieldBlueprint fieldBlueprint = columnBlueprint.getMappedFieldBlueprint();
            Converter customColumnSerializer = null;

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
                    customColumnSerializer = columnBlueprint.getCustomSerializer();
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
                throw new PhotonException("Cannot save entity '%s' because a value for column '%s' could not be determined.",
                    entityBlueprint.getEntityClassName(),
                    columnBlueprint.getColumnName()
                );
            }

            insertStatement.setNextParameter(fieldValue, columnBlueprint.getColumnDataType(), customColumnSerializer);
        }
    }

    private void constructOrphanEntityInstance(PhotonQueryResultRow queryResultRow, boolean columnsFullyQualified)
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
            setInstanceFieldToDatabaseValue(entry.getKey(), entry.getValue(), columnsFullyQualified);
        }

        for(FieldBlueprint fieldBlueprint : entityBlueprint.getCompoundCustomValueMapperFields())
        {
            Map<String, Object> databaseValues = queryResultRow
                .getValues()
                .stream()
                .filter(v -> entityBlueprint.getFieldsForColumnNameQualified(v.getKey()).contains(fieldBlueprint))
                .collect(Collectors.toMap(v -> v.getKey(), v -> v.getValue()));

            Map<String, Object> valuesToSet = fieldBlueprint.getCompoundEntityFieldValueMapping().setFieldValues(entityInstance, databaseValues);
            setInstanceFieldsToValues(valuesToSet);
        }

        for (FieldBlueprint fieldBlueprint : entityBlueprint.getForeignKeyListFields())
        {
            Collection fieldCollection = createCompatibleCollection(fieldBlueprint.getFieldClass());
            setInstanceFieldToValue(fieldBlueprint, fieldCollection);
        }
    }

    private void setInstanceFieldToDatabaseValue(String columnName, Object databaseValue, boolean isColumnNameQualified)
    {
        FieldBlueprint fieldBlueprint;

        if(isColumnNameQualified)
        {
            fieldBlueprint = entityBlueprint.getFieldForColumnNameQualified(columnName);

            if (StringUtils.equals(columnName, entityBlueprint.getTableBlueprint().getPrimaryKeyColumnNameQualified()))
            {
                primaryKeyValue = databaseValue;
            }
            if (StringUtils.equals(columnName, entityBlueprint.getTableBlueprint().getForeignKeyToParentColumnNameQualified()))
            {
                foreignKeyToParentValue = databaseValue;
            }
        }
        else
        {
            fieldBlueprint = entityBlueprint.getFieldForColumnNameUnqualified(columnName);

            if (StringUtils.equals(columnName, entityBlueprint.getTableBlueprint().getPrimaryKeyColumn().getColumnName()))
            {
                primaryKeyValue = databaseValue;
            }
            if (StringUtils.equals(columnName, entityBlueprint.getTableBlueprint().getPrimaryKeyColumn().getColumnName()))
            {
                foreignKeyToParentValue = databaseValue;
            }
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
        Converter converter = fieldBlueprint.getCustomHydrater();
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
            throw new PhotonException(
                ex,
                "Failed to set value for field '%s' to '%s' on entity '%s'.",
                field.getName(),
                value,
                entityBlueprint.getEntityClassName()
            );
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

        throw new PhotonException("Unable to create instance of collection type '%s'.", collectionClass.getName());
    }
}
