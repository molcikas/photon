package com.github.molcikas.photon;

import com.github.molcikas.photon.blueprints.AggregateBlueprint;
import com.github.molcikas.photon.blueprints.AggregateEntityBlueprint;
import com.github.molcikas.photon.blueprints.AggregateBlueprintBuilder;
import com.github.molcikas.photon.blueprints.EntityBlueprintConstructorService;
import com.github.molcikas.photon.options.PhotonOptions;
import com.github.molcikas.photon.sqlbuilders.*;
import com.github.molcikas.photon.exceptions.PhotonException;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * The top-level class for Photon.
 */
public class Photon
{
    private final DataSource dataSource;
    private final Map<Class, AggregateBlueprint> registeredAggregates;
    private final Map<String, AggregateBlueprint> registeredViewModelAggregates;

    private final SelectSqlBuilderService selectSqlBuilderService;
    private final UpdateSqlBuilderService updateSqlBuilderService;
    private final InsertSqlBuilderService insertSqlBuilderService;
    private final DeleteSqlBuilderService deleteSqlBuilderService;
    private final EntityBlueprintConstructorService entityBlueprintConstructorService;

    private final PhotonOptions photonOptions;

    /**
     * Constructs a new photon. Use this constructor if not using a connection pooler. Uses the default photon
     * options.
     *
     * @param url - the JDBC URL
     * @param user - the database user
     * @param password - the database password
     */
    public Photon(String url, String user, String password)
    {
        this(new GenericDataSource(url, user, password));
    }

    /**
     * Constructs a new photon. Use this constructor if not using a connection pooler.
     *
     * @param url - the JDBC URL
     * @param user - the database user
     * @param password - the database password
     * @param photonOptions - the photon options to use
     */
    public Photon(String url, String user, String password, PhotonOptions photonOptions)
    {
        this(new GenericDataSource(url, user, password), photonOptions);
    }

    /**
     * Constructs a new photon. The data source will be used for creating transactions. Uses the default photon
     * options.
     *
     * @param dataSource - the data source used for creating transactions
     */
    public Photon(DataSource dataSource)
    {
        this(dataSource, null);
    }

    /**
     * Constructs a new photon. The data source will be used for creating transactions.
     *
     * @param dataSource - the data source used for creating transactions
     * @param photonOptions - the photon options to use
     */
    public Photon(DataSource dataSource, PhotonOptions photonOptions)
    {
        this.dataSource = dataSource;
        this.registeredAggregates = new HashMap<>();
        this.registeredViewModelAggregates = new HashMap<>();
        this.photonOptions = photonOptions != null ? photonOptions : PhotonOptions.defaultOptions();

        SqlJoinClauseBuilderService sqlJoinClauseBuilderService = new SqlJoinClauseBuilderService();
        this.selectSqlBuilderService = new SelectSqlBuilderService(sqlJoinClauseBuilderService);
        this.updateSqlBuilderService = new UpdateSqlBuilderService();
        this.insertSqlBuilderService = new InsertSqlBuilderService();
        this.deleteSqlBuilderService = new DeleteSqlBuilderService(sqlJoinClauseBuilderService);
        this.entityBlueprintConstructorService = new EntityBlueprintConstructorService();
    }

    public PhotonOptions getOptions()
    {
        return photonOptions;
    }

    /**
     * Obtains a connection and starts a new transaction.
     * @return - the transaction
     */
    public PhotonTransaction beginTransaction()
    {
        return new PhotonTransaction(
            getConnection(),
            registeredAggregates,
            registeredViewModelAggregates,
            photonOptions,
            entityBlueprintConstructorService
        );
    }

    /**
     * Creates a builder for constructing an aggregate entity blueprint.
     *
     * @param aggregateRootClass - the aggregate root entity class
     * @return - the aggregate entity builder for the aggregate root
     */
    public AggregateBlueprintBuilder registerAggregate(Class aggregateRootClass)
    {
        return new AggregateBlueprintBuilder(aggregateRootClass, this, entityBlueprintConstructorService);
    }

    /**
     * Creates a builder for constructing a view model aggregate blueprint.
     *
     * View model aggregate blueprints can be used for querying but cannot be used for inserting, updating, or deleting.
     * Unlike regular aggregate blueprints, you can register the same class multiple times as long as each view model
     * has a unique name. Set alsoRegisterBlueprintForSaving to true to have this blueprint aldo be used for inserting,
     * updating, and deleting the aggregate.
     *
     * @param aggregateRootClass - the aggregate root entity class
     * @param aggregateBlueprintName - the name for the aggregate blueprint
     * @param alsoRegisterBlueprintForSaving - Also register the blueprint as the one to use for inserting, updating,
     *                                       and deleting the aggregate class.
     * @return - the aggregate entity blueprint builder for the aggregate root
     */
    public AggregateBlueprintBuilder registerViewModelAggregate(
        Class aggregateRootClass,
        String aggregateBlueprintName,
        boolean alsoRegisterBlueprintForSaving)
    {
        return new AggregateBlueprintBuilder(aggregateRootClass, aggregateBlueprintName, alsoRegisterBlueprintForSaving, this, entityBlueprintConstructorService);
    }

    /**
     * Do not call this method directly. Instead, call registerAggregate().
     *
     * @param aggregateBlueprintName - the name for the aggregate blueprint
     * @param registerBlueprintForSaving - register class as well so that queried aggregates can be inserted, updated,
     *                                   or deleted
     * @param aggregateRootEntityBlueprint - the built aggregate root entity blueprint
     */
    public void registerBuiltAggregateBlueprint(
        String aggregateBlueprintName,
        boolean registerBlueprintForSaving,
        AggregateEntityBlueprint aggregateRootEntityBlueprint)
    {
        if(aggregateRootEntityBlueprint == null)
        {
            throw new PhotonException("AggregateBlueprint root entityBlueprint cannot be null.");
        }

        AggregateBlueprint blueprint = new AggregateBlueprint(
            aggregateRootEntityBlueprint,
            selectSqlBuilderService,
            updateSqlBuilderService,
            insertSqlBuilderService,
            deleteSqlBuilderService,
            photonOptions
        );

        if(StringUtils.isNotEmpty(aggregateBlueprintName))
        {
            registeredViewModelAggregates.put(aggregateBlueprintName, blueprint);
        }

        if(registerBlueprintForSaving)
        {
            if (registeredAggregates.containsKey(aggregateRootEntityBlueprint.getEntityClass()))
            {
                // TODO: Print warning that aggregate was already registered and old registration is being replaced.
            }
            registeredAggregates.put(aggregateRootEntityBlueprint.getEntityClass(), blueprint);
        }
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
