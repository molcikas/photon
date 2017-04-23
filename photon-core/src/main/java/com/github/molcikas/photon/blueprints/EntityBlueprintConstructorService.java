package com.github.molcikas.photon.blueprints;

import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.converters.Converter;

import java.lang.reflect.Field;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class EntityBlueprintConstructorService
{
    public List<FieldBlueprint> getFieldsForEntity(
        Class entityClass,
        List<String> ignoredFields,
        Map<String, EntityFieldValueMapping> customDatabaseColumns,
        Map<String, String> customFieldToColumnMappings,
        Map<String, AggregateEntityBlueprint> childEntities,
        Map<String, ForeignKeyListBlueprint> foreignKeyListBlueprints,
        Map<String, Converter> customToFieldValueConverters)
    {
        final List<String> ignoredFieldsFinal = ignoredFields != null ? ignoredFields : Collections.emptyList();
        final Map<String, EntityFieldValueMapping> customDatabaseColumnsFinal = customDatabaseColumns != null ? customDatabaseColumns : new HashMap<>();
        final Map<String, String> customFieldToColumnMappingsFinal = customFieldToColumnMappings != null ? customFieldToColumnMappings : new HashMap<>();
        final Map<String, AggregateEntityBlueprint> childEntitiesFinal = childEntities != null ? childEntities : new HashMap<>();
        final Map<String, ForeignKeyListBlueprint> foreignKeyListBlueprintsFinal = foreignKeyListBlueprints != null ? foreignKeyListBlueprints : new HashMap<>();
        final Map<String, Converter> customToFieldValueConvertersFinal = customToFieldValueConverters != null ? customToFieldValueConverters : new HashMap<>();

        List<Field> reflectedFields = Arrays.asList(entityClass.getDeclaredFields());

        List<FieldBlueprint> fields = reflectedFields
            .stream()
            .filter(f -> !ignoredFieldsFinal.contains(f.getName()))
            .map(reflectedField -> new FieldBlueprint(
                reflectedField,
                customFieldToColumnMappingsFinal.containsKey(reflectedField.getName()) ?
                    customFieldToColumnMappingsFinal.get(reflectedField.getName()) :
                    reflectedField.getName(),
                childEntitiesFinal.get(reflectedField.getName()),
                foreignKeyListBlueprintsFinal.get(reflectedField.getName()),
                customToFieldValueConvertersFinal.get(reflectedField.getName()),
                null
            ))
            .collect(Collectors.toList());

        fields.addAll(
            customDatabaseColumnsFinal.entrySet()
                .stream()
                .map(e -> new FieldBlueprint(
                    null,
                    e.getKey(),
                    null,
                    null,
                    null,
                    e.getValue()
                ))
                .collect(Collectors.toList())
        );

        return fields;
    }

    public List<ColumnBlueprint> getColumnsForEntityFields(
        List<FieldBlueprint> fields,
        String idFieldName,
        boolean isPrimaryKeyAutoIncrement,
        String foreignKeyToParentColumnName,
        Map<String, Integer> customColumnDataTypes,
        Map<String, Converter> customToDatabaseValueConverters)
    {
        if(customColumnDataTypes == null)
        {
            customColumnDataTypes = new HashMap<>();
        }
        if(customToDatabaseValueConverters == null)
        {
            customToDatabaseValueConverters = new HashMap<>();
        }

        List<FieldBlueprint> fieldsWithColumnMappings = fields
            .stream()
            .filter(f -> f.getFieldType() != FieldType.ForeignKeyList)
            .collect(Collectors.toList());

        List<ColumnBlueprint> columns = new ArrayList<>(fieldsWithColumnMappings.size() + 2); // 2 extra for primary key and foreign key to parent.

        for(FieldBlueprint fieldBlueprint : fieldsWithColumnMappings)
        {
            String fieldName = fieldBlueprint.getFieldName();
            String columnName = fieldBlueprint.getMappedColumnName();
            Integer columnDataType = customColumnDataTypes.containsKey(columnName) ?
                customColumnDataTypes.get(columnName) :
                defaultColumnDataTypeForField(fieldBlueprint.getFieldClass());
            if(columnDataType != null)
            {
                boolean isPrimaryKey = idFieldName != null && StringUtils.equals(fieldName, idFieldName);
                ColumnBlueprint columnBlueprint = new ColumnBlueprint(
                    columnName,
                    columnDataType,
                    isPrimaryKey,
                    isPrimaryKey && isPrimaryKeyAutoIncrement,
                    foreignKeyToParentColumnName != null && StringUtils.equals(fieldName, foreignKeyToParentColumnName),
                    customToDatabaseValueConverters.get(columnName),
                    fieldBlueprint,
                    columns.size()
                );
                columns.add(columnBlueprint);
            }
        }

        return columns;
    }

    // TODO: Move this to a static class
    public static Integer defaultColumnDataTypeForField(Class fieldType)
    {
        if(fieldType == null)
        {
            return null;
        }

        if(fieldType.equals(int.class) || fieldType.equals(Integer.class))
        {
            return Types.INTEGER;
        }

        if(fieldType.equals(long.class) || fieldType.equals(Long.class))
        {
            return Types.BIGINT;
        }

        if(fieldType.equals(float.class) || fieldType.equals(Float.class))
        {
            return Types.FLOAT;
        }

        if(fieldType.equals(double.class) || fieldType.equals(Double.class))
        {
            return Types.DOUBLE;
        }

        if(fieldType.equals(boolean.class) || fieldType.equals(Boolean.class))
        {
            return Types.BOOLEAN;
        }

        if(fieldType.equals(UUID.class))
        {
            return Types.BINARY;
        }

        if(fieldType.equals(String.class))
        {
            return Types.VARCHAR;
        }

        if(fieldType.isEnum())
        {
            return Types.INTEGER;
        }

        if(fieldType.equals(Date.class) ||
            fieldType.equals(Instant.class) ||
            fieldType.equals(ZonedDateTime.class) ||
            fieldType.equals(LocalDate.class) ||
            fieldType.equals(LocalDateTime.class))
        {
            return Types.TIMESTAMP;
        }

        return null;
    }
}
