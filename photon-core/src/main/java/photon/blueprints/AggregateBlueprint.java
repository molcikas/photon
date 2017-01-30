package photon.blueprints;

import photon.exceptions.PhotonException;
import photon.sqlbuilders.DeleteSqlBuilderService;
import photon.sqlbuilders.InsertSqlBuilderService;
import photon.sqlbuilders.SelectSqlBuilderService;
import photon.sqlbuilders.UpdateSqlBuilderService;

import java.util.*;

public class AggregateBlueprint
{
    private final EntityBlueprint aggregateRootEntityBlueprint;
    private final Map<EntityBlueprint, String> entitySelectSqlTemplates;
    private final Map<EntityBlueprint, String> entityUpdateSqlTemplates;
    private final Map<EntityBlueprint, String> entityInsertSqlTemplates;
    private final Map<EntityBlueprint, String>  deleteAllChildrenSqlTemplates;
    private final Map<EntityBlueprint, String>  deleteChildrenExceptSqlTemplates;

    public EntityBlueprint getAggregateRootEntityBlueprint()
    {
        return aggregateRootEntityBlueprint;
    }

    public Class getAggregateRootClass()
    {
        return aggregateRootEntityBlueprint.getEntityClass();
    }

    public Map<EntityBlueprint, String> getEntitySelectSqlTemplates()
    {
        return Collections.unmodifiableMap(entitySelectSqlTemplates);
    }

    public String getEntityUpdateSqlTemplate(EntityBlueprint entityBlueprint)
    {
        return entityUpdateSqlTemplates.get(entityBlueprint);
    }

    public String getEntityInsertSqlTemplate(EntityBlueprint entityBlueprint)
    {
        return entityInsertSqlTemplates.get(entityBlueprint);
    }

    public String getDeleteAllChildrenSqlTemplate(EntityBlueprint entityBlueprint)
    {
        return deleteAllChildrenSqlTemplates.get(entityBlueprint);
    }

    public String getDeleteChildrenExceptSqlTemplate(EntityBlueprint entityBlueprint)
    {
        return deleteChildrenExceptSqlTemplates.get(entityBlueprint);
    }

    public AggregateBlueprint(
        EntityBlueprint aggregateRootEntityBlueprint,
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
        this.deleteAllChildrenSqlTemplates = deleteSqlBuilderService.buildDeleteAllChildrenSqlTemplates(aggregateRootEntityBlueprint);
        this.deleteChildrenExceptSqlTemplates = deleteSqlBuilderService.buildDeleteChildrenExceptSqlTemplates(aggregateRootEntityBlueprint);
    }
}
