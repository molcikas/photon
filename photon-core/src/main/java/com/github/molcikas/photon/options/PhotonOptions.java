package com.github.molcikas.photon.options;

public class PhotonOptions
{
    private final String delimitIdentifierStart;
    private final String getDelimitIdentifierEnd;
    private final DefaultTableName defaultTableName;

    public String getDelimitIdentifierStart()
    {
        return delimitIdentifierStart;
    }

    public String getGetDelimitIdentifierEnd()
    {
        return getDelimitIdentifierEnd;
    }

    public DefaultTableName getDefaultTableName()
    {
        return defaultTableName;
    }

    public PhotonOptions(
        String delimitIdentifierStart,
        String getDelimitIdentifierEnd,
        DefaultTableName defaultTableName)
    {
        this.delimitIdentifierStart = delimitIdentifierStart != null ? delimitIdentifierStart : "";
        this.getDelimitIdentifierEnd = getDelimitIdentifierEnd != null ? getDelimitIdentifierEnd : "";
        this.defaultTableName = defaultTableName != null ? defaultTableName : DefaultTableName.ClassName;
    }

    public static PhotonOptions defaultOptions()
    {
        return new PhotonOptions(null, null, null);
    }
}
