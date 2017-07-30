package com.github.molcikas.photon.sqlbuilders;

import com.github.molcikas.photon.blueprints.TableBlueprint;

import java.util.*;

public final class SqlJoinClauseBuilderService
{
    public static void buildJoinClauseSql(
        StringBuilder sqlBuilder,
        TableBlueprint tableBlueprint,
        List<TableBlueprint> parentTableBlueprints)
    {
        TableBlueprint childTableBlueprint = tableBlueprint;
        for(TableBlueprint parentTableBlueprint : parentTableBlueprints)
        {
            sqlBuilder.append(String.format("\nJOIN [%s] ON [%s].[%s] = [%s].[%s]",
                parentTableBlueprint.getTableName(),
                parentTableBlueprint.getTableName(),
                parentTableBlueprint.getPrimaryKeyColumnName(),
                childTableBlueprint.getTableName(),
                childTableBlueprint.getForeignKeyToParentColumnName()
            ));
            childTableBlueprint = parentTableBlueprint;
        }
    }
}
