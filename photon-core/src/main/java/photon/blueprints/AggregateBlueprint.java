package photon.blueprints;

import photon.exceptions.PhotonException;
import photon.query.SelectSqlBuilderService;

import java.util.*;

public class AggregateBlueprint
{
    private final EntityBlueprint aggregateRootEntityBlueprint;
    private final Map<EntityBlueprint, String> entitySelectSql;

    public EntityBlueprint getAggregateRootEntityBlueprint()
    {
        return aggregateRootEntityBlueprint;
    }

    public Class getAggregateRootClass()
    {
        return aggregateRootEntityBlueprint.getEntityClass();
    }

    public String getAggregateRootClassName()
    {
        return aggregateRootEntityBlueprint.getEntityClassName();
    }

    public Map<EntityBlueprint, String> getEntitySelectSql()
    {
        return Collections.unmodifiableMap(entitySelectSql);
    }

    public AggregateBlueprint(EntityBlueprint aggregateRootEntityBlueprint, SelectSqlBuilderService selectSqlBuilderService)
    {
        if(aggregateRootEntityBlueprint == null)
        {
            throw new PhotonException("AggregateBlueprint root entityBlueprint cannot be null.");
        }
        this.aggregateRootEntityBlueprint = aggregateRootEntityBlueprint;
        this.entitySelectSql = selectSqlBuilderService.buildSelectSql(aggregateRootEntityBlueprint);
    }
}
