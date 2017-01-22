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
        List<EntityBlueprint> parentEntities,
        Map<EntityBlueprint, String> entitySelectSqlMap)
    {
        int initialCapacity = entityBlueprint.getColumns().size() * 16 + 64;
        StringBuilder selectSqlBuilder = new StringBuilder(initialCapacity);

        buildSelectClauseSql(selectSqlBuilder, entityBlueprint);
        buildFromAndJoinClauseSql(selectSqlBuilder, entityBlueprint, parentEntities);
        buildWhereClauseSql(selectSqlBuilder, aggregateRootEntityBlueprint);
        buildOrderBySql(selectSqlBuilder, entityBlueprint, aggregateRootEntityBlueprint, parentEntities);

        entitySelectSqlMap.put(entityBlueprint, selectSqlBuilder.toString());

        final List<EntityBlueprint> childrenParentEntities = new ArrayList<>(parentEntities.size() + 1);
        childrenParentEntities.addAll(parentEntities);
        childrenParentEntities.add(entityBlueprint);
        entityBlueprint
            .getFields()
            .values()
            .stream()
            .filter(entityField -> entityField.getChildEntityBlueprint() != null)
            .forEach(entityField -> buildSelectSqlRecursive(entityField.getChildEntityBlueprint(), aggregateRootEntityBlueprint, childrenParentEntities, entitySelectSqlMap));
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

    private void buildOrderBySql(StringBuilder selectSqlBuilder, EntityBlueprint entityBlueprint, EntityBlueprint aggregateRootEntityBlueprint, List<EntityBlueprint> parentEntities)
    {
        StringBuilder orderByParentKeys = new StringBuilder();
        if(entityBlueprint.equals(aggregateRootEntityBlueprint))
        {
            orderByParentKeys.append(String.format("`%s`.`%s`",
                entityBlueprint.getTableName(),
                entityBlueprint.getPrimaryKeyColumnName()
            ));
        }
        else
        {
            for (EntityBlueprint parentEntityBlueprint : parentEntities)
            {
                orderByParentKeys.append(String.format("`%s`.`%s`%s",
                    parentEntityBlueprint.getTableName(),
                    parentEntityBlueprint.getPrimaryKeyColumnName(),
                    parentEntityBlueprint.equals(aggregateRootEntityBlueprint) ? "" : ", "
                ));
            }
        }

        selectSqlBuilder.append(String.format("\nORDER BY %s%s",
            orderByParentKeys.toString(),
            getOrderByColumnSql(entityBlueprint)
        ));
    }

    private String getOrderByColumnSql(EntityBlueprint entityBlueprint)
    {
        if(StringUtils.isBlank(entityBlueprint.getOrderByColumnName()))
        {
            return "";
        }
        return String.format(", `%s`.`%s` %s",
            entityBlueprint.getTableName(),
            entityBlueprint.getOrderByColumnName(),
            entityBlueprint.getOrderByDirection().sqlSortDirection
        );
    }
}
