package com.github.molcikas.photon.options;

import com.github.molcikas.photon.blueprints.table.ColumnDataType;
import lombok.Builder;
import lombok.Getter;

/**
 * The options for photon.
 */
@Getter
public class PhotonOptions
{
    public static final ColumnDataType DEFAULT_UUID_DATA_TYPE = ColumnDataType.BINARY;

    private final String delimitIdentifierStart;
    private final String delimitIdentifierEnd;
    private final DefaultTableName defaultTableName;
    private final boolean enableJdbcGetGeneratedKeys;
    private final ColumnDataType defaultUuidDataType;

    /**
     * Constructor. Defaults the UUID data type to PhotonOptions.DEFAULT_UUID_DATA_TYPE.
     *
     * @param delimitIdentifierStart - The delimiter identifier start (e.g. "[" for SQL Server or "`" for MySQL)
     * @param delimitIdentifierEnd - The delimiter identifier end (e.g. "]" for SQL Server or "`" for MySQL)
     * @param defaultTableName - The strategy for determining the default table name for an entity
     * @param enableJdbcGetGeneratedKeys - Whether to use Statement.RETURN_GENERATED_KEYS when creating prepared
     *                                   statements. Set this to false for Oracle databases.
     * @param defaultUuidDataType - Default java.sql.Types value for UUID fields. Set this to null for Postgres
     *                            databases.
     */
    @Builder
    public PhotonOptions(
        String delimitIdentifierStart,
        String delimitIdentifierEnd,
        DefaultTableName defaultTableName,
        Boolean enableJdbcGetGeneratedKeys,
        ColumnDataType defaultUuidDataType)
    {
        this.delimitIdentifierStart = delimitIdentifierStart != null ? delimitIdentifierStart : "";
        this.delimitIdentifierEnd = delimitIdentifierEnd != null ? delimitIdentifierEnd : "";
        this.defaultTableName = defaultTableName != null ? defaultTableName : DefaultTableName.ClassName;
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
        return new PhotonOptions(null, null, null, null, DEFAULT_UUID_DATA_TYPE);
    }

    /**
     * Recommended options for MySQL databases.
     *
     * @return - the photon options
     */
    public static PhotonOptionsBuilder mysqlOptions()
    {
        return PhotonOptions
            .builder()
            .delimitIdentifierStart("`")
            .delimitIdentifierEnd("`")
            .defaultUuidDataType(ColumnDataType.BINARY);
    }

    /**
     * Recommended options for Oracle databases.
     *
     * @return - the photon options
     */
    public static PhotonOptionsBuilder oracleOptions()
    {
        return PhotonOptions
            .builder()
            .enableJdbcGetGeneratedKeys(false)
            .defaultUuidDataType(ColumnDataType.BINARY);
    }

    /**
     * Recommended options for Postgres databases.
     *
     * @return - the photon options
     */
    public static PhotonOptionsBuilder postgresOptions()
    {
        return PhotonOptions
            .builder()
            .delimitIdentifierStart("\"")
            .delimitIdentifierEnd("\"")
            .defaultUuidDataType(null);
    }

    /**
     * Recommended options for SQL Server databases.
     *
     * @return - the photon options
     */
    public static PhotonOptionsBuilder sqlServerOptions()
    {
        return PhotonOptions
            .builder()
            .delimitIdentifierStart("[")
            .delimitIdentifierEnd("]")
            .defaultUuidDataType(ColumnDataType.BINARY);
    }
}
