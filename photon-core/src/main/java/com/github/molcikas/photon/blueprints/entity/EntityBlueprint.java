package com.github.molcikas.photon.blueprints.entity;

import com.github.molcikas.photon.blueprints.table.ColumnBlueprint;
import com.github.molcikas.photon.blueprints.table.TableBlueprint;
import com.github.molcikas.photon.exceptions.PhotonException;
import lombok.Getter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
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
    private final Map<String, FieldBlueprint> fields;

    @Getter
    private final List<FieldBlueprint> fieldsWithChildEntities;

    @Getter
    private final List<FieldBlueprint> flattenedCollectionFields;

    @Getter
    private final List<FieldBlueprint> compoundCustomValueMapperFields;

    @Getter
    private final FieldBlueprint versionField;

    @Getter
    private final ChildCollectionConstructor childCollectionConstructor;

    @Getter
    private final TableBlueprint tableBlueprint;

    @Getter
    private final List<TableBlueprint> joinedTableBlueprints;

    private final List<ColumnBlueprint> allColumns;

    @Getter
    private final List<String> allColumnNames;

    @Getter
    private final List<String> allColumnNamesLowerCase;

    @Getter
    private final List<String> allColumnNamesQualified;

    @Getter
    private final List<String> allColumnNamesQualifiedLowerCase;

    @Getter
    private final List<TableBlueprint> tableBlueprintsForInsertOrUpdate;

    @Getter
    private final List<TableBlueprint> tableBlueprintsForDelete;

    private final ListValuedMap<String, FieldBlueprint> fieldsForColumnNameQualified;

    private final Map<String, FieldBlueprint> fieldForColumnNameQualified;

    private final Map<String, FieldBlueprint> fieldForColumnNameUnqualified;

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
        this.tableBlueprint = tableBlueprint;
        this.joinedTableBlueprints = Collections.unmodifiableList(joinedTableBlueprints);
        this.childCollectionConstructor = childCollectionConstructor;

        this.fields = fields
            .stream()
            .collect(Collectors.toMap(FieldBlueprint::getFieldName, f -> f));

        this.fieldsWithChildEntities = Collections.unmodifiableList(fields
            .stream()
            .filter(f -> f.getFieldType() == FieldType.Entity || f.getFieldType() == FieldType.EntityList)
            .collect(Collectors.toList()));

        this.flattenedCollectionFields = Collections.unmodifiableList(fields
            .stream()
            .filter(f -> f.getFieldType() == FieldType.FlattenedCollection)
            .collect(Collectors.toList()));

        this.compoundCustomValueMapperFields = Collections.unmodifiableList(fields
            .stream()
            .filter(f -> f.getFieldType() == FieldType.CompoundCustomValueMapper)
            .collect(Collectors.toList()));

        this.allColumns = Collections.unmodifiableList(ListUtils.union(
            tableBlueprint.getColumns(),
            joinedTableBlueprints.stream().flatMap(j -> j.getColumns().stream()).collect(Collectors.toList())
        ));

        this.allColumnNames = Collections.unmodifiableList(allColumns
            .stream()
            .map(ColumnBlueprint::getColumnName)
            .collect(Collectors.toList()));

        this.allColumnNamesLowerCase = Collections.unmodifiableList(allColumnNames
            .stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList()));

        this.allColumnNamesQualified = Collections.unmodifiableList(allColumns
            .stream()
            .map(ColumnBlueprint::getColumnNameQualified)
            .collect(Collectors.toList()));

        this.allColumnNamesQualifiedLowerCase = allColumnNamesQualified
            .stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList());

        this.fieldsForColumnNameQualified = new ArrayListValuedHashMap<>();
        this.fieldForColumnNameQualified = new HashMap<>();
        this.fieldForColumnNameUnqualified = new HashMap<>();
        for(ColumnBlueprint columnBlueprint : allColumns)
        {
            if(columnBlueprint.getMappedFieldBlueprint() != null)
            {
                fieldsForColumnNameQualified.put(
                    columnBlueprint.getColumnNameQualified(),
                    columnBlueprint.getMappedFieldBlueprint());
                fieldForColumnNameQualified.put(
                    columnBlueprint.getColumnNameQualified(),
                    columnBlueprint.getMappedFieldBlueprint());
                fieldForColumnNameUnqualified.put(
                    columnBlueprint.getColumnName(),
                    columnBlueprint.getMappedFieldBlueprint()
                );
            }
        }

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
        FieldBlueprint fieldBlueprint = fields.get(fieldName);
        if(fieldBlueprint == null)
        {
            return null;
        }
        return fieldBlueprint.getReflectedField();
    }

    public String getEntityClassName()
    {
        return entityClass.getName();
    }

    public FieldBlueprint getFieldForColumnNameUnqualified(String columnNameUnqualified)
    {
        return fieldForColumnNameUnqualified.get(columnNameUnqualified);
    }

    public FieldBlueprint getFieldForColumnNameQualified(String columnNameQualified)
    {
        return fieldForColumnNameQualified.get(columnNameQualified);
    }

    public List<FieldBlueprint> getFieldsForColumnNameQualified(String columnNameQualified)
    {
        List<FieldBlueprint> fieldBlueprints = fieldsForColumnNameQualified.get(columnNameQualified);
        if(fieldBlueprints == null)
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(fieldBlueprints);
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
