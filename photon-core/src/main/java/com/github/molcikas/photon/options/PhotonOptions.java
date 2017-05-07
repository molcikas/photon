package com.github.molcikas.photon.options;

import java.sql.Types;

public class PhotonOptions
{
    public static final Integer DEFAULT_UUID_DATA_TYPE = Types.BINARY;

    private final String delimitIdentifierStart;
    private final String delimitIdentifierEnd;
    private final DefaultTableName defaultTableName;
    private final boolean enableBatchInsertsForAutoIncrementEntities;
    private final boolean enableJdbcGetGeneratedKeys;
    private final Integer defaultUuidDataType;

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

    public Integer getDefaultUuidDataType()
    {
        return defaultUuidDataType;
    }

    public PhotonOptions(
        String delimitIdentifierStart,
        String delimitIdentifierEnd,
        DefaultTableName defaultTableName,
        Boolean enableBatchInsertsForAutoIncrementEntities,
        Boolean enableJdbcGetGeneratedKeys)
    {
        this(delimitIdentifierStart, delimitIdentifierEnd, defaultTableName, enableBatchInsertsForAutoIncrementEntities, enableJdbcGetGeneratedKeys, DEFAULT_UUID_DATA_TYPE);
    }

    public PhotonOptions(
        String delimitIdentifierStart,
        String delimitIdentifierEnd,
        DefaultTableName defaultTableName,
        Boolean enableBatchInsertsForAutoIncrementEntities,
        Boolean enableJdbcGetGeneratedKeys,
        Integer defaultUuidDataType)
    {
        this.delimitIdentifierStart = delimitIdentifierStart != null ? delimitIdentifierStart : "";
        this.delimitIdentifierEnd = delimitIdentifierEnd != null ? delimitIdentifierEnd : "";
        this.defaultTableName = defaultTableName != null ? defaultTableName : DefaultTableName.ClassName;
        this.enableBatchInsertsForAutoIncrementEntities = enableBatchInsertsForAutoIncrementEntities != null ? enableBatchInsertsForAutoIncrementEntities : true;
        this.enableJdbcGetGeneratedKeys = enableJdbcGetGeneratedKeys != null ? enableJdbcGetGeneratedKeys : true;
        this.defaultUuidDataType = defaultUuidDataType;
    }

    public static PhotonOptions defaultOptions()
    {
        return new PhotonOptions(null, null, null, null, null, DEFAULT_UUID_DATA_TYPE);
    }
}
