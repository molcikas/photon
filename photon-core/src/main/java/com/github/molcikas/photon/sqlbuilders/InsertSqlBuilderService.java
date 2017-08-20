package com.github.molcikas.photon.sqlbuilders;

import com.github.molcikas.photon.blueprints.*;
import com.github.molcikas.photon.options.PhotonOptions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class InsertSqlBuilderService
{
    private static final Logger log = LoggerFactory.getLogger(InsertSqlBuilderService.class);

    public static void buildInsertSqlTemplates(EntityBlueprint aggregateRootEntityBlueprint, PhotonOptions photonOptions)
    {
        buildInsertSqlRecursive(aggregateRootEntityBlueprint, photonOptions);
    }

    private static void buildInsertSqlRecursive(
        EntityBlueprint entityBlueprint,
        PhotonOptions photonOptions)
    {
        buildInsertSqlForTableBlueprint(entityBlueprint.getTableBlueprint(), photonOptions, false);
        buildInsertSqlForTableBlueprint(entityBlueprint.getTableBlueprint(), photonOptions, true);
        for(TableBlueprint joinedTableBlueprint : entityBlueprint.getJoinedTableBlueprints())
        {
            buildInsertSqlForTableBlueprint(joinedTableBlueprint, photonOptions, false);
            buildInsertSqlForTableBlueprint(joinedTableBlueprint, photonOptions, true);
        }

        entityBlueprint.getForeignKeyListFields().forEach(f -> buildInsertKeysFromForeignTableSql(f, photonOptions));

        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildInsertSqlRecursive(entityField.getChildEntityBlueprint(), photonOptions));
    }

    private static void buildInsertSqlForTableBlueprint(TableBlueprint tableBlueprint, PhotonOptions photonOptions, boolean alwaysIncludePrimaryKey)
    {
        int initialCapacity = tableBlueprint.getColumns().size() * 16 + 64;
        StringBuilder sqlBuilder = new StringBuilder(initialCapacity);

        buildInsertClauseSql(sqlBuilder, tableBlueprint);
        buildValuesClauseSql(sqlBuilder, tableBlueprint, alwaysIncludePrimaryKey);

        String insertSql = SqlBuilderApplyOptionsService.applyPhotonOptionsToSql(sqlBuilder.toString(), photonOptions);
        log.debug("{}Insert Sql for {}:\n{}", alwaysIncludePrimaryKey ? "Always Include Primary Key " : "", tableBlueprint.getTableName(), insertSql);
        if(alwaysIncludePrimaryKey)
        {
            tableBlueprint.setInsertWithPrimaryKeySql(insertSql);
        }
        else
        {
            tableBlueprint.setInsertSql(insertSql);
        }
    }

    private static void buildInsertClauseSql(StringBuilder sqlBuilder, TableBlueprint tableBlueprint)
    {
        sqlBuilder.append(String.format("INSERT INTO [%s]", tableBlueprint.getTableName()));
    }

    private static void buildValuesClauseSql(StringBuilder sqlBuilder, TableBlueprint tableBlueprint, boolean alwaysIncludePrimaryKey)
    {
        List<ColumnBlueprint> columnBlueprints = tableBlueprint.getColumnsForInsertStatement(alwaysIncludePrimaryKey);
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

    private static void buildInsertKeysFromForeignTableSql(FieldBlueprint fieldBlueprint, PhotonOptions photonOptions)
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
