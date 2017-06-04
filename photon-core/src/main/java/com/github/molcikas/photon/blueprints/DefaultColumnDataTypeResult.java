package com.github.molcikas.photon.blueprints;

public class DefaultColumnDataTypeResult
{
    public final boolean foundDataType;
    public final ColumnDataType dataType;

    public DefaultColumnDataTypeResult(ColumnDataType dataType)
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
