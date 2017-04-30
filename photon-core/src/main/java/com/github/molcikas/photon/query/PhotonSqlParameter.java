package com.github.molcikas.photon.query;

import com.github.molcikas.photon.blueprints.EntityBlueprintConstructorService;
import com.github.molcikas.photon.options.PhotonOptions;

import java.util.Collection;

public class PhotonSqlParameter
{
    private final int index;
    private final String name;
    private Object value;
    private Integer dataType;
    private boolean isCollection;

    public int getIndex()
    {
        return index;
    }

    public String getName()
    {
        return name;
    }

    public Object getValue()
    {
        return value;
    }

    public Integer getDataType()
    {
        return dataType;
    }

    public boolean isCollection()
    {
        return isCollection;
    }

    public PhotonSqlParameter(int index, String name)
    {
        this.index = index;
        this.name = name;
    }

    public void assignValue(Object value, PhotonOptions photonOptions)
    {
        this.value = value;
        this.dataType = value != null ? EntityBlueprintConstructorService.defaultColumnDataTypeForField(value.getClass(), photonOptions).dataType : null;
        this.isCollection = value != null && Collection.class.isAssignableFrom(value.getClass());
    }

    public void assignValue(Object value, Integer dataType)
    {
        this.value = value;
        this.dataType = dataType;
        this.isCollection = value != null && Collection.class.isAssignableFrom(value.getClass());
    }
}
