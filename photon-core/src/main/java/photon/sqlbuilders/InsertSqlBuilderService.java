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
        Map<EntityBlueprint, String> entitySqlMap)
    {
        int initialCapacity = entityBlueprint.getColumns().size() * 16 + 64;
        StringBuilder sqlBuilder = new StringBuilder(initialCapacity);

        buildInsertClauseSql(sqlBuilder, entityBlueprint);
        buildValuesClauseSql(sqlBuilder, entityBlueprint);

        entitySqlMap.put(entityBlueprint, sqlBuilder.toString());

        //System.out.println(sqlBuilder.toString());

        final List<EntityBlueprint> childParentBlueprints = new ArrayList<>(parentBlueprints.size() + 1);
        childParentBlueprints.addAll(parentBlueprints);
        childParentBlueprints.add(entityBlueprint);
        entityBlueprint
            .getFieldsWithChildEntities()
            .forEach(entityField -> buildInsertSqlRecursive(entityField.getChildEntityBlueprint(), childParentBlueprints, entitySqlMap));
    }

    private void buildInsertClauseSql(StringBuilder sqlBuilder, EntityBlueprint entityBlueprint)
    {
        sqlBuilder.append(String.format("INSERT INTO `%s`", entityBlueprint.getTableName()));
    }

    private void buildValuesClauseSql(StringBuilder sqlBuilder, EntityBlueprint entityBlueprint)
    {
        sqlBuilder.append("\n(");
        List<ColumnBlueprint> columnBlueprints = entityBlueprint.getColumnsForInsertStatement();
        int index = 0;

        for(ColumnBlueprint columnBlueprint : columnBlueprints)
        {
            sqlBuilder.append(String.format("`%s`%s",
                columnBlueprint.getColumnName(),
                index < columnBlueprints.size() - 1 ? ", " : ""
            ));
            index++;
        }

        sqlBuilder.append(")\nVALUES\n(");
        index = 0;

        for(ColumnBlueprint columnBlueprint : columnBlueprints)
        {
            sqlBuilder.append(String.format("?%s",
                index < columnBlueprints.size() - 1 ? ", " : ""
            ));
            index++;
        }

        sqlBuilder.append(")");
    }
}
