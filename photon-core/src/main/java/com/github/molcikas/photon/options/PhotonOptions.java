package com.github.molcikas.photon.options;

public class PhotonOptions
{
    private final String delimitIdentifierStart;
    private final String delimitIdentifierEnd;
    private final DefaultTableName defaultTableName;
    private final boolean enableBatchInsertsForAutoIncrementEntities;

    public String getDelimitIdentifierStart()
    {
        return delimitIdentifierStart;
    }

    public String getGetDelimitIdentifierEnd()
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

    public PhotonOptions(
        String delimitIdentifierStart,
        String delimitIdentifierEnd,
        DefaultTableName defaultTableName,
        Boolean enableBatchInsertsForAutoIncrementEntities)
    {
        this.delimitIdentifierStart = delimitIdentifierStart != null ? delimitIdentifierStart : "";
        this.delimitIdentifierEnd = delimitIdentifierEnd != null ? delimitIdentifierEnd : "";
        this.defaultTableName = defaultTableName != null ? defaultTableName : DefaultTableName.ClassName;
        this.enableBatchInsertsForAutoIncrementEntities = enableBatchInsertsForAutoIncrementEntities != null ? enableBatchInsertsForAutoIncrementEntities : true;
    }

    public static PhotonOptions defaultOptions()
    {
        return new PhotonOptions(null, null, null, null);
    }
}
