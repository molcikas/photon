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
        buildUpdateSqlRecursive(aggregateRootEntityBlueprint, Collections.emptyList(), photonOptions);
    }

    private static void buildUpdateSqlRecursive(
        EntityBlueprint entityBlueprint,
        List<EntityBlueprint> parentBlueprints,
        PhotonOptions photonOptions)
    {
        int initialCapacity = entityBlueprint.getRootTableBlueprint().getColumns().size() * 16 + 64;
        StringBuilder sqlBuilder = new StringBuilder(initialCapacity);

        buildUpdateClauseSql(sqlBuilder, entityBlueprint.getRootTableBlueprint());
        buildSetClauseSql(sqlBuilder, entityBlueprint.getRootTableBlueprint());
        buildWhereClauseSql(sqlBuilder, entityBlueprint.getRootTableBlueprint());

        String sql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(sqlBuilder.toString(), photonOptions);
        log.debug("Update Sql:\n" + sql);
        entityBlueprint.getRootTableBlueprint().setUpdateSql(sql);

        final List<EntityBlueprint> childParentBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        childParentBlueprints.addAll(parentBlueprints);
        childParentBlueprints.add(entityBlueprint);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildUpdateSqlRecursive(entityField.getChildEntityBlueprint(), childParentBlueprints, photonOptions));
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
