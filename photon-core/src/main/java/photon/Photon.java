package photon;

import photon.exceptions.PhotonException;
import photon.blueprints.AggregateBlueprint;
import photon.blueprints.EntityBlueprint;
import photon.blueprints.EntityBlueprintBuilder;
import photon.transaction.PhotonTransaction;
import photon.transaction.SelectSqlBuilderService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public class Photon
{
    private final DataSource dataSource;
    private final Map<Class, AggregateBlueprint> registeredAggregates;

    private final SelectSqlBuilderService selectSqlBuilderService;

    public Photon(DataSource dataSource)
    {
        this.dataSource = dataSource;
        this.registeredAggregates = new HashMap<>();

        this.selectSqlBuilderService = new SelectSqlBuilderService();
    }

    public PhotonTransaction beginTransaction()
    {
        try
        {
            Connection connection = dataSource.getConnection();
            return new PhotonTransaction(connection, registeredAggregates);
        }
        catch(Exception ex)
        {
            throw new PhotonException("Error getting connection.", ex);
        }
    }

    public EntityBlueprintBuilder registerAggregate(Class aggregateRootClass)
    {
        return new EntityBlueprintBuilder(aggregateRootClass, this);
    }

    public void registerAggregate(EntityBlueprint aggregateRootEntityBlueprint)
    {
        if(aggregateRootEntityBlueprint == null)
        {
            throw new PhotonException("AggregateBlueprint root entityBlueprint cannot be null.");
        }
        if(registeredAggregates.containsKey(aggregateRootEntityBlueprint.getEntityClass()))
        {
            throw new PhotonException(String.format("The aggregate '%s' is already registered with this instance of Photon.", aggregateRootEntityBlueprint.getEntityClassName()));
        }
        registeredAggregates.put(aggregateRootEntityBlueprint.getEntityClass(), new AggregateBlueprint(aggregateRootEntityBlueprint, selectSqlBuilderService));
    }
}
