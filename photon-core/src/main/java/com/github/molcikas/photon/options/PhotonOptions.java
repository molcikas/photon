package com.github.molcikas.photon.options;

import com.github.molcikas.photon.blueprints.table.ColumnDataType;

/**
 * The options for photon.
 */
public class PhotonOptions
{
    public static final ColumnDataType DEFAULT_UUID_DATA_TYPE = ColumnDataType.BINARY;

    private final String delimitIdentifierStart;
    private final String delimitIdentifierEnd;
    private final DefaultTableName defaultTableName;
    private final boolean enableBatchInsertsForAutoIncrementEntities;
    private final boolean enableJdbcGetGeneratedKeys;
    private final ColumnDataType defaultUuidDataType;

    public String getDelimitIdentifierStart()
    {
        return delimitIdentifierStart;
    }

    public String getDelimitIdentifierEnd()
    {
        return delimitIdentifierEnd;
    }

    public DefaultTableName getDefaultTableName()
    {
        return defaultTableName;
    }

    public boolean isEnableBatchInsertsForAutoIncrementEntities()
    {
        return enableBatchInsertsForAutoIncrementEntities;
    }

    public boolean isEnableJdbcGetGeneratedKeys()
    {
        return enableJdbcGetGeneratedKeys;
    }

    public ColumnDataType getDefaultUuidDataType()
    {
        return defaultUuidDataType;
    }

    /**
     * Constructor. Defaults the UUID data type to PhotonOptions.DEFAULT_UUID_DATA_TYPE.
     *
     * @param delimitIdentifierStart - The delimiter identifier start (e.g. "[" for SQL Server or "`" for MySQL)
     * @param delimitIdentifierEnd - The delimiter identifier end (e.g. "]" for SQL Server or "`" for MySQL)
     * @param defaultTableName - The strategy for determining the default table name for an entity
     * @param enableBatchInsertsForAutoIncrementEntities - Whether to enable batch inserts for entities with auto
     *                                                   increment primary keys. Set this to true for SQL Server.
     * @param enableJdbcGetGeneratedKeys - Whether to use Statement.RETURN_GENERATED_KEYS when creating prepared
     *                                   statements. Set this to false for Oracle databases.
     */
    public PhotonOptions(
        String delimitIdentifierStart,
        String delimitIdentifierEnd,
        DefaultTableName defaultTableName,
        Boolean enableBatchInsertsForAutoIncrementEntities,
        Boolean enableJdbcGetGeneratedKeys)
    {
        this(delimitIdentifierStart, delimitIdentifierEnd, defaultTableName, enableBatchInsertsForAutoIncrementEntities, enableJdbcGetGeneratedKeys, DEFAULT_UUID_DATA_TYPE);
    }

    /**
     * Constructor. Defaults the UUID data type to PhotonOptions.DEFAULT_UUID_DATA_TYPE.
     *
     * @param delimitIdentifierStart - The delimiter identifier start (e.g. "[" for SQL Server or "`" for MySQL)
     * @param delimitIdentifierEnd - The delimiter identifier end (e.g. "]" for SQL Server or "`" for MySQL)
     * @param defaultTableName - The strategy for determining the default table name for an entity
     * @param enableBatchInsertsForAutoIncrementEntities - Whether to enable batch inserts for entities with auto
     *                                                   increment primary keys. Set this to true for SQL Server.
     * @param enableJdbcGetGeneratedKeys - Whether to use Statement.RETURN_GENERATED_KEYS when creating prepared
     *                                   statements. Set this to false for Oracle databases.
     * @param defaultUuidDataType - Default java.sql.Types value for UUID fields. Set this to null for Postgres
     *                            databases.
     */
    public PhotonOptions(
        String delimitIdentifierStart,
        String delimitIdentifierEnd,
        DefaultTableName defaultTableName,
        Boolean enableBatchInsertsForAutoIncrementEntities,
        Boolean enableJdbcGetGeneratedKeys,
        ColumnDataType defaultUuidDataType)
    {
        this.delimitIdentifierStart = delimitIdentifierStart != null ? delimitIdentifierStart : "";
        this.delimitIdentifierEnd = delimitIdentifierEnd != null ? delimitIdentifierEnd : "";
        this.defaultTableName = defaultTableName != null ? defaultTableName : DefaultTableName.ClassName;
        this.enableBatchInsertsForAutoIncrementEntities = enableBatchInsertsForAutoIncrementEntities != null ? enableBatchInsertsForAutoIncrementEntities : true;
        this.enableJdbcGetGeneratedKeys = enableJdbcGetGeneratedKeys != null ? enableJdbcGetGeneratedKeys : true;
        this.defaultUuidDataType = defaultUuidDataType;
    }

    /**
     * Creates a PhotonOptions object with the default options.
     *
     * @return - the photon options
     */
    public static PhotonOptions defaultOptions()
    {
        return new PhotonOptions(null, null, null, null, null, DEFAULT_UUID_DATA_TYPE);
    }
}
