package com.github.molcikas.photon;

import com.github.molcikas.photon.blueprints.AggregateBlueprint;
import com.github.molcikas.photon.blueprints.AggregateEntityBlueprint;
import com.github.molcikas.photon.blueprints.AggregateEntityBlueprintBuilder;
import com.github.molcikas.photon.blueprints.EntityBlueprintConstructorService;
import com.github.molcikas.photon.options.PhotonOptions;
import com.github.molcikas.photon.sqlbuilders.*;
import com.github.molcikas.photon.exceptions.PhotonException;

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

    private final PhotonOptions photonOptions;

    public Photon(String url, String user, String password)
    {
        this(new GenericDataSource(url, user, password));
    }

    public Photon(String url, String user, String password, PhotonOptions photonOptions)
    {
        this(new GenericDataSource(url, user, password), photonOptions);
    }

    public Photon(DataSource dataSource)
    {
        this(dataSource, null);
    }

    public Photon(DataSource dataSource, PhotonOptions photonOptions)
    {
        this.dataSource = dataSource;
        this.registeredAggregates = new HashMap<>();
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

    public PhotonTransaction beginTransaction()
    {
        return new PhotonTransaction(getConnection(), registeredAggregates, photonOptions, entityBlueprintConstructorService);
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
                deleteSqlBuilderService,
                photonOptions
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
