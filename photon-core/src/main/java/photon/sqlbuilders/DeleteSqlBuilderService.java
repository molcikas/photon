package photon.sqlbuilders;

import photon.blueprints.ColumnBlueprint;
import photon.blueprints.EntityBlueprint;

import java.util.*;
import java.util.stream.Collectors;

public class DeleteSqlBuilderService
{
    public Map<EntityBlueprint, String> buildDeleteAllChildrenSqlTemplates(EntityBlueprint aggregateRootEntityBlueprint)
    {
        Map<EntityBlueprint, String> entityDeleteAllChildrenSqlMap = new HashMap<>();
        buildDeleteAllChildrenSqlRecursive(aggregateRootEntityBlueprint, Collections.emptyList(), entityDeleteAllChildrenSqlMap);
        return entityDeleteAllChildrenSqlMap;
    }

    public Map<EntityBlueprint, String> buildDeleteChildrenExceptSqlTemplates(EntityBlueprint aggregateRootEntityBlueprint)
    {
        Map<EntityBlueprint, String> entityDeleteChildrenExceptSqlMap = new HashMap<>();
        buildDeleteChildrenExceptSqlRecursive(aggregateRootEntityBlueprint, Collections.emptyList(), entityDeleteChildrenExceptSqlMap);
        return entityDeleteChildrenExceptSqlMap;
    }

    private void buildDeleteAllChildrenSqlRecursive(
        EntityBlueprint entityBlueprint,
        List<EntityBlueprint> parentBlueprints,
        Map<EntityBlueprint, String> entityDeleteAllChildrenSqlMap)
    {
        String sql = String.format("DELETE FROM `%s` WHERE `%s`.`%s` = ?",
            entityBlueprint.getTableName(),
            entityBlueprint.getTableName(),
            entityBlueprint.getForeignKeyToParentColumnName()
        );

        entityDeleteAllChildrenSqlMap.put(entityBlueprint, sql);

        final List<EntityBlueprint> childParentBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        childParentBlueprints.addAll(parentBlueprints);
        childParentBlueprints.add(entityBlueprint);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildDeleteAllChildrenSqlRecursive(entityField.getChildEntityBlueprint(), childParentBlueprints, entityDeleteAllChildrenSqlMap));
    }

    private void buildDeleteChildrenExceptSqlRecursive(
        EntityBlueprint entityBlueprint,
        List<EntityBlueprint> parentBlueprints,
        Map<EntityBlueprint, String> entityChildrenExceptSqlMap)
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

        final List<EntityBlueprint> childParentBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        childParentBlueprints.addAll(parentBlueprints);
        childParentBlueprints.add(entityBlueprint);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildDeleteChildrenExceptSqlRecursive(entityField.getChildEntityBlueprint(), childParentBlueprints, entityChildrenExceptSqlMap));
    }


}
