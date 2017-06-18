package com.github.molcikas.photon.blueprints;

import com.github.molcikas.photon.options.PhotonOptions;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.converters.Converter;

import java.lang.reflect.Field;
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
        List<MappedClassBlueprint> mappedClasses,
        List<String> ignoredFields,
        Map<String, EntityFieldValueMapping> customDatabaseColumns,
        Map<List<String>, CompoundEntityFieldValueMapping> customCompoundDatabaseColumns,
        Map<String, String> customFieldToColumnMappings,
        Map<String, AggregateEntityBlueprint> childEntities,
        Map<String, ForeignKeyListBlueprint> foreignKeyListBlueprints,
        Map<String, Converter> customFieldHydraters)
    {
        final List<MappedClassBlueprint> mappedClassesFinal = mappedClasses != null ? mappedClasses : Collections.emptyList();
        final List<String> ignoredFieldsFinal = ignoredFields != null ? ignoredFields : Collections.emptyList();
        final Map<String, EntityFieldValueMapping> customDatabaseColumnsFinal = customDatabaseColumns != null ? customDatabaseColumns : new HashMap<>();
        final Map<String, String> customFieldToColumnMappingsFinal = customFieldToColumnMappings != null ? customFieldToColumnMappings : new HashMap<>();
        final Map<List<String>, CompoundEntityFieldValueMapping> customCompoundDatabaseColumnsFinal = customCompoundDatabaseColumns != null ? customCompoundDatabaseColumns : new HashMap<>();
        final Map<String, AggregateEntityBlueprint> childEntitiesFinal = childEntities != null ? childEntities : new HashMap<>();
        final Map<String, ForeignKeyListBlueprint> foreignKeyListBlueprintsFinal = foreignKeyListBlueprints != null ? foreignKeyListBlueprints : new HashMap<>();
        final Map<String, Converter> customFieldHydratersFinal = customFieldHydraters != null ? customFieldHydraters : new HashMap<>();

        MultiValuedMap<Class, Field> reflectedFieldsMap = new HashSetValuedHashMap<>();
        reflectedFieldsMap.putAll(entityClass, Arrays.asList(entityClass.getDeclaredFields()));

        for(MappedClassBlueprint mappedClassBlueprint : mappedClassesFinal)
        {
            List<Field> reflectedFieldsForMappedClass = mappedClassBlueprint.getIncludedFields();
            for(Field field : reflectedFieldsForMappedClass)
            {
                if(reflectedFieldsMap.values().stream().noneMatch(c -> c.getName().equals(field.getName())))
                {
                    reflectedFieldsMap.put(mappedClassBlueprint.getMappedClass(), field);
                }
            }
        }

        List<FieldBlueprint> fields = reflectedFieldsMap
            .entries()
            .stream()
            .filter(entry -> !ignoredFieldsFinal.contains(entry.getValue().getName()))
            .map(entry -> new FieldBlueprint(
                entry.getValue(),
                Collections.singletonList(customFieldToColumnMappingsFinal.containsKey(entry.getValue().getName()) ?
                    customFieldToColumnMappingsFinal.get(entry.getValue().getName()) :
                    entry.getValue().getName()),
                childEntitiesFinal.get(entry.getValue().getName()),
                foreignKeyListBlueprintsFinal.get(entry.getValue().getName()),
                customFieldHydratersFinal.get(entry.getValue().getName()),
                null,
                null
            ))
            .collect(Collectors.toList());

        fields.addAll(
            customDatabaseColumnsFinal.entrySet()
                .stream()
                .map(e -> new FieldBlueprint(
                    null,
                    Collections.singletonList(e.getKey()),
                    null,
                    null,
                    null,
                    e.getValue(),
                    null
                ))
                .collect(Collectors.toList())
        );

        fields.addAll(
            customCompoundDatabaseColumnsFinal.entrySet()
                .stream()
                .map(e -> new FieldBlueprint(
                    null,
                    e.getKey(),
                    null,
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
        Map<String, ColumnDataType> customColumnDataTypes,
        Map<String, Converter> customDatabaseColumnSerializers,
        PhotonOptions photonOptions)
    {
        if(customColumnDataTypes == null)
        {
            customColumnDataTypes = new HashMap<>();
        }
        if(customDatabaseColumnSerializers == null)
        {
            customDatabaseColumnSerializers = new HashMap<>();
        }

        List<FieldBlueprint> fieldsWithColumnMappings = fields
            .stream()
            .filter(f -> f.getFieldType() != FieldType.ForeignKeyList)
            .collect(Collectors.toList());

        List<ColumnBlueprint> columns = new ArrayList<>(fieldsWithColumnMappings.size() + 2); // 2 extra for primary key and foreign key to parent.

        for(FieldBlueprint fieldBlueprint : fieldsWithColumnMappings)
        {
            String fieldName = fieldBlueprint.getFieldName();
            List<String> columnNames = fieldBlueprint.getMappedColumnNames();
            for(String columnName : columnNames)
            {
                DefaultColumnDataTypeResult columnDataTypeResult = customColumnDataTypes.containsKey(columnName) ?
                    new DefaultColumnDataTypeResult(customColumnDataTypes.get(columnName)) :
                    defaultColumnDataTypeForField(fieldBlueprint.getFieldClass(), photonOptions);
                if (columnDataTypeResult.foundDataType)
                {
                    boolean isPrimaryKey = idFieldName != null && StringUtils.equals(fieldName, idFieldName);
                    ColumnBlueprint columnBlueprint = new ColumnBlueprint(
                        columnName,
                        columnDataTypeResult.dataType,
                        isPrimaryKey,
                        isPrimaryKey && isPrimaryKeyAutoIncrement,
                        foreignKeyToParentColumnName != null && StringUtils.equals(fieldName,
                            foreignKeyToParentColumnName),
                        customDatabaseColumnSerializers.get(columnName),
                        fieldBlueprint,
                        columns.size()
                    );
                    columns.add(columnBlueprint);
                }
            }
        }

        return columns;
    }

    // TODO: Move this to a static class
    public static DefaultColumnDataTypeResult defaultColumnDataTypeForField(Class fieldType, PhotonOptions photonOptions)
    {
        if(fieldType == null)
        {
            return DefaultColumnDataTypeResult.notFound();
        }

        if(fieldType.equals(int.class) || fieldType.equals(Integer.class))
        {
            return new DefaultColumnDataTypeResult(ColumnDataType.INTEGER);
        }

        if(fieldType.equals(long.class) || fieldType.equals(Long.class))
        {
            return new DefaultColumnDataTypeResult(ColumnDataType.BIGINT);
        }

        if(fieldType.equals(float.class) || fieldType.equals(Float.class))
        {
            return new DefaultColumnDataTypeResult(ColumnDataType.FLOAT);
        }

        if(fieldType.equals(double.class) || fieldType.equals(Double.class))
        {
            return new DefaultColumnDataTypeResult(ColumnDataType.DOUBLE);
        }

        if(fieldType.equals(boolean.class) || fieldType.equals(Boolean.class))
        {
            return new DefaultColumnDataTypeResult(ColumnDataType.BOOLEAN);
        }

        if(fieldType.equals(UUID.class))
        {
            return new DefaultColumnDataTypeResult(photonOptions != null ? photonOptions.getDefaultUuidDataType() : PhotonOptions.DEFAULT_UUID_DATA_TYPE);
        }

        if(fieldType.equals(String.class))
        {
            return new DefaultColumnDataTypeResult(ColumnDataType.VARCHAR);
        }

        if(fieldType.isEnum())
        {
            return new DefaultColumnDataTypeResult(ColumnDataType.INTEGER);
        }

        if(fieldType.equals(Date.class) ||
            fieldType.equals(Instant.class) ||
            fieldType.equals(ZonedDateTime.class) ||
            fieldType.equals(LocalDate.class) ||
            fieldType.equals(LocalDateTime.class))
        {
            return new DefaultColumnDataTypeResult(ColumnDataType.TIMESTAMP);
        }

        return DefaultColumnDataTypeResult.notFound();
    }
}
