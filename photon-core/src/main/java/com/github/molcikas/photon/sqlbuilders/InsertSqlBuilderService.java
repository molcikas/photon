package com.github.molcikas.photon.sqlbuilders;

import com.github.molcikas.photon.blueprints.ColumnBlueprint;
import com.github.molcikas.photon.blueprints.FieldBlueprint;
import com.github.molcikas.photon.blueprints.ForeignKeyListBlueprint;
import com.github.molcikas.photon.blueprints.AggregateEntityBlueprint;
import com.github.molcikas.photon.options.PhotonOptions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class InsertSqlBuilderService
{
    private static final Logger log = LoggerFactory.getLogger(InsertSqlBuilderService.class);

    public void buildInsertSqlTemplates(AggregateEntityBlueprint aggregateRootEntityBlueprint, PhotonOptions photonOptions)
    {
        buildInsertSqlRecursive(aggregateRootEntityBlueprint, Collections.emptyList(), photonOptions);
    }

    private void buildInsertSqlRecursive(
        AggregateEntityBlueprint entityBlueprint,
        List<AggregateEntityBlueprint> parentBlueprints,
        PhotonOptions photonOptions)
    {
        int initialCapacity = entityBlueprint.getColumns().size() * 16 + 64;
        StringBuilder sqlBuilder = new StringBuilder(initialCapacity);

        buildInsertClauseSql(sqlBuilder, entityBlueprint);
        buildValuesClauseSql(sqlBuilder, entityBlueprint);

        String sql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(sqlBuilder.toString(), photonOptions);
        log.debug("Insert Sql:\n" + sql);
        entityBlueprint.setInsertSql(sql);

        entityBlueprint.getForeignKeyListFields().forEach(f -> this.buildInsertKeysFromForeignTableSql(f, photonOptions));

        final List<AggregateEntityBlueprint> childParentBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        childParentBlueprints.addAll(parentBlueprints);
        childParentBlueprints.add(entityBlueprint);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildInsertSqlRecursive(entityField.getChildEntityBlueprint(), childParentBlueprints, photonOptions));
    }

    private void buildInsertClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint entityBlueprint)
    {
        sqlBuilder.append(String.format("INSERT INTO [%s]", entityBlueprint.getTableName()));
    }

    private void buildValuesClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint entityBlueprint)
    {
        List<ColumnBlueprint> columnBlueprints = entityBlueprint.getColumnsForInsertStatement();
        List<String> columnNames = new ArrayList<>(columnBlueprints.size());
        List<String> questionMarks = new ArrayList<>(columnBlueprints.size());

        for(ColumnBlueprint columnBlueprint : columnBlueprints)
        {
            columnNames.add(String.format("[%s]", columnBlueprint.getColumnName()));
            questionMarks.add("?");
        }

        sqlBuilder.append("\n(");
        sqlBuilder.append(StringUtils.join(columnNames, ","));
        sqlBuilder.append(")\nVALUES\n(");
        sqlBuilder.append(StringUtils.join(questionMarks, ","));
        sqlBuilder.append(")");
    }

    private void buildInsertKeysFromForeignTableSql(FieldBlueprint fieldBlueprint, PhotonOptions photonOptions)
    {
        ForeignKeyListBlueprint foreignKeyListBlueprint = fieldBlueprint.getForeignKeyListBlueprint();

        String sql = String.format("INSERT INTO [%s] ([%s], [%s]) VALUES (?, ?)",
            foreignKeyListBlueprint.getForeignTableName(),
            foreignKeyListBlueprint.getForeignTableKeyColumnName(),
            foreignKeyListBlueprint.getForeignTableJoinColumnName()
        );

        sql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(sql, photonOptions);
        log.debug("Insert Foreign Key List Sql:\n" + sql);
        foreignKeyListBlueprint.setInsertSql(sql);
    }
}
