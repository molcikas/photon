package com.github.molcikas.photon.blueprints;

import com.github.molcikas.photon.exceptions.PhotonException;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class EntityBlueprint
{
    private final Class entityClass;
    private final EntityClassDiscriminator entityClassDiscriminator;
    private final List<FieldBlueprint> fields;
    private final TableBlueprint tableBlueprint;
    private final List<TableBlueprint> joinedTableBlueprints;

    private final List<ColumnBlueprint> allColumns;
    private final List<TableBlueprint> tableBlueprintsForInsert;

    private String selectSql;
    private String selectWhereSql;

    EntityBlueprint(
        Class entityClass,
        EntityClassDiscriminator entityClassDiscriminator,
        List<FieldBlueprint> fields,
        TableBlueprint tableBlueprint,
        List<TableBlueprint> joinedTableBlueprints)
    {
        this.entityClass = entityClass;
        this.entityClassDiscriminator = entityClassDiscriminator;
        this.fields = fields;
        this.tableBlueprint = tableBlueprint;
        this.joinedTableBlueprints = joinedTableBlueprints;

        this.allColumns = Collections.unmodifiableList(ListUtils.union(
            tableBlueprint.getColumns(),
            joinedTableBlueprints.stream().flatMap(j -> j.getColumns().stream()).collect(Collectors.toList())
        ));

        List<TableBlueprint> tableBlueprintsForInsert = ListUtils.union(Collections.singletonList(tableBlueprint), joinedTableBlueprints);
        Collections.reverse(tableBlueprintsForInsert);
        this.tableBlueprintsForInsert = Collections.unmodifiableList(tableBlueprintsForInsert);
    }

    public Class getEntityClass()
    {
        return entityClass;
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
                ex,
                "Error getting constructor for entity class '%s'. Make sure the class has a parameterless constructor (private is ok).",
                classToConstruct
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

    public FieldBlueprint getFieldForColumnNameUnqualified(String columnNameUnqualified)
    {
        return allColumns
            .stream()
            .filter(c -> c.getColumnName().equals(columnNameUnqualified))
            .map(ColumnBlueprint::getMappedFieldBlueprint)
            .findFirst()
            .orElse(null);
    }

    public FieldBlueprint getFieldForColumnNameQualified(String columnNameQualified)
    {
        List<FieldBlueprint> fields = getFieldsForColumnNameQualified(columnNameQualified);
        return !fields.isEmpty() ? fields.get(0) : null;
    }

    public List<FieldBlueprint> getFieldsForColumnNameQualified(String columnNameQualified)
    {
        return allColumns
            .stream()
            .filter(c -> c.getColumnNameQualified().equals(columnNameQualified))
            .map(ColumnBlueprint::getMappedFieldBlueprint)
            .collect(Collectors.toList());
    }

    public List<FieldBlueprint> getCompoundCustomValueMapperFields()
    {
        return fields
            .stream()
            .filter(f -> f.getFieldType() == FieldType.CompoundCustomValueMapper)
            .collect(Collectors.toList());
    }

    public TableBlueprint getTableBlueprint()
    {
        return tableBlueprint;
    }

    public List<TableBlueprint> getJoinedTableBlueprints()
    {
        return Collections.unmodifiableList(joinedTableBlueprints);
    }

    public List<TableBlueprint> getTableBlueprintsForInsert()
    {
        return tableBlueprintsForInsert;
    }

    public String getSelectSql()
    {
        return selectSql;
    }

    public String getSelectWhereSql()
    {
        return selectWhereSql;
    }

    public List<String> getAllColumnNames()
    {
        return allColumns
            .stream()
            .map(ColumnBlueprint::getColumnName)
            .collect(Collectors.toList());
    }

    public List<String> getAllColumnNamesQualified()
    {
        return allColumns
            .stream()
            .map(ColumnBlueprint::getColumnNameQualified)
            .collect(Collectors.toList());
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
}
