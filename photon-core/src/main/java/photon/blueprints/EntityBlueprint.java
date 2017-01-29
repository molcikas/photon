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
    private final String orderByColumnName;
    private final SortDirection orderByDirection;
    private final Map<String, FieldBlueprint> fields;
    private List<ColumnBlueprint> columns;

    private ColumnBlueprint primaryKeyColumn;
    private ColumnBlueprint foreignKeyToParentColumn;

    // TODO: Add getter for getEntityClass().getDeclaredField()
    public Class getEntityClass()
    {
        return entityClass;
    }

    public String getOrderByColumnName()
    {
        return orderByColumnName;
    }

    public SortDirection getOrderByDirection()
    {
        return orderByDirection;
    }

    public Map<String, FieldBlueprint> getFields()
    {
        return Collections.unmodifiableMap(fields);
    }

    public List<ColumnBlueprint> getColumns()
    {
        return Collections.unmodifiableList(columns);
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
        this.orderByDirection = orderByDirection;

        List<Field> reflectedFields = Arrays.asList(entityClass.getDeclaredFields());

        fields = new HashMap<>();
        for(Field reflectedField : reflectedFields)
        {
            fields.put(reflectedField.getName(), new FieldBlueprint(
                reflectedField.getName(),
                reflectedField.getType(),
                customFieldToColumnMappings.containsKey(reflectedField.getName()) ?
                    customFieldToColumnMappings.get(reflectedField.getName()) :
                    reflectedField.getName(),
                childEntities.get(reflectedField.getName())
            ));
        }

        columns = new ArrayList<>(fields.size() + 2); // 2 extra for primary key and foreign key to parent.

        for(String entityFieldName : fields.keySet())
        {
            FieldBlueprint fieldBlueprint = fields.get(entityFieldName);
            Integer columnDataType = customColumnDataTypes.containsKey(entityFieldName) ?
                customColumnDataTypes.get(entityFieldName) :
                defaultColumnDataTypeForField(fieldBlueprint.getFieldClass());
            if(columnDataType != null)
            {
                ColumnBlueprint columnBlueprint = new ColumnBlueprint(
                    fieldBlueprint.getColumnName(),
                    columnDataType,
                    entityFieldName.equals(idFieldName),
                    entityFieldName.equals(foreignKeyToParentColumnName),
                    fieldBlueprint,
                    columns.size()
                );
                columns.add(columnBlueprint);
                if(columnBlueprint.isPrimaryKeyColumn())
                {
                    primaryKeyColumn = columnBlueprint;
                }
                if(columnBlueprint.isForeignKeyToParentColumn())
                {
                    foreignKeyToParentColumn = columnBlueprint;
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
                idFieldName.equals(foreignKeyToParentColumnName),
                null,
                columns.size()
            );
            columns.add(primaryKeyColumn);
        }

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
                true,
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
        return foreignKeyToParentColumn != null ? foreignKeyToParentColumn.getColumnName() : null;
    }

    public FieldBlueprint getFieldForColumnName(String columnName)
    {
        return fields
            .values()
            .stream()
            .filter(f -> StringUtils.equals(f.getColumnName(), columnName))
            .findFirst()
            .orElse(null);
    }

    public List<FieldBlueprint> getFieldsWithChildEntities()
    {
        return fields
            .values()
            .stream()
            .filter(f -> f.getChildEntityBlueprint() != null)
            .collect(Collectors.toList());
    }

    public Optional<ColumnBlueprint> getColumn(String columnName)
    {
        return columns
            .stream()
            .filter(c -> c.getColumnName().equals(columnName))
            .findFirst();
    }

    private void normalizeColumnOrder()
    {
        // Sort columns by putting primary key columns at the end, then sort by current column index.
        columns = columns
            .stream()
            .sorted((c1, c2) -> c1.getColumnIndex() - c2.getColumnIndex())
            .sorted((c1, c2) -> c1.isPrimaryKeyColumn() == c2.isPrimaryKeyColumn() ? 0 : c1.isPrimaryKeyColumn() ? 1 : -1)
            .collect(Collectors.toList());

        int i = 0;
        for(ColumnBlueprint columnBlueprint : columns)
        {
            columnBlueprint.moveColumnToIndex(i);
            i++;
        }
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
