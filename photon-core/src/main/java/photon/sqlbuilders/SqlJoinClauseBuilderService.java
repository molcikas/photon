package photon.sqlbuilders;

import photon.blueprints.AggregateEntityBlueprint;

import java.util.*;

public class SqlJoinClauseBuilderService
{
    public void buildJoinClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint entityBlueprint, List<AggregateEntityBlueprint> parentEntities)
    {
        AggregateEntityBlueprint childEntityBlueprint = entityBlueprint;
        for(AggregateEntityBlueprint parentEntityBlueprint : parentEntities)
        {
            sqlBuilder.append(String.format("\nJOIN `%s` ON `%s`.`%s` = `%s`.`%s`",
                parentEntityBlueprint.getTableName(),
                parentEntityBlueprint.getTableName(),
                parentEntityBlueprint.getPrimaryKeyColumnName(),
                childEntityBlueprint.getTableName(),
                childEntityBlueprint.getForeignKeyToParentColumnName()
            ));
            childEntityBlueprint = parentEntityBlueprint;
        }
    }
}
