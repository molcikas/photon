package photon.query;

import photon.blueprints.AggregateBlueprint;
import photon.blueprints.ColumnBlueprint;
import photon.blueprints.AggregateEntityBlueprint;
import photon.blueprints.FieldBlueprint;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        AggregateEntityBlueprint entityBlueprint = (AggregateEntityBlueprint) populatedEntities.get(0).getEntityBlueprint();
        List<FieldBlueprint> fieldsWithChildEntities = entityBlueprint.getFieldsWithChildEntities();
        List<PopulatedEntity> updatedPopulatedEntities = updatePopulatedEntities(populatedEntities, parentPopulatedEntity);
        List<PopulatedEntity> populatedEntitiesToInsert = populatedEntities
            .stream()
            .filter(p -> !updatedPopulatedEntities.contains(p))
            .collect(Collectors.toList());

        insertPopulatedEntities(populatedEntitiesToInsert, parentPopulatedEntity);
        List<PopulatedEntity> populatedEntitiesNeedingForeignKeyToParentSet = populatedEntitiesToInsert
            .stream()
            .filter(p -> p.getEntityBlueprint().getPrimaryKeyColumn().isAutoIncrementColumn() && p.getPrimaryKeyValue() != null)
            .collect(Collectors.toList());

        setForeignKeyToParentForPopulatedEntities(populatedEntitiesNeedingForeignKeyToParentSet, fieldsWithChildEntities);

        deleteOrphanChildEntities(updatedPopulatedEntities, fieldsWithChildEntities, entityBlueprint.getPrimaryKeyColumn().getColumnDataType());

        for(PopulatedEntity populatedEntity : populatedEntities)
        {
            for (FieldBlueprint fieldBlueprint : fieldsWithChildEntities)
            {
                List<PopulatedEntity> fieldPopulatedEntities = populatedEntity.getChildPopulatedEntitiesForField(fieldBlueprint);
                saveEntitiesRecursive(fieldPopulatedEntities, populatedEntity);
            }
        }
    }

    private List<PopulatedEntity> updatePopulatedEntities(List<PopulatedEntity> populatedEntities, PopulatedEntity parentPopulatedEntity)
    {
        if(populatedEntities == null || populatedEntities.isEmpty())
        {
            return Collections.emptyList();
        }

        String updateSql = populatedEntities.get(0).getEntityBlueprint().getUpdateSql();
        final List<PopulatedEntity> attemptedUpdatedPopulatedEntities = new ArrayList<>(populatedEntities.size());
        List<PopulatedEntity> updatedPopulatedEntities;

        try(PhotonPreparedStatement updateStatement = new PhotonPreparedStatement(updateSql, connection))
        {
            for (PopulatedEntity populatedEntity : populatedEntities)
            {
                boolean addedToBatch = populatedEntity.addUpdateToBatch(updateStatement, parentPopulatedEntity);
                if(addedToBatch)
                {
                    attemptedUpdatedPopulatedEntities.add(populatedEntity);
                }
            }

            int[] rowUpdateCounts = updateStatement.executeBatch();

            updatedPopulatedEntities = IntStream
                .range(0, attemptedUpdatedPopulatedEntities.size())
                .filter(i -> rowUpdateCounts[i] > 0)
                .mapToObj(i -> attemptedUpdatedPopulatedEntities.get(i))
                .collect(Collectors.toList());

            return updatedPopulatedEntities;
        }
    }

    private void insertPopulatedEntities(List<PopulatedEntity> populatedEntities, PopulatedEntity parentPopulatedEntity)
    {
        if(populatedEntities == null || populatedEntities.isEmpty())
        {
            return;
        }

        AggregateEntityBlueprint entityBlueprint = (AggregateEntityBlueprint) populatedEntities.get(0).getEntityBlueprint();
        String insertSql = entityBlueprint.getInsertSql();

        try (PhotonPreparedStatement insertStatement = new PhotonPreparedStatement(insertSql, connection))
        {
            for (PopulatedEntity populatedEntity : populatedEntities)
            {
                populatedEntity.addInsertToBatch(insertStatement, parentPopulatedEntity);
            }

            insertStatement.executeBatch();

            if (entityBlueprint.getPrimaryKeyColumn().isAutoIncrementColumn())
            {
                List<Long> generatedKeys = insertStatement.getGeneratedKeys();
                int index = 0;
                for (PopulatedEntity populatedEntity : populatedEntities)
                {
                    populatedEntity.setPrimaryKeyValue(generatedKeys.get(index));
                    index++;
                }
            }
        }
    }

    private void setForeignKeyToParentForPopulatedEntities(List<PopulatedEntity> populatedEntities, List<FieldBlueprint> fieldsWithChildEntities)
    {
        for(PopulatedEntity populatedEntity : populatedEntities)
        {
            for (FieldBlueprint fieldBlueprint : fieldsWithChildEntities)
            {
                List<PopulatedEntity> fieldPopulatedEntities = populatedEntity.getChildPopulatedEntitiesForField(fieldBlueprint);

                for (PopulatedEntity fieldPopulatedEntity : fieldPopulatedEntities)
                {
                    fieldPopulatedEntity.setForeignKeyToParentValue(populatedEntity.getPrimaryKeyValue());
                }
            }
        }
    }

    private void deleteOrphanChildEntities(List<PopulatedEntity> populatedEntities, List<FieldBlueprint> fieldsWithChildEntities, Integer primaryKeyDataType)
    {
        for (FieldBlueprint fieldBlueprint : fieldsWithChildEntities)
        {
            String deleteChildrenExceptSql = fieldBlueprint.getChildEntityBlueprint().getDeleteChildrenExceptSql();
            ColumnBlueprint childPrimaryKeyColumn = fieldBlueprint.getChildEntityBlueprint().getPrimaryKeyColumn();

            try(PhotonPreparedStatement deleteAllChildrenExceptStatement = new PhotonPreparedStatement(deleteChildrenExceptSql, connection))
            {
                for(PopulatedEntity populatedEntity : populatedEntities)
                {
                    List<PopulatedEntity> fieldPopulatedEntities = populatedEntity.getChildPopulatedEntitiesForField(fieldBlueprint);

                    List<Object> childrenToSave;
                    if(childPrimaryKeyColumn.getMappedFieldBlueprint() == null)
                    {
                        // If the child does not have a primary key, then it has to be re-inserted on every save.
                        childrenToSave = Collections.emptyList();
                    }
                    else
                    {
                        childrenToSave = fieldPopulatedEntities
                            .stream()
                            .filter(p -> p.getPrimaryKeyValue() != null)
                            .map(PopulatedEntity::getPrimaryKeyValue)
                            .collect(Collectors.toList());
                    }

                    deleteAllChildrenExceptStatement.setNextParameter(populatedEntity.getPrimaryKeyValue(), primaryKeyDataType);
                    deleteAllChildrenExceptStatement.setNextArrayParameter(childrenToSave, childPrimaryKeyColumn.getColumnDataType());
                    deleteAllChildrenExceptStatement.addToBatch();
                }

                deleteAllChildrenExceptStatement.executeBatch();
            }
        }
    }
}
