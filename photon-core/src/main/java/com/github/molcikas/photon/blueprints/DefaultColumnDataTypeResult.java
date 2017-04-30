package com.github.molcikas.photon.blueprints;

public class DefaultColumnDataTypeResult
{
    public final boolean foundDataType;
    public final Integer dataType;

    public DefaultColumnDataTypeResult(Integer dataType)
    {
        this.foundDataType = true;
        this.dataType = dataType;
    }

    public DefaultColumnDataTypeResult()
    {
        this.foundDataType = false;
        this.dataType = null;
    }

    public static DefaultColumnDataTypeResult notFound()
    {
        return new DefaultColumnDataTypeResult();
    }
}
