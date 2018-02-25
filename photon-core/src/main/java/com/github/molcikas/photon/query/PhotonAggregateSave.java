package com.github.molcikas.photon.query;

import com.github.molcikas.photon.PhotonEntityState;
import com.github.molcikas.photon.blueprints.*;
import com.github.molcikas.photon.blueprints.entity.EntityBlueprint;
import com.github.molcikas.photon.blueprints.entity.FieldBlueprint;
import com.github.molcikas.photon.blueprints.entity.FlattenedCollectionBlueprint;
import com.github.molcikas.photon.blueprints.table.TableBlueprint;
import com.github.molcikas.photon.blueprints.table.TableBlueprintAndKey;
import com.github.molcikas.photon.blueprints.table.TableValue;
import com.github.molcikas.photon.converters.Convert;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.exceptions.PhotonOptimisticConcurrencyException;
import com.github.molcikas.photon.options.PhotonOptions;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PhotonAggregateSave
{
    private final AggregateBlueprint aggregateBlueprint;
    private final Connection connection;
    private final PhotonEntityState photonEntityState;
    private final PhotonOptions photonOptions;

    public PhotonAggregateSave(
        AggregateBlueprint aggregateBlueprint,
        Connection connection,
        PhotonEntityState photonEntityState,
        PhotonOptions photonOptions)
    {
        this.aggregateBlueprint = aggregateBlueprint;
        this.connection = connection;
        this.photonEntityState = photonEntityState;
        this.photonOptions = photonOptions;
    }

    public void save(Object aggregateInstance, Collection<String> fieldPathsToExclude)
    {
        PopulatedEntity aggregateRootEntity = new PopulatedEntity(aggregateBlueprint.getAggregateRootEntityBlueprint(), aggregateInstance);
        saveEntitiesRecursive(aggregateBlueprint.getAggregateRootEntityBlueprint(), Collections.singletonList(aggregateRootEntity), null, null, false, fieldPathsToExclude, "");
    }

    public void saveAll(Collection<?> aggregateInstances, Collection<String> fieldPathsToExclude)
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

    public void insertAll(Collection<?> aggregateInstances)
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
        Collection<String> fieldPathsToExclude,
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
                .filter(p -> tableBlueprint.isApplicableForEntityClass(p.getEntityInstance().getClass()))
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

        if(!isInsert && entityBlueprint.getVersionField() != null)
        {
            if(!populatedEntitiesToInsertList.isEmpty())
            {
                throw new PhotonOptimisticConcurrencyException();
            }

            for(PopulatedEntity populatedEntity : populatedEntities)
            {
                populatedEntity.incrementVersionNumber();
            }
        }

        insertPopulatedEntities(populatedEntitiesToInsert, parentPopulatedEntity, parentFieldBlueprint, entityBlueprint);

        setForeignKeyToParentForPopulatedEntities(populatedEntities, entityBlueprint.getFieldsWithChildEntities());

        insertAndDeleteFlattenedCollectionFields(populatedEntities, entityBlueprint.getFlattenedCollectionFields());

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

        List<TableValue> childIds = populatedEntities
            .stream()
            .map(PopulatedEntity::getPrimaryKeyValue)
            .filter(Objects::nonNull) // Auto increment entities that have not been inserted yet will have null primary key values.
            .map(TableValue::new)
            .collect(Collectors.toList());

        Set<TableValue> trackedKeys =
            photonEntityState.getTrackedChildrenKeys(parentFieldBlueprint, parentPopulatedEntity.getPrimaryKey());
        if(trackedKeys != null && new HashSet<>(childIds).equals(trackedKeys))
        {
            return;
        }

        if(tableBlueprint.isPrimaryKeyMappedToField())
        {
            String primaryKeyColumnName = tableBlueprint.getPrimaryKeyColumnName();
            String selectOrphansSql = tableBlueprint.getSelectOrphansSql();
            if(selectOrphansSql == null)
            {
                // If the primary key and foreign key to parent are equal, there won't be any select orphans sql because
                // there can't be any orphans, so just return.
                return;
            }

            List<Object> orphanIds;

            try(PhotonPreparedStatement statement = new PhotonPreparedStatement(selectOrphansSql, false, connection, photonOptions))
            {
                statement.setNextParameter(
                    parentPopulatedEntity.getPrimaryKeyValue(),
                    tableBlueprint.getParentTableBlueprint().getPrimaryKeyColumn().getColumnDataType(),
                    tableBlueprint.getParentTableBlueprint().getPrimaryKeyColumnSerializer());
                statement.setNextArrayParameter(
                    childIds.stream().map(TableValue::getValue).collect(Collectors.toList()),
                    tableBlueprint.getPrimaryKeyColumn().getColumnDataType(),
                    tableBlueprint.getPrimaryKeyColumnSerializer());
                List<PhotonQueryResultRow> rows =
                    statement.executeQuery(Collections.singletonList(primaryKeyColumnName));
                orphanIds =
                    rows.stream().map(r -> r.getValue(primaryKeyColumnName)).collect(Collectors.toList());
            }

            if(orphanIds.size() == 0)
            {
                return;
            }

            deleteOrphansAndTheirChildrenRecursive(orphanIds, entityBlueprint, Collections.emptyList());

            photonEntityState.untrackChildrenRecursive(
                parentFieldBlueprint,
                parentPopulatedEntity.getPrimaryKey(),
                entityBlueprint,
                tableBlueprint,
                orphanIds.stream().map(TableValue::new).collect(Collectors.toList()));
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
                if(tableBlueprint.isApplicableForEntityClass(populatedEntity.getEntityInstance().getClass()))
                {
                    continue;
                }

                String primaryKeyColumnName = tableBlueprint.getPrimaryKeyColumnName();
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

        Map<TableBlueprint, List<PopulatedEntity>> updatedOrUpdateToDateEntities = new LinkedHashMap<>();

        for(TableBlueprint tableBlueprint : entityBlueprint.getTableBlueprintsForInsertOrUpdate())
        {
            String updateSql = tableBlueprint.getUpdateSql();
            final List<PopulatedEntity> attemptedUpdatedPopulatedEntities = new ArrayList<>(populatedEntities.size());
            List<PopulatedEntity> upToDateEntities = new ArrayList<>();
            Map<TableBlueprintAndKey, List<ParameterValue>> updatedTrackedValues = new HashMap<>();

            try(PhotonPreparedStatement updateStatement = new PhotonPreparedStatement(updateSql, false, connection, photonOptions))
            {
                for (PopulatedEntity<?> populatedEntity : populatedEntities)
                {
                    if(!tableBlueprint.isApplicableForEntityClass(populatedEntity.getEntityInstance().getClass()))
                    {
                        continue;
                    }

                    List<ParameterValue> trackedValues =
                        photonEntityState.getTrackedValues(tableBlueprint, populatedEntity.getPrimaryKey());
                    PopulatedEntity.GetParameterValuesResult result =
                        populatedEntity.getParameterValuesForUpdate(tableBlueprint, parentPopulatedEntity, trackedValues);
                    if(result.isSkipped())
                    {
                        continue;
                    }

                    if(result.getValues().isEmpty())
                    {
                        upToDateEntities.add(populatedEntity);
                        continue;
                    }

                    updateStatement.setNextParameters(result.getValues());
                    updateStatement.addToBatch();
                    attemptedUpdatedPopulatedEntities.add(populatedEntity);

                    if(!trackedValues.isEmpty())
                    {
                        updatedTrackedValues.put(
                            new TableBlueprintAndKey(tableBlueprint, populatedEntity.getPrimaryKey()),
                            result.getValues());
                    }
                }

                int[] rowUpdateCounts = updateStatement.executeBatch();

                List<PopulatedEntity> updatedPopulatedEntitiesForTable = IntStream
                    .range(0, attemptedUpdatedPopulatedEntities.size())
                    .filter(i -> rowUpdateCounts[i] > 0)
                    .mapToObj(attemptedUpdatedPopulatedEntities::get)
                    .collect(Collectors.toList());

                updatedPopulatedEntitiesForTable.addAll(upToDateEntities);
                updatedOrUpdateToDateEntities.put(tableBlueprint, updatedPopulatedEntitiesForTable);

                for(Map.Entry<TableBlueprintAndKey, List<ParameterValue>> entry : updatedTrackedValues.entrySet())
                {
                    photonEntityState.updateTrackedValues(entry.getKey(), entry.getValue());
                }
            }
        }

        return updatedOrUpdateToDateEntities;
    }

    private void insertPopulatedEntities(
        Map<TableBlueprint, List<PopulatedEntity>> populatedEntities,
        PopulatedEntity parentPopulatedEntity,
        FieldBlueprint parentFieldBlueprint,
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
                    if(!tableBlueprint.isApplicableForEntityClass(populatedEntity.getEntityInstance().getClass()))
                    {
                        continue;
                    }
                    String insertSql = tableBlueprint.getInsertSql();
                    boolean populateGeneratedKeys = tableBlueprint.getPrimaryKeyColumn().isAutoIncrementColumn();
                    boolean shouldInsertUsingPrimaryKeySql = tableBlueprint.shouldInsertUsingPrimaryKeySql(populatedEntity, tableBlueprint);
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
                        List<ParameterValue> values = populatedEntity
                            .getParameterValuesForInsert(tableBlueprint, parentPopulatedEntity, shouldInsertUsingPrimaryKeySql);
                        insertStatement.setNextParameters(values);
                        insertStatement.executeInsert();
                        Long generatedKey = insertStatement.getGeneratedKeys().get(0);
                        populatedEntity.setPrimaryKeyValue(generatedKey);

                        updateTrackedValuesAndAddTrackedChild(
                            populatedEntity,
                            tableBlueprint,
                            parentPopulatedEntity,
                            parentFieldBlueprint
                        );
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
                        if(!tableBlueprint.isApplicableForEntityClass(populatedEntity.getEntityInstance().getClass()))
                        {
                            continue;
                        }
                        if(tableBlueprint.shouldInsertUsingPrimaryKeySql(populatedEntity, tableBlueprint))
                        {
                            insertEntityWithPrimaryKeySqlBatchList.add(populatedEntity);
                            continue;
                        }
                        List<ParameterValue> values =
                            populatedEntity.getParameterValuesForInsert(tableBlueprint, parentPopulatedEntity, false);
                        insertStatement.setNextParameters(values);
                        insertStatement.addToBatch();
                        insertedEntityBatchList.add(populatedEntity);
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

                    for (PopulatedEntity populatedEntity : insertedEntityBatchList)
                    {
                        updateTrackedValuesAndAddTrackedChild(populatedEntity,
                            tableBlueprint,
                            parentPopulatedEntity,
                            parentFieldBlueprint
                        );
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
                            List<ParameterValue> values =
                                populatedEntity.getParameterValuesForInsert(tableBlueprint, parentPopulatedEntity, true);
                            insertStatement.setNextParameters(values);
                            insertStatement.addToBatch();
                            insertedEntityBatchList.add(populatedEntity);
                        }

                        insertStatement.executeBatch();

                        for(PopulatedEntity populatedEntity : insertEntityWithPrimaryKeySqlBatchList)
                        {
                            updateTrackedValuesAndAddTrackedChild(
                                populatedEntity,
                                tableBlueprint,
                                parentPopulatedEntity,
                                parentFieldBlueprint
                            );
                        }
                    }
                }
            }
        }
    }

    private void updateTrackedValuesAndAddTrackedChild(
        PopulatedEntity<?> populatedEntity,
        TableBlueprint tableBlueprint,
        PopulatedEntity<?> parentPopulatedEntity,
        FieldBlueprint parentFieldBlueprint)
    {
        photonEntityState.updateTrackedValues(
            tableBlueprint,
            populatedEntity.getPrimaryKey(),
            populatedEntity.getParameterValues(tableBlueprint, parentPopulatedEntity));
        if(parentPopulatedEntity != null)
        {
            photonEntityState.addTrackedChild(
                parentFieldBlueprint,
                parentPopulatedEntity.getPrimaryKey(),
                populatedEntity.getEntityBlueprint(),
                populatedEntity.getPrimaryKey());
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

    private void insertAndDeleteFlattenedCollectionFields(List<PopulatedEntity> populatedEntities, List<FieldBlueprint> flattenedCollectionFields)
    {
        if(populatedEntities == null || populatedEntities.isEmpty())
        {
            return;
        }

        List<Object> ids = populatedEntities
            .stream()
            .map(PopulatedEntity::getPrimaryKeyValue)
            .collect(Collectors.toList());

        for (FieldBlueprint fieldBlueprint : flattenedCollectionFields)
        {
            FlattenedCollectionBlueprint flattenedCollectionBlueprint = fieldBlueprint.getFlattenedCollectionBlueprint();
            Map<TableValue, Collection> existingFlattenedCollectionValues = getExistingFlattenedCollectionValues(ids, flattenedCollectionBlueprint);

            try(PhotonPreparedStatement insertStatement = new PhotonPreparedStatement(flattenedCollectionBlueprint.getInsertSql(), false, connection, photonOptions))
            {
                for (PopulatedEntity populatedEntity : populatedEntities)
                {
                    Collection flattenedCollectionValues = (Collection) populatedEntity.getInstanceValue(fieldBlueprint, null);
                    if(flattenedCollectionValues == null)
                    {
                        flattenedCollectionValues = Collections.emptyList();
                    }

                    Collection trackedValues = photonEntityState
                        .getTrackedFlattenedCollectionValues(fieldBlueprint, populatedEntity.getPrimaryKey());
                    if(trackedValues != null && CollectionUtils.isEqualCollection(trackedValues, flattenedCollectionValues))
                    {
                        continue;
                    }

                    EntityBlueprint populatedEntityBlueprint = populatedEntity.getEntityBlueprint();
                    Collection existingCollectionValues = existingFlattenedCollectionValues.get(populatedEntity.getPrimaryKey());
                    if(existingCollectionValues == null)
                    {
                        existingCollectionValues = Collections.emptyList();
                    }
                    final Collection existingCollectionValuesFinal = existingCollectionValues;

                    final Collection flattenedCollectionValuesFinal = (Collection) flattenedCollectionValues
                        .stream()
                        .distinct()
                        .collect(Collectors.toList());

                    Collection valuesToDelete = (Collection) existingCollectionValues
                        .stream()
                        .filter(value -> !flattenedCollectionValuesFinal.contains(value))
                        .collect(Collectors.toList());

                    if(!valuesToDelete.isEmpty())
                    {
                        try(PhotonPreparedStatement deleteStatement = new PhotonPreparedStatement(
                            flattenedCollectionBlueprint.getDeleteForeignKeysSql(),
                            false,
                            connection,
                            photonOptions))
                        {
                            deleteStatement.setNextArrayParameter(valuesToDelete, flattenedCollectionBlueprint.getColumnDataType(), null);
                            deleteStatement.setNextParameter(populatedEntity.getPrimaryKeyValue(), populatedEntityBlueprint.getTableBlueprint().getPrimaryKeyColumn().getColumnDataType(), populatedEntityBlueprint.getTableBlueprint().getPrimaryKeyColumnSerializer());
                            deleteStatement.executeUpdate();
                        }
                    }

                    Collection valuesToInsert = (Collection) flattenedCollectionValuesFinal
                        .stream()
                        .filter(value -> !existingCollectionValuesFinal.contains(value))
                        .collect(Collectors.toList());

                    for (Object foreignKeyValue : valuesToInsert)
                    {
                        insertStatement.setNextParameter(foreignKeyValue, flattenedCollectionBlueprint.getColumnDataType(), null);
                        insertStatement.setNextParameter(
                            populatedEntity.getPrimaryKeyValue(),
                            populatedEntityBlueprint.getTableBlueprint().getPrimaryKeyColumn().getColumnDataType(),
                            populatedEntityBlueprint.getTableBlueprint().getPrimaryKeyColumnSerializer());
                        insertStatement.addToBatch();
                    }
                }

                insertStatement.executeBatch();
            }
        }
    }

    private Map<TableValue, Collection> getExistingFlattenedCollectionValues(
        List<Object> ids,
        FlattenedCollectionBlueprint flattenedCollectionBlueprint)
    {
        Map<TableValue, Collection> existingFlattenedCollectionValues = new HashMap<>();
        List<PhotonQueryResultRow> photonQueryResultRows;

        try (PhotonPreparedStatement statement = new PhotonPreparedStatement(flattenedCollectionBlueprint.getSelectSql(), false, connection, photonOptions))
        {
            statement.setNextArrayParameter(ids, flattenedCollectionBlueprint.getColumnDataType(), null);
            photonQueryResultRows = statement.executeQuery(flattenedCollectionBlueprint.getSelectColumnNames());
        }

        Converter valueConverter = Convert.getConverterIfExists(flattenedCollectionBlueprint.getFieldClass());

        for(PhotonQueryResultRow photonQueryResultRow : photonQueryResultRows)
        {
            Object joinColumnValue = photonQueryResultRow.getValue(flattenedCollectionBlueprint.getForeignKeyToParent());
            Object value = valueConverter.convert(photonQueryResultRow.getValue(flattenedCollectionBlueprint.getColumnName()));
            Collection values = existingFlattenedCollectionValues.computeIfAbsent(new TableValue(joinColumnValue), k -> new ArrayList<>());
            values.add(value);
        }

        return existingFlattenedCollectionValues;
    }
}
