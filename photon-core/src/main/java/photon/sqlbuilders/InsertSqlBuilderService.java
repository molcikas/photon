package photon.sqlbuilders;

import photon.blueprints.ColumnBlueprint;
import photon.blueprints.EntityBlueprint;

import java.util.*;

public class InsertSqlBuilderService
{
    public Map<EntityBlueprint, String> buildInsertSqlTemplates(EntityBlueprint aggregateRootEntityBlueprint)
    {
        Map<EntityBlueprint, String> entityUpdateSqlMap = new HashMap<>();
        buildInsertSqlRecursive(aggregateRootEntityBlueprint, Collections.emptyList(), entityUpdateSqlMap);
        return entityUpdateSqlMap;
    }

    private void buildInsertSqlRecursive(
        EntityBlueprint entityBlueprint,
        List<EntityBlueprint> parentBlueprints,
        Map<EntityBlueprint, String> entityUpdateSqlMap)
    {
        int initialCapacity = entityBlueprint.getColumns().size() * 16 + 64;
        StringBuilder sqlBuilder = new StringBuilder(initialCapacity);

        buildInsertClauseSql(sqlBuilder, entityBlueprint);
        buildValuesClauseSql(sqlBuilder, entityBlueprint);

        entityUpdateSqlMap.put(entityBlueprint, sqlBuilder.toString());

        //System.out.println(sqlBuilder.toString());

        final List<EntityBlueprint> childParentBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        childParentBlueprints.addAll(parentBlueprints);
        childParentBlueprints.add(entityBlueprint);
        entityBlueprint
            .getFields()
            .values()
            .stream()
            .filter(entityField -> entityField.getChildEntityBlueprint() != null)
            .forEach(entityField -> buildInsertSqlRecursive(entityField.getChildEntityBlueprint(), childParentBlueprints, entityUpdateSqlMap));
    }

    private void buildInsertClauseSql(StringBuilder sqlBuilder, EntityBlueprint entityBlueprint)
    {
        sqlBuilder.append(String.format("INSERT INTO `%s`", entityBlueprint.getTableName()));
    }

    private void buildValuesClauseSql(StringBuilder sqlBuilder, EntityBlueprint entityBlueprint)
    {
        sqlBuilder.append("\n(");

        for(ColumnBlueprint columnBlueprint : entityBlueprint.getColumns())
        {
            sqlBuilder.append(String.format("`%s`%s",
                columnBlueprint.getColumnName(),
                columnBlueprint.getColumnIndex() < entityBlueprint.getColumns().size() - 1 ? ", " : ""
            ));
        }

        sqlBuilder.append(")\nVALUES\n(");

        for(ColumnBlueprint columnBlueprint : entityBlueprint.getColumns())
        {
            sqlBuilder.append(String.format("?%s",
                columnBlueprint.getColumnIndex() < entityBlueprint.getColumns().size() - 1 ? ", " : ""
            ));
        }

        sqlBuilder.append(")");
    }
}
