package com.github.molcikas.photon.sqlbuilders;

import com.github.molcikas.photon.blueprints.ColumnBlueprint;
import com.github.molcikas.photon.blueprints.EntityBlueprint;
import com.github.molcikas.photon.blueprints.TableBlueprint;
import com.github.molcikas.photon.options.PhotonOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public final class UpdateSqlBuilderService
{
    private static final Logger log = LoggerFactory.getLogger(UpdateSqlBuilderService.class);

    public static void buildUpdateSqlTemplates(EntityBlueprint aggregateRootEntityBlueprint, PhotonOptions photonOptions)
    {
        buildUpdateSqlRecursive(aggregateRootEntityBlueprint, photonOptions);
    }

    private static void buildUpdateSqlRecursive(
        EntityBlueprint entityBlueprint,
        PhotonOptions photonOptions)
    {
        buildUpdateSqlForTableBlueprint(entityBlueprint.getTableBlueprint(), photonOptions);
        for(TableBlueprint joinedTableBlueprint : entityBlueprint.getJoinedTableBlueprints())
        {
            buildUpdateSqlForTableBlueprint(joinedTableBlueprint, photonOptions);
        }

        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildUpdateSqlRecursive(entityField.getChildEntityBlueprint(), photonOptions));
    }

    private static void buildUpdateSqlForTableBlueprint(TableBlueprint tableBlueprint, PhotonOptions photonOptions)
    {
        int initialCapacity = tableBlueprint.getColumns().size() * 16 + 64;
        StringBuilder sqlBuilder = new StringBuilder(initialCapacity);

        buildUpdateClauseSql(sqlBuilder, tableBlueprint);
        buildSetClauseSql(sqlBuilder, tableBlueprint);
        buildWhereClauseSql(sqlBuilder, tableBlueprint);

        String updateSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(sqlBuilder.toString(), photonOptions);
        log.debug("Update Sql for {}:\n{}", tableBlueprint.getTableName(), updateSql);
        tableBlueprint.setUpdateSql(updateSql);
    }

    private static void buildUpdateClauseSql(StringBuilder sqlBuilder, TableBlueprint tableBlueprint)
    {
        sqlBuilder.append(String.format("UPDATE [%s]", tableBlueprint.getTableName()));
    }

    private static void buildSetClauseSql(StringBuilder sqlBuilder, TableBlueprint tableBlueprint)
    {
        sqlBuilder.append("\nSET ");

        Collection<ColumnBlueprint> columnBlueprints = tableBlueprint
            .getColumns()
            .stream()
            .filter(c -> !c.isPrimaryKeyColumn())
            .sorted(Comparator.comparingInt(ColumnBlueprint::getColumnIndex))
            .collect(Collectors.toList());
        int collectionIndex = 0;

        for(ColumnBlueprint columnBlueprint : columnBlueprints)
        {
            sqlBuilder.append(String.format("[%s].[%s] = ?%s",
                tableBlueprint.getTableName(),
                columnBlueprint.getColumnName(),
                collectionIndex < columnBlueprints.size() - 1 ? ", " : ""
            ));
            collectionIndex++;
        }
    }

    private static void buildWhereClauseSql(StringBuilder sqlBuilder, TableBlueprint tableBlueprint)
    {
        sqlBuilder.append(String.format("\nWHERE [%s].[%s] = ?",
            tableBlueprint.getTableName(),
            tableBlueprint.getPrimaryKeyColumnName()
        ));
    }
}
