package photon;

import photon.blueprints.*;
import photon.exceptions.PhotonException;
import photon.sqlbuilders.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public class Photon
{
    private final DataSource dataSource;
    private final Map<Class, AggregateBlueprint> registeredAggregates;

    private final SelectSqlBuilderService selectSqlBuilderService;
    private final UpdateSqlBuilderService updateSqlBuilderService;
    private final InsertSqlBuilderService insertSqlBuilderService;
    private final DeleteSqlBuilderService deleteSqlBuilderService;
    private final EntityBlueprintConstructorService entityBlueprintConstructorService;

    public Photon(DataSource dataSource)
    {
        this.dataSource = dataSource;
        this.registeredAggregates = new HashMap<>();

        SqlJoinClauseBuilderService sqlJoinClauseBuilderService = new SqlJoinClauseBuilderService();
        this.selectSqlBuilderService = new SelectSqlBuilderService(sqlJoinClauseBuilderService);
        this.updateSqlBuilderService = new UpdateSqlBuilderService();
        this.insertSqlBuilderService = new InsertSqlBuilderService();
        this.deleteSqlBuilderService = new DeleteSqlBuilderService(sqlJoinClauseBuilderService);
        this.entityBlueprintConstructorService = new EntityBlueprintConstructorService();
    }

    public Photon(String url, String user, String password)
    {
        this(new GenericDataSource(url, user, password));
    }

    public PhotonTransaction beginTransaction()
    {
        return new PhotonTransaction(getConnection(), registeredAggregates, entityBlueprintConstructorService);
    }

    public AggregateEntityBlueprintBuilder registerAggregate(Class aggregateRootClass)
    {
        return new AggregateEntityBlueprintBuilder(aggregateRootClass, this, entityBlueprintConstructorService);
    }

    public void registerAggregate(AggregateEntityBlueprint aggregateRootEntityBlueprint)
    {
        if(aggregateRootEntityBlueprint == null)
        {
            throw new PhotonException("AggregateBlueprint root entityBlueprint cannot be null.");
        }
        if(registeredAggregates.containsKey(aggregateRootEntityBlueprint.getEntityClass()))
        {
            // TODO: Print warning that aggregate was already registered and old registration is being replaced.
        }
        registeredAggregates.put(
            aggregateRootEntityBlueprint.getEntityClass(),
            new AggregateBlueprint(
                aggregateRootEntityBlueprint,
                selectSqlBuilderService,
                updateSqlBuilderService,
                insertSqlBuilderService,
                deleteSqlBuilderService
            )
        );
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
