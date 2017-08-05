package com.github.molcikas.photon.blueprints;

import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.exceptions.PhotonException;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class TableBlueprint
{
    private List<ColumnBlueprint> columns;
    private ColumnBlueprint primaryKeyColumn;
    private ColumnBlueprint foreignKeyToParentColumn;
    private boolean isPrimaryKeyMappedToField;

    private String tableName;
    private String orderBySql;
    private TableBlueprint parentTableBlueprint;

    private String selectSql;
    private String selectWhereSql;
    private String updateSql;
    private String insertSql;
    private String deleteSql;
    private String deleteChildrenExceptSql;
    private String selectOrphansSql;
    private Map<Integer, String> deleteOrphansSql;

    TableBlueprint(List<ColumnBlueprint> columns,
                   ColumnBlueprint primaryKeyColumn,
                   ColumnBlueprint foreignKeyToParentColumn,
                   boolean isPrimaryKeyMappedToField,
                   String tableName,
                   String orderBySql)
    {
        this.columns = columns;
        this.primaryKeyColumn = primaryKeyColumn;
        this.foreignKeyToParentColumn = foreignKeyToParentColumn;
        this.isPrimaryKeyMappedToField = isPrimaryKeyMappedToField;
        this.tableName = tableName;
        this.orderBySql = orderBySql;
        this.deleteOrphansSql = new HashMap<>();
    }

    public List<ColumnBlueprint> getColumns()
    {
        return Collections.unmodifiableList(columns);
    }

    public ColumnBlueprint getPrimaryKeyColumn()
    {
        return primaryKeyColumn;
    }

    public ColumnBlueprint getForeignKeyToParentColumn()
    {
        return foreignKeyToParentColumn;
    }

    public String getForeignKeyToParentColumnName()
    {
        return foreignKeyToParentColumn != null ? foreignKeyToParentColumn.getColumnName() : null;
    }

    public boolean isPrimaryKeyMappedToField()
    {
        return isPrimaryKeyMappedToField;
    }

    public String getTableName()
    {
        return tableName;
    }

    public String getOrderBySql()
    {
        return orderBySql;
    }

    public String getPrimaryKeyColumnName()
    {
        return primaryKeyColumn != null ? primaryKeyColumn.getColumnName() : null;
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

    public Converter getPrimaryKeyColumnSerializer()
    {
        return primaryKeyColumn.getCustomSerializer();
    }

    public String getSelectSql()
    {
        return selectSql;
    }

    public String getSelectWhereSql()
    {
        return selectWhereSql;
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

    public String getSelectOrphansSql()
    {
        return selectOrphansSql;
    }

    public String getDeleteOrphansSql(int level)
    {
        return deleteOrphansSql.get(level);
    }

    public void setSelectSql(String selectSql)
    {
        if(StringUtils.isBlank(selectSql))
        {
            throw new PhotonException("Select SQL cannot be blank.");
        }
        this.selectSql = selectSql;
    }

    public void setSelectWhereSql(String selectWhereSql)
    {
        if(StringUtils.isBlank(selectWhereSql))
        {
            throw new PhotonException("Select Where SQL cannot be blank.");
        }
        this.selectWhereSql = selectWhereSql;
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

    public void setSelectOrphansSql(String selectOrphansSql)
    {
        this.selectOrphansSql = selectOrphansSql;
    }

    public void setDeleteOrphansSql(String deleteOrphanSql, int parentLevelsUpForOrphanIds)
    {
        deleteOrphansSql.put(parentLevelsUpForOrphanIds, deleteOrphanSql);
    }

    public void setParentTableBlueprint(TableBlueprint parentTableBlueprint)
    {
        this.parentTableBlueprint = parentTableBlueprint;
    }
}
