package org.photon.sqlbuilders;

import org.photon.blueprints.AggregateEntityBlueprint;
import org.photon.blueprints.FieldBlueprint;
import org.photon.blueprints.ForeignKeyListBlueprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeleteSqlBuilderService
{
    private final SqlJoinClauseBuilderService sqlJoinClauseBuilderService;

    public DeleteSqlBuilderService(SqlJoinClauseBuilderService sqlJoinClauseBuilderService)
    {
        this.sqlJoinClauseBuilderService = sqlJoinClauseBuilderService;
    }

    public void buildDeleteSqlTemplates(AggregateEntityBlueprint aggregateRootEntityBlueprint)
    {
        buildDeleteSqlTemplatesRecursive(aggregateRootEntityBlueprint, Collections.emptyList());
    }

    private void buildDeleteSqlTemplatesRecursive(
        AggregateEntityBlueprint entityBlueprint,
        List<AggregateEntityBlueprint> parentEntityBlueprints)
    {
        String deleteSql = String.format("DELETE FROM `%s` WHERE `%s` IN (?)",
            entityBlueprint.getTableName(),
            entityBlueprint.getPrimaryKeyColumnName()
        );

        entityBlueprint.setDeleteSql(deleteSql);

        String deleteChildrenExceptSql = String.format("DELETE FROM `%s` WHERE `%s` = ? AND `%s` NOT IN (?)",
            entityBlueprint.getTableName(),
            entityBlueprint.getForeignKeyToParentColumnName(),
            entityBlueprint.getPrimaryKeyColumnName()
        );

        entityBlueprint.setDeleteChildrenExceptSql(deleteChildrenExceptSql);

        buildDeleteOrphansSqlRecursive(entityBlueprint, parentEntityBlueprints);

        entityBlueprint.getForeignKeyListFields().forEach(this::buildDeleteKeysFromForeignTableSql);

        final List<AggregateEntityBlueprint> childParentEntityBlueprints = new ArrayList<>(parentEntityBlueprints.size() + 1);
        childParentEntityBlueprints.add(entityBlueprint);
        childParentEntityBlueprints.addAll(parentEntityBlueprints);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildDeleteSqlTemplatesRecursive(entityField.getChildEntityBlueprint(), childParentEntityBlueprints));
    }

    private void buildDeleteOrphansSqlRecursive(
        AggregateEntityBlueprint entityBlueprint,
        List<AggregateEntityBlueprint> parentEntityBlueprints)
    {
        if(parentEntityBlueprints.isEmpty())
        {
            String deleteOrphansSql = String.format(
                "DELETE FROM `%s` WHERE `%s` IN (?)",
                entityBlueprint.getTableName(),
                entityBlueprint.getPrimaryKeyColumnName()
            );
            entityBlueprint.setDeleteOrphansSql(deleteOrphansSql, 0);
            return;
        }

        AggregateEntityBlueprint rootEntityBlueprint = parentEntityBlueprints.size() > 0 ?
            parentEntityBlueprints.get(parentEntityBlueprints.size() - 1) :
            entityBlueprint;

        StringBuilder deleteOrphansSql = new StringBuilder();

        deleteOrphansSql.append(String.format(
            "DELETE FROM `%s` WHERE `%s` IN (" +
            "\nSELECT `%s`.`%s`" +
            "\nFROM `%s`",
            entityBlueprint.getTableName(),
            entityBlueprint.getPrimaryKeyColumnName(),
            entityBlueprint.getTableName(),
            entityBlueprint.getPrimaryKeyColumnName(),
            entityBlueprint.getTableName()
        ));
        sqlJoinClauseBuilderService.buildJoinClauseSql(deleteOrphansSql, entityBlueprint, parentEntityBlueprints);
        deleteOrphansSql.append(String.format(
            "\nWHERE `%s`.`%s` IN (?)" +
            "\n)",
            rootEntityBlueprint.getTableName(),
            rootEntityBlueprint.getPrimaryKeyColumnName()
        ));

        entityBlueprint.setDeleteOrphansSql(deleteOrphansSql.toString(), parentEntityBlueprints.size());

        buildDeleteOrphansSqlRecursive(
            entityBlueprint,
            parentEntityBlueprints.subList(0, parentEntityBlueprints.size() - 1)
        );
    }

    private void buildDeleteKeysFromForeignTableSql(FieldBlueprint fieldBlueprint)
    {
        ForeignKeyListBlueprint foreignKeyListBlueprint = fieldBlueprint.getForeignKeyListBlueprint();

        String deleteSql = String.format("DELETE FROM `%s` WHERE `%s` IN (?)",
            foreignKeyListBlueprint.getForeignTableName(),
            foreignKeyListBlueprint.getForeignTableJoinColumnName()
        );

        foreignKeyListBlueprint.setDeleteSql(deleteSql);

        String deleteForeignKeysSql = String.format("DELETE FROM `%s` WHERE `%s` IN (?) AND `%s` = ?",
            foreignKeyListBlueprint.getForeignTableName(),
            foreignKeyListBlueprint.getForeignTableKeyColumnName(),
            foreignKeyListBlueprint.getForeignTableJoinColumnName()
        );

        foreignKeyListBlueprint.setDeleteForeignKeysSql(deleteForeignKeysSql);
    }
}
