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
    private final EntityBlueprint aggregateRootEntityBlueprint;
    private final Map<String, EntityBlueprint> entityBlueprints;

    public EntityBlueprint getAggregateRootEntityBlueprint()
    {
        return aggregateRootEntityBlueprint;
    }

    public Class<T> getAggregateRootClass()
    {
        return aggregateRootEntityBlueprint.getEntityClass();
    }

    public AggregateBlueprint(
        EntityBlueprint aggregateRootEntityBlueprint,
        PhotonOptions photonOptions)
    {
        if(aggregateRootEntityBlueprint == null)
        {
            throw new PhotonException("Aggregate root entity cannot be null.");
        }

        this.entityBlueprints = new HashMap<>();
        findAllEntityBlueprintsRecursive(aggregateRootEntityBlueprint, "");

        this.aggregateRootEntityBlueprint = aggregateRootEntityBlueprint;
        SelectSqlBuilderService.buildSelectSqlTemplates(aggregateRootEntityBlueprint, photonOptions);
        UpdateSqlBuilderService.buildUpdateSqlTemplates(aggregateRootEntityBlueprint, photonOptions);
        InsertSqlBuilderService.buildInsertSqlTemplates(aggregateRootEntityBlueprint, photonOptions);
        DeleteSqlBuilderService.buildDeleteSqlTemplates(aggregateRootEntityBlueprint, photonOptions);
    }

    public Map<String, EntityBlueprint> getEntityBlueprints(List<String> excludedFieldPaths)
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

        Map<String, EntityBlueprint> includedEntityBlueprints = entityBlueprints
            .entrySet()
            .stream()
            .filter(e -> !excludedFieldPaths.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return includedEntityBlueprints;
    }

    private void findAllEntityBlueprintsRecursive(EntityBlueprint entityBlueprint, String fieldPath)
    {
        this.entityBlueprints.put(fieldPath,  entityBlueprint);
        for(FieldBlueprint fieldBlueprint : entityBlueprint.getFieldsWithChildEntities())
        {
            String childFieldPath = fieldPath + (StringUtils.isEmpty(fieldPath) ? "" : ".") + fieldBlueprint.getFieldName();
            findAllEntityBlueprintsRecursive(fieldBlueprint.getChildEntityBlueprint(), childFieldPath);
        }
    }
}
