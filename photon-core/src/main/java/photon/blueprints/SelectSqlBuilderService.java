package photon.blueprints;

import org.apache.commons.lang3.StringUtils;
import photon.blueprints.ColumnBlueprint;
import photon.blueprints.EntityBlueprint;

import java.util.*;

public class SelectSqlBuilderService
{
    public Map<EntityBlueprint, String> buildSelectSqlTemplates(EntityBlueprint aggregateRootEntityBlueprint)
    {
        Map<EntityBlueprint, String> entitySelectSqlMap = new HashMap<>();
        buildSelectSqlRecursive(aggregateRootEntityBlueprint, aggregateRootEntityBlueprint, Collections.emptyList(), entitySelectSqlMap);
        return entitySelectSqlMap;
    }

    private void buildSelectSqlRecursive(
        EntityBlueprint entityBlueprint,
        EntityBlueprint aggregateRootEntityBlueprint,
        List<EntityBlueprint> parentBlueprints,
        Map<EntityBlueprint, String> entitySelectSqlMap)
    {
        int initialCapacity = entityBlueprint.getColumns().size() * 16 + 64;
        StringBuilder selectSqlBuilder = new StringBuilder(initialCapacity);

        buildSelectClauseSql(selectSqlBuilder, entityBlueprint);
        buildFromAndJoinClauseSql(selectSqlBuilder, entityBlueprint, parentBlueprints);
        buildWhereClauseSql(selectSqlBuilder, aggregateRootEntityBlueprint);
        buildOrderBySql(selectSqlBuilder, entityBlueprint, parentBlueprints);

        entitySelectSqlMap.put(entityBlueprint, selectSqlBuilder.toString());

        final List<EntityBlueprint> childParentBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        childParentBlueprints.addAll(parentBlueprints);
        childParentBlueprints.add(entityBlueprint);
        entityBlueprint
            .getFields()
            .values()
            .stream()
            .filter(entityField -> entityField.getChildEntityBlueprint() != null)
            .forEach(entityField -> buildSelectSqlRecursive(entityField.getChildEntityBlueprint(), aggregateRootEntityBlueprint, childParentBlueprints, entitySelectSqlMap));
    }

    private void buildSelectClauseSql(StringBuilder selectSqlBuilder, EntityBlueprint entityBlueprint)
    {
        Map<String, ColumnBlueprint> columns = entityBlueprint.getColumns();

        selectSqlBuilder.append("SELECT " );

        int columnIndex = 0;
        for(ColumnBlueprint columnBlueprint : entityBlueprint.getColumns().values())
        {
            selectSqlBuilder.append(String.format("`%s`.`%s`%s",
                entityBlueprint.getTableName(),
                columnBlueprint.getColumnName(),
                columnIndex < columns.size() - 1 ? ", " : ""
            ));
            columnIndex++;
        }
    }

    private void buildFromAndJoinClauseSql(StringBuilder selectSqlBuilder, EntityBlueprint entityBlueprint, List<EntityBlueprint> parentEntities)
    {
        selectSqlBuilder.append("\nFROM `").append(entityBlueprint.getEntityClass().getSimpleName().toLowerCase()).append("`");

        EntityBlueprint childEntityBlueprint = entityBlueprint;
        for(EntityBlueprint parentEntityBlueprint : parentEntities)
        {
            selectSqlBuilder.append(String.format("\nJOIN `%s` ON `%s`.`%s` = `%s`.`%s`",
                parentEntityBlueprint.getTableName(),
                parentEntityBlueprint.getTableName(),
                parentEntityBlueprint.getPrimaryKeyColumnName(),
                childEntityBlueprint.getTableName(),
                childEntityBlueprint.getForeignKeyToParentColumnName()
            ));
            childEntityBlueprint = parentEntityBlueprint;
        }
    }

    private void buildWhereClauseSql(StringBuilder selectSqlBuilder, EntityBlueprint aggregateRootEntityBlueprint)
    {
        selectSqlBuilder.append(String.format("\nWHERE `%s`.`%s` IN (%s)",
            aggregateRootEntityBlueprint.getTableName(),
            aggregateRootEntityBlueprint.getPrimaryKeyColumnName(),
            "%s" // Leave a marker for JDBC question marks to be inserted.
        ));
    }

    private void buildOrderBySql(
        StringBuilder selectSqlBuilder,
        EntityBlueprint childBlueprint,
        List<EntityBlueprint> parentBlueprints)
    {
        List<EntityBlueprint> entityBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        entityBlueprints.addAll(parentBlueprints);
        entityBlueprints.add(childBlueprint);

        selectSqlBuilder.append("\nORDER BY ");

        for (EntityBlueprint entityBlueprint : entityBlueprints)
        {
            boolean isPrimaryKeySort = StringUtils.equals(entityBlueprint.getOrderByColumnName(), entityBlueprint.getPrimaryKeyColumnName());
            selectSqlBuilder.append(String.format("`%s`.`%s` %s%s",
                entityBlueprint.getTableName(),
                entityBlueprint.getOrderByColumnName(),
                entityBlueprint.getOrderByDirection().sqlSortDirection,
                isPrimaryKeySort && entityBlueprint.equals(childBlueprint) ? "" : ", "
            ));
            if(!isPrimaryKeySort)
            {
                // If it's not a primary key sort, we need to add the primary key as a secondary sort, otherwise the
                // building might fail because it expects entities to be grouped by parent.
                selectSqlBuilder.append(String.format("`%s`.`%s`%s",
                    entityBlueprint.getTableName(),
                    entityBlueprint.getPrimaryKeyColumnName(),
                    entityBlueprint.equals(childBlueprint) ? "" : ", "
                ));
            }
        }
    }
}
