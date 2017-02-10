package photon.sqlbuilders;

import photon.blueprints.AggregateEntityBlueprint;
import photon.blueprints.FieldBlueprint;
import photon.blueprints.ForeignKeyListBlueprint;

import java.util.*;

public class DeleteSqlBuilderService
{
    public void buildDeleteChildrenExceptSqlTemplates(AggregateEntityBlueprint aggregateRootEntityBlueprint)
    {
        buildDeleteChildrenExceptSqlRecursive(aggregateRootEntityBlueprint, Collections.emptyList());
    }

    private void buildDeleteChildrenExceptSqlRecursive(
        AggregateEntityBlueprint entityBlueprint,
        List<AggregateEntityBlueprint> parentBlueprints)
    {
        String sql = String.format("DELETE FROM `%s` WHERE `%s`.`%s` = ? AND `%s`.`%s` NOT IN (?)",
            entityBlueprint.getTableName(),
            entityBlueprint.getTableName(),
            entityBlueprint.getForeignKeyToParentColumnName(),
            entityBlueprint.getTableName(),
            entityBlueprint.getPrimaryKeyColumnName()
        );

        entityBlueprint.setDeleteChildrenExceptSql(sql);

        entityBlueprint.getForeignKeyListFields().forEach(this::buildDeleteKeysFromForeignTableSql);

        final List<AggregateEntityBlueprint> childParentBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        childParentBlueprints.addAll(parentBlueprints);
        childParentBlueprints.add(entityBlueprint);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildDeleteChildrenExceptSqlRecursive(entityField.getChildEntityBlueprint(), childParentBlueprints));
    }

    private void buildDeleteKeysFromForeignTableSql(FieldBlueprint fieldBlueprint)
    {
        ForeignKeyListBlueprint foreignKeyListBlueprint = fieldBlueprint.getForeignKeyListBlueprint();

        String sql = String.format("DELETE FROM `%s` WHERE `%s` IN (?) AND `%s` = ?",
            foreignKeyListBlueprint.getForeignTableName(),
            foreignKeyListBlueprint.getForeignTableKeyColumnName(),
            foreignKeyListBlueprint.getForeignTableJoinColumnName()
        );

        foreignKeyListBlueprint.setDeleteSql(sql);
    }
}
