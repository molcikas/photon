package photon.query;

import photon.blueprints.AggregateBlueprint;
import photon.blueprints.ColumnBlueprint;
import photon.blueprints.EntityBlueprint;
import photon.blueprints.FieldBlueprint;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

public class PhotonAggregateSave
{
    private final AggregateBlueprint aggregateBlueprint;
    private final Connection connection;

    public PhotonAggregateSave(
        AggregateBlueprint aggregateBlueprint,
        Connection connection)
    {
        this.aggregateBlueprint = aggregateBlueprint;
        this.connection = connection;
    }

    public void save(Object aggregateRootInstance)
    {
        PopulatedEntity aggregateRootEntity = new PopulatedEntity(aggregateBlueprint.getAggregateRootEntityBlueprint(), aggregateRootInstance);
        saveEntitiesRecursive(Collections.singletonList(aggregateRootEntity), null);
    }

    private void saveEntitiesRecursive(List<PopulatedEntity> populatedEntities, PopulatedEntity parentPopulatedEntity)
    {
        if(populatedEntities == null || populatedEntities.isEmpty())
        {
            return;
        }

        EntityBlueprint entityBlueprint = populatedEntities.get(0).getEntityBlueprint();
        String updateSqlTemplate = aggregateBlueprint.getEntityUpdateSqlTemplate(entityBlueprint);
        String insertSqlTemplate = aggregateBlueprint.getEntityInsertSqlTemplate(entityBlueprint);

        try(PhotonPreparedStatement updateStatement = new PhotonPreparedStatement(connection, updateSqlTemplate);
            PhotonPreparedStatement insertStatement = new PhotonPreparedStatement(connection, insertSqlTemplate))
        {
            for(PopulatedEntity populatedEntity : populatedEntities)
            {
                updateStatement.resetParameterCounter();
                insertStatement.resetParameterCounter();
                int rowsUpdated = populatedEntity.performUpdate(updateStatement, parentPopulatedEntity);

                if(rowsUpdated == 0)
                {
                    populatedEntity.performInsert(insertStatement, parentPopulatedEntity);
                }

                for(FieldBlueprint fieldBlueprint : entityBlueprint.getFieldsWithChildEntities())
                {
                    List<PopulatedEntity> fieldPopulatedEntities = populatedEntity.getChildPopulatedEntitiesForField(fieldBlueprint);
                    deleteEntityOrphansForField(
                        fieldBlueprint,
                        fieldPopulatedEntities,
                        populatedEntity.getPrimaryKeyValue(),
                        populatedEntity.getEntityBlueprint().getPrimaryKeyColumn().getColumnDataType());

                    saveEntitiesRecursive(fieldPopulatedEntities, populatedEntity);
                }
            }
        }
    }

    private void deleteEntityOrphansForField(
        FieldBlueprint fieldBlueprint,
        List<PopulatedEntity> fieldPopulatedEntities,
        Object primaryKeyValue,
        Integer primaryKeyDataType)
    {
        ColumnBlueprint childPrimaryKeyColumn = fieldBlueprint.getChildEntityBlueprint().getPrimaryKeyColumn();

        if(childPrimaryKeyColumn.getMappedFieldBlueprint() == null || fieldPopulatedEntities.size() == 0)
        {
            String deleteAllChildrenSql = aggregateBlueprint.getDeleteAllChildrenSqlTemplate(fieldBlueprint.getChildEntityBlueprint());
            PhotonPreparedStatement deleteAllOrphans = new PhotonPreparedStatement(connection, deleteAllChildrenSql);
            deleteAllOrphans.setNextParameter(primaryKeyValue, primaryKeyDataType);
            deleteAllOrphans.executeUpdate();
        }
        else
        {
            List<Object> childPrimaryKeyValues = fieldPopulatedEntities
                .stream()
                .filter(p -> p.getPrimaryKeyValue() != null)
                .map(p -> p.getPrimaryKeyValue())
                .collect(Collectors.toList());

            String deleteChildrenExceptSql = String.format(aggregateBlueprint.getDeleteChildrenExceptSqlTemplate(fieldBlueprint.getChildEntityBlueprint()),
                getQuestionMarks(childPrimaryKeyValues.size())
            );

            PhotonPreparedStatement deleteChildrenExcept = new PhotonPreparedStatement(connection, deleteChildrenExceptSql);
            deleteChildrenExcept.setNextParameter(primaryKeyValue, primaryKeyDataType);
            childPrimaryKeyValues.forEach(p -> deleteChildrenExcept.setNextParameter(p, childPrimaryKeyColumn.getColumnDataType()));
            deleteChildrenExcept.executeUpdate();
        }
    }

    // TODO: Put this code in central place
    private String getQuestionMarks(int count)
    {
        StringBuilder questionMarks = new StringBuilder(count * 2 - 1);
        for(int i = 0; i < count; i++)
        {
            if(i < count - 1)
            {
                questionMarks.append("?,");
            }
            else
            {
                questionMarks.append("?");
            }
        }
        return questionMarks.toString();
    }
}
