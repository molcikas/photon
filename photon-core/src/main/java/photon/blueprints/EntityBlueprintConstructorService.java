package photon.blueprints;

import java.lang.reflect.Field;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

public class EntityBlueprintConstructorService
{
    public List<FieldBlueprint> getFieldsForEntity(
        Class entityClass,
        Map<String, String> customFieldToColumnMappings,
        Map<String, AggregateEntityBlueprint> childEntities,
        Map<String, ForeignKeyListBlueprint> foreignKeyListBlueprints)
    {
        final Map<String, String> customFieldToColumnMappingsFinal = customFieldToColumnMappings != null ? customFieldToColumnMappings : new HashMap<>();
        final Map<String, AggregateEntityBlueprint> childEntitiesFinal = childEntities != null ? childEntities : new HashMap<>();
        final Map<String, ForeignKeyListBlueprint> foreignKeyListBlueprintsFinal = foreignKeyListBlueprints != null ? foreignKeyListBlueprints : new HashMap<>();

        List<Field> reflectedFields = Arrays.asList(entityClass.getDeclaredFields());

        List<FieldBlueprint> fields = reflectedFields
            .stream()
            .map(reflectedField -> new FieldBlueprint(
                reflectedField,
                customFieldToColumnMappingsFinal.containsKey(reflectedField.getName()) ?
                    customFieldToColumnMappingsFinal.get(reflectedField.getName()) :
                    reflectedField.getName(),
                childEntitiesFinal.get(reflectedField.getName()),
                foreignKeyListBlueprintsFinal.get(reflectedField.getName())
            ))
            .collect(Collectors.toList());

        return fields;
    }

    public List<ColumnBlueprint> getColumnsForEntityFields(
        List<FieldBlueprint> fields,
        Map<String, Integer> customColumnDataTypes,
        String idFieldName,
        boolean isPrimaryKeyAutoIncrement,
        String foreignKeyToParentColumnName)
    {
        if(customColumnDataTypes == null)
        {
            customColumnDataTypes = new HashMap<>();
        }

        List<FieldBlueprint> fieldsWithColumnMappings = fields
            .stream()
            .filter(f -> f.getFieldType() != FieldType.ForeignKeyList)
            .collect(Collectors.toList());

        List<ColumnBlueprint> columns = new ArrayList<>(fieldsWithColumnMappings.size() + 2); // 2 extra for primary key and foreign key to parent.

        for(FieldBlueprint fieldBlueprint : fieldsWithColumnMappings)
        {
            String fieldName = fieldBlueprint.getFieldName();
            Integer columnDataType = customColumnDataTypes.containsKey(fieldName) ?
                customColumnDataTypes.get(fieldName) :
                defaultColumnDataTypeForField(fieldBlueprint.getFieldClass());
            if(columnDataType != null)
            {
                boolean isPrimaryKey = idFieldName != null && fieldName.equals(idFieldName);
                ColumnBlueprint columnBlueprint = new ColumnBlueprint(
                    fieldBlueprint.getMappedColumnName(),
                    columnDataType,
                    isPrimaryKey,
                    isPrimaryKey && isPrimaryKeyAutoIncrement,
                    foreignKeyToParentColumnName != null && fieldName.equals(foreignKeyToParentColumnName),
                    fieldBlueprint,
                    columns.size()
                );
                columns.add(columnBlueprint);
            }
        }

        return columns;
    }

    public Integer defaultColumnDataTypeForField(Class fieldType)
    {
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

        if(fieldType.equals(java.lang.String.class))
        {
            return Types.VARCHAR;
        }

        return null;
    }
}
