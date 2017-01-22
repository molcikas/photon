package photon.blueprints;

import photon.exceptions.PhotonException;

import java.util.*;

public class AggregateBlueprint
{
    private final EntityBlueprint aggregateRootEntityBlueprint;
    private final Map<EntityBlueprint, String> entitySelectSqlTemplates;

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

    public AggregateBlueprint(EntityBlueprint aggregateRootEntityBlueprint, SelectSqlBuilderService selectSqlBuilderService)
    {
        if(aggregateRootEntityBlueprint == null)
        {
            throw new PhotonException("AggregateBlueprint root entityBlueprint cannot be null.");
        }
        this.aggregateRootEntityBlueprint = aggregateRootEntityBlueprint;
        this.entitySelectSqlTemplates = selectSqlBuilderService.buildSelectSqlTemplates(aggregateRootEntityBlueprint);
    }
}
