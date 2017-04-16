package org.photon.blueprints;

import org.photon.exceptions.PhotonException;
import org.photon.sqlbuilders.DeleteSqlBuilderService;
import org.photon.sqlbuilders.InsertSqlBuilderService;
import org.photon.sqlbuilders.SelectSqlBuilderService;
import org.photon.sqlbuilders.UpdateSqlBuilderService;

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
        DeleteSqlBuilderService deleteSqlBuilderService)
    {
        if(aggregateRootEntityBlueprint == null)
        {
            throw new PhotonException("Aggregate root entity cannot be null.");
        }

        this.entityBlueprints = new ArrayList<>();
        findAllAggregateEntityBlueprints(aggregateRootEntityBlueprint);

        this.aggregateRootEntityBlueprint = aggregateRootEntityBlueprint;
        selectSqlBuilderService.buildSelectSqlTemplates(aggregateRootEntityBlueprint);
        updateSqlBuilderService.buildUpdateSqlTemplates(aggregateRootEntityBlueprint);
        insertSqlBuilderService.buildInsertSqlTemplates(aggregateRootEntityBlueprint);
        deleteSqlBuilderService.buildDeleteSqlTemplates(aggregateRootEntityBlueprint);
    }

    private void findAllAggregateEntityBlueprints(AggregateEntityBlueprint aggregateEntityBlueprint)
    {
        this.entityBlueprints.add(aggregateEntityBlueprint);
        for(FieldBlueprint fieldBlueprint : aggregateEntityBlueprint.getFieldsWithChildEntities())
        {
            findAllAggregateEntityBlueprints(fieldBlueprint.getChildEntityBlueprint());
        }
    }
}
