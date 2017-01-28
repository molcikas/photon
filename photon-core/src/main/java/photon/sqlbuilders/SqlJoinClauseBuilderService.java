package photon.sqlbuilders;

import photon.blueprints.EntityBlueprint;

import java.util.*;

public class SqlJoinClauseBuilderService
{
    public void buildJoinClauseSql(StringBuilder sqlBuilder, EntityBlueprint entityBlueprint, List<EntityBlueprint> parentEntities)
    {
        EntityBlueprint childEntityBlueprint = entityBlueprint;
        for(EntityBlueprint parentEntityBlueprint : parentEntities)
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
