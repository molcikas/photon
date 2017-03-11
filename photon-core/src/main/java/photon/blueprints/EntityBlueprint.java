package photon.blueprints;

import org.apache.commons.lang3.StringUtils;
import photon.converters.Converter;
import photon.exceptions.PhotonException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EntityBlueprint
{
    protected Class entityClass;
    protected Constructor entityConstructor;
    protected String orderByColumnName;
    protected SortDirection orderByDirection;
    protected List<FieldBlueprint> fields;
    protected List<ColumnBlueprint> columns;
    protected ColumnBlueprint primaryKeyColumn;

    protected String selectSql;
    protected String updateSql;
    protected String insertSql;
    protected String deleteSql;
    protected String deleteChildrenExceptSql;

    public Class getEntityClass()
    {
        return entityClass;
    }

    public Constructor getEntityConstructor()
    {
        return entityConstructor;
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

    public String getSelectSql()
    {
        return selectSql;
    }

    public String getUpdateSql()
    {
        return updateSql;
    }

    public String getInsertSql()
    {
        return insertSql;
    }

    public String getDeleteSql()
    {
        return deleteSql;
    }

    public String getDeleteChildrenExceptSql()
    {
        return deleteChildrenExceptSql;
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
        this.fields = entityBlueprintConstructorService.getFieldsForEntity(entityClass, null, null, customFieldToColumnMappings, null, null, null, null);
        this.columns = entityBlueprintConstructorService.getColumnsForEntityFields(fields, customColumnDataTypes, idFieldName, isPrimaryKeyAutoIncrement, null);

        try
        {
            this.entityConstructor = entityClass.getDeclaredConstructor();
            entityConstructor.setAccessible(true);
        }
        catch (Exception ex)
        {
            throw new PhotonException(
                String.format("Error getting constructor for entity '%s'. Make sure the entity has a parameterless constructor (private is ok).", getEntityClassName()),
                ex
            );
        }

        primaryKeyColumn = columns.stream().filter(ColumnBlueprint::isPrimaryKeyColumn).findFirst().orElse(null);

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
            .filter(f -> StringUtils.equals(f.getMappedColumnName(), columnName))
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

    public Converter getPrimaryKeyCustomToDatabaseValueConverter()
    {
        ColumnBlueprint primaryKeyColumn = getPrimaryKeyColumn();
        return primaryKeyColumn.getMappedFieldBlueprint() != null ?
            primaryKeyColumn.getMappedFieldBlueprint().getCustomToDatabaseValueConverter() : null;
    }

    public void setSelectSql(String selectSql)
    {
        if(StringUtils.isBlank(selectSql))
        {
            throw new PhotonException("Select SQL cannot be blank.");
        }
        this.selectSql = selectSql;
    }

    public void setUpdateSql(String updateSql)
    {
        if(StringUtils.isBlank(updateSql))
        {
            throw new PhotonException("Update SQL cannot be blank.");
        }
        this.updateSql = updateSql;
    }

    public void setInsertSql(String insertSql)
    {
        if(StringUtils.isBlank(insertSql))
        {
            throw new PhotonException("Insert SQL cannot be blank.");
        }
        this.insertSql = insertSql;
    }

    public void setDeleteSql(String deleteSql)
    {
        if(StringUtils.isBlank(deleteSql))
        {
            throw new PhotonException("Delete SQL cannot be blank.");
        }
        this.deleteSql = deleteSql;
    }

    public void setDeleteChildrenExceptSql(String deleteChildrenExceptSql)
    {
        if(StringUtils.isBlank(deleteChildrenExceptSql))
        {
            throw new PhotonException("Delete children SQL cannot be blank.");
        }
        this.deleteChildrenExceptSql = deleteChildrenExceptSql;
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
