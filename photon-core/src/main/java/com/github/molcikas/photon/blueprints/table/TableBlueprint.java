package com.github.molcikas.photon.blueprints.table;

import com.github.molcikas.photon.PhotonUtils;
import com.github.molcikas.photon.blueprints.entity.EntityBlueprint;
import com.github.molcikas.photon.blueprints.entity.FieldBlueprint;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.query.PopulatedEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

public class TableBlueprint
{
    private List<ColumnBlueprint> columns;
    private ColumnBlueprint primaryKeyColumn;
    private ColumnBlueprint foreignKeyToParentColumn;
    private boolean isPrimaryKeyMappedToField;

    private String tableName;
    private JoinType joinType;
    private String orderBySql;
    private TableBlueprint parentTableBlueprint;
    private String parentTableName;
    private Class entityClass;

    @Getter  @Setter
    private String selectSql;

    @Getter  @Setter
    private String selectWithQuestionSql;

    @Getter  @Setter
    private String selectWhereSql;

    @Getter  @Setter
    private String selectByIdSql;

    @Getter  @Setter
    private String updateSql;

    @Getter  @Setter
    private String insertSql;

    @Getter  @Setter
    private String insertWithPrimaryKeySql;

    @Getter  @Setter
    private String deleteSql;

    @Getter  @Setter
    private String deleteChildrenExceptSql;

    @Getter  @Setter
    private String selectOrphansSql;

    private Map<Integer, String> deleteOrphansSql;

    TableBlueprint(
        TableBlueprint parentTableBlueprint,
        String parentTableName,
        JoinType joinType,
        Class entityClass,
        List<ColumnBlueprint> columns,
        ColumnBlueprint primaryKeyColumn,
        ColumnBlueprint foreignKeyToParentColumn,
        boolean isPrimaryKeyMappedToField,
        String tableName,
        String orderBySql)
    {
        this.parentTableBlueprint = parentTableBlueprint;
        this.parentTableName = parentTableName;
        this.joinType = joinType;
        this.entityClass = entityClass;
        this.columns = columns;
        this.primaryKeyColumn = primaryKeyColumn;
        this.foreignKeyToParentColumn = foreignKeyToParentColumn;
        this.isPrimaryKeyMappedToField = isPrimaryKeyMappedToField;
        this.tableName = tableName;
        this.orderBySql = orderBySql;
        this.deleteOrphansSql = new HashMap<>();
    }

    public JoinType getJoinType()
    {
        return joinType;
    }

    public Class getEntityClass()
    {
        return entityClass;
    }

    public List<ColumnBlueprint> getColumns()
    {
        return Collections.unmodifiableList(columns);
    }

    public ColumnBlueprint getPrimaryKeyColumn()
    {
        return primaryKeyColumn;
    }

    public String getPrimaryKeyColumnName()
    {
        return primaryKeyColumn != null ? primaryKeyColumn.getColumnName() : null;
    }

    public ColumnBlueprint getForeignKeyToParentColumn()
    {
        return foreignKeyToParentColumn;
    }

    public String getForeignKeyToParentColumnNameQualified()
    {
        return foreignKeyToParentColumn != null ? foreignKeyToParentColumn.getColumnNameQualified() : null;
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

    public TableBlueprint getParentTableBlueprint()
    {
        return parentTableBlueprint;
    }

    public String getParentTableName()
    {
        return parentTableName;
    }

    public String getPrimaryKeyColumnNameQualified()
    {
        return primaryKeyColumn != null ? primaryKeyColumn.getColumnNameQualified() : null;
    }

    public List<ColumnBlueprint> getColumnsForInsertStatement(boolean alwaysIncludePrimaryKey)
    {
        if(alwaysIncludePrimaryKey)
        {
            return Collections.unmodifiableList(columns);
        }
        return columns
            .stream()
            .filter(c -> !c.isPrimaryKeyColumn() || (c.isPrimaryKeyColumn() && !c.isAutoIncrementColumn()))
            .collect(Collectors.toList());
    }

    public ColumnBlueprint getVersionColumn(EntityBlueprint entityBlueprint)
    {
        FieldBlueprint versionField = entityBlueprint.getVersionField();
        if(versionField == null)
        {
            return null;
        }
        return columns
            .stream()
            .filter(c -> versionField.equals(c.getMappedFieldBlueprint()))
            .findFirst()
            .orElse(null);
    }

    public boolean shouldInsertUsingPrimaryKeySql(PopulatedEntity populatedEntity, TableBlueprint tableBlueprint)
    {
        if (!primaryKeyColumn.isAutoIncrementColumn())
        {
            boolean isNumericType = tableBlueprint.isPrimaryKeyMappedToField() &&
                PhotonUtils.isNumericType(tableBlueprint.getPrimaryKeyColumn().getMappedFieldBlueprint().getFieldClass());
            if(populatedEntity.getPrimaryKeyValue() == null && isNumericType)
            {
                throw new PhotonException(
                    "Cannot insert %s entity with a null primary key because the primary key is not an auto increment column.",
                    populatedEntity.getEntityBlueprint().getEntityClassName());
            }
            return false;
        }
        Object primaryKey = populatedEntity.getPrimaryKeyValue();
        if (primaryKey == null || !PhotonUtils.isNumericType(primaryKey.getClass()))
        {
            return false;
        }
        return ((Number) primaryKey).longValue() != 0L;
    }

    public boolean isApplicableForEntityClass(Class entityClass)
    {
        return this.entityClass == null || entityClass.isAssignableFrom(this.entityClass);
    }

    public Converter getPrimaryKeyColumnSerializer()
    {
        return primaryKeyColumn.getCustomSerializer();
    }

    public String getDeleteOrphansSql(int level)
    {
        return deleteOrphansSql.get(level);
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
