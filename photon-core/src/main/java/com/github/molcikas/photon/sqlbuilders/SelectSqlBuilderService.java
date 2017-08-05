package com.github.molcikas.photon.sqlbuilders;

import com.github.molcikas.photon.blueprints.*;
import com.github.molcikas.photon.options.PhotonOptions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class SelectSqlBuilderService
{
    private static final Logger log = LoggerFactory.getLogger(SelectSqlBuilderService.class);

    public static void buildSelectSqlTemplates(EntityBlueprint entityBlueprint, PhotonOptions photonOptions)
    {
        buildSelectSqlRecursive(entityBlueprint, entityBlueprint.getTableBlueprint(), Collections.emptyList(), photonOptions);
    }

    private static void buildSelectSqlRecursive(
        EntityBlueprint entityBlueprint,
        TableBlueprint rootMainTableBlueprint,
        List<TableBlueprint> parentTableBlueprints,
        PhotonOptions photonOptions)
    {
        buildAllSelectSqlForTableBlueprint(
            entityBlueprint.getTableBlueprint(),
            rootMainTableBlueprint,
            parentTableBlueprints,
            photonOptions
        );

        for(TableBlueprint joinTableBlueprint : entityBlueprint.getJoinedTableBlueprints())
        {
            List<TableBlueprint> joinedParents = new ArrayList<>();
            TableBlueprint parent = joinTableBlueprint.getParentTableBlueprint();
            while(parent != null)
            {
                joinedParents.add(parent);
                parent = parent.getParentTableBlueprint();
            }
            List<TableBlueprint> allParents = new ArrayList<>(joinedParents.size() + parentTableBlueprints.size());
            allParents.addAll(joinedParents);
            allParents.addAll(parentTableBlueprints);

            buildAllSelectSqlForTableBlueprint(
                joinTableBlueprint,
                rootMainTableBlueprint,
                allParents,
                photonOptions
            );
        }

        entityBlueprint.getForeignKeyListFields().forEach(f -> buildSelectKeysFromForeignTableSql(f, photonOptions));

        final List<TableBlueprint> childParentTableBlueprints = new ArrayList<>(parentTableBlueprints.size() + 1);
        childParentTableBlueprints.add(entityBlueprint.getTableBlueprint());
        childParentTableBlueprints.addAll(parentTableBlueprints);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildSelectSqlRecursive(
                entityField.getChildEntityBlueprint(),
                rootMainTableBlueprint,
                childParentTableBlueprints,
                photonOptions
            ));
    }

    private static void buildAllSelectSqlForTableBlueprint(
        TableBlueprint tableBlueprint,
        TableBlueprint rootMainTableBlueprint,
        List<TableBlueprint> parentTableBlueprints,
        PhotonOptions photonOptions)
    {
        buildSelectSql(tableBlueprint, rootMainTableBlueprint,
            parentTableBlueprints, photonOptions);
        buildSelectWhereSql(tableBlueprint, parentTableBlueprints, photonOptions);
        buildSelectOrphansSql(tableBlueprint, photonOptions);
    }

    private static void buildSelectSql(
        TableBlueprint tableBlueprint,
        TableBlueprint rootMainTableBlueprint,
        List<TableBlueprint> parentTableBlueprints,
        PhotonOptions photonOptions)
    {
        int initialCapacity = tableBlueprint.getColumns().size() * 16 + 64;
        StringBuilder sqlBuilder = new StringBuilder(initialCapacity);

        buildSelectClauseSql(sqlBuilder, tableBlueprint);
        buildFromClauseSql(sqlBuilder, tableBlueprint);
        SqlJoinClauseBuilderService.buildJoinClauseSql(sqlBuilder, tableBlueprint, parentTableBlueprints);
        buildWhereClauseSql(sqlBuilder, rootMainTableBlueprint);
        buildOrderBySql(sqlBuilder, tableBlueprint, parentTableBlueprints);

        String selectSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(sqlBuilder.toString(), photonOptions);
        log.debug("Select Sql for {}:\n{}", tableBlueprint.getTableName(), selectSql);
        tableBlueprint.setSelectSql(selectSql);
    }

    private static void buildSelectWhereSql(
        TableBlueprint tableBlueprint,
        List<TableBlueprint> parentTableBlueprints,
        PhotonOptions photonOptions)
    {
        int initialCapacity = tableBlueprint.getColumns().size() * 16 + 64;
        StringBuilder sqlBuilder = new StringBuilder(initialCapacity);

        buildSelectClauseSql(sqlBuilder, tableBlueprint);
        buildFromClauseSql(sqlBuilder, tableBlueprint);
        SqlJoinClauseBuilderService.buildJoinClauseSql(sqlBuilder, tableBlueprint, parentTableBlueprints);
        buildOpenWhereClauseSql(sqlBuilder);
        buildOrderBySql(sqlBuilder, tableBlueprint, parentTableBlueprints);

        String selectWhereSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(sqlBuilder.toString(), photonOptions);
        log.debug("Select Where Sql for {}:\n{}", tableBlueprint.getTableName(), selectWhereSql);
        tableBlueprint.setSelectWhereSql(selectWhereSql);
    }

    private static void buildSelectClauseSql(StringBuilder sqlBuilder, TableBlueprint tableBlueprint)
    {
        sqlBuilder.append("SELECT " );

        for(ColumnBlueprint columnBlueprint : tableBlueprint.getColumns())
        {
            sqlBuilder.append(String.format("[%s].[%s]%s",
                tableBlueprint.getTableName(),
                columnBlueprint.getColumnName(),
                columnBlueprint.getColumnIndex() < tableBlueprint.getColumns().size() - 1 ? ", " : ""
            ));
        }
    }

    private static void buildFromClauseSql(StringBuilder sqlBuilder, TableBlueprint tableBlueprint)
    {
        sqlBuilder.append(String.format("\nFROM [%s]", tableBlueprint.getTableName()));
    }

    private static void buildWhereClauseSql(StringBuilder sqlBuilder, TableBlueprint tableBlueprint)
    {
        sqlBuilder.append(String.format("\nWHERE [%s].[%s] IN (%s)",
            tableBlueprint.getTableName(),
            tableBlueprint.getPrimaryKeyColumnName(),
            "%s"
        ));
    }

    private static void buildOpenWhereClauseSql(StringBuilder sqlBuilder)
    {
        sqlBuilder.append("\nWHERE (%s)");
    }

    private static void buildOrderBySql(
        StringBuilder sqlBuilder,
        TableBlueprint tableBlueprint,
        List<TableBlueprint> parentTableBlueprints)
    {
        List<TableBlueprint> tableBlueprints = new ArrayList<>(parentTableBlueprints.size() + 1);
        tableBlueprints.add(tableBlueprint);
        tableBlueprints.addAll(parentTableBlueprints);
        Collections.reverse(tableBlueprints);

        sqlBuilder.append("\nORDER BY ");

        StringBuilder orderBySqlBuilder = new StringBuilder();

        for (TableBlueprint table : tableBlueprints)
        {
            if(StringUtils.isNotBlank(table.getOrderBySql()))
            {
                orderBySqlBuilder.append(table.getOrderBySql()).append(", ");
            }

            // We need to add the primary key as a secondary sort, otherwise entity selects might fail because
            // it expects child entities to be sorted by parent.
            orderBySqlBuilder.append(String.format("[%s].[%s], ",
                table.getTableName(),
                table.getPrimaryKeyColumnName()
            ));
        }

        // Replace repeated commas with a single comma and trim commas at the end
        String orderBySql = orderBySqlBuilder
            .toString()
            .replaceAll(", *, *", ", ")
            .replaceAll(", *$", "");

        sqlBuilder.append(orderBySql);
    }

    private static void buildSelectOrphansSql(TableBlueprint tableBlueprint, PhotonOptions photonOptions)
    {
        if(tableBlueprint.getForeignKeyToParentColumn() == null)
        {
            // This is the aggregate root's main table, which does not have an orphans check since it is the root.
            return;
        }
        if(StringUtils.equals(tableBlueprint.getPrimaryKeyColumnName(), tableBlueprint.getForeignKeyToParentColumnName()))
        {
            // This table and the parent table share the same id, so there can't be any orphans.
            return;
        }
        if(!tableBlueprint.isPrimaryKeyMappedToField())
        {
            // If the entity does not know its primary key value, we can't determine what the orphans are. On save,
            // we'll have to delete all children and re-insert them.
            return;
        }

        String selectOrphansSql = String.format(
            "SELECT [%s] FROM [%s] WHERE [%s] = ? AND [%s] NOT IN (?)",
            tableBlueprint.getPrimaryKeyColumnName(),
            tableBlueprint.getTableName(),
            tableBlueprint.getForeignKeyToParentColumnName(),
            tableBlueprint.getPrimaryKeyColumnName()
        );

        selectOrphansSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(selectOrphansSql, photonOptions);
        log.debug("Select Orphans Sql for {}:\n{}", tableBlueprint.getTableName(), selectOrphansSql);
        tableBlueprint.setSelectOrphansSql(selectOrphansSql);
    }

    private static void buildSelectKeysFromForeignTableSql(FieldBlueprint fieldBlueprint, PhotonOptions photonOptions)
    {
        ForeignKeyListBlueprint foreignKeyListBlueprint = fieldBlueprint.getForeignKeyListBlueprint();

        String foreignKeyListSql = String.format("SELECT [%s], [%s] FROM [%s] WHERE [%s] IN (?) ORDER BY [%s]",
            foreignKeyListBlueprint.getForeignTableKeyColumnName(),
            foreignKeyListBlueprint.getForeignTableJoinColumnName(),
            foreignKeyListBlueprint.getForeignTableName(),
            foreignKeyListBlueprint.getForeignTableJoinColumnName(),
            foreignKeyListBlueprint.getForeignTableJoinColumnName()
        );

        foreignKeyListSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(foreignKeyListSql, photonOptions);
        log.debug("Select Foreign Key List Sql for {}:\n{}", fieldBlueprint.getFieldName(), foreignKeyListSql);
        foreignKeyListBlueprint.setSelectSql(foreignKeyListSql);
    }
}
