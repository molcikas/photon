package com.github.molcikas.photon.query;

import com.github.molcikas.photon.blueprints.entity.ChildCollectionConstructor;
import com.github.molcikas.photon.blueprints.entity.EntityBlueprint;
import com.github.molcikas.photon.blueprints.entity.FieldBlueprint;
import com.github.molcikas.photon.blueprints.entity.FieldType;
import com.github.molcikas.photon.blueprints.table.ColumnBlueprint;
import com.github.molcikas.photon.blueprints.table.TableBlueprint;
import com.github.molcikas.photon.blueprints.table.TableValue;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.converters.Convert;
import com.github.molcikas.photon.exceptions.PhotonException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class PopulatedEntity<T>
{
    @Getter
    private final EntityBlueprint entityBlueprint;

    @Getter
    private T entityInstance;

    @Getter
    private Object primaryKeyValue;

    @Getter
    private Object foreignKeyToParentValue;

    // TODO: Does this need to be a field? Can it be changed back to a method arg?
    private PhotonQueryResultRow photonQueryResultRow;

    @Getter
    @Setter
    private PopulatedEntity<?> parentPopulatedEntity;

    public PopulatedEntity(EntityBlueprint entityBlueprint, PhotonQueryResultRow queryResultRow)
    {
        this(entityBlueprint, queryResultRow, true);
    }

    public PopulatedEntity(EntityBlueprint entityBlueprint, PhotonQueryResultRow photonQueryResultRow, boolean columnsFullyQualified)
    {
        this.entityBlueprint = entityBlueprint;
        this.photonQueryResultRow = photonQueryResultRow;
        constructOrphanEntityInstance(columnsFullyQualified);
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

        return getInstanceValue(columnBlueprint.getMappedFieldBlueprint(), columnBlueprint);
    }

    public TableValue getPrimaryKey()
    {
        return new TableValue(primaryKeyValue);
    }

    public TableValue getForeignKeyToParent()
    {
        return new TableValue(foreignKeyToParentValue);
    }

    public Map<String, Object> getDatabaseValuesForCompoundField(FieldBlueprint fieldBlueprint)
    {
        return fieldBlueprint.getCompoundEntityFieldValueMapping().getDatabaseValues(entityInstance);
    }

    public Object getInstanceValue(FieldBlueprint fieldBlueprint, ColumnBlueprint columnBlueprint)
    {
        if(fieldBlueprint == null)
        {
            return null;
        }

        if(fieldBlueprint.getEntityFieldValueMapping() != null)
        {
            Object fieldValue = fieldBlueprint.getEntityFieldValueMapping().getFieldValue(entityInstance);
            if(columnBlueprint == null)
            {
                return fieldValue;
            }
            return PhotonPreparedStatement.convertValue(new ParameterValue(fieldValue, columnBlueprint));
        }

        Exception thrownException;

        try
        {
            Field field = entityBlueprint.getReflectedField(fieldBlueprint.getFieldName());
            Object fieldValue = field.get(entityInstance);
            if(columnBlueprint == null)
            {
                return fieldValue;
            }
            return PhotonPreparedStatement.convertValue(new ParameterValue(fieldValue, columnBlueprint));
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

    public List<PopulatedEntity<?>> getChildPopulatedEntitiesForField(FieldBlueprint fieldBlueprint)
    {
        Collection childEntityInstances;
        Object fieldValue = getInstanceValue(fieldBlueprint, null);
        EntityBlueprint childEntityBlueprint = fieldBlueprint.getChildEntityBlueprint();
        ChildCollectionConstructor childCollectionConstructor = childEntityBlueprint.getChildCollectionConstructor();

        if(fieldValue == null)
        {
            childEntityInstances = Collections.emptyList();
        }
        else if(childCollectionConstructor != null)
        {
            childEntityInstances = childCollectionConstructor.toCollection(fieldValue, entityInstance);
        }
        else if(Collection.class.isAssignableFrom(fieldValue.getClass()))
        {
            childEntityInstances = (Collection) fieldValue;
        }
        else
        {
            childEntityInstances = Collections.singletonList(fieldValue);
        }

        if(childEntityInstances == null)
        {
            return Collections.emptyList();
        }

        return (List<PopulatedEntity<?>>) childEntityInstances
            .stream()
            .map(instance -> new PopulatedEntity<>(fieldBlueprint.getChildEntityBlueprint(), instance))
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

    public void incrementVersionNumber()
    {
        for(TableBlueprint tableBlueprint : entityBlueprint.getTableBlueprintsForInsertOrUpdate())
        {
            ColumnBlueprint versionColumn = tableBlueprint.getVersionColumn(entityBlueprint);
            if(versionColumn == null)
            {
                continue;
            }
            Number version = (Number) getInstanceValue(versionColumn);
            Long incrementedVersionLong = version != null ? version.longValue() + 1 : 0;
            Converter converter = Convert.getConverter(versionColumn.getMappedFieldBlueprint().getFieldClass());
            Object incrementedVersion = converter.convert(incrementedVersionLong);
            setInstanceFieldToValue(versionColumn.getMappedFieldBlueprint(), incrementedVersion);
        }
    }

    public void appendValueToFlattenedCollectionField(FieldBlueprint fieldBlueprint, Object value)
    {
        if(fieldBlueprint.getFieldType() != FieldType.FlattenedCollection)
        {
            throw new PhotonException("Field '%s' is not a foreign key list field.", fieldBlueprint.getFieldName());
        }

        Object fieldCollection = getInstanceValue(fieldBlueprint, null);

        Converter converter = Convert.getConverterIfExists(fieldBlueprint.getFlattenedCollectionBlueprint().getFieldClass());
        Object fieldValue = converter.convert(value);
        ((Collection) fieldCollection).add(fieldValue);
    }

    public void mapEntityInstanceChildren(PopulatedEntityMap populatedEntityMap)
    {
        for(FieldBlueprint fieldBlueprint : entityBlueprint.getFieldsWithChildEntities())
        {
            ChildCollectionConstructor childCollectionConstructor = fieldBlueprint.getChildEntityBlueprint().getChildCollectionConstructor();

            if(fieldBlueprint.getFieldType() == FieldType.EntityList)
            {
                Collection collection = childCollectionConstructor != null ? new ArrayList() : createCompatibleCollection(fieldBlueprint.getFieldClass());
                populatedEntityMap.setParentAndAddChildrenToCollection(collection, fieldBlueprint.getChildEntityBlueprint(), this);

                if(collection.isEmpty())
                {
                    if(Arrays.stream(entityInstance.getClass().getDeclaredFields()).noneMatch(f -> f.getName().equals(fieldBlueprint.getFieldName())))
                    {
                        // If the instance does not have the field and the collection is empty, just skip it.
                        continue;
                    }
                }

                try
                {
                    Field field = entityBlueprint.getReflectedField(fieldBlueprint.getFieldName());
                    Object fieldValue = collection;
                    if(childCollectionConstructor != null)
                    {
                        fieldValue = childCollectionConstructor.toFieldValue(collection, entityInstance);
                    }
                    field.set(entityInstance, fieldValue);
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
                PopulatedEntity<?> childPopulatedEntity =
                    populatedEntityMap.setParentAndGetNextChild(fieldBlueprint.getChildEntityBlueprint(), this);
                if(childPopulatedEntity != null)
                {
                    try
                    {
                        Field field = entityBlueprint.getReflectedField(fieldBlueprint.getFieldName());
                        field.set(entityInstance, childPopulatedEntity.getEntityInstance());
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

    public GetParameterValuesResult getParameterValuesForUpdate(
        TableBlueprint tableBlueprint,
        PopulatedEntity parentPopulatedEntity,
        Map<String, ParameterValue> trackedValues)
    {
        if(primaryKeyValue == null)
        {
            return GetParameterValuesResult.skipped();
        }

        if(tableBlueprint.getPrimaryKeyColumn().isAutoIncrementColumn() && primaryKeyValue.equals(0))
        {
            return GetParameterValuesResult.skipped();
        }

        boolean isChanged = false;
        Map<String, ParameterValue> parameterValues = new LinkedHashMap<>();
        Map<String, Object> values = new HashMap<>();

        ColumnBlueprint versionColumn = tableBlueprint.getVersionColumn(entityBlueprint);
        Number version = null;
        Long incrementedVersion = null;
        if(versionColumn != null)
        {
            version = (Number) getInstanceValue(versionColumn);
            incrementedVersion = version != null ? version.longValue() + 1 : 0;
        }

        for (ColumnBlueprint columnBlueprint : tableBlueprint.getColumns())
        {
            Object fieldValue;
            FieldBlueprint fieldBlueprint = columnBlueprint.getMappedFieldBlueprint();
            ParameterValue trackedValue =
                trackedValues != null ? trackedValues.get(columnBlueprint.getColumnName()) : null;

            if(columnBlueprint.equals(versionColumn))
            {
                fieldValue = incrementedVersion;
            }
            else if (fieldBlueprint != null)
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
                    fieldValue = getInstanceValue(fieldBlueprint, null);
                }
            }
            else if (columnBlueprint.isForeignKeyToParentColumn())
            {
                fieldValue = parentPopulatedEntity.getPrimaryKeyValue();
            }
            else
            {
                return GetParameterValuesResult.unchanged();
            }

            ParameterValue value = new ParameterValue(fieldValue, columnBlueprint);

            boolean isColumnChanged = trackedValue == null || !trackedValue.equals(value);
            if(isColumnChanged)
            {
                isChanged = true;
            }

            if(isColumnChanged || columnBlueprint.isPrimaryKeyColumn() || columnBlueprint.equals(versionColumn))
            {
                parameterValues.put(columnBlueprint.getColumnName(), value);
            }
        }

        if(trackedValues != null && !isChanged)
        {
            return GetParameterValuesResult.unchanged();
        }

        if(versionColumn != null)
        {
            parameterValues.put(versionColumn.getColumnName() + "_Where", new ParameterValue(version, versionColumn));
        }

        return new GetParameterValuesResult(false, true, parameterValues);
    }

    public List<ParameterValue> getParameterValuesForInsert(
        TableBlueprint tableBlueprint,
        PopulatedEntity parentPopulatedEntity,
        boolean alwaysIncludePrimaryKey)
    {
        List<ParameterValue> parameterValues = new ArrayList<>();
        Map<String, Object> values = new HashMap<>();

        for (ColumnBlueprint columnBlueprint : tableBlueprint.getColumnsForInsertStatement(alwaysIncludePrimaryKey))
        {
            Object fieldValue;
            FieldBlueprint fieldBlueprint = columnBlueprint.getMappedFieldBlueprint();

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
                    fieldValue = getInstanceValue(fieldBlueprint, null);
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

            parameterValues.add(new ParameterValue(fieldValue, columnBlueprint));
        }

        return parameterValues;
    }

    @SneakyThrows
    private void constructOrphanEntityInstance(boolean columnsFullyQualified)
    {
        Constructor<T> constructor = entityBlueprint.getEntityConstructor(photonQueryResultRow.getValuesMap());

        entityInstance = constructor.newInstance();

        for(Map.Entry<String, Object> entry : photonQueryResultRow.getValues())
        {
            setInstanceFieldToDatabaseValue(entry.getKey(), entry.getValue(), columnsFullyQualified);
        }

        for(FieldBlueprint fieldBlueprint : entityBlueprint.getCompoundCustomValueMapperFields())
        {
            Map<String, Object> databaseValues = photonQueryResultRow
                .getValues()
                .stream()
                .filter(v -> entityBlueprint.getFieldsForColumnNameQualified(v.getKey()).contains(fieldBlueprint))
                .collect(Collectors.toMap(v -> v.getKey(), v -> v.getValue()));

            Map<String, Object> valuesToSet = fieldBlueprint.getCompoundEntityFieldValueMapping().setFieldValues(entityInstance, databaseValues);
            setInstanceFieldsToValues(valuesToSet);
        }

        for (FieldBlueprint fieldBlueprint : entityBlueprint.getFlattenedCollectionFields())
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

            if (StringUtils.equals(columnName, entityBlueprint.getTableBlueprint().getPrimaryKeyColumnName()))
            {
                primaryKeyValue = databaseValue;
            }
            if (StringUtils.equals(columnName, entityBlueprint.getTableBlueprint().getPrimaryKeyColumnName()))
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
