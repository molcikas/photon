package com.github.molcikas.photon.sqlbuilders;

import com.github.molcikas.photon.blueprints.table.TableBlueprint;

import java.util.*;

public final class SqlJoinClauseBuilderService
{
    public static void buildChildToParentJoinClauseSql(
        StringBuilder sqlBuilder,
        TableBlueprint tableBlueprint,
        boolean alwaysUseInnerJoins)
    {
        while(tableBlueprint.getParentTableBlueprint() != null)
        {
            sqlBuilder.append(String.format("\n%s [%s] ON [%s].[%s] = [%s].[%s]",
                alwaysUseInnerJoins ? "JOIN" : tableBlueprint.getJoinType().getJoinSql(),
                tableBlueprint.getParentTableBlueprint().getTableName(),
                tableBlueprint.getParentTableBlueprint().getTableName(),
                tableBlueprint.getParentTableBlueprint().getPrimaryKeyColumnName(),
                tableBlueprint.getTableName(),
                tableBlueprint.getForeignKeyToParentColumn().getColumnName()
            ));
            tableBlueprint = tableBlueprint.getParentTableBlueprint();
        }
    }

    public static void buildParentToEachChildJoinClauseSql(
        StringBuilder sqlBuilder,
        TableBlueprint parentTableBlueprint,
        List<TableBlueprint> childTableBlueprints)
    {
        for(TableBlueprint childTableBlueprint : childTableBlueprints)
        {
            sqlBuilder.append(String.format("\n%s [%s] ON [%s].[%s] = [%s].[%s]",
                childTableBlueprint.getJoinType().getJoinSql(),
                childTableBlueprint.getTableName(),
                childTableBlueprint.getTableName(),
                childTableBlueprint.getForeignKeyToParentColumn().getColumnName(),
                parentTableBlueprint.getTableName(),
                parentTableBlueprint.getPrimaryKeyColumnName()
            ));
        }
    }
}
