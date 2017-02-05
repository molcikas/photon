package photon.blueprints;

import photon.exceptions.PhotonException;
import photon.sqlbuilders.DeleteSqlBuilderService;
import photon.sqlbuilders.InsertSqlBuilderService;
import photon.sqlbuilders.SelectSqlBuilderService;
import photon.sqlbuilders.UpdateSqlBuilderService;

import java.util.*;

public class AggregateBlueprint
{
    private final AggregateEntityBlueprint aggregateRootEntityBlueprint;
    private final Map<AggregateEntityBlueprint, String> entitySelectSqlTemplates;
    private final Map<AggregateEntityBlueprint, String> entityUpdateSqlTemplates;
    private final Map<AggregateEntityBlueprint, String> entityInsertSqlTemplates;
    private final Map<AggregateEntityBlueprint, String>  deleteChildrenExceptSqlTemplates;

    public AggregateEntityBlueprint getAggregateRootEntityBlueprint()
    {
        return aggregateRootEntityBlueprint;
    }

    public Class getAggregateRootClass()
    {
        return aggregateRootEntityBlueprint.getEntityClass();
    }

    public Map<AggregateEntityBlueprint, String> getEntitySelectSqlTemplates()
    {
        return Collections.unmodifiableMap(entitySelectSqlTemplates);
    }

    public String getEntityUpdateSqlTemplate(AggregateEntityBlueprint entityBlueprint)
    {
        return entityUpdateSqlTemplates.get(entityBlueprint);
    }

    public String getEntityInsertSqlTemplate(AggregateEntityBlueprint entityBlueprint)
    {
        return entityInsertSqlTemplates.get(entityBlueprint);
    }

    public String getDeleteChildrenExceptSqlTemplate(AggregateEntityBlueprint entityBlueprint)
    {
        return deleteChildrenExceptSqlTemplates.get(entityBlueprint);
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
        this.aggregateRootEntityBlueprint = aggregateRootEntityBlueprint;
        this.entitySelectSqlTemplates = selectSqlBuilderService.buildSelectSqlTemplates(aggregateRootEntityBlueprint);
        this.entityUpdateSqlTemplates = updateSqlBuilderService.buildUpdateSqlTemplates(aggregateRootEntityBlueprint);
        this.entityInsertSqlTemplates = insertSqlBuilderService.buildInsertSqlTemplates(aggregateRootEntityBlueprint);
        this.deleteChildrenExceptSqlTemplates = deleteSqlBuilderService.buildDeleteChildrenExceptSqlTemplates(aggregateRootEntityBlueprint);
    }
}
