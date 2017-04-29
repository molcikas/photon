package com.github.molcikas.photon.sqlbuilders;

import com.github.molcikas.photon.blueprints.ColumnBlueprint;
import com.github.molcikas.photon.blueprints.AggregateEntityBlueprint;
import com.github.molcikas.photon.options.PhotonOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class UpdateSqlBuilderService
{
    private static final Logger log = LoggerFactory.getLogger(UpdateSqlBuilderService.class);

    public void buildUpdateSqlTemplates(AggregateEntityBlueprint aggregateRootEntityBlueprint, PhotonOptions photonOptions)
    {
        buildUpdateSqlRecursive(aggregateRootEntityBlueprint, Collections.emptyList(), photonOptions);
    }

    private void buildUpdateSqlRecursive(
        AggregateEntityBlueprint entityBlueprint,
        List<AggregateEntityBlueprint> parentBlueprints,
        PhotonOptions photonOptions)
    {
        int initialCapacity = entityBlueprint.getColumns().size() * 16 + 64;
        StringBuilder sqlBuilder = new StringBuilder(initialCapacity);

        buildUpdateClauseSql(sqlBuilder, entityBlueprint);
        buildSetClauseSql(sqlBuilder, entityBlueprint);
        buildWhereClauseSql(sqlBuilder, entityBlueprint);

        String sql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(sqlBuilder.toString(), photonOptions);
        log.debug("Update Sql:\n" + sql);
        entityBlueprint.setUpdateSql(sql);

        final List<AggregateEntityBlueprint> childParentBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        childParentBlueprints.addAll(parentBlueprints);
        childParentBlueprints.add(entityBlueprint);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildUpdateSqlRecursive(entityField.getChildEntityBlueprint(), childParentBlueprints, photonOptions));
    }

    private void buildUpdateClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint entityBlueprint)
    {
        sqlBuilder.append(String.format("UPDATE [%s]", entityBlueprint.getTableName()));
    }

    private void buildSetClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint entityBlueprint)
    {
        sqlBuilder.append("\nSET ");

        Collection<ColumnBlueprint> columnBlueprints = entityBlueprint
            .getColumns()
            .stream()
            .filter(c -> !c.isPrimaryKeyColumn())
            .sorted(Comparator.comparingInt(ColumnBlueprint::getColumnIndex))
            .collect(Collectors.toList());
        int collectionIndex = 0;

        for(ColumnBlueprint columnBlueprint : columnBlueprints)
        {
            sqlBuilder.append(String.format("[%s].[%s] = ?%s",
                entityBlueprint.getTableName(),
                columnBlueprint.getColumnName(),
                collectionIndex < columnBlueprints.size() - 1 ? ", " : ""
            ));
            collectionIndex++;
        }
    }

    private void buildWhereClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint entityBlueprint)
    {
        sqlBuilder.append(String.format("\nWHERE [%s].[%s] = ?",
            entityBlueprint.getTableName(),
            entityBlueprint.getPrimaryKeyColumnName()
        ));
    }
}
