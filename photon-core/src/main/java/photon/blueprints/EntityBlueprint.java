package photon.blueprints;

import org.apache.commons.lang3.StringUtils;
import photon.exceptions.PhotonException;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EntityBlueprint
{
    protected Class entityClass;
    protected String orderByColumnName;
    protected SortDirection orderByDirection;
    protected List<FieldBlueprint> fields;
    protected List<ColumnBlueprint> columns;

    protected ColumnBlueprint primaryKeyColumn;

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

    protected EntityBlueprint()
    {
    }

    public EntityBlueprint(
        Class entityClass,
        String idFieldName,
        boolean isPrimaryKeyAutoIncrement,
        String orderByColumnName,
        SortDirection orderByDirection,
        Map<String, Integer> customColumnDataTypes,
        Map<String, String> customFieldToColumnMappings,
        EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        if(entityClass == null)
        {
            throw new PhotonException("EntityBlueprint class cannot be null.");
        }
        if(orderByDirection == null)
        {
            orderByDirection = SortDirection.Ascending;
        }

        this.entityClass = entityClass;
        this.orderByDirection = orderByDirection;
        this.fields = entityBlueprintConstructorService.getFieldsForEntity(entityClass, customFieldToColumnMappings, null);
        this.columns = entityBlueprintConstructorService.getColumnsForEntityFields(fields, customColumnDataTypes, idFieldName, isPrimaryKeyAutoIncrement, null);

        for(ColumnBlueprint columnBlueprint : columns)
        {
            if(columnBlueprint.isPrimaryKeyColumn())
            {
                primaryKeyColumn = columnBlueprint;
            }
        }

        if(StringUtils.isBlank(orderByColumnName) && primaryKeyColumn != null)
        {
            orderByColumnName = primaryKeyColumn.getColumnName();
        }
        this.orderByColumnName = orderByColumnName;

        normalizeColumnOrder();
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
        return entityClass.getSimpleName().toLowerCase();
    }

    public String getPrimaryKeyColumnName()
    {
        return primaryKeyColumn != null ? primaryKeyColumn.getColumnName() : null;
    }

    public FieldBlueprint getFieldForColumnName(String columnName)
    {
        return fields
            .stream()
            .filter(f -> StringUtils.equals(f.getColumnName(), columnName))
            .findFirst()
            .orElse(null);
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

    protected void normalizeColumnOrder()
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
}
