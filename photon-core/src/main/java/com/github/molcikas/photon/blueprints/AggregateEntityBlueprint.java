package com.github.molcikas.photon.blueprints;

import com.github.molcikas.photon.options.DefaultTableName;
import com.github.molcikas.photon.options.PhotonOptions;
import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.exceptions.PhotonException;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class AggregateEntityBlueprint extends EntityBlueprint
{
    private ColumnBlueprint foreignKeyToParentColumn;
    private boolean isPrimaryKeyMappedToField;

    private String selectOrphansSql;
    private Map<Integer, String> deleteOrphansSql;

    public ColumnBlueprint getForeignKeyToParentColumn()
    {
        return foreignKeyToParentColumn;
    }

    public boolean isPrimaryKeyMappedToField()
    {
        return isPrimaryKeyMappedToField;
    }

    public String getSelectOrphansSql()
    {
        return selectOrphansSql;
    }

    public String getDeleteOrphanSql(int parentLevelsUpForOrphanIds)
    {
        return deleteOrphansSql.get(parentLevelsUpForOrphanIds);
    }

    AggregateEntityBlueprint(
        Class entityClass,
        String tableName,
        String idFieldName,
        boolean isPrimaryKeyAutoIncrement,
        String foreignKeyToParentColumnName,
        String orderByColumnName,
        SortDirection orderByDirection,
        Map<String, Integer> customColumnDataTypes,
        List<String> ignoredFields,
        Map<String, EntityFieldValueMapping> customDatabaseColumns,
        Map<String, String> customFieldToColumnMappings,
        Map<String, AggregateEntityBlueprint> childEntities,
        Map<String, ForeignKeyListBlueprint> foreignKeyListBlueprints,
        Map<String, Converter> customToFieldValueConverters,
        Map<String, Converter> customToDatabaseValueConverters,
        PhotonOptions photonOptions,
        EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        if(entityClass == null)
        {
            throw new PhotonException("EntityBlueprint class cannot be null.");
        }
        if(StringUtils.isBlank(idFieldName))
        {
            idFieldName = determineDefaultIdFieldName(entityClass);
            if(idFieldName == null)
            {
                throw new PhotonException(String.format("Id not specified for '%s' and unable to determine a default id field.", entityClass.getName()));
            }
        }
        if(orderByDirection == null)
        {
            orderByDirection = SortDirection.Ascending;
        }

        this.deleteOrphansSql = new HashMap<>();
        this.entityClass = entityClass;
        this.tableName = determineTableName(tableName, entityClass, photonOptions);
        this.orderByDirection = orderByDirection;
        this.fields = entityBlueprintConstructorService.getFieldsForEntity(entityClass, ignoredFields, customDatabaseColumns, customFieldToColumnMappings, childEntities, foreignKeyListBlueprints, customToFieldValueConverters);
        this.columns = entityBlueprintConstructorService.getColumnsForEntityFields(fields, idFieldName, isPrimaryKeyAutoIncrement, foreignKeyToParentColumnName, customColumnDataTypes, customToDatabaseValueConverters, photonOptions);

        try
        {
            this.entityConstructor = entityClass.getDeclaredConstructor();
            entityConstructor.setAccessible(true);
        }
        catch (Exception ex)
        {
            throw new PhotonException(
                String.format("Error getting constructor for entity '%s'. Make sure the entity has a parameterless constructor (private is ok).", getEntityClassName()),
                ex
            );
        }

        for(ColumnBlueprint columnBlueprint : columns)
        {
            if(columnBlueprint.isPrimaryKeyColumn())
            {
                primaryKeyColumn = columnBlueprint;
            }
            if(columnBlueprint.isForeignKeyToParentColumn())
            {
                foreignKeyToParentColumn = columnBlueprint;
            }
        }

        if(primaryKeyColumn == null)
        {
            if(!customColumnDataTypes.containsKey(idFieldName))
            {
                throw new PhotonException(String.format("The column data type for '%s' must be specified since it is the id and is not in the entity.", idFieldName));
            }
            primaryKeyColumn = new ColumnBlueprint(
                idFieldName,
                customColumnDataTypes.get(idFieldName),
                true,
                isPrimaryKeyAutoIncrement,
                idFieldName.equals(foreignKeyToParentColumnName),
                customToDatabaseValueConverters.get(idFieldName),
                null,
                columns.size()
            );
            columns.add(primaryKeyColumn);
        }
        this.isPrimaryKeyMappedToField = getPrimaryKeyColumn().getMappedFieldBlueprint() != null;

        if(StringUtils.isNotBlank(foreignKeyToParentColumnName) && foreignKeyToParentColumn == null)
        {
            if(!customColumnDataTypes.containsKey(foreignKeyToParentColumnName))
            {
                throw new PhotonException(String.format("The column data type for '%s' must be specified since it is a foreign key and is not in the entity '%s'.", foreignKeyToParentColumnName, entityClass.getName()));
            }
            foreignKeyToParentColumn = new ColumnBlueprint(
                foreignKeyToParentColumnName,
                customColumnDataTypes.get(foreignKeyToParentColumnName),
                false,
                false,
                true,
                customToDatabaseValueConverters.get(foreignKeyToParentColumnName),
                null,
                columns.size()
            );
            columns.add(foreignKeyToParentColumn);
        }

        if(StringUtils.isBlank(orderByColumnName))
        {
            orderByColumnName = primaryKeyColumn.getColumnName();
        }
        this.orderByColumnName = orderByColumnName;

        if(!getColumn(orderByColumnName).isPresent())
        {
            throw new PhotonException(String.format("The order by column '%s' is not a column for the entity '%s'.", orderByColumnName, entityClass.getName()));
        }

        normalizeColumnOrder();
    }

    public String getForeignKeyToParentColumnName()
    {
        return foreignKeyToParentColumn != null ? foreignKeyToParentColumn.getColumnName() : null;
    }

    public List<FieldBlueprint> getFieldsWithChildEntities()
    {
        return fields
            .stream()
            .filter(f -> f.getFieldType() == FieldType.Entity || f.getFieldType() == FieldType.EntityList)
            .collect(Collectors.toList());
    }

    public List<FieldBlueprint> getForeignKeyListFields()
    {
        return fields
            .stream()
            .filter(f -> f.getFieldType() == FieldType.ForeignKeyList)
            .collect(Collectors.toList());
    }

    public void setSelectOrphansSql(String selectOrphansSql)
    {
        this.selectOrphansSql = selectOrphansSql;
    }

    public void setDeleteOrphansSql(String deleteOrphanSql, int parentLevelsUpForOrphanIds)
    {
        deleteOrphansSql.put(parentLevelsUpForOrphanIds, deleteOrphanSql);
    }

    private String determineTableName(String tableName, Class entityClass, PhotonOptions photonOptions)
    {
        if(StringUtils.isNotBlank(tableName))
        {
            return tableName;
        }

        switch(photonOptions.getDefaultTableName())
        {
            default:
            case ClassName:
                return entityClass.getSimpleName();
            case ClassNameLowerCase:
                return entityClass.getSimpleName().toLowerCase();
        }
    }

    private String determineDefaultIdFieldName(Class entityClass)
    {
        List<Field> reflectedFields = Arrays.asList(entityClass.getDeclaredFields());

        Optional<Field> idField = reflectedFields.stream().filter(f -> f.getName().equalsIgnoreCase("id")).findFirst();
        if(idField.isPresent())
        {
            return idField.get().getName();
        }

        String fullIdName = entityClass.getSimpleName().toLowerCase() + "Id";
        Optional<Field> fullIdField = reflectedFields.stream().filter(f -> f.getName().equalsIgnoreCase(fullIdName)).findFirst();

        return fullIdField.isPresent() ? fullIdField.get().getName() : null;
    }
}
