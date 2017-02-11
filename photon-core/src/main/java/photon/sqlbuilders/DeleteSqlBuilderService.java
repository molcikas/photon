package photon.sqlbuilders;

import photon.blueprints.AggregateEntityBlueprint;
import photon.blueprints.FieldBlueprint;
import photon.blueprints.ForeignKeyListBlueprint;

public class DeleteSqlBuilderService
{
    public void buildDeleteSqlTemplates(AggregateEntityBlueprint aggregateRootEntityBlueprint)
    {
        buildDeleteSqlTemplatesRecursive(aggregateRootEntityBlueprint);
    }

    private void buildDeleteSqlTemplatesRecursive(
        AggregateEntityBlueprint entityBlueprint)
    {
        String deleteSql = String.format("DELETE FROM `%s` WHERE `%s` IN (?)",
            entityBlueprint.getTableName(),
            entityBlueprint.getPrimaryKeyColumnName()
        );

        entityBlueprint.setDeleteSql(deleteSql);

        String deleteChildrenExceptSql = String.format("DELETE FROM `%s` WHERE `%s` = ? AND `%s` NOT IN (?)",
            entityBlueprint.getTableName(),
            entityBlueprint.getForeignKeyToParentColumnName(),
            entityBlueprint.getPrimaryKeyColumnName()
        );

        entityBlueprint.setDeleteChildrenExceptSql(deleteChildrenExceptSql);

        entityBlueprint.getForeignKeyListFields().forEach(this::buildDeleteKeysFromForeignTableSql);

        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildDeleteSqlTemplatesRecursive(entityField.getChildEntityBlueprint()));
    }

    private void buildDeleteKeysFromForeignTableSql(FieldBlueprint fieldBlueprint)
    {
        ForeignKeyListBlueprint foreignKeyListBlueprint = fieldBlueprint.getForeignKeyListBlueprint();

        String deleteSql = String.format("DELETE FROM `%s` WHERE `%s` IN (?)",
            foreignKeyListBlueprint.getForeignTableName(),
            foreignKeyListBlueprint.getForeignTableJoinColumnName()
        );

        foreignKeyListBlueprint.setDeleteSql(deleteSql);

        String deleteForeignKeysSql = String.format("DELETE FROM `%s` WHERE `%s` IN (?) AND `%s` = ?",
            foreignKeyListBlueprint.getForeignTableName(),
            foreignKeyListBlueprint.getForeignTableKeyColumnName(),
            foreignKeyListBlueprint.getForeignTableJoinColumnName()
        );

        foreignKeyListBlueprint.setDeleteForeignKeysSql(deleteForeignKeysSql);
    }
}
