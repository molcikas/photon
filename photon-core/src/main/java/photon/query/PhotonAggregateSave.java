package photon.query;

import photon.blueprints.*;
import photon.converters.Convert;
import photon.converters.Converter;
import photon.sqlbuilders.SqlJoinClauseBuilderService;

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
        saveEntitiesRecursive(aggregateBlueprint.getAggregateRootEntityBlueprint(), Collections.singletonList(aggregateRootEntity), null, null);
    }

    public void saveAll(List<?> aggregateRootInstances)
    {
        List<PopulatedEntity> aggregateRootEntities = aggregateRootInstances
            .stream()
            .map(instance -> new PopulatedEntity(aggregateBlueprint.getAggregateRootEntityBlueprint(), instance))
            .collect(Collectors.toList());
        saveEntitiesRecursive(aggregateBlueprint.getAggregateRootEntityBlueprint(), aggregateRootEntities, null, null);
    }

    private void saveEntitiesRecursive(
        AggregateEntityBlueprint entityBlueprint,
        List<PopulatedEntity> populatedEntities,
        PopulatedEntity parentPopulatedEntity,
        FieldBlueprint parentFieldBlueprint)
    {
        if(populatedEntities == null)
        {
            populatedEntities = Collections.emptyList();
        }

        deleteOrphans(entityBlueprint, populatedEntities, parentPopulatedEntity, parentFieldBlueprint);

        if(populatedEntities.isEmpty())
        {
            return;
        }

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

        insertAndDeleteForeignKeyListFields(populatedEntities, entityBlueprint.getForeignKeyListFields());

        for(PopulatedEntity populatedEntity : populatedEntities)
        {
            for (FieldBlueprint fieldBlueprint : fieldsWithChildEntities)
            {
                List<PopulatedEntity> fieldPopulatedEntities = populatedEntity.getChildPopulatedEntitiesForField(fieldBlueprint);
                saveEntitiesRecursive(fieldBlueprint.getChildEntityBlueprint(), fieldPopulatedEntities, populatedEntity, fieldBlueprint);
            }
        }
    }

    private void deleteOrphans(
        AggregateEntityBlueprint entityBlueprint,
        List<PopulatedEntity> populatedEntities,
        PopulatedEntity parentPopulatedEntity,
        FieldBlueprint parentFieldBlueprint)
    {
        if(parentFieldBlueprint == null)
        {
            return;
        }

        EntityBlueprint parentPopulatedEntityBlueprint = parentPopulatedEntity.getEntityBlueprint();

        if(entityBlueprint.isPrimaryKeyMappedToField())
        {
            List<?> childIds = populatedEntities
                .stream()
                .map(PopulatedEntity::getPrimaryKeyValue)
                .filter(value -> value != null) // Auto increment entities that have not been inserted yet will have null primary key values.
                .collect(Collectors.toList());
            List<?> orphanIds;

            try(PhotonPreparedStatement statement = new PhotonPreparedStatement(entityBlueprint.getSelectOrphansSql(), connection))
            {
                statement.setNextParameter(parentPopulatedEntity.getPrimaryKeyValue(), parentPopulatedEntityBlueprint.getPrimaryKeyColumn().getColumnDataType(), parentPopulatedEntityBlueprint.getPrimaryKeyCustomToDatabaseValueConverter());
                statement.setNextArrayParameter(childIds, entityBlueprint.getPrimaryKeyColumn().getColumnDataType(), entityBlueprint.getPrimaryKeyCustomToDatabaseValueConverter());
                List<PhotonQueryResultRow> rows = statement.executeQuery(Collections.singletonList(entityBlueprint.getPrimaryKeyColumnName()));
                orphanIds = rows.stream().map(r -> r.getValue(entityBlueprint.getPrimaryKeyColumnName())).collect(Collectors.toList());
            }
            if(orphanIds.size() > 0)
            {
                deleteOrphansAndTheirChildrenRecursive(orphanIds, entityBlueprint, Collections.emptyList());
            }
        }
        else
        {
            // If a child does not have a primary key, then it has to be deleted and re-inserted on every save.
            try(PhotonPreparedStatement statement = new PhotonPreparedStatement(entityBlueprint.getDeleteChildrenExceptSql(), connection))
            {
                statement.setNextParameter(parentPopulatedEntity.getPrimaryKeyValue(), parentPopulatedEntityBlueprint.getPrimaryKeyColumn().getColumnDataType(), parentPopulatedEntityBlueprint.getPrimaryKeyCustomToDatabaseValueConverter());
                statement.setNextParameter(Collections.emptyList(), null, null);
                statement.executeUpdate();
            }
        }
    }

    private void deleteOrphansAndTheirChildrenRecursive(
        List<?> orphanIds,
        AggregateEntityBlueprint entityBlueprint,
        List<AggregateEntityBlueprint> parentEntityBlueprints)
    {
        AggregateEntityBlueprint rootEntityBlueprint = parentEntityBlueprints.size() > 0 ?
            parentEntityBlueprints.get(parentEntityBlueprints.size() - 1) :
            entityBlueprint;

        for(FieldBlueprint fieldBlueprint : entityBlueprint.getFieldsWithChildEntities())
        {
            List<AggregateEntityBlueprint> childParentEntityBlueprints = new ArrayList<>(parentEntityBlueprints.size() + 1);
            childParentEntityBlueprints.add(entityBlueprint);
            childParentEntityBlueprints.addAll(parentEntityBlueprints);
            deleteOrphansAndTheirChildrenRecursive(orphanIds, fieldBlueprint.getChildEntityBlueprint(), childParentEntityBlueprints);
        }

        try(PhotonPreparedStatement statement = new PhotonPreparedStatement(entityBlueprint.getDeleteOrphanSql(parentEntityBlueprints.size()), connection))
        {
            statement.setNextArrayParameter(orphanIds, rootEntityBlueprint.getPrimaryKeyColumn().getColumnDataType(), rootEntityBlueprint.getPrimaryKeyCustomToDatabaseValueConverter());
            statement.executeUpdate();
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
                    EntityBlueprint populatedEntityBlueprint = populatedEntity.getEntityBlueprint();
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
                            deleteStatement.setNextArrayParameter(foreignKeyValuesToDelete, foreignKeyListBlueprint.getForeignTableKeyColumnType(), null);
                            deleteStatement.setNextParameter(populatedEntity.getPrimaryKeyValue(), populatedEntityBlueprint.getPrimaryKeyColumn().getColumnDataType(), populatedEntityBlueprint.getPrimaryKeyCustomToDatabaseValueConverter());
                            deleteStatement.executeUpdate();
                        }
                    }

                    Collection foreignKeyValuesToInsert = (Collection) currentForeignKeyListValuesFinal
                        .stream()
                        .filter(value -> !databaseForeignKeyListValuesForEntityFinal.contains(value))
                        .collect(Collectors.toList());

                    for (Object foreignKeyValue : foreignKeyValuesToInsert)
                    {
                        insertStatement.setNextParameter(foreignKeyValue, foreignKeyListBlueprint.getForeignTableKeyColumnType(), null);
                        insertStatement.setNextParameter(populatedEntity.getPrimaryKeyValue(), populatedEntityBlueprint.getPrimaryKeyColumn().getColumnDataType(), populatedEntityBlueprint.getPrimaryKeyCustomToDatabaseValueConverter());
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
            statement.setNextArrayParameter(ids, foreignKeyListBlueprint.getForeignTableKeyColumnType(), null);
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
