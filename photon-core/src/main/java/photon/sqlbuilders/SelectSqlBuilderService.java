package photon.sqlbuilders;

import org.apache.commons.lang3.StringUtils;
import photon.blueprints.ColumnBlueprint;
import photon.blueprints.EntityBlueprint;

import java.util.*;

public class SelectSqlBuilderService
{
    private final SqlJoinClauseBuilderService sqlJoinClauseBuilderService;

    public SelectSqlBuilderService(SqlJoinClauseBuilderService sqlJoinClauseBuilderService)
    {
        this.sqlJoinClauseBuilderService = sqlJoinClauseBuilderService;
    }

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
        StringBuilder sqlBuilder = new StringBuilder(initialCapacity);

        buildSelectClauseSql(sqlBuilder, entityBlueprint);
        buildFromClauseSql(sqlBuilder, entityBlueprint);
        sqlJoinClauseBuilderService.buildJoinClauseSql(sqlBuilder, entityBlueprint, parentBlueprints);
        buildWhereClauseSql(sqlBuilder, aggregateRootEntityBlueprint);
        buildOrderBySql(sqlBuilder, entityBlueprint, parentBlueprints);

        entitySelectSqlMap.put(entityBlueprint, sqlBuilder.toString());

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

    private void buildSelectClauseSql(StringBuilder sqlBuilder, EntityBlueprint entityBlueprint)
    {
        sqlBuilder.append("SELECT " );

        for(ColumnBlueprint columnBlueprint : entityBlueprint.getColumns())
        {
            sqlBuilder.append(String.format("`%s`.`%s`%s",
                entityBlueprint.getTableName(),
                columnBlueprint.getColumnName(),
                columnBlueprint.getColumnIndex() < entityBlueprint.getColumns().size() - 1 ? ", " : ""
            ));
        }
    }

    private void buildFromClauseSql(StringBuilder sqlBuilder, EntityBlueprint entityBlueprint)
    {
        sqlBuilder.append(String.format("\nFROM `%s`", entityBlueprint.getTableName()));
    }

    private void buildWhereClauseSql(StringBuilder sqlBuilder, EntityBlueprint aggregateRootEntityBlueprint)
    {
        sqlBuilder.append(String.format("\nWHERE `%s`.`%s` IN (%s)",
            aggregateRootEntityBlueprint.getTableName(),
            aggregateRootEntityBlueprint.getPrimaryKeyColumnName(),
            "%s" // Leave a marker for JDBC question marks to be inserted.
        ));
    }

    private void buildOrderBySql(
        StringBuilder sqlBuilder,
        EntityBlueprint childBlueprint,
        List<EntityBlueprint> parentBlueprints)
    {
        List<EntityBlueprint> entityBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        entityBlueprints.addAll(parentBlueprints);
        entityBlueprints.add(childBlueprint);

        sqlBuilder.append("\nORDER BY ");

        for (EntityBlueprint entityBlueprint : entityBlueprints)
        {
            boolean isPrimaryKeySort = StringUtils.equals(entityBlueprint.getOrderByColumnName(), entityBlueprint.getPrimaryKeyColumnName());
            sqlBuilder.append(String.format("`%s`.`%s` %s%s",
                entityBlueprint.getTableName(),
                entityBlueprint.getOrderByColumnName(),
                entityBlueprint.getOrderByDirection().sqlSortDirection,
                isPrimaryKeySort && entityBlueprint.equals(childBlueprint) ? "" : ", "
            ));
            if(!isPrimaryKeySort)
            {
                // If it's not a primary key sort, we need to add the primary key as a secondary sort, otherwise the
                // entity connecting might fail because it expects entities to be sorted by parent.
                sqlBuilder.append(String.format("`%s`.`%s`%s",
                    entityBlueprint.getTableName(),
                    entityBlueprint.getPrimaryKeyColumnName(),
                    entityBlueprint.equals(childBlueprint) ? "" : ", "
                ));
            }
        }
    }
}
