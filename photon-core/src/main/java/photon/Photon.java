package photon;

import photon.exceptions.PhotonException;
import photon.blueprints.AggregateBlueprint;
import photon.blueprints.EntityBlueprint;
import photon.blueprints.EntityBlueprintBuilder;
import photon.blueprints.SelectSqlBuilderService;

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

    public Photon(String url, String user, String password)
    {
        this(new GenericDataSource(url, user, password));
    }

    public PhotonConnection open()
    {
        return new PhotonConnection(getConnection(), false, registeredAggregates);
    }

    public PhotonConnection beginTransaction()
    {
        return new PhotonConnection(getConnection(), true, registeredAggregates);
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

    private Connection getConnection()
    {
        try
        {
            return dataSource.getConnection();
        }
        catch(Exception ex)
        {
            throw new PhotonException("Error getting connection.", ex);
        }
    }
}
