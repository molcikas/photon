package com.github.molcikas.photon.sqlbuilders;

import com.github.molcikas.photon.blueprints.TableBlueprint;

import java.util.*;

public final class SqlJoinClauseBuilderService
{
    public static void buildChildToParentJoinClauseSql(
        StringBuilder sqlBuilder,
        TableBlueprint tableBlueprint)
    {
        while(tableBlueprint.getParentTableBlueprint() != null)
        {
            sqlBuilder.append(String.format("\nJOIN [%s] ON [%s].[%s] = [%s].[%s]",
                tableBlueprint.getParentTableBlueprint().getTableName(),
                tableBlueprint.getParentTableBlueprint().getTableName(),
                tableBlueprint.getParentTableBlueprint().getPrimaryKeyColumn().getColumnName(),
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
            sqlBuilder.append(String.format("\nJOIN [%s] ON [%s].[%s] = [%s].[%s]",
                childTableBlueprint.getTableName(),
                childTableBlueprint.getTableName(),
                childTableBlueprint.getForeignKeyToParentColumn().getColumnName(),
                parentTableBlueprint.getTableName(),
                parentTableBlueprint.getPrimaryKeyColumn().getColumnName()
            ));
        }
    }
}
