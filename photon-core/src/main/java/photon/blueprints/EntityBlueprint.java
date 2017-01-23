package photon.blueprints;

import org.apache.commons.lang3.StringUtils;
import photon.exceptions.PhotonException;

import java.lang.reflect.Field;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

public class EntityBlueprint
{
    private final Class entityClass;
    private final String idFieldName;
    private final String orderByColumnName;
    private final SortDirection orderByDirection;
    private final Map<String, EntityFieldBlueprint> fields;
    private final Map<String, ColumnBlueprint> columns;

    private ColumnBlueprint primaryKeyColumn;
    private ColumnBlueprint foreignKeyToParent;

    public Class getEntityClass()
    {
        return entityClass;
    }

    public String getIdFieldName()
    {
        return idFieldName;
    }

    public String getOrderByColumnName()
    {
        return orderByColumnName;
    }

    public SortDirection getOrderByDirection()
    {
        return orderByDirection;
    }

    public Map<String, EntityFieldBlueprint> getFields()
    {
        return Collections.unmodifiableMap(fields);
    }

    public Map<String, ColumnBlueprint> getColumns()
    {
        return Collections.unmodifiableMap(columns);
    }

    public ColumnBlueprint getPrimaryKeyColumn()
    {
        return primaryKeyColumn;
    }

    public EntityBlueprint(
        Class entityClass,
        String idFieldName,
        String foreignKeyToParentColumnName,
        String orderByColumnName,
        SortDirection orderByDirection,
        Map<String, Integer> customColumnDataTypes,
        Map<String, String> customFieldToColumnMappings,
        Map<String, EntityBlueprint> childEntities)
    {
        if(entityClass == null)
        {
            throw new PhotonException("EntityBlueprint class cannot be null.");
        }
        if(StringUtils.isBlank(idFieldName))
        {
            throw new PhotonException("EntityBlueprint id cannot be blank.");
        }
        if(orderByDirection == null)
        {
            orderByDirection = SortDirection.Ascending;
        }

        this.entityClass = entityClass;
        this.idFieldName = idFieldName;
        this.orderByDirection = orderByDirection;

        List<Field> reflectedFields = Arrays.asList(entityClass.getDeclaredFields());

        fields = new HashMap<>();
        for(Field reflectedField : reflectedFields)
        {
            fields.put(reflectedField.getName(), new EntityFieldBlueprint(
                reflectedField.getName(),
                reflectedField.getType(),
                customFieldToColumnMappings.containsKey(reflectedField.getName()) ?
                    customFieldToColumnMappings.get(reflectedField.getName()) :
                    reflectedField.getName(),
                childEntities.get(reflectedField.getName())
            ));
        }

        columns = new HashMap<>();
        for(String entityFieldName : fields.keySet())
        {
            EntityFieldBlueprint entityFieldBlueprint = fields.get(entityFieldName);
            Integer columnDataType = customColumnDataTypes.containsKey(entityFieldName) ?
                customColumnDataTypes.get(entityFieldName) :
                defaultColumnDataTypeForField(entityFieldBlueprint.getFieldClass());
            if(columnDataType != null)
            {
                ColumnBlueprint columnBlueprint = new ColumnBlueprint(
                    entityFieldBlueprint.getColumnName(),
                    columnDataType,
                    entityFieldName.equals(idFieldName),
                    entityFieldBlueprint
                );
                columns.put(entityFieldBlueprint.getColumnName(), columnBlueprint);
                if(columnBlueprint.isPrimaryKeyColumn())
                {
                    primaryKeyColumn = columnBlueprint;
                }
            }
        }

        if(primaryKeyColumn == null)
        {
            if(!customColumnDataTypes.containsKey(idFieldName))
            {
                throw new PhotonException(String.format("The column data type for '%s' must be specified since it is the id and is not in the entityBlueprint.", idFieldName));
            }
            primaryKeyColumn = new ColumnBlueprint(
                idFieldName,
                customColumnDataTypes.get(idFieldName),
                true,
                null
            );
            columns.put(idFieldName, primaryKeyColumn);
        }

        if(StringUtils.isNotBlank(foreignKeyToParentColumnName) && !columns.containsKey(foreignKeyToParentColumnName))
        {
            if(!customColumnDataTypes.containsKey(foreignKeyToParentColumnName))
            {
                throw new PhotonException(String.format("The column data type for '%s' must be specified since it is a foreign key and is not in the entity '%s'.", foreignKeyToParentColumnName, entityClass.getName()));
            }
            columns.put(foreignKeyToParentColumnName, new ColumnBlueprint(
                foreignKeyToParentColumnName,
                customColumnDataTypes.get(foreignKeyToParentColumnName),
                false,
                null
            ));
        }

        if(StringUtils.isBlank(orderByColumnName))
        {
            orderByColumnName = primaryKeyColumn.getColumnName();
        }
        this.orderByColumnName = orderByColumnName;

        if(!columns.containsKey(orderByColumnName))
        {
            throw new PhotonException(String.format("The order by column '%s' is not a column for the entity '%s'.", orderByColumnName, entityClass.getName()));
        }

        foreignKeyToParent = columns.get(foreignKeyToParentColumnName);
    }

    public String getEntityClassName()
    {
        return entityClass.getName();
    }

    public String getTableName()
    {
        return entityClass.getSimpleName().toLowerCase();
    }

    public String getPrimaryKeyColumnName()
    {
        return primaryKeyColumn != null ? primaryKeyColumn.getColumnName() : null;
    }

    public String getForeignKeyToParentColumnName()
    {
        return foreignKeyToParent != null ? foreignKeyToParent.getColumnName() : null;
    }

    public EntityFieldBlueprint getFieldForColumnName(String columnName)
    {
        return fields
            .values()
            .stream()
            .filter(f -> StringUtils.equals(f.getColumnName(), columnName))
            .findFirst()
            .orElse(null);
    }

    public List<EntityFieldBlueprint> getFieldsWithChildEntities()
    {
        return fields
            .values()
            .stream()
            .filter(f -> f.getChildEntityBlueprint() != null)
            .collect(Collectors.toList());
    }

    private Integer defaultColumnDataTypeForField(Class fieldType)
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
