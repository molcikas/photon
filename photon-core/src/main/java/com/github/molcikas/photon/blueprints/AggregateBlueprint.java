package com.github.molcikas.photon.blueprints;

import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.options.PhotonOptions;
import com.github.molcikas.photon.sqlbuilders.DeleteSqlBuilderService;
import com.github.molcikas.photon.sqlbuilders.InsertSqlBuilderService;
import com.github.molcikas.photon.sqlbuilders.SelectSqlBuilderService;
import com.github.molcikas.photon.sqlbuilders.UpdateSqlBuilderService;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class AggregateBlueprint<T>
{
    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode
    private class EntityBlueprintKey
    {
        private final String fieldPath;
        private final int order;
    }

    private final EntityBlueprint aggregateRootEntityBlueprint;
    private final Map<EntityBlueprintKey, EntityBlueprint> entityBlueprints;

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
        findAllEntityBlueprintsRecursive(aggregateRootEntityBlueprint, "", new Integer[] {0});

        this.aggregateRootEntityBlueprint = aggregateRootEntityBlueprint;
        SelectSqlBuilderService.buildSelectSqlTemplates(aggregateRootEntityBlueprint, photonOptions);
        UpdateSqlBuilderService.buildUpdateSqlTemplates(aggregateRootEntityBlueprint, photonOptions);
        InsertSqlBuilderService.buildInsertSqlTemplates(aggregateRootEntityBlueprint, photonOptions);
        DeleteSqlBuilderService.buildDeleteSqlTemplates(aggregateRootEntityBlueprint, photonOptions);
    }

    public List<EntityBlueprint> getEntityBlueprints(List<String> excludedFieldPaths)
    {
        List<String> fieldPaths = entityBlueprints
            .keySet()
            .stream()
            .map(EntityBlueprintKey::getFieldPath)
            .collect(Collectors.toList());

        Optional<String> missingPath = excludedFieldPaths
            .stream()
            .filter(f -> !fieldPaths.contains(f))
            .findFirst();
        if(missingPath.isPresent())
        {
            throw new PhotonException(
                "The field path '%s' does not exist for '%s'.",
                missingPath.get(),
                aggregateRootEntityBlueprint.getEntityClassName()
            );
        }

        return entityBlueprints
            .entrySet()
            .stream()
            .filter(e -> !excludedFieldPaths.contains(e.getKey().getFieldPath()))
            .sorted(Comparator.comparingInt(e -> e.getKey().getOrder()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    /**
     * Pass the order inside an array so that it gets passed by reference instead of by value.
     *
     * @param entityBlueprint
     * @param fieldPath
     * @param order
     */
    private void findAllEntityBlueprintsRecursive(EntityBlueprint entityBlueprint, String fieldPath, Integer[] order)
    {
        entityBlueprints.put(new EntityBlueprintKey(fieldPath, order[0]),  entityBlueprint);
        order[0]++;
        for(FieldBlueprint fieldBlueprint : entityBlueprint.getFieldsWithChildEntities())
        {
            String childFieldPath = fieldPath + (StringUtils.isEmpty(fieldPath) ? "" : ".") + fieldBlueprint.getFieldName();
            findAllEntityBlueprintsRecursive(fieldBlueprint.getChildEntityBlueprint(), childFieldPath, order);
        }
    }
}
