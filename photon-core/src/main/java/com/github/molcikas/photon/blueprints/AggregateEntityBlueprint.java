package com.github.molcikas.photon.blueprints;

import com.github.molcikas.photon.options.PhotonOptions;
import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.exceptions.PhotonException;

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
        List<MappedClassBlueprint> mappedClasses,
        EntityClassDiscriminator entityClassDiscriminator,
        String tableName,
        String idFieldName,
        boolean isPrimaryKeyAutoIncrement,
        String foreignKeyToParentColumnName,
        String orderBySql,
        Map<String, ColumnDataType> customColumnDataTypes,
        List<String> ignoredFields,
        Map<String, EntityFieldValueMapping> customDatabaseColumns,
        Map<List<String>, CompoundEntityFieldValueMapping> customCompoundDatabaseColumns,
        Map<String, String> customFieldToColumnMappings,
        Map<String, AggregateEntityBlueprint> childEntities,
        Map<String, ForeignKeyListBlueprint> foreignKeyListBlueprints,
        Map<String, Converter> customFieldHydraters,
        Map<String, Converter> customDatabaseColumnSerializers,
        PhotonOptions photonOptions,
        EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        if(entityClass == null)
        {
            throw new PhotonException("EntityBlueprint class cannot be null.");
        }

        this.deleteOrphansSql = new HashMap<>();
        this.entityClass = entityClass;
        this.entityClassDiscriminator = entityClassDiscriminator;
        this.tableName = determineTableName(tableName, entityClass, photonOptions);
        this.fields = entityBlueprintConstructorService.getFieldsForEntity(entityClass, mappedClasses, ignoredFields, customDatabaseColumns, customCompoundDatabaseColumns, customFieldToColumnMappings, childEntities, foreignKeyListBlueprints, customFieldHydraters);

        if(StringUtils.isBlank(idFieldName))
        {
            idFieldName = determineDefaultIdFieldName();
            if(idFieldName == null)
            {
                throw new PhotonException(String.format("Id not specified for '%s' and unable to determine a default id field.", entityClass.getName()));
            }
        }

        this.columns = entityBlueprintConstructorService.getColumnsForEntityFields(fields, idFieldName, isPrimaryKeyAutoIncrement, foreignKeyToParentColumnName, customColumnDataTypes, customDatabaseColumnSerializers, photonOptions);

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
                customDatabaseColumnSerializers.get(idFieldName),
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
                customDatabaseColumnSerializers.get(foreignKeyToParentColumnName),
                null,
                columns.size()
            );
            columns.add(foreignKeyToParentColumn);
        }

        this.orderBySql = orderBySql;

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

    private String determineDefaultIdFieldName()
    {
        Optional<FieldBlueprint> idField = fields.stream().filter(f -> f.getFieldName().equalsIgnoreCase("id")).findFirst();
        if(idField.isPresent())
        {
            return idField.get().getFieldName();
        }

        String fullIdName = entityClass.getSimpleName().toLowerCase() + "Id";
        Optional<FieldBlueprint> fullIdField = fields.stream().filter(f -> f.getFieldName().equalsIgnoreCase(fullIdName)).findFirst();

        return fullIdField.isPresent() ? fullIdField.get().getFieldName() : null;
    }
}
