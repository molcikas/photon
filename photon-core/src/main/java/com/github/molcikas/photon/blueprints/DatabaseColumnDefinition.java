package com.github.molcikas.photon.blueprints;

import com.github.molcikas.photon.exceptions.PhotonException;
import org.apache.commons.lang3.StringUtils;

public class DatabaseColumnDefinition
{
    private final String columnName;

    private final ColumnDataType columnDataType;

    public String getColumnName()
    {
        return columnName;
    }

    public ColumnDataType getColumnDataType()
    {
        return columnDataType;
    }

    public DatabaseColumnDefinition(String columnName, ColumnDataType columnDataType)
    {
        if(StringUtils.isBlank(columnName))
        {
            throw new PhotonException("Column name cannot be blank.");
        }

        this.columnName = columnName;
        this.columnDataType = columnDataType;
    }

    public DatabaseColumnDefinition(String columnName)
    {
        this(columnName, null);
    }
}
