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
import com.github.molcikas.photon.sqlbuilders.UpdateSqlBuilderService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

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
            Orphans.findAndDelete(entityBlueprint, populatedEntities, parentPopulatedEntity,
                parentFieldBlueprint, photonEntityState, connection, photonOptions);
            Orphans.findAndDeleteJoined(entityBlueprint, populatedEntities, connection, photonOptions);
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
            final List<PopulatedEntity> updatedPopulatedEntitiesForTable = new ArrayList<>(populatedEntities.size());

            for (PopulatedEntity<?> populatedEntity : populatedEntities)
            {
                if(!tableBlueprint.isApplicableForEntityClass(populatedEntity.getEntityInstance().getClass()))
                {
                    continue;
                }

                Map<String, ParameterValue> trackedValues =
                    photonEntityState.getTrackedValues(tableBlueprint, populatedEntity.getPrimaryKey());
                GetParameterValuesResult valuesForUpdateResult =
                    populatedEntity.getParameterValuesForUpdate(tableBlueprint, parentPopulatedEntity, trackedValues);

                if(valuesForUpdateResult.isSkipped())
                {
                    continue;
                }
                if(!valuesForUpdateResult.isChanged())
                {
                    updatedPopulatedEntitiesForTable.add(populatedEntity);
                    continue;
                }

                String updateSql = tableBlueprint.getUpdateSql(valuesForUpdateResult.getValues().keySet(), photonOptions);

                try(PhotonPreparedStatement updateStatement = new PhotonPreparedStatement(updateSql, false, connection, photonOptions))
                {
                    updateStatement.setNextParameters(new ArrayList<>(valuesForUpdateResult.getValues().values()));
                    int rowsUpdated = updateStatement.executeUpdate();

                    if(rowsUpdated > 0)
                    {
                        updatedPopulatedEntitiesForTable.add(populatedEntity);
                    }

                    if(!trackedValues.isEmpty())
                    {
                        photonEntityState.updateTrackedValues(
                            new TableBlueprintAndKey(tableBlueprint, populatedEntity.getPrimaryKey()),
                            valuesForUpdateResult.getValues());
                    }
                }
            }

            updatedOrUpdateToDateEntities.put(tableBlueprint, updatedPopulatedEntitiesForTable);
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
            for (PopulatedEntity populatedEntity : populatedEntities.get(tableBlueprint))
            {
                if (!tableBlueprint.isApplicableForEntityClass(populatedEntity.getEntityInstance().getClass()))
                {
                    continue;
                }
                String insertSql = tableBlueprint.getInsertSql();
                boolean populateGeneratedKeys = tableBlueprint.getPrimaryKeyColumn().isAutoIncrementColumn();
                boolean shouldInsertUsingPrimaryKeySql = tableBlueprint
                    .shouldInsertUsingPrimaryKeySql(populatedEntity, tableBlueprint);
                if (shouldInsertUsingPrimaryKeySql)
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
                        .getParameterValuesForInsert(tableBlueprint, parentPopulatedEntity,
                            shouldInsertUsingPrimaryKeySql);
                    insertStatement.setNextParameters(values);
                    insertStatement.executeInsert();

                    if (populateGeneratedKeys)
                    {
                        Long generatedKey = insertStatement.getGeneratedKeys().get(0);
                        populatedEntity.setPrimaryKeyValue(generatedKey);
                    }

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

    private void updateTrackedValuesAndAddTrackedChild(
        PopulatedEntity<?> populatedEntity,
        TableBlueprint tableBlueprint,
        PopulatedEntity<?> parentPopulatedEntity,
        FieldBlueprint parentFieldBlueprint)
    {
        photonEntityState.updateTrackedValues(
            tableBlueprint,
            populatedEntity.getPrimaryKey(),
            populatedEntity.getParameterValuesForUpdate(tableBlueprint, parentPopulatedEntity, null).getValues());
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

        for (FieldBlueprint fieldBlueprint : flattenedCollectionFields)
        {
            FlattenedCollectionBlueprint flattenedCollectionBlueprint = fieldBlueprint.getFlattenedCollectionBlueprint();

            for (PopulatedEntity populatedEntity : populatedEntities)
            {
                Collection values = (Collection) populatedEntity.getInstanceValue(fieldBlueprint, null);
                if(values == null)
                {
                    values = Collections.emptyList();
                }

                Collection existingValues = photonEntityState
                    .getTrackedFlattenedCollectionValues(fieldBlueprint, populatedEntity.getPrimaryKey());
                if (CollectionUtils.isEmpty(existingValues))
                {
                    existingValues = getExistingFlattenedCollectionValues(
                            populatedEntity.getPrimaryKeyValue(),
                            flattenedCollectionBlueprint);
                }
                if (existingValues == null)
                {
                    existingValues = Collections.emptyList();
                }
                if(CollectionUtils.isEqualCollection(existingValues, values))
                {
                    continue;
                }

                EntityBlueprint populatedEntityBlueprint = populatedEntity.getEntityBlueprint();
                final Collection existingValuesFinal = existingValues;

                final Collection valuesFinal = (Collection) values
                    .stream()
                    .distinct()
                    .collect(Collectors.toList());

                Collection valuesToDelete = (Collection) existingValues
                    .stream()
                    .filter(value -> !valuesFinal.contains(value))
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

                Collection valuesToInsert = (Collection) valuesFinal
                    .stream()
                    .filter(value -> !existingValuesFinal.contains(value))
                    .collect(Collectors.toList());

                for (Object foreignKeyValue : valuesToInsert)
                {
                    try(PhotonPreparedStatement insertStatement = new PhotonPreparedStatement(flattenedCollectionBlueprint.getInsertSql(), false, connection, photonOptions))
                    {
                        insertStatement.setNextParameter(foreignKeyValue, flattenedCollectionBlueprint.getColumnDataType(), null);
                        insertStatement.setNextParameter(
                                populatedEntity.getPrimaryKeyValue(),
                                populatedEntityBlueprint.getTableBlueprint().getPrimaryKeyColumn().getColumnDataType(),
                                populatedEntityBlueprint.getTableBlueprint().getPrimaryKeyColumnSerializer());
                        insertStatement.executeInsert();
                    }
                }
            }
        }
    }

    private Collection getExistingFlattenedCollectionValues(
        Object id,
        FlattenedCollectionBlueprint flattenedCollectionBlueprint)
    {
        Collection exitingValues = new ArrayList();
        List<PhotonQueryResultRow> photonQueryResultRows;

        try (PhotonPreparedStatement statement = new PhotonPreparedStatement(flattenedCollectionBlueprint.getSelectSql(), false, connection, photonOptions))
        {
            statement.setNextArrayParameter(Collections.singletonList(id), flattenedCollectionBlueprint.getColumnDataType(), null);
            photonQueryResultRows = statement.executeQuery(
                flattenedCollectionBlueprint.getSelectColumnNames(),
                flattenedCollectionBlueprint.getSelectColumnNamesLowerCase());
        }

        Converter valueConverter = Convert.getConverterIfExists(flattenedCollectionBlueprint.getFieldClass());

        for(PhotonQueryResultRow photonQueryResultRow : photonQueryResultRows)
        {
            Object value = valueConverter.convert(photonQueryResultRow.getValue(flattenedCollectionBlueprint.getColumnName()));
            exitingValues.add(value);
        }

        return exitingValues;
    }
}
