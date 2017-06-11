package com.github.molcikas.photon.blueprints;

import com.github.molcikas.photon.options.PhotonOptions;
import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.exceptions.PhotonException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class EntityBlueprint
{
    protected Class entityClass;
    protected EntityClassDiscriminator entityClassDiscriminator;
    protected String tableName;
    protected String orderBySql;
    protected List<FieldBlueprint> fields;
    protected List<ColumnBlueprint> columns;
    protected ColumnBlueprint primaryKeyColumn;

    protected String selectSql;
    protected String selectWhereSql;
    protected String updateSql;
    protected String insertSql;
    protected String deleteSql;
    protected String deleteChildrenExceptSql;

    public Class getEntityClass()
    {
        return entityClass;
    }

    public String getOrderBySql()
    {
        return orderBySql;
    }

    public List<FieldBlueprint> getFields()
    {
        return Collections.unmodifiableList(fields);
    }

    public List<ColumnBlueprint> getColumns()
    {
        return Collections.unmodifiableList(columns);
    }

    public ColumnBlueprint getPrimaryKeyColumn()
    {
        return primaryKeyColumn;
    }

    public String getSelectSql()
    {
        return selectSql;
    }

    public String getSelectWhereSql()
    {
        return selectWhereSql;
    }

    public String getUpdateSql()
    {
        return updateSql;
    }

    public String getInsertSql()
    {
        return insertSql;
    }

    public String getDeleteSql()
    {
        return deleteSql;
    }

    public String getDeleteChildrenExceptSql()
    {
        return deleteChildrenExceptSql;
    }

    protected EntityBlueprint()
    {
    }

    public EntityBlueprint(
        Class entityClass,
        List<MappedClassBlueprint> mappedClasses,
        EntityClassDiscriminator entityClassDiscriminator,
        String tableName,
        String idFieldName,
        boolean isPrimaryKeyAutoIncrement,
        String orderBySql,
        Map<String, ColumnDataType> customColumnDataTypes,
        List<String> ignoredFields,
        Map<String, EntityFieldValueMapping> customDatabaseColumns,
        Map<List<String>, CompoundEntityFieldValueMapping> customCompoundDatabaseColumns,
        Map<String, String> customFieldToColumnMappings,
        Map<String, Converter> customToFieldValueConverters,
        Map<String, Converter> customToDatabaseValueConverters,
        PhotonOptions photonOptions,
        EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        if(entityClass == null)
        {
            throw new PhotonException("EntityBlueprint class cannot be null.");
        }

        this.entityClass = entityClass;
        this.entityClassDiscriminator = entityClassDiscriminator;
        this.tableName = StringUtils.isBlank(tableName) ? entityClass.getSimpleName().toLowerCase() : tableName;
        this.fields = entityBlueprintConstructorService.getFieldsForEntity(entityClass, mappedClasses, ignoredFields, customDatabaseColumns, customCompoundDatabaseColumns, customFieldToColumnMappings, null, null, customToFieldValueConverters);
        this.columns = entityBlueprintConstructorService.getColumnsForEntityFields(fields, idFieldName, isPrimaryKeyAutoIncrement, null, customColumnDataTypes, customToDatabaseValueConverters, photonOptions);

        primaryKeyColumn = columns.stream().filter(ColumnBlueprint::isPrimaryKeyColumn).findFirst().orElse(null);

        if(StringUtils.isBlank(orderBySql) && primaryKeyColumn != null)
        {
            orderBySql = primaryKeyColumn.getColumnName();
        }
        this.orderBySql = orderBySql;

        normalizeColumnOrder();
    }

    public Constructor getEntityConstructor(Map<String, Object> entityValues)
    {
        Class classToConstruct = null;

        if(entityClassDiscriminator != null)
        {
            classToConstruct = entityClassDiscriminator.getClassForEntity(entityValues);
        }
        if(classToConstruct == null)
        {
            classToConstruct = entityClass;
        }

        try
        {
            Constructor constructor = classToConstruct.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        }
        catch (Exception ex)
        {
            throw new PhotonException(
                String.format("Error getting constructor for entity class '%s'. Make sure the class has a parameterless constructor (private is ok).", classToConstruct),
                ex
            );
        }
    }

    public Field getReflectedField(String fieldName)
    {
        return fields
            .stream()
            .filter(f -> f.getFieldName().equals(fieldName))
            .map(FieldBlueprint::getReflectedField)
            .findFirst()
            .orElse(null);
    }

    public String getEntityClassName()
    {
        return entityClass.getName();
    }

    public String getTableName()
    {
        return tableName;
    }

    public String getPrimaryKeyColumnName()
    {
        return primaryKeyColumn != null ? primaryKeyColumn.getColumnName() : null;
    }

    public FieldBlueprint getFieldForColumnName(String columnName)
    {
        return fields
            .stream()
            .filter(f -> StringUtils.equals(f.getMappedColumnName(), columnName))
            .findFirst()
            .orElse(null);
    }

    public List<FieldBlueprint> getCompoundCustomValueMapperFields()
    {
        return fields
            .stream()
            .filter(f -> f.getFieldType() == FieldType.CompoundCustomValueMapper)
            .collect(Collectors.toList());
    }

    public Optional<ColumnBlueprint> getColumn(String columnName)
    {
        return columns
            .stream()
            .filter(c -> c.getColumnName().equals(columnName))
            .findFirst();
    }

    public List<ColumnBlueprint> getColumnsForInsertStatement()
    {
        return columns
            .stream()
            .filter(c -> !c.isPrimaryKeyColumn() || (c.isPrimaryKeyColumn() && !c.isAutoIncrementColumn()))
            .collect(Collectors.toList());
    }

    public List<String> getColumnNames()
    {
        return columns
            .stream()
            .map(ColumnBlueprint::getColumnName)
            .collect(Collectors.toList());
    }

    public Converter getPrimaryKeyCustomToDatabaseValueConverter()
    {
        ColumnBlueprint primaryKeyColumn = getPrimaryKeyColumn();
        return primaryKeyColumn.getCustomToDatabaseValueConverter();
    }

    public void setSelectSql(String selectSql)
    {
        if(StringUtils.isBlank(selectSql))
        {
            throw new PhotonException("Select SQL cannot be blank.");
        }
        this.selectSql = selectSql;
    }

    public void setSelectWhereSql(String selectWhereSql)
    {
        if(StringUtils.isBlank(selectWhereSql))
        {
            throw new PhotonException("Select Where SQL cannot be blank.");
        }
        this.selectWhereSql = selectWhereSql;
    }

    public void setUpdateSql(String updateSql)
    {
        if(StringUtils.isBlank(updateSql))
        {
            throw new PhotonException("Update SQL cannot be blank.");
        }
        this.updateSql = updateSql;
    }

    public void setInsertSql(String insertSql)
    {
        if(StringUtils.isBlank(insertSql))
        {
            throw new PhotonException("Insert SQL cannot be blank.");
        }
        this.insertSql = insertSql;
    }

    public void setDeleteSql(String deleteSql)
    {
        if(StringUtils.isBlank(deleteSql))
        {
            throw new PhotonException("Delete SQL cannot be blank.");
        }
        this.deleteSql = deleteSql;
    }

    public void setDeleteChildrenExceptSql(String deleteChildrenExceptSql)
    {
        if(StringUtils.isBlank(deleteChildrenExceptSql))
        {
            throw new PhotonException("Delete children SQL cannot be blank.");
        }
        this.deleteChildrenExceptSql = deleteChildrenExceptSql;
    }

    protected void normalizeColumnOrder()
    {
        // Sort columns by putting primary key columns at the end, then sort by current column index.
        columns = columns
            .stream()
            .sorted(Comparator.comparingInt(ColumnBlueprint::getColumnIndex))
            .sorted((c1, c2) -> c1.isPrimaryKeyColumn() == c2.isPrimaryKeyColumn() ? 0 : c1.isPrimaryKeyColumn() ? 1 : -1)
            .collect(Collectors.toList());

        int i = 0;
        for(ColumnBlueprint columnBlueprint : columns)
        {
            columnBlueprint.moveColumnToIndex(i);
            i++;
        }
    }
}
