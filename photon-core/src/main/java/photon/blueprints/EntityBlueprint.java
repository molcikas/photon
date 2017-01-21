package photon.blueprints;

import org.apache.commons.lang3.StringUtils;
import photon.exceptions.PhotonException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
        this.orderByColumnName = orderByColumnName;
        this.orderByDirection = orderByDirection;

        List<Field> privateFields = Arrays.stream(entityClass.getDeclaredFields())
            .filter(field -> Modifier.isPrivate(field.getModifiers()) || Modifier.isProtected(field.getModifiers()))
            .collect(Collectors.toList());

        fields = new HashMap<>();
        for(Field privateField : privateFields)
        {
            fields.put(privateField.getName(), new EntityFieldBlueprint(
                privateField.getName(),
                privateField.getType(),
                childEntities.get(privateField.getName())
            ));
        }

        columns = new HashMap<>();
        for(String entityFieldName : fields.keySet())
        {
            EntityFieldBlueprint entityFieldBlueprint = fields.get(entityFieldName);
            Integer columnDataType = customColumnDataTypes.containsKey(entityFieldName) ?
                customColumnDataTypes.get(entityFieldName) :
                ColumnDataType.defaultForFieldType(entityFieldBlueprint.fieldClass);
            if(columnDataType != null)
            {
                ColumnBlueprint columnBlueprint = new ColumnBlueprint(
                    entityFieldName,
                    columnDataType,
                    entityFieldName.equals(idFieldName),
                    entityFieldBlueprint
                );
                columns.put(entityFieldName, columnBlueprint);
                if(columnBlueprint.isPrimaryKeyColumn)
                {
                    primaryKeyColumn = columnBlueprint;
                }
            }
        }

        if(!columns.containsKey(idFieldName))
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
                throw new PhotonException(String.format("The column data type for '%s' must be specified since it is a foreign key and is not in the entity '%s'.", foreignKeyToParent, entityClass.getName()));
            }
            columns.put(foreignKeyToParentColumnName, new ColumnBlueprint(
                foreignKeyToParentColumnName,
                customColumnDataTypes.get(foreignKeyToParentColumnName),
                false,
                null
            ));
        }

        if(StringUtils.isNotBlank(orderByColumnName) && !columns.containsKey(orderByColumnName))
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
        return primaryKeyColumn != null ? primaryKeyColumn.columnName : null;
    }

    public String getForeignKeyToParentColumnName()
    {
        return foreignKeyToParent != null ? foreignKeyToParent.columnName : null;
    }

    public EntityFieldBlueprint getFieldForColumnName(String columnName)
    {
        return fields.get(columnName);
    }
}
