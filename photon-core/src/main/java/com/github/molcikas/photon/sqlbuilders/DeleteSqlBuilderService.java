package com.github.molcikas.photon.sqlbuilders;

import com.github.molcikas.photon.blueprints.EntityBlueprint;
import com.github.molcikas.photon.blueprints.FieldBlueprint;
import com.github.molcikas.photon.blueprints.ForeignKeyListBlueprint;
import com.github.molcikas.photon.blueprints.TableBlueprint;
import com.github.molcikas.photon.options.PhotonOptions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DeleteSqlBuilderService
{
    private static final Logger log = LoggerFactory.getLogger(DeleteSqlBuilderService.class);

    public static void buildDeleteSqlTemplates(EntityBlueprint aggregateRootEntityBlueprint, PhotonOptions photonOptions)
    {
        buildDeleteSqlTemplatesRecursive(aggregateRootEntityBlueprint, Collections.emptyList(), photonOptions);
    }

    private static void buildDeleteSqlTemplatesRecursive(
        EntityBlueprint entityBlueprint,
        List<TableBlueprint> parentTableBlueprints,
        PhotonOptions photonOptions)
    {
        buildDeleteSql(entityBlueprint.getTableBlueprint(), photonOptions);
        buildDeleteChildrenExceptSql(entityBlueprint.getTableBlueprint(), photonOptions);
        buildDeleteOrphansSqlRecursive(entityBlueprint.getTableBlueprint(), parentTableBlueprints, photonOptions);

        for(TableBlueprint joinTableBlueprint : entityBlueprint.getJoinedTableBlueprints())
        {
            buildDeleteSql(joinTableBlueprint, photonOptions);
            buildDeleteChildrenExceptSql(joinTableBlueprint, photonOptions);

            // Note: No need to add the main table here because they are inner joined in a one-to-one relationship
            buildDeleteOrphansSqlRecursive(joinTableBlueprint, parentTableBlueprints, photonOptions);
        }

        entityBlueprint.getForeignKeyListFields().forEach(f -> buildDeleteKeysFromForeignTableSql(f, photonOptions));

        final List<TableBlueprint> childParentTableBlueprints = new ArrayList<>(parentTableBlueprints.size() + 1);
        childParentTableBlueprints.add(entityBlueprint.getTableBlueprint());
        childParentTableBlueprints.addAll(parentTableBlueprints);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildDeleteSqlTemplatesRecursive(entityField.getChildEntityBlueprint(), childParentTableBlueprints, photonOptions));
    }

    private static void buildDeleteSql(
        TableBlueprint tableBlueprint,
        PhotonOptions photonOptions)
    {
        String deleteSql = String.format("DELETE FROM [%s] WHERE [%s] IN (?)",
            tableBlueprint.getTableName(),
            tableBlueprint.getPrimaryKeyColumnName()
        );

        deleteSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(deleteSql, photonOptions);
        log.debug("Delete Sql for {}:\n{}", tableBlueprint.getTableName(), deleteSql);
        tableBlueprint.setDeleteSql(deleteSql);
    }

    private static void buildDeleteChildrenExceptSql(TableBlueprint tableBlueprint, PhotonOptions photonOptions)
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

        String deleteChildrenExceptSql = String.format("DELETE FROM [%s] WHERE [%s] = ? AND [%s] NOT IN (?)",
            tableBlueprint.getTableName(),
            tableBlueprint.getForeignKeyToParentColumnName(),
            tableBlueprint.getPrimaryKeyColumnName()
        );

        deleteChildrenExceptSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(deleteChildrenExceptSql, photonOptions);
        log.debug("Delete Children Except Sql for {}:\n{}", tableBlueprint.getTableName(), deleteChildrenExceptSql);
        tableBlueprint.setDeleteChildrenExceptSql(deleteChildrenExceptSql);
    }

    private static void buildDeleteOrphansSqlRecursive(
        TableBlueprint tableBlueprint,
        List<TableBlueprint> parentTableBlueprints,
        PhotonOptions photonOptions)
    {
        if(parentTableBlueprints.isEmpty())
        {
            String deleteOrphansSql = String.format(
                "DELETE FROM [%s] WHERE [%s] IN (?)",
                tableBlueprint.getTableName(),
                tableBlueprint.getPrimaryKeyColumnName()
            );
            deleteOrphansSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(deleteOrphansSql, photonOptions);
            log.debug("Delete Orphans Level {} Sql for {}:\n{}", parentTableBlueprints.size(), tableBlueprint.getTableName(), deleteOrphansSql);
            tableBlueprint.setDeleteOrphansSql(deleteOrphansSql, 0);
            return;
        }

        TableBlueprint rootTableBlueprint = parentTableBlueprints.size() > 0 ?
            parentTableBlueprints.get(parentTableBlueprints.size() - 1) :
            tableBlueprint;

        StringBuilder deleteOrphansSqlBuilder = new StringBuilder();

        deleteOrphansSqlBuilder.append(String.format(
            "DELETE FROM [%s] WHERE [%s] IN (" +
            "\nSELECT [%s].[%s]" +
            "\nFROM [%s]",
            tableBlueprint.getTableName(),
            tableBlueprint.getPrimaryKeyColumnName(),
            tableBlueprint.getTableName(),
            tableBlueprint.getPrimaryKeyColumnName(),
            tableBlueprint.getTableName()
        ));
        SqlJoinClauseBuilderService.buildChildToParentJoinClauseSql(deleteOrphansSqlBuilder, tableBlueprint, parentTableBlueprints);
        deleteOrphansSqlBuilder.append(String.format(
            "\nWHERE [%s].[%s] IN (?)" +
            "\n)",
            rootTableBlueprint.getTableName(),
            rootTableBlueprint.getPrimaryKeyColumnName()
        ));

        String deleteOrphansSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(deleteOrphansSqlBuilder.toString(), photonOptions);
        log.debug("Delete Orphans Sql Level {} for {}:\n{}", parentTableBlueprints.size(), tableBlueprint.getTableName(), deleteOrphansSql);
        tableBlueprint.setDeleteOrphansSql(deleteOrphansSql, parentTableBlueprints.size());

        buildDeleteOrphansSqlRecursive(
            tableBlueprint,
            parentTableBlueprints.subList(0, parentTableBlueprints.size() - 1),
            photonOptions
        );
    }

    private static void buildDeleteKeysFromForeignTableSql(FieldBlueprint fieldBlueprint, PhotonOptions photonOptions)
    {
        ForeignKeyListBlueprint foreignKeyListBlueprint = fieldBlueprint.getForeignKeyListBlueprint();

        String deleteSql = String.format("DELETE FROM [%s] WHERE [%s] IN (?)",
            foreignKeyListBlueprint.getForeignTableName(),
            foreignKeyListBlueprint.getForeignTableJoinColumnName()
        );

        deleteSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(deleteSql, photonOptions);
        log.debug("Delete All Foreign Key Sql for {}:\n{}", fieldBlueprint.getFieldName(), deleteSql);
        foreignKeyListBlueprint.setDeleteSql(deleteSql);

        String deleteForeignKeysSql = String.format("DELETE FROM [%s] WHERE [%s] IN (?) AND [%s] = ?",
            foreignKeyListBlueprint.getForeignTableName(),
            foreignKeyListBlueprint.getForeignTableKeyColumnName(),
            foreignKeyListBlueprint.getForeignTableJoinColumnName()
        );

        deleteForeignKeysSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(deleteForeignKeysSql, photonOptions);
        log.debug("Delete Foreign Keys Sql for {}:\n{}", fieldBlueprint.getFieldName(), deleteForeignKeysSql);
        foreignKeyListBlueprint.setDeleteForeignKeysSql(deleteForeignKeysSql);
    }
}
