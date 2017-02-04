package photon.sqlbuilders;

import photon.blueprints.AggregateEntityBlueprint;

import java.util.*;

public class DeleteSqlBuilderService
{
    public Map<AggregateEntityBlueprint, String> buildDeleteAllChildrenSqlTemplates(AggregateEntityBlueprint aggregateRootEntityBlueprint)
    {
        Map<AggregateEntityBlueprint, String> entityDeleteAllChildrenSqlMap = new HashMap<>();
        buildDeleteAllChildrenSqlRecursive(aggregateRootEntityBlueprint, Collections.emptyList(), entityDeleteAllChildrenSqlMap);
        return entityDeleteAllChildrenSqlMap;
    }

    public Map<AggregateEntityBlueprint, String> buildDeleteChildrenExceptSqlTemplates(AggregateEntityBlueprint aggregateRootEntityBlueprint)
    {
        Map<AggregateEntityBlueprint, String> entityDeleteChildrenExceptSqlMap = new HashMap<>();
        buildDeleteChildrenExceptSqlRecursive(aggregateRootEntityBlueprint, Collections.emptyList(), entityDeleteChildrenExceptSqlMap);
        return entityDeleteChildrenExceptSqlMap;
    }

    private void buildDeleteAllChildrenSqlRecursive(
        AggregateEntityBlueprint entityBlueprint,
        List<AggregateEntityBlueprint> parentBlueprints,
        Map<AggregateEntityBlueprint, String> entityDeleteAllChildrenSqlMap)
    {
        String sql = String.format("DELETE FROM `%s` WHERE `%s`.`%s` = ?",
            entityBlueprint.getTableName(),
            entityBlueprint.getTableName(),
            entityBlueprint.getForeignKeyToParentColumnName()
        );

        entityDeleteAllChildrenSqlMap.put(entityBlueprint, sql);

        final List<AggregateEntityBlueprint> childParentBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        childParentBlueprints.addAll(parentBlueprints);
        childParentBlueprints.add(entityBlueprint);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildDeleteAllChildrenSqlRecursive(entityField.getChildEntityBlueprint(), childParentBlueprints, entityDeleteAllChildrenSqlMap));
    }

    private void buildDeleteChildrenExceptSqlRecursive(
        AggregateEntityBlueprint entityBlueprint,
        List<AggregateEntityBlueprint> parentBlueprints,
        Map<AggregateEntityBlueprint, String> entityChildrenExceptSqlMap)
    {
        String sql = String.format("DELETE FROM `%s` WHERE `%s`.`%s` = ? AND `%s`.`%s` NOT IN (%s)",
            entityBlueprint.getTableName(),
            entityBlueprint.getTableName(),
            entityBlueprint.getForeignKeyToParentColumnName(),
            entityBlueprint.getTableName(),
            entityBlueprint.getPrimaryKeyColumnName(),
            "%s" // Placeholder for question marks for the number of child ids.
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
