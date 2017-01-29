package photon.sqlbuilders;

import photon.blueprints.ColumnBlueprint;
import photon.blueprints.EntityBlueprint;

import java.util.*;
import java.util.stream.Collectors;

public class UpdateSqlBuilderService
{
    public Map<EntityBlueprint, String> buildUpdateSqlTemplates(EntityBlueprint aggregateRootEntityBlueprint)
    {
        Map<EntityBlueprint, String> entityUpdateSqlMap = new HashMap<>();
        buildUpdateSqlRecursive(aggregateRootEntityBlueprint, Collections.emptyList(), entityUpdateSqlMap);
        return entityUpdateSqlMap;
    }

    private void buildUpdateSqlRecursive(
        EntityBlueprint entityBlueprint,
        List<EntityBlueprint> parentBlueprints,
        Map<EntityBlueprint, String> entityUpdateSqlMap)
    {
        int initialCapacity = entityBlueprint.getColumns().size() * 16 + 64;
        StringBuilder sqlBuilder = new StringBuilder(initialCapacity);

        buildUpdateClauseSql(sqlBuilder, entityBlueprint);
        buildSetClauseSql(sqlBuilder, entityBlueprint);
        buildWhereClauseSql(sqlBuilder, entityBlueprint);

        entityUpdateSqlMap.put(entityBlueprint, sqlBuilder.toString());

        final List<EntityBlueprint> childParentBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        childParentBlueprints.addAll(parentBlueprints);
        childParentBlueprints.add(entityBlueprint);
        entityBlueprint
            .getFields()
            .values()
            .stream()
            .filter(entityField -> entityField.getChildEntityBlueprint() != null)
            .forEach(entityField -> buildUpdateSqlRecursive(entityField.getChildEntityBlueprint(), childParentBlueprints, entityUpdateSqlMap));
    }

    private void buildUpdateClauseSql(StringBuilder sqlBuilder, EntityBlueprint entityBlueprint)
    {
        sqlBuilder.append(String.format("UPDATE `%s`", entityBlueprint.getTableName()));
    }

    private void buildSetClauseSql(StringBuilder sqlBuilder, EntityBlueprint entityBlueprint)
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

    private void buildWhereClauseSql(StringBuilder sqlBuilder, EntityBlueprint entityBlueprint)
    {
        sqlBuilder.append(String.format("\nWHERE `%s`.`%s` = ?",
            entityBlueprint.getTableName(),
            entityBlueprint.getPrimaryKeyColumnName()
        ));
    }
}
