package photon.sqlbuilders;

import photon.blueprints.AggregateEntityBlueprint;

import java.util.*;

public class DeleteSqlBuilderService
{
    public Map<AggregateEntityBlueprint, String> buildDeleteChildrenExceptSqlTemplates(AggregateEntityBlueprint aggregateRootEntityBlueprint)
    {
        Map<AggregateEntityBlueprint, String> entityDeleteChildrenExceptSqlMap = new HashMap<>();
        buildDeleteChildrenExceptSqlRecursive(aggregateRootEntityBlueprint, Collections.emptyList(), entityDeleteChildrenExceptSqlMap);
        return entityDeleteChildrenExceptSqlMap;
    }

    private void buildDeleteChildrenExceptSqlRecursive(
        AggregateEntityBlueprint entityBlueprint,
        List<AggregateEntityBlueprint> parentBlueprints,
        Map<AggregateEntityBlueprint, String> entityChildrenExceptSqlMap)
    {
        String sql = String.format("DELETE FROM `%s` WHERE `%s`.`%s` = ? AND `%s`.`%s` NOT IN (?)",
            entityBlueprint.getTableName(),
            entityBlueprint.getTableName(),
            entityBlueprint.getForeignKeyToParentColumnName(),
            entityBlueprint.getTableName(),
            entityBlueprint.getPrimaryKeyColumnName()
        );

        entityChildrenExceptSqlMap.put(entityBlueprint, sql);

        final List<AggregateEntityBlueprint> childParentBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        childParentBlueprints.addAll(parentBlueprints);
        childParentBlueprints.add(entityBlueprint);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildDeleteChildrenExceptSqlRecursive(entityField.getChildEntityBlueprint(), childParentBlueprints, entityChildrenExceptSqlMap));
    }


}
