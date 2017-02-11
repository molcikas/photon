package photon.query;

import photon.blueprints.*;
import photon.converters.Convert;
import photon.converters.Converter;

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

    public void saveAll(List<?> aggregateRootInstances)
    {
        List<PopulatedEntity> aggregateRootEntities = aggregateRootInstances
            .stream()
            .map(instance -> new PopulatedEntity(aggregateBlueprint.getAggregateRootEntityBlueprint(), instance))
            .collect(Collectors.toList());
        saveEntitiesRecursive(aggregateRootEntities, null);
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

        insertAndDeleteForeignKeyListFields(populatedEntities, entityBlueprint.getForeignKeyListFields());

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
        // TODO: 3 level entity, if you delete the middle level the last level will throw a foreign key error!

        for (FieldBlueprint fieldBlueprint : fieldsWithChildEntities)
        {
            String deleteChildrenExceptSql = fieldBlueprint.getChildEntityBlueprint().getDeleteChildrenExceptSql();
            ColumnBlueprint childPrimaryKeyColumn = fieldBlueprint.getChildEntityBlueprint().getPrimaryKeyColumn();

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

                try(PhotonPreparedStatement deleteAllChildrenExceptStatement = new PhotonPreparedStatement(deleteChildrenExceptSql, connection))
                {
                    deleteAllChildrenExceptStatement.setNextParameter(populatedEntity.getPrimaryKeyValue(), primaryKeyDataType);
                    deleteAllChildrenExceptStatement.setNextArrayParameter(childrenToSave, childPrimaryKeyColumn.getColumnDataType());
                    deleteAllChildrenExceptStatement.executeUpdate();
                }
            }
        }
    }

    private void insertAndDeleteForeignKeyListFields(List<PopulatedEntity> populatedEntities, List<FieldBlueprint> foreignKeyListFields)
    {
        if(populatedEntities == null || populatedEntities.isEmpty())
        {
            return;
        }

        AggregateEntityBlueprint entityBlueprint = (AggregateEntityBlueprint) populatedEntities.get(0).getEntityBlueprint();
        List<Object> ids = populatedEntities
            .stream()
            .map(PopulatedEntity::getPrimaryKeyValue)
            .collect(Collectors.toList());

        for (FieldBlueprint fieldBlueprint : foreignKeyListFields)
        {
            ForeignKeyListBlueprint foreignKeyListBlueprint = fieldBlueprint.getForeignKeyListBlueprint();
            Map<Object, Collection> existingDatabaseForeignKeyListValues = getExistingDatabaseForeignKeyListValues(
                ids,
                foreignKeyListBlueprint,
                entityBlueprint.getPrimaryKeyColumn().getMappedFieldBlueprint().getFieldClass()
            );

            try(PhotonPreparedStatement insertStatement = new PhotonPreparedStatement(foreignKeyListBlueprint.getInsertSql(), connection))
            {
                for (PopulatedEntity populatedEntity : populatedEntities)
                {
                    Collection databaseForeignKeyListValuesForEntity = existingDatabaseForeignKeyListValues.get(populatedEntity.getPrimaryKeyValue());
                    if(databaseForeignKeyListValuesForEntity == null)
                    {
                        databaseForeignKeyListValuesForEntity = Collections.emptyList();
                    }
                    final Collection databaseForeignKeyListValuesForEntityFinal = databaseForeignKeyListValuesForEntity;
                    Collection currentForeignKeyListValues = (Collection) populatedEntity.getInstanceValue(fieldBlueprint);
                    if(currentForeignKeyListValues == null)
                    {
                        currentForeignKeyListValues = Collections.emptyList();
                    }
                    final Collection currentForeignKeyListValuesFinal = (Collection) currentForeignKeyListValues
                        .stream()
                        .distinct()
                        .collect(Collectors.toList());

                    Collection foreignKeyValuesToDelete = (Collection) databaseForeignKeyListValuesForEntityFinal
                        .stream()
                        .filter(value -> !currentForeignKeyListValuesFinal.contains(value))
                        .collect(Collectors.toList());

                    if(!foreignKeyValuesToDelete.isEmpty())
                    {
                        try(PhotonPreparedStatement deleteStatement = new PhotonPreparedStatement(foreignKeyListBlueprint.getDeleteForeignKeysSql(), connection))
                        {
                            deleteStatement.setNextArrayParameter(foreignKeyValuesToDelete, foreignKeyListBlueprint.getForeignTableKeyColumnType());
                            deleteStatement.setNextParameter(populatedEntity.getPrimaryKeyValue(), populatedEntity.getEntityBlueprint().getPrimaryKeyColumn().getColumnDataType());
                            deleteStatement.executeUpdate();
                        }
                    }

                    Collection foreignKeyValuesToInsert = (Collection) currentForeignKeyListValuesFinal
                        .stream()
                        .filter(value -> !databaseForeignKeyListValuesForEntityFinal.contains(value))
                        .collect(Collectors.toList());

                    for (Object foreignKeyValue : foreignKeyValuesToInsert)
                    {
                        insertStatement.setNextParameter(foreignKeyValue, foreignKeyListBlueprint.getForeignTableKeyColumnType());
                        insertStatement.setNextParameter(populatedEntity.getPrimaryKeyValue(), populatedEntity.getEntityBlueprint().getPrimaryKeyColumn().getColumnDataType());
                        insertStatement.addToBatch();
                    }
                }

                insertStatement.executeBatch();
            }
        }
    }

    private Map<Object, Collection> getExistingDatabaseForeignKeyListValues(List<Object> ids, ForeignKeyListBlueprint foreignKeyListBlueprint, Class primaryKeyFieldClass)
    {
        Map<Object, Collection> existingDatabaseForeignKeyListValues = new HashMap<>();
        List<PhotonQueryResultRow> photonQueryResultRows;

        try (PhotonPreparedStatement statement = new PhotonPreparedStatement(foreignKeyListBlueprint.getSelectSql(), connection))
        {
            statement.setNextArrayParameter(ids, foreignKeyListBlueprint.getForeignTableKeyColumnType());
            photonQueryResultRows = statement.executeQuery(foreignKeyListBlueprint.getSelectColumnNames());
        }

        Converter joinColumnConverter = Convert.getConverterIfExists(primaryKeyFieldClass);
        Converter foreignKeyConverter = Convert.getConverterIfExists(foreignKeyListBlueprint.getFieldListItemClass());

        for(PhotonQueryResultRow photonQueryResultRow : photonQueryResultRows)
        {
            Object joinColumnValue = joinColumnConverter.convert(photonQueryResultRow.getValue(foreignKeyListBlueprint.getForeignTableJoinColumnName()));
            Object foreignKeyValue = foreignKeyConverter.convert(photonQueryResultRow.getValue(foreignKeyListBlueprint.getForeignTableKeyColumnName()));
            Collection existingForeignKeysList = existingDatabaseForeignKeyListValues.get(joinColumnValue);
            if(existingForeignKeysList == null)
            {
                existingForeignKeysList = new ArrayList<>();
                existingDatabaseForeignKeyListValues.put(joinColumnValue, existingForeignKeysList);
            }
            existingForeignKeysList.add(foreignKeyValue);
        }

        return existingDatabaseForeignKeyListValues;
    }
}
