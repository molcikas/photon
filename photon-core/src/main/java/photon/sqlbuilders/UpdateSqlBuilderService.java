package photon.sqlbuilders;

import photon.blueprints.ColumnBlueprint;
import photon.blueprints.AggregateEntityBlueprint;

import java.util.*;
import java.util.stream.Collectors;

public class UpdateSqlBuilderService
{
    public void buildUpdateSqlTemplates(AggregateEntityBlueprint aggregateRootEntityBlueprint)
    {
        buildUpdateSqlRecursive(aggregateRootEntityBlueprint, Collections.emptyList());
    }

    private void buildUpdateSqlRecursive(
        AggregateEntityBlueprint entityBlueprint,
        List<AggregateEntityBlueprint> parentBlueprints)
    {
        int initialCapacity = entityBlueprint.getColumns().size() * 16 + 64;
        StringBuilder sqlBuilder = new StringBuilder(initialCapacity);

        buildUpdateClauseSql(sqlBuilder, entityBlueprint);
        buildSetClauseSql(sqlBuilder, entityBlueprint);
        buildWhereClauseSql(sqlBuilder, entityBlueprint);

        entityBlueprint.setUpdateSql(sqlBuilder.toString());

        //System.out.println(sqlBuilder.toString());

        final List<AggregateEntityBlueprint> childParentBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        childParentBlueprints.addAll(parentBlueprints);
        childParentBlueprints.add(entityBlueprint);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildUpdateSqlRecursive(entityField.getChildEntityBlueprint(), childParentBlueprints));
    }

    private void buildUpdateClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint entityBlueprint)
    {
        sqlBuilder.append(String.format("UPDATE `%s`", entityBlueprint.getTableName()));
    }

    private void buildSetClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint entityBlueprint)
    {
        sqlBuilder.append("\nSET ");

        Collection<ColumnBlueprint> columnBlueprints = entityBlueprint
            .getColumns()
            .stream()
            .filter(c -> !c.isPrimaryKeyColumn())
            .sorted((c1, c2) -> c1.getColumnIndex() - c2.getColumnIndex())
            .collect(Collectors.toList());
        int collectionIndex = 0;

        for(ColumnBlueprint columnBlueprint : columnBlueprints)
        {
            sqlBuilder.append(String.format("`%s`.`%s` = ?%s",
                entityBlueprint.getTableName(),
                columnBlueprint.getColumnName(),
                collectionIndex < columnBlueprints.size() - 1 ? ", " : ""
            ));
            collectionIndex++;
        }
    }

    private void buildWhereClauseSql(StringBuilder sqlBuilder, AggregateEntityBlueprint entityBlueprint)
    {
        sqlBuilder.append(String.format("\nWHERE `%s`.`%s` = ?",
            entityBlueprint.getTableName(),
            entityBlueprint.getPrimaryKeyColumnName()
        ));
    }
}
