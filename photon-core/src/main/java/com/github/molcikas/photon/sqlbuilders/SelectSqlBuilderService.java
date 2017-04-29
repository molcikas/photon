package com.github.molcikas.photon.sqlbuilders;

import com.github.molcikas.photon.options.PhotonOptions;
import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.blueprints.AggregateEntityBlueprint;
import com.github.molcikas.photon.blueprints.ColumnBlueprint;
import com.github.molcikas.photon.blueprints.FieldBlueprint;
import com.github.molcikas.photon.blueprints.ForeignKeyListBlueprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SelectSqlBuilderService
{
    private static final Logger log = LoggerFactory.getLogger(SelectSqlBuilderService.class);

    private final SqlJoinClauseBuilderService sqlJoinClauseBuilderService;

    public SelectSqlBuilderService(SqlJoinClauseBuilderService sqlJoinClauseBuilderService)
    {
        this.sqlJoinClauseBuilderService = sqlJoinClauseBuilderService;
    }

    public void buildSelectSqlTemplates(AggregateEntityBlueprint aggregateRootEntityBlueprint, PhotonOptions photonOptions)
    {
        buildSelectSqlRecursive(aggregateRootEntityBlueprint, aggregateRootEntityBlueprint, Collections.emptyList(), photonOptions);
    }

    private void buildSelectSqlRecursive(
        AggregateEntityBlueprint entityBlueprint,
        AggregateEntityBlueprint aggregateRootEntityBlueprint,
        List<AggregateEntityBlueprint> parentEntityBlueprints,
        PhotonOptions photonOptions)
    {
        int initialCapacity = entityBlueprint.getColumns().size() * 16 + 64;
        StringBuilder sqlBuilder = new StringBuilder(initialCapacity);

        buildSelectClauseSql(sqlBuilder, entityBlueprint);
        buildFromClauseSql(sqlBuilder, entityBlueprint);
        sqlJoinClauseBuilderService.buildJoinClauseSql(sqlBuilder, entityBlueprint, parentEntityBlueprints);
        buildWhereClauseSql(sqlBuilder, aggregateRootEntityBlueprint);
        buildOrderBySql(sqlBuilder, entityBlueprint, parentEntityBlueprints);

        String sql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(sqlBuilder.toString(), photonOptions);
        log.debug("Select Sql:\n" + sql);
        entityBlueprint.setSelectSql(sql);

        buildSelectOrphansSql(entityBlueprint, photonOptions);
        entityBlueprint.getForeignKeyListFields().forEach(e -> this.buildSelectKeysFromForeignTableSql(e, photonOptions));

        final List<AggregateEntityBlueprint> childParentEntityBlueprints = new ArrayList<>(parentEntityBlueprints.size() + 1);
        childParentEntityBlueprints.add(entityBlueprint);
        childParentEntityBlueprints.addAll(parentEntityBlueprints);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildSelectSqlRecursive(
                entityField.getChildEntityBlueprint(),
                aggregateRootEntityBlueprint,
                childParentEntityBlueprints,
                photonOptions
            ));
    }

    private void buildSelectClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint entityBlueprint)
    {
        sqlBuilder.append("SELECT " );

        for(ColumnBlueprint columnBlueprint : entityBlueprint.getColumns())
        {
            sqlBuilder.append(String.format("[%s].[%s]%s",
                entityBlueprint.getTableName(),
                columnBlueprint.getColumnName(),
                columnBlueprint.getColumnIndex() < entityBlueprint.getColumns().size() - 1 ? ", " : ""
            ));
        }
    }

    private void buildFromClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint entityBlueprint)
    {
        sqlBuilder.append(String.format("\nFROM [%s]", entityBlueprint.getTableName()));
    }

    private void buildWhereClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint aggregateRootEntityBlueprint)
    {
        sqlBuilder.append(String.format("\nWHERE [%s].[%s] IN (%s)",
            aggregateRootEntityBlueprint.getTableName(),
            aggregateRootEntityBlueprint.getPrimaryKeyColumnName(),
            "%s"
        ));
    }

    private void buildOrderBySql(
        StringBuilder sqlBuilder,
        AggregateEntityBlueprint childBlueprint,
        List<AggregateEntityBlueprint> parentBlueprints)
    {
        List<AggregateEntityBlueprint> entityBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        entityBlueprints.add(childBlueprint);
        entityBlueprints.addAll(parentBlueprints);
        Collections.reverse(entityBlueprints);

        sqlBuilder.append("\nORDER BY ");

        for (AggregateEntityBlueprint entityBlueprint : entityBlueprints)
        {
            boolean isPrimaryKeySort = StringUtils.equals(entityBlueprint.getOrderByColumnName(), entityBlueprint.getPrimaryKeyColumnName());
            sqlBuilder.append(String.format("[%s].[%s] %s%s",
                entityBlueprint.getTableName(),
                entityBlueprint.getOrderByColumnName(),
                entityBlueprint.getOrderByDirection().sqlSortDirection,
                isPrimaryKeySort && entityBlueprint.equals(childBlueprint) ? "" : ", "
            ));
            if(!isPrimaryKeySort)
            {
                // If it's not a primary key sort, we need to add the primary key as a secondary sort, otherwise the
                // entity connecting might fail because it expects entities to be sorted by parent.
                sqlBuilder.append(String.format("[%s].[%s]%s",
                    entityBlueprint.getTableName(),
                    entityBlueprint.getPrimaryKeyColumnName(),
                    entityBlueprint.equals(childBlueprint) ? "" : ", "
                ));
            }
        }
    }

    private void buildSelectOrphansSql(AggregateEntityBlueprint entityBlueprint, PhotonOptions photonOptions)
    {
        if(!entityBlueprint.isPrimaryKeyMappedToField())
        {
            // If the entity does not know its primary key value, we can't determine what the orphans are. On save,
            // we'll have to delete all children and re-insert them.
            return;
        }

        String selectOrphansSql = String.format(
            "SELECT [%s] FROM [%s] WHERE [%s] = ? AND [%s] NOT IN (?)",
            entityBlueprint.getPrimaryKeyColumnName(),
            entityBlueprint.getTableName(),
            entityBlueprint.getForeignKeyToParentColumnName(),
            entityBlueprint.getPrimaryKeyColumnName()
        );

        selectOrphansSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(selectOrphansSql, photonOptions);
        log.debug("Select Orphans Sql:\n" + selectOrphansSql);
        entityBlueprint.setSelectOrphansSql(selectOrphansSql);
    }

    private void buildSelectKeysFromForeignTableSql(FieldBlueprint fieldBlueprint, PhotonOptions photonOptions)
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
        log.debug("Select Foreign Key List Sql:\n" + foreignKeyListSql);
        foreignKeyListBlueprint.setSelectSql(foreignKeyListSql);
    }
}
