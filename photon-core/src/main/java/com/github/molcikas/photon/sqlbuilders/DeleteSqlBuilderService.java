package com.github.molcikas.photon.sqlbuilders;

import com.github.molcikas.photon.blueprints.EntityBlueprint;
import com.github.molcikas.photon.blueprints.FieldBlueprint;
import com.github.molcikas.photon.blueprints.ForeignKeyListBlueprint;
import com.github.molcikas.photon.blueprints.TableBlueprint;
import com.github.molcikas.photon.options.PhotonOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class DeleteSqlBuilderService
{
    private static final Logger log = LoggerFactory.getLogger(DeleteSqlBuilderService.class);

    public static void buildDeleteSqlTemplates(EntityBlueprint aggregateRootEntityBlueprint, PhotonOptions photonOptions)
    {
        buildDeleteSqlTemplatesRecursive(aggregateRootEntityBlueprint, Collections.emptyList(), photonOptions);
    }

    private static void buildDeleteSqlTemplatesRecursive(
        EntityBlueprint entityBlueprint,
        List<EntityBlueprint> parentEntityBlueprints,
        PhotonOptions photonOptions)
    {
        TableBlueprint tableBlueprint = entityBlueprint.getTableBlueprint();

        String deleteSql = String.format("DELETE FROM [%s] WHERE [%s] IN (?)",
            tableBlueprint.getTableName(),
            tableBlueprint.getPrimaryKeyColumnName()
        );

        deleteSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(deleteSql, photonOptions);
        log.debug("Delete Sql:\n" + deleteSql);
        tableBlueprint.setDeleteSql(deleteSql);

        String deleteChildrenExceptSql = String.format("DELETE FROM [%s] WHERE [%s] = ? AND [%s] NOT IN (?)",
            tableBlueprint.getTableName(),
            tableBlueprint.getForeignKeyToParentColumnName(),
            tableBlueprint.getPrimaryKeyColumnName()
        );

        deleteChildrenExceptSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(deleteChildrenExceptSql, photonOptions);
        log.debug("Delete Children Except Sql:\n" + deleteChildrenExceptSql);
        tableBlueprint.setDeleteChildrenExceptSql(deleteChildrenExceptSql);

        List<TableBlueprint> parentTableBlueprints = parentEntityBlueprints
            .stream()
            .map(EntityBlueprint::getTableBlueprint)
            .collect(Collectors.toList());

        buildDeleteOrphansSqlRecursive(tableBlueprint, parentTableBlueprints, photonOptions);

        entityBlueprint.getForeignKeyListFields().forEach(f -> buildDeleteKeysFromForeignTableSql(f, photonOptions));

        final List<EntityBlueprint> childParentEntityBlueprints = new ArrayList<>(parentEntityBlueprints.size() + 1);
        childParentEntityBlueprints.add(entityBlueprint);
        childParentEntityBlueprints.addAll(parentEntityBlueprints);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildDeleteSqlTemplatesRecursive(entityField.getChildEntityBlueprint(), childParentEntityBlueprints, photonOptions));
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
            log.debug("Delete Orphans Sql:\n" + deleteOrphansSql);
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
        SqlJoinClauseBuilderService.buildJoinClauseSql(deleteOrphansSqlBuilder, tableBlueprint, parentTableBlueprints);
        deleteOrphansSqlBuilder.append(String.format(
            "\nWHERE [%s].[%s] IN (?)" +
            "\n)",
            rootTableBlueprint.getTableName(),
            rootTableBlueprint.getPrimaryKeyColumnName()
        ));

        String deleteOrphansSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(deleteOrphansSqlBuilder.toString(), photonOptions);
        log.debug("Delete Orphans Sql:\n" + deleteOrphansSql);
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
        log.debug("Delete All Foreign Key Sql:\n" + deleteSql);
        foreignKeyListBlueprint.setDeleteSql(deleteSql);

        String deleteForeignKeysSql = String.format("DELETE FROM [%s] WHERE [%s] IN (?) AND [%s] = ?",
            foreignKeyListBlueprint.getForeignTableName(),
            foreignKeyListBlueprint.getForeignTableKeyColumnName(),
            foreignKeyListBlueprint.getForeignTableJoinColumnName()
        );

        deleteForeignKeysSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(deleteForeignKeysSql, photonOptions);
        log.debug("Delete Foreign Keys Sql:\n" + deleteForeignKeysSql);
        foreignKeyListBlueprint.setDeleteForeignKeysSql(deleteForeignKeysSql);
    }
}
