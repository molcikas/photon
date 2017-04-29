package com.github.molcikas.photon.blueprints;

import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.options.PhotonOptions;
import com.github.molcikas.photon.sqlbuilders.DeleteSqlBuilderService;
import com.github.molcikas.photon.sqlbuilders.InsertSqlBuilderService;
import com.github.molcikas.photon.sqlbuilders.SelectSqlBuilderService;
import com.github.molcikas.photon.sqlbuilders.UpdateSqlBuilderService;

import java.util.*;

public class AggregateBlueprint<T>
{
    private final AggregateEntityBlueprint aggregateRootEntityBlueprint;
    private final List<AggregateEntityBlueprint> entityBlueprints;

    public AggregateEntityBlueprint getAggregateRootEntityBlueprint()
    {
        return aggregateRootEntityBlueprint;
    }

    public List<AggregateEntityBlueprint> getEntityBlueprints()
    {
        return Collections.unmodifiableList(entityBlueprints);
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

        this.entityBlueprints = new ArrayList<>();
        findAllAggregateEntityBlueprintsRecursive(aggregateRootEntityBlueprint);

        this.aggregateRootEntityBlueprint = aggregateRootEntityBlueprint;
        selectSqlBuilderService.buildSelectSqlTemplates(aggregateRootEntityBlueprint, photonOptions);
        updateSqlBuilderService.buildUpdateSqlTemplates(aggregateRootEntityBlueprint, photonOptions);
        insertSqlBuilderService.buildInsertSqlTemplates(aggregateRootEntityBlueprint, photonOptions);
        deleteSqlBuilderService.buildDeleteSqlTemplates(aggregateRootEntityBlueprint, photonOptions);
    }

    private void findAllAggregateEntityBlueprintsRecursive(AggregateEntityBlueprint aggregateEntityBlueprint)
    {
        this.entityBlueprints.add(aggregateEntityBlueprint);
        for(FieldBlueprint fieldBlueprint : aggregateEntityBlueprint.getFieldsWithChildEntities())
        {
            findAllAggregateEntityBlueprintsRecursive(fieldBlueprint.getChildEntityBlueprint());
        }
    }
}
