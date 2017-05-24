package com.github.molcikas.photon.blueprints;

import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.options.PhotonOptions;
import com.github.molcikas.photon.sqlbuilders.DeleteSqlBuilderService;
import com.github.molcikas.photon.sqlbuilders.InsertSqlBuilderService;
import com.github.molcikas.photon.sqlbuilders.SelectSqlBuilderService;
import com.github.molcikas.photon.sqlbuilders.UpdateSqlBuilderService;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class AggregateBlueprint<T>
{
    private final AggregateEntityBlueprint aggregateRootEntityBlueprint;
    private final Map<String, AggregateEntityBlueprint> entityBlueprints;

    public AggregateEntityBlueprint getAggregateRootEntityBlueprint()
    {
        return aggregateRootEntityBlueprint;
    }

    public Class<T> getAggregateRootClass()
    {
        return aggregateRootEntityBlueprint.getEntityClass();
    }

    public AggregateBlueprint(
        AggregateEntityBlueprint aggregateRootEntityBlueprint,
        SelectSqlBuilderService selectSqlBuilderService,
        UpdateSqlBuilderService updateSqlBuilderService,
        InsertSqlBuilderService insertSqlBuilderService,
        DeleteSqlBuilderService deleteSqlBuilderService,
        PhotonOptions photonOptions)
    {
        if(aggregateRootEntityBlueprint == null)
        {
            throw new PhotonException("Aggregate root entity cannot be null.");
        }

        this.entityBlueprints = new HashMap<>();
        findAllAggregateEntityBlueprintsRecursive(aggregateRootEntityBlueprint, "");

        this.aggregateRootEntityBlueprint = aggregateRootEntityBlueprint;
        selectSqlBuilderService.buildSelectSqlTemplates(aggregateRootEntityBlueprint, photonOptions);
        updateSqlBuilderService.buildUpdateSqlTemplates(aggregateRootEntityBlueprint, photonOptions);
        insertSqlBuilderService.buildInsertSqlTemplates(aggregateRootEntityBlueprint, photonOptions);
        deleteSqlBuilderService.buildDeleteSqlTemplates(aggregateRootEntityBlueprint, photonOptions);
    }

    public Map<String, AggregateEntityBlueprint> getEntityBlueprints(List<String> excludedFieldPaths)
    {
        Optional<String> missingPath = excludedFieldPaths
            .stream()
            .filter(f -> !entityBlueprints.containsKey(f))
            .findFirst();
        if(missingPath.isPresent())
        {
            throw new PhotonException(String.format(
                "The field path '%s' does not exist for '%s'.",
                missingPath.get(),
                aggregateRootEntityBlueprint.getEntityClassName()
            ));
        }

        Map<String, AggregateEntityBlueprint> includedEntityBlueprints = entityBlueprints
            .entrySet()
            .stream()
            .filter(e -> !excludedFieldPaths.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return includedEntityBlueprints;
    }

    private void findAllAggregateEntityBlueprintsRecursive(AggregateEntityBlueprint aggregateEntityBlueprint, String fieldPath)
    {
        this.entityBlueprints.put(fieldPath,  aggregateEntityBlueprint);
        for(FieldBlueprint fieldBlueprint : aggregateEntityBlueprint.getFieldsWithChildEntities())
        {
            String childFieldPath = fieldPath + (StringUtils.isEmpty(fieldPath) ? "" : ".") + fieldBlueprint.getFieldName();
            findAllAggregateEntityBlueprintsRecursive(fieldBlueprint.getChildEntityBlueprint(), childFieldPath);
        }
    }
}
