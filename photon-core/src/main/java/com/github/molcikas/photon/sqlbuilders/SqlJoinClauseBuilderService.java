package com.github.molcikas.photon.sqlbuilders;

import com.github.molcikas.photon.blueprints.TableBlueprint;

import java.util.*;

public final class SqlJoinClauseBuilderService
{
    public static void buildChildToParentJoinClauseSql(
        StringBuilder sqlBuilder,
        TableBlueprint childTableBlueprint,
        List<TableBlueprint> parentTableBlueprints)
    {
        TableBlueprint nextChildTableBlueprint = childTableBlueprint;
        for(TableBlueprint parentTableBlueprint : parentTableBlueprints)
        {
            sqlBuilder.append(String.format("\nJOIN [%s] ON [%s].[%s] = [%s].[%s]",
                parentTableBlueprint.getTableName(),
                parentTableBlueprint.getTableName(),
                parentTableBlueprint.getPrimaryKeyColumnName(),
                nextChildTableBlueprint.getTableName(),
                nextChildTableBlueprint.getForeignKeyToParentColumnName()
            ));
            nextChildTableBlueprint = parentTableBlueprint;
        }
    }

    public static void buildParentToChildJoinClauseSql(
        StringBuilder sqlBuilder,
        TableBlueprint parentTableBlueprint,
        List<TableBlueprint> childTableBlueprints)
    {
        TableBlueprint nextParentTableBlueprint = parentTableBlueprint;
        for(TableBlueprint childTableBlueprint : childTableBlueprints)
        {
            sqlBuilder.append(String.format("\nJOIN [%s] ON [%s].[%s] = [%s].[%s]",
                childTableBlueprint.getTableName(),
                childTableBlueprint.getTableName(),
                childTableBlueprint.getForeignKeyToParentColumnName(),
                nextParentTableBlueprint.getTableName(),
                nextParentTableBlueprint.getPrimaryKeyColumnName()
            ));
            nextParentTableBlueprint = childTableBlueprint;
        }
    }
}
