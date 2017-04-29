package com.github.molcikas.photon.sqlbuilders;

import com.github.molcikas.photon.blueprints.AggregateEntityBlueprint;
import com.github.molcikas.photon.blueprints.FieldBlueprint;
import com.github.molcikas.photon.blueprints.ForeignKeyListBlueprint;
import com.github.molcikas.photon.options.PhotonOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeleteSqlBuilderService
{
    private static final Logger log = LoggerFactory.getLogger(DeleteSqlBuilderService.class);

    private final SqlJoinClauseBuilderService sqlJoinClauseBuilderService;

    public DeleteSqlBuilderService(SqlJoinClauseBuilderService sqlJoinClauseBuilderService)
    {
        this.sqlJoinClauseBuilderService = sqlJoinClauseBuilderService;
    }

    public void buildDeleteSqlTemplates(AggregateEntityBlueprint aggregateRootEntityBlueprint, PhotonOptions photonOptions)
    {
        buildDeleteSqlTemplatesRecursive(aggregateRootEntityBlueprint, Collections.emptyList(), photonOptions);
    }

    private void buildDeleteSqlTemplatesRecursive(
        AggregateEntityBlueprint entityBlueprint,
        List<AggregateEntityBlueprint> parentEntityBlueprints,
        PhotonOptions photonOptions)
    {
        String deleteSql = String.format("DELETE FROM [%s] WHERE [%s] IN (?)",
            entityBlueprint.getTableName(),
            entityBlueprint.getPrimaryKeyColumnName()
        );

        deleteSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(deleteSql, photonOptions);
        log.debug("Delete Sql:\n" + deleteSql);
        entityBlueprint.setDeleteSql(deleteSql);

        String deleteChildrenExceptSql = String.format("DELETE FROM [%s] WHERE [%s] = ? AND [%s] NOT IN (?)",
            entityBlueprint.getTableName(),
            entityBlueprint.getForeignKeyToParentColumnName(),
            entityBlueprint.getPrimaryKeyColumnName()
        );

        deleteChildrenExceptSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(deleteChildrenExceptSql, photonOptions);
        log.debug("Delete Children Except Sql:\n" + deleteChildrenExceptSql);
        entityBlueprint.setDeleteChildrenExceptSql(deleteChildrenExceptSql);

        buildDeleteOrphansSqlRecursive(entityBlueprint, parentEntityBlueprints, photonOptions);

        entityBlueprint.getForeignKeyListFields().forEach(f -> this.buildDeleteKeysFromForeignTableSql(f, photonOptions));

        final List<AggregateEntityBlueprint> childParentEntityBlueprints = new ArrayList<>(parentEntityBlueprints.size() + 1);
        childParentEntityBlueprints.add(entityBlueprint);
        childParentEntityBlueprints.addAll(parentEntityBlueprints);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildDeleteSqlTemplatesRecursive(entityField.getChildEntityBlueprint(), childParentEntityBlueprints, photonOptions));
    }

    private void buildDeleteOrphansSqlRecursive(
        AggregateEntityBlueprint entityBlueprint,
        List<AggregateEntityBlueprint> parentEntityBlueprints,
        PhotonOptions photonOptions)
    {
        if(parentEntityBlueprints.isEmpty())
        {
            String deleteOrphansSql = String.format(
                "DELETE FROM [%s] WHERE [%s] IN (?)",
                entityBlueprint.getTableName(),
                entityBlueprint.getPrimaryKeyColumnName()
            );
            deleteOrphansSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(deleteOrphansSql, photonOptions);
            log.debug("Delete Orphans Sql:\n" + deleteOrphansSql);
            entityBlueprint.setDeleteOrphansSql(deleteOrphansSql, 0);
            return;
        }

        AggregateEntityBlueprint rootEntityBlueprint = parentEntityBlueprints.size() > 0 ?
            parentEntityBlueprints.get(parentEntityBlueprints.size() - 1) :
            entityBlueprint;

        StringBuilder deleteOrphansSqlBuilder = new StringBuilder();

        deleteOrphansSqlBuilder.append(String.format(
            "DELETE FROM [%s] WHERE [%s] IN (" +
            "\nSELECT [%s].[%s]" +
            "\nFROM [%s]",
            entityBlueprint.getTableName(),
            entityBlueprint.getPrimaryKeyColumnName(),
            entityBlueprint.getTableName(),
            entityBlueprint.getPrimaryKeyColumnName(),
            entityBlueprint.getTableName()
        ));
        sqlJoinClauseBuilderService.buildJoinClauseSql(deleteOrphansSqlBuilder, entityBlueprint, parentEntityBlueprints);
        deleteOrphansSqlBuilder.append(String.format(
            "\nWHERE [%s].[%s] IN (?)" +
            "\n)",
            rootEntityBlueprint.getTableName(),
            rootEntityBlueprint.getPrimaryKeyColumnName()
        ));

        String deleteOrphansSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(deleteOrphansSqlBuilder.toString(), photonOptions);
        log.debug("Delete Orphans Sql:\n" + deleteOrphansSql);
        entityBlueprint.setDeleteOrphansSql(deleteOrphansSql, parentEntityBlueprints.size());

        buildDeleteOrphansSqlRecursive(
            entityBlueprint,
            parentEntityBlueprints.subList(0, parentEntityBlueprints.size() - 1),
            photonOptions
        );
    }

    private void buildDeleteKeysFromForeignTableSql(FieldBlueprint fieldBlueprint, PhotonOptions photonOptions)
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
