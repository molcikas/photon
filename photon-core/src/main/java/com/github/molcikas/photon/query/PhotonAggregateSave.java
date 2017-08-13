package com.github.molcikas.photon.query;

import com.github.molcikas.photon.blueprints.*;
import com.github.molcikas.photon.converters.Convert;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.options.PhotonOptions;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PhotonAggregateSave
{
    private final AggregateBlueprint aggregateBlueprint;
    private final Connection connection;
    private final PhotonOptions photonOptions;

    public PhotonAggregateSave(
        AggregateBlueprint aggregateBlueprint,
        Connection connection,
        PhotonOptions photonOptions)
    {
        this.aggregateBlueprint = aggregateBlueprint;
        this.connection = connection;
        this.photonOptions = photonOptions;
    }

    public void save(Object aggregateInstance, List<String> fieldPathsToExclude)
    {
        PopulatedEntity aggregateRootEntity = new PopulatedEntity(aggregateBlueprint.getAggregateRootEntityBlueprint(), aggregateInstance);
        saveEntitiesRecursive(aggregateBlueprint.getAggregateRootEntityBlueprint(), Collections.singletonList(aggregateRootEntity), null, null, false, fieldPathsToExclude, "");
    }

    public void saveAll(List<?> aggregateInstances, List<String> fieldPathsToExclude)
    {
        List<PopulatedEntity> aggregateRootEntities =  aggregateInstances
            .stream()
            .map(instance -> new PopulatedEntity(aggregateBlueprint.getAggregateRootEntityBlueprint(), instance))
            .collect(Collectors.toList());
        saveEntitiesRecursive(aggregateBlueprint.getAggregateRootEntityBlueprint(), aggregateRootEntities, null, null, false, fieldPathsToExclude, "");
    }

    public void insert(Object aggregateInstance)
    {
        PopulatedEntity aggregateRootEntity = new PopulatedEntity(aggregateBlueprint.getAggregateRootEntityBlueprint(),  aggregateInstance);
        saveEntitiesRecursive(aggregateBlueprint.getAggregateRootEntityBlueprint(), Collections.singletonList(aggregateRootEntity), null, null, true, null, "");
    }

    public void insertAll(List<?> aggregateInstances)
    {
        List<PopulatedEntity> aggregateRootEntities =  aggregateInstances
            .stream()
            .map(instance -> new PopulatedEntity(aggregateBlueprint.getAggregateRootEntityBlueprint(), instance))
            .collect(Collectors.toList());
        saveEntitiesRecursive(aggregateBlueprint.getAggregateRootEntityBlueprint(), aggregateRootEntities, null, null, true, null, "");
    }

    private void saveEntitiesRecursive(
        EntityBlueprint entityBlueprint,
        List<PopulatedEntity> populatedEntities,
        PopulatedEntity parentPopulatedEntity,
        FieldBlueprint parentFieldBlueprint,
        boolean isInsert,
        List<String> fieldPathsToExclude,
        String fieldPath)
    {
        if(populatedEntities == null)
        {
            populatedEntities = Collections.emptyList();
        }
        if(fieldPathsToExclude == null)
        {
            fieldPathsToExclude = Collections.emptyList();
        }

        if(fieldPathsToExclude.contains(fieldPath))
        {
            return;
        }

        if(!isInsert)
        {
            findAndDeleteOrphans(entityBlueprint, populatedEntities, parentPopulatedEntity, parentFieldBlueprint);
            findAndDeleteJoinedOrphans(entityBlueprint, populatedEntities);
        }

        if(populatedEntities.isEmpty())
        {
            return;
        }

        Map<TableBlueprint, List<PopulatedEntity>> updatedPopulatedEntities;
        if(isInsert)
        {
            updatedPopulatedEntities = entityBlueprint
                .getTableBlueprintsForInsertOrUpdate()
                .stream()
                .collect(Collectors.toMap(k -> k, v -> Collections.emptyList()));
        }
        else
        {
            updatedPopulatedEntities = updatePopulatedEntities(populatedEntities, parentPopulatedEntity, entityBlueprint);
        }

        Map<TableBlueprint, List<PopulatedEntity>> populatedEntitiesToInsert = new LinkedHashMap<>();

        for(TableBlueprint tableBlueprint : entityBlueprint.getTableBlueprintsForInsertOrUpdate())
        {
            List<PopulatedEntity> entitiesToInsert = populatedEntities
                .stream()
                .filter(p -> !updatedPopulatedEntities.get(tableBlueprint).contains(p))
                .collect(Collectors.toList());
            populatedEntitiesToInsert.put(tableBlueprint, entitiesToInsert);
        }

        List<PopulatedEntity> populatedEntitiesToInsertList = populatedEntitiesToInsert
            .values()
            .stream()
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toList());

        insertPopulatedEntities(populatedEntitiesToInsert, parentPopulatedEntity, entityBlueprint);

        setForeignKeyToParentForPopulatedEntities(populatedEntities, entityBlueprint.getFieldsWithChildEntities());

        insertAndDeleteForeignKeyListFields(populatedEntities, entityBlueprint.getForeignKeyListFields());

        for(PopulatedEntity populatedEntity : populatedEntities)
        {
            for (FieldBlueprint fieldBlueprint : entityBlueprint.getFieldsWithChildEntities())
            {
                String childFieldPath = fieldPath + (StringUtils.isBlank(fieldPath) ? "" : ".") + fieldBlueprint.getFieldName();
                List<PopulatedEntity> fieldPopulatedEntities = populatedEntity.getChildPopulatedEntitiesForField(fieldBlueprint);
                saveEntitiesRecursive(
                    fieldBlueprint.getChildEntityBlueprint(),
                    fieldPopulatedEntities,
                    populatedEntity,
                    fieldBlueprint,
                    populatedEntitiesToInsertList.contains(populatedEntity),
                    fieldPathsToExclude,
                    childFieldPath
                );
            }
        }
    }

    private void findAndDeleteOrphans(
        EntityBlueprint entityBlueprint,
        List<PopulatedEntity> populatedEntities,
        PopulatedEntity parentPopulatedEntity,
        FieldBlueprint parentFieldBlueprint)
    {
        if(parentFieldBlueprint == null)
        {
            return;
        }

        TableBlueprint tableBlueprint = entityBlueprint.getTableBlueprint();

        if(tableBlueprint.isPrimaryKeyMappedToField())
        {
            String primaryKeyColumnName = tableBlueprint.getPrimaryKeyColumn().getColumnName();
            String selectOrphansSql = tableBlueprint.getSelectOrphansSql();
            if(selectOrphansSql == null)
            {
                // If the primary key and foreign key to parent are equal, there won't be any select orhans sql because
                // there can't be any orphans, so just return.
                return;
            }

            List<?> childIds = populatedEntities
                .stream()
                .map(PopulatedEntity::getPrimaryKeyValue)
                .filter(Objects::nonNull) // Auto increment entities that have not been inserted yet will have null primary key values.
                .collect(Collectors.toList());
            List<?> orphanIds;

            try(PhotonPreparedStatement statement = new PhotonPreparedStatement(selectOrphansSql, false, connection, photonOptions))
            {
                statement.setNextParameter(
                    parentPopulatedEntity.getPrimaryKeyValue(),
                    tableBlueprint.getParentTableBlueprint().getPrimaryKeyColumn().getColumnDataType(),
                    tableBlueprint.getParentTableBlueprint().getPrimaryKeyColumnSerializer());
                statement.setNextArrayParameter(
                    childIds,
                    tableBlueprint.getPrimaryKeyColumn().getColumnDataType(),
                    tableBlueprint.getPrimaryKeyColumnSerializer());
                List<PhotonQueryResultRow> rows =
                    statement.executeQuery(Collections.singletonList(primaryKeyColumnName));
                orphanIds =
                    rows.stream().map(r -> r.getValue(primaryKeyColumnName)).collect(Collectors.toList());
            }
            if(orphanIds.size() > 0)
            {
                deleteOrphansAndTheirChildrenRecursive(orphanIds, entityBlueprint, Collections.emptyList());
            }
        }
        else
        {
            // If a child does not have a primary key, then it has to be deleted and re-inserted on every save.
            try(PhotonPreparedStatement statement = new PhotonPreparedStatement(
                tableBlueprint.getDeleteChildrenExceptSql(),
                false,
                connection,
                photonOptions))
            {
                statement.setNextParameter(
                    parentPopulatedEntity.getPrimaryKeyValue(),
                    tableBlueprint.getParentTableBlueprint().getPrimaryKeyColumn().getColumnDataType(),
                    tableBlueprint.getParentTableBlueprint().getPrimaryKeyColumnSerializer());
                statement.setNextParameter(Collections.emptyList(), null, null);
                statement.executeUpdate();
            }
        }
    }

    private void deleteOrphansAndTheirChildrenRecursive(
        List<?> orphanIds,
        EntityBlueprint entityBlueprint,
        List<EntityBlueprint> parentEntityBlueprints)
    {
        TableBlueprint rootEntityTableBlueprint = parentEntityBlueprints.size() > 0 ?
            parentEntityBlueprints.get(parentEntityBlueprints.size() - 1).getTableBlueprint() :
            entityBlueprint.getTableBlueprint();

        for(FieldBlueprint fieldBlueprint : entityBlueprint.getFieldsWithChildEntities())
        {
            List<EntityBlueprint> childParentEntityBlueprints = new ArrayList<>(parentEntityBlueprints.size() + 1);
            childParentEntityBlueprints.add(entityBlueprint);
            childParentEntityBlueprints.addAll(parentEntityBlueprints);
            deleteOrphansAndTheirChildrenRecursive(
                orphanIds,
                fieldBlueprint.getChildEntityBlueprint(),
                childParentEntityBlueprints);
        }

        for(TableBlueprint tableBlueprint : entityBlueprint.getTableBlueprintsForDelete())
        {
            try(PhotonPreparedStatement statement = new PhotonPreparedStatement(
                tableBlueprint.getDeleteOrphansSql(parentEntityBlueprints.size()),
                false,
                connection,
                photonOptions))
            {
                statement.setNextArrayParameter(
                    orphanIds,
                    rootEntityTableBlueprint.getPrimaryKeyColumn().getColumnDataType(),
                    rootEntityTableBlueprint.getPrimaryKeyColumnSerializer());
                statement.executeUpdate();
            }
        }
    }

    private void findAndDeleteJoinedOrphans(EntityBlueprint entityBlueprint, List<PopulatedEntity> populatedEntities)
    {
        for(PopulatedEntity populatedEntity : populatedEntities)
        {
            for (TableBlueprint tableBlueprint : entityBlueprint.getJoinedTableBlueprints())
            {
                if(!tableBlueprint.isApplicableForEntityClass(populatedEntity.getEntityInstance().getClass()))
                {
                    String primaryKeyColumnName = tableBlueprint.getPrimaryKeyColumn().getColumnName();
                    List<?> orphanIds;

                    try(PhotonPreparedStatement statement = new PhotonPreparedStatement(
                        tableBlueprint.getSelectByIdSql(),
                        false,
                        connection,
                        photonOptions))
                    {
                        statement.setNextParameter(
                            populatedEntity.getPrimaryKeyValue(),
                            tableBlueprint.getPrimaryKeyColumn().getColumnDataType(),
                            tableBlueprint.getPrimaryKeyColumnSerializer());
                        List<PhotonQueryResultRow> rows =
                            statement.executeQuery(Collections.singletonList(primaryKeyColumnName));
                        orphanIds =
                            rows.stream().map(r -> r.getValue(primaryKeyColumnName)).collect(Collectors.toList());
                    }
                    if(orphanIds.size() > 0)
                    {
                        deleteTableOrphansAndItsChildrenRecursive(orphanIds, entityBlueprint, tableBlueprint);
                    }
                }
            }
        }
    }

    private void deleteTableOrphansAndItsChildrenRecursive(
        List<?> orphanIds,
        EntityBlueprint entityBlueprint,
        TableBlueprint tableBlueprint)
    {
        List<FieldBlueprint> fieldsWithChildEntities = entityBlueprint
            .getFieldsWithChildEntities()
            .stream()
            .filter(t -> tableBlueprint.equals(t.getChildEntityBlueprint().getTableBlueprint().getParentTableBlueprint()))
            .collect(Collectors.toList());

        for(FieldBlueprint fieldBlueprint : fieldsWithChildEntities)
        {
            List<EntityBlueprint> childParentEntityBlueprints = new ArrayList<>(1);
            childParentEntityBlueprints.add(entityBlueprint);
            deleteOrphansAndTheirChildrenRecursive(
                orphanIds,
                fieldBlueprint.getChildEntityBlueprint(),
                childParentEntityBlueprints);
        }

        try(PhotonPreparedStatement statement = new PhotonPreparedStatement(
            tableBlueprint.getDeleteOrphansSql(0),
            false,
            connection,
            photonOptions))
        {
            statement.setNextArrayParameter(
                orphanIds,
                entityBlueprint.getTableBlueprint().getPrimaryKeyColumn().getColumnDataType(),
                entityBlueprint.getTableBlueprint().getPrimaryKeyColumnSerializer());
            statement.executeUpdate();
        }
    }

    private Map<TableBlueprint, List<PopulatedEntity>> updatePopulatedEntities(
        List<PopulatedEntity> populatedEntities,
        PopulatedEntity parentPopulatedEntity,
        EntityBlueprint entityBlueprint)
    {
        if(populatedEntities == null || populatedEntities.isEmpty())
        {
            return Collections.emptyMap();
        }

        Map<TableBlueprint, List<PopulatedEntity>> updatedPopulatedEntities = new LinkedHashMap<>();

        for(TableBlueprint tableBlueprint : entityBlueprint.getTableBlueprintsForInsertOrUpdate())
        {
            String updateSql = tableBlueprint.getUpdateSql();
            final List<PopulatedEntity> attemptedUpdatedPopulatedEntities = new ArrayList<>(populatedEntities.size());

            try(PhotonPreparedStatement updateStatement = new PhotonPreparedStatement(updateSql, false, connection, photonOptions))
            {
                for (PopulatedEntity populatedEntity : populatedEntities)
                {
                    if(tableBlueprint.isApplicableForEntityClass(populatedEntity.getEntityInstance().getClass()))
                    {
                        boolean addedToBatch = populatedEntity.addUpdateToBatch(updateStatement, tableBlueprint, parentPopulatedEntity);
                        if(addedToBatch)
                        {
                            attemptedUpdatedPopulatedEntities.add(populatedEntity);
                        }
                    }
                }

                int[] rowUpdateCounts = updateStatement.executeBatch();

                List<PopulatedEntity> updatedPopulatedEntitiesForTable = IntStream
                    .range(0, attemptedUpdatedPopulatedEntities.size())
                    .filter(i -> rowUpdateCounts[i] > 0)
                    .mapToObj(i -> attemptedUpdatedPopulatedEntities.get(i))
                    .collect(Collectors.toList());

                updatedPopulatedEntities.put(tableBlueprint, updatedPopulatedEntitiesForTable);
            }
        }

        return updatedPopulatedEntities;
    }

    private void insertPopulatedEntities(
        Map<TableBlueprint, List<PopulatedEntity>> populatedEntities,
        PopulatedEntity parentPopulatedEntity,
        EntityBlueprint entityBlueprint)
    {
        if(populatedEntities == null || populatedEntities.isEmpty())
        {
            return;
        }
        for(TableBlueprint tableBlueprint : entityBlueprint.getTableBlueprintsForInsertOrUpdate())
        {
            if (tableBlueprint.getPrimaryKeyColumn().isAutoIncrementColumn() && !photonOptions.isEnableBatchInsertsForAutoIncrementEntities())
            {
                for (PopulatedEntity populatedEntity : populatedEntities.get(tableBlueprint))
                {
                    if(tableBlueprint.isApplicableForEntityClass(populatedEntity.getEntityInstance().getClass()))
                    {
                        String insertSql = tableBlueprint.getInsertSql();
                        boolean populateGeneratedKeys = tableBlueprint.getPrimaryKeyColumn().isAutoIncrementColumn();
                        boolean shouldInsertUsingPrimaryKeySql = tableBlueprint.shouldInsertUsingPrimaryKeySql(populatedEntity);
                        if(shouldInsertUsingPrimaryKeySql)
                        {
                            insertSql = tableBlueprint.getInsertWithPrimaryKeySql();
                            populateGeneratedKeys = false;
                        }
                        try (PhotonPreparedStatement insertStatement = new PhotonPreparedStatement(
                            insertSql,
                            populateGeneratedKeys,
                            connection,
                            photonOptions))
                        {
                            populatedEntity.addParametersToInsertStatement(
                                insertStatement,
                                tableBlueprint,
                                parentPopulatedEntity,
                                shouldInsertUsingPrimaryKeySql);
                            insertStatement.executeInsert();
                            Long generatedKey = insertStatement.getGeneratedKeys().get(0);
                            populatedEntity.setPrimaryKeyValue(generatedKey);
                        }
                    }
                }
            }
            else
            {
                List<PopulatedEntity> insertedEntityBatchList = new ArrayList<>();
                List<PopulatedEntity> insertEntityWithPrimaryKeySqlBatchList = new ArrayList<>();

                try (PhotonPreparedStatement insertStatement = new PhotonPreparedStatement(
                    tableBlueprint.getInsertSql(),
                    tableBlueprint.getPrimaryKeyColumn().isAutoIncrementColumn(),
                    connection,
                    photonOptions))
                {
                    for (PopulatedEntity populatedEntity : populatedEntities.get(tableBlueprint))
                    {
                        if(tableBlueprint.isApplicableForEntityClass(populatedEntity.getEntityInstance().getClass()))
                        {
                            if(tableBlueprint.shouldInsertUsingPrimaryKeySql(populatedEntity))
                            {
                                insertEntityWithPrimaryKeySqlBatchList.add(populatedEntity);
                            }
                            else
                            {
                                populatedEntity.addInsertToBatch(insertStatement, tableBlueprint, parentPopulatedEntity, false);
                                insertedEntityBatchList.add(populatedEntity);
                            }
                        }
                    }

                    insertStatement.executeBatch();

                    if (tableBlueprint.getPrimaryKeyColumn().isAutoIncrementColumn())
                    {
                        List<Long> generatedKeys = insertStatement.getGeneratedKeys();
                        int index = 0;
                        for (PopulatedEntity populatedEntity : insertedEntityBatchList)
                        {
                            populatedEntity.setPrimaryKeyValue(generatedKeys.get(index));
                            index++;
                        }
                    }
                }

                if(!insertEntityWithPrimaryKeySqlBatchList.isEmpty())
                {
                    try (PhotonPreparedStatement insertStatement = new PhotonPreparedStatement(
                        tableBlueprint.getInsertWithPrimaryKeySql(),
                        false,
                        connection,
                        photonOptions))
                    {
                        for (PopulatedEntity populatedEntity : insertEntityWithPrimaryKeySqlBatchList)
                        {
                            populatedEntity.addInsertToBatch(insertStatement, tableBlueprint, parentPopulatedEntity, true);
                            insertedEntityBatchList.add(populatedEntity);
                        }

                        insertStatement.executeBatch();
                    }
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

        EntityBlueprint entityBlueprint = populatedEntities.get(0).getEntityBlueprint();
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
                entityBlueprint.getTableBlueprint().getPrimaryKeyColumn().getMappedFieldBlueprint().getFieldClass()
            );

            try(PhotonPreparedStatement insertStatement = new PhotonPreparedStatement(foreignKeyListBlueprint.getInsertSql(), false, connection, photonOptions))
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
                        try(PhotonPreparedStatement deleteStatement = new PhotonPreparedStatement(
                            foreignKeyListBlueprint.getDeleteForeignKeysSql(),
                            false,
                            connection,
                            photonOptions))
                        {
                            deleteStatement.setNextArrayParameter(foreignKeyValuesToDelete, foreignKeyListBlueprint.getForeignTableKeyColumnType(), null);
                            deleteStatement.setNextParameter(populatedEntity.getPrimaryKeyValue(), populatedEntityBlueprint.getTableBlueprint().getPrimaryKeyColumn().getColumnDataType(), populatedEntityBlueprint.getTableBlueprint().getPrimaryKeyColumnSerializer());
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
                        insertStatement.setNextParameter(populatedEntity.getPrimaryKeyValue(), populatedEntityBlueprint.getTableBlueprint().getPrimaryKeyColumn().getColumnDataType(), populatedEntityBlueprint.getTableBlueprint().getPrimaryKeyColumnSerializer());
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

        try (PhotonPreparedStatement statement = new PhotonPreparedStatement(foreignKeyListBlueprint.getSelectSql(), false, connection, photonOptions))
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
