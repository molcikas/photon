package com.github.molcikas.photon.blueprints.entity;

import com.github.molcikas.photon.blueprints.table.ColumnBlueprint;
import com.github.molcikas.photon.blueprints.table.TableBlueprint;
import com.github.molcikas.photon.exceptions.PhotonException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class EntityBlueprint
{
    @Getter
    private final Class entityClass;

    private final EntityClassDiscriminator entityClassDiscriminator;
    private final List<FieldBlueprint> fields;

    @Getter
    private final FieldBlueprint versionField;

    @Getter
    private final ChildCollectionConstructor childCollectionConstructor;

    @Getter
    private final TableBlueprint tableBlueprint;

    private final List<TableBlueprint> joinedTableBlueprints;

    private final List<ColumnBlueprint> allColumns;
    private final List<TableBlueprint> tableBlueprintsForInsertOrUpdate;
    private final List<TableBlueprint> tableBlueprintsForDelete;

    EntityBlueprint(
        Class entityClass,
        EntityClassDiscriminator entityClassDiscriminator,
        List<FieldBlueprint> fields,
        TableBlueprint tableBlueprint,
        List<TableBlueprint> joinedTableBlueprints,
        boolean mainTableInsertedFirst,
        ChildCollectionConstructor childCollectionConstructor)
    {
        this.entityClass = entityClass;
        this.entityClassDiscriminator = entityClassDiscriminator;
        this.fields = fields;
        this.tableBlueprint = tableBlueprint;
        this.joinedTableBlueprints = joinedTableBlueprints;
        this.childCollectionConstructor = childCollectionConstructor;

        this.allColumns = Collections.unmodifiableList(ListUtils.union(
            tableBlueprint.getColumns(),
            joinedTableBlueprints.stream().flatMap(j -> j.getColumns().stream()).collect(Collectors.toList())
        ));

        List<TableBlueprint> allTableBlueprints = ListUtils.union(Collections.singletonList(tableBlueprint), joinedTableBlueprints);
        if(mainTableInsertedFirst)
        {
            this.tableBlueprintsForInsertOrUpdate = Collections.unmodifiableList(new ArrayList<>(allTableBlueprints));
            Collections.reverse(allTableBlueprints);
            this.tableBlueprintsForDelete = Collections.unmodifiableList(new ArrayList<>(allTableBlueprints));
        }
        else
        {
            this.tableBlueprintsForDelete = Collections.unmodifiableList(new ArrayList<>(allTableBlueprints));
            Collections.reverse(allTableBlueprints);
            this.tableBlueprintsForInsertOrUpdate = Collections.unmodifiableList(new ArrayList<>(allTableBlueprints));
        }

        this.versionField = fields
            .stream()
            .filter(FieldBlueprint::isVersionField)
            .findFirst()
            .orElse(null);
    }

    public List<FieldBlueprint> getFieldsWithChildEntities()
    {
        return fields
            .stream()
            .filter(f -> f.getFieldType() == FieldType.Entity || f.getFieldType() == FieldType.EntityList)
            .collect(Collectors.toList());
    }

    public List<FieldBlueprint> getFlattenedCollectionFields()
    {
        return fields
            .stream()
            .filter(f -> f.getFieldType() == FieldType.FlattenedCollection)
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
                "Error getting constructor for entity class '%s'. Make sure the class has a parameterless constructor (private is ok). If this is a nested inner class, make sure the class declaration is static.",
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

    public List<TableBlueprint> getJoinedTableBlueprints()
    {
        return Collections.unmodifiableList(joinedTableBlueprints);
    }

    public List<TableBlueprint> getTableBlueprintsForInsertOrUpdate()
    {
        return tableBlueprintsForInsertOrUpdate;
    }

    public List<TableBlueprint> getTableBlueprintsForDelete()
    {
        return tableBlueprintsForDelete;
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

    void setMainTableBlueprintParent(List<TableBlueprint> parentEntityTableBlueprints)
    {
        TableBlueprint parent = parentEntityTableBlueprints
            .stream()
            .filter(t -> StringUtils.equalsIgnoreCase(t.getTableName(), tableBlueprint.getParentTableName()))
            .findFirst()
            .orElseThrow(() -> new PhotonException("Parent table '%s' for '%s' was not found",
                tableBlueprint.getParentTableName(), tableBlueprint.getTableName()));
        tableBlueprint.setParentTableBlueprint(parent);
    }
}
