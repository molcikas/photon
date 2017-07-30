package com.github.molcikas.photon.query;

import com.github.molcikas.photon.blueprints.ColumnDataType;
import com.github.molcikas.photon.blueprints.TableBlueprintBuilder;
import com.github.molcikas.photon.options.PhotonOptions;

import java.util.Collection;

public class PhotonSqlParameter
{
    private final String name;
    private Object value;
    private ColumnDataType dataType;
    private boolean isCollection;

    public String getName()
    {
        return name;
    }

    public Object getValue()
    {
        return value;
    }

    public ColumnDataType getDataType()
    {
        return dataType;
    }

    public boolean isCollection()
    {
        return isCollection;
    }

    public PhotonSqlParameter(String name)
    {
        this.name = name;
    }

    public void assignValue(Object value, PhotonOptions photonOptions)
    {
        this.value = value;
        this.dataType = value != null ? TableBlueprintBuilder.defaultColumnDataTypeForField(value.getClass(), photonOptions).dataType : null;
        this.isCollection = value != null && Collection.class.isAssignableFrom(value.getClass());
    }

    public void assignValue(Object value, ColumnDataType dataType)
    {
        this.value = value;
        this.dataType = dataType;
        this.isCollection = value != null && Collection.class.isAssignableFrom(value.getClass());
    }
}
