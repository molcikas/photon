package photon.sqlbuilders;

import org.apache.commons.lang3.StringUtils;
import photon.blueprints.ColumnBlueprint;
import photon.blueprints.AggregateEntityBlueprint;
import photon.blueprints.FieldBlueprint;
import photon.blueprints.ForeignKeyListBlueprint;

import java.util.*;

public class SelectSqlBuilderService
{
    private final SqlJoinClauseBuilderService sqlJoinClauseBuilderService;

    public SelectSqlBuilderService(SqlJoinClauseBuilderService sqlJoinClauseBuilderService)
    {
        this.sqlJoinClauseBuilderService = sqlJoinClauseBuilderService;
    }

    public void buildSelectSqlTemplates(AggregateEntityBlueprint aggregateRootEntityBlueprint)
    {
        buildSelectSqlRecursive(aggregateRootEntityBlueprint, aggregateRootEntityBlueprint, Collections.emptyList());
    }

    private void buildSelectSqlRecursive(
        AggregateEntityBlueprint entityBlueprint,
        AggregateEntityBlueprint aggregateRootEntityBlueprint,
        List<AggregateEntityBlueprint> parentBlueprints)
    {
        int initialCapacity = entityBlueprint.getColumns().size() * 16 + 64;
        StringBuilder sqlBuilder = new StringBuilder(initialCapacity);

        buildSelectClauseSql(sqlBuilder, entityBlueprint);
        buildFromClauseSql(sqlBuilder, entityBlueprint);
        sqlJoinClauseBuilderService.buildJoinClauseSql(sqlBuilder, entityBlueprint, parentBlueprints);
        buildWhereClauseSql(sqlBuilder, aggregateRootEntityBlueprint);
        buildOrderBySql(sqlBuilder, entityBlueprint, parentBlueprints);

        entityBlueprint.setSelectSql(sqlBuilder.toString());

        //System.out.println(sqlBuilder.toString());

        entityBlueprint.getForeignKeyListFields().forEach(this::buildSelectKeysFromForeignTableSql);

        final List<AggregateEntityBlueprint> childParentEntityBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        childParentEntityBlueprints.add(entityBlueprint);
        childParentEntityBlueprints.addAll(parentBlueprints);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildSelectSqlRecursive(entityField.getChildEntityBlueprint(), aggregateRootEntityBlueprint, childParentEntityBlueprints));
    }

    private void buildSelectClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint entityBlueprint)
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

    private void buildFromClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint entityBlueprint)
    {
        sqlBuilder.append(String.format("\nFROM `%s`", entityBlueprint.getTableName()));
    }

    private void buildWhereClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint aggregateRootEntityBlueprint)
    {
        sqlBuilder.append(String.format("\nWHERE `%s`.`%s` IN (?)",
            aggregateRootEntityBlueprint.getTableName(),
            aggregateRootEntityBlueprint.getPrimaryKeyColumnName()
        ));
    }

    private void buildOrderBySql(
        StringBuilder sqlBuilder,
        AggregateEntityBlueprint childBlueprint,
        List<AggregateEntityBlueprint> parentBlueprints)
    {
        List<AggregateEntityBlueprint> entityBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        entityBlueprints.add(childBlueprint);
        entityBlueprints.addAll(parentBlueprints);
        Collections.reverse(entityBlueprints);

        sqlBuilder.append("\nORDER BY ");

        for (AggregateEntityBlueprint entityBlueprint : entityBlueprints)
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

    private void buildSelectKeysFromForeignTableSql(FieldBlueprint fieldBlueprint)
    {
        ForeignKeyListBlueprint foreignKeyListBlueprint = fieldBlueprint.getForeignKeyListBlueprint();

        String sql = String.format("SELECT `%s`, `%s` FROM `%s` WHERE `%s` IN (?) ORDER BY `%s`",
            foreignKeyListBlueprint.getForeignTableKeyColumnName(),
            foreignKeyListBlueprint.getForeignTableJoinColumnName(),
            foreignKeyListBlueprint.getForeignTableName(),
            foreignKeyListBlueprint.getForeignTableJoinColumnName(),
            foreignKeyListBlueprint.getForeignTableJoinColumnName()
        );

        foreignKeyListBlueprint.setSelectSql(sql);
    }
}
