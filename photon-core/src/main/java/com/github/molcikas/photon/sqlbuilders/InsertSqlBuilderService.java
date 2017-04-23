package com.github.molcikas.photon.sqlbuilders;

import com.github.molcikas.photon.blueprints.ColumnBlueprint;
import com.github.molcikas.photon.blueprints.FieldBlueprint;
import com.github.molcikas.photon.blueprints.ForeignKeyListBlueprint;
import com.github.molcikas.photon.blueprints.AggregateEntityBlueprint;

import java.util.*;

public class InsertSqlBuilderService
{
    public void buildInsertSqlTemplates(AggregateEntityBlueprint aggregateRootEntityBlueprint)
    {
        buildInsertSqlRecursive(aggregateRootEntityBlueprint, Collections.emptyList());
    }

    private void buildInsertSqlRecursive(
        AggregateEntityBlueprint entityBlueprint,
        List<AggregateEntityBlueprint> parentBlueprints)
    {
        int initialCapacity = entityBlueprint.getColumns().size() * 16 + 64;
        StringBuilder sqlBuilder = new StringBuilder(initialCapacity);

        buildInsertClauseSql(sqlBuilder, entityBlueprint);
        buildValuesClauseSql(sqlBuilder, entityBlueprint);

        entityBlueprint.setInsertSql(sqlBuilder.toString());

        //System.out.println(sqlBuilder.toString());

        entityBlueprint.getForeignKeyListFields().forEach(this::buildInsertKeysFromForeignTableSql);

        final List<AggregateEntityBlueprint> childParentBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        childParentBlueprints.addAll(parentBlueprints);
        childParentBlueprints.add(entityBlueprint);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildInsertSqlRecursive(entityField.getChildEntityBlueprint(), childParentBlueprints));
    }

    private void buildInsertClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint entityBlueprint)
    {
        sqlBuilder.append(String.format("INSERT INTO `%s`", entityBlueprint.getTableName()));
    }

    private void buildValuesClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint entityBlueprint)
    {
        sqlBuilder.append("\n(");
        List<ColumnBlueprint> columnBlueprints = entityBlueprint.getColumnsForInsertStatement();
        int index = 0;

        for(ColumnBlueprint columnBlueprint : columnBlueprints)
        {
            sqlBuilder.append(String.format("`%s`%s",
                columnBlueprint.getColumnName(),
                index < columnBlueprints.size() - 1 ? ", " : ""
            ));
            index++;
        }

        sqlBuilder.append(")\nVALUES\n(");
        index = 0;

        for(ColumnBlueprint columnBlueprint : columnBlueprints)
        {
            sqlBuilder.append(String.format("?%s",
                index < columnBlueprints.size() - 1 ? ", " : ""
            ));
            index++;
        }

        sqlBuilder.append(")");
    }

    private void buildInsertKeysFromForeignTableSql(FieldBlueprint fieldBlueprint)
    {
        ForeignKeyListBlueprint foreignKeyListBlueprint = fieldBlueprint.getForeignKeyListBlueprint();

        String sql = String.format("INSERT INTO `%s` (`%s`, `%s`) VALUES (?, ?)",
            foreignKeyListBlueprint.getForeignTableName(),
            foreignKeyListBlueprint.getForeignTableKeyColumnName(),
            foreignKeyListBlueprint.getForeignTableJoinColumnName()
        );

        foreignKeyListBlueprint.setInsertSql(sql);
    }
}
