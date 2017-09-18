package com.github.molcikas.photon.blueprints.table;

import com.github.molcikas.photon.blueprints.entity.FieldBlueprint;
import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.exceptions.PhotonException;

public class ColumnBlueprint
{
    private final String columnNameQualified;
    private final String columnName;
    private final ColumnDataType columnDataType;
    private final boolean isPrimaryKeyColumn;
    private final boolean isAutoIncrementColumn;
    private final boolean isForeignKeyToParentColumn;
    private final Converter customSerializer;

    // Reference to the entity field that this database column is mapped to. This can (but does not have to)
    // be null if this column is an unmapped primary key or a foreign key to the parent.
    private final FieldBlueprint mappedFieldBlueprint;

    private int columnIndex;

    public String getColumnNameQualified()
    {
        return columnNameQualified;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public ColumnDataType getColumnDataType()
    {
        return columnDataType;
    }

    public boolean isPrimaryKeyColumn()
    {
        return isPrimaryKeyColumn;
    }

    public boolean isAutoIncrementColumn()
    {
        return isAutoIncrementColumn;
    }

    public boolean isForeignKeyToParentColumn()
    {
        return isForeignKeyToParentColumn;
    }

    public Converter getCustomSerializer()
    {
        return customSerializer;
    }

    public FieldBlueprint getMappedFieldBlueprint()
    {
        return mappedFieldBlueprint;
    }

    public int getColumnIndex()
    {
        return columnIndex;
    }

    public ColumnBlueprint(
        String tableName,
        String columnName,
        ColumnDataType columnDataType,
        boolean isPrimaryKeyColumn,
        boolean isAutoIncrementColumn,
        boolean isForeignKeyToParentColumn,
        Converter customSerializer,
        FieldBlueprint mappedFieldBlueprint,
        int columnIndex)
    {
        if(StringUtils.isBlank(tableName))
        {
            throw new PhotonException("Table name cannot be blank.");
        }
        if(StringUtils.isBlank(columnName))
        {
            throw new PhotonException("Column name cannot be blank.");
        }
        if(isAutoIncrementColumn && !isPrimaryKeyColumn)
        {
            throw new PhotonException("The column '%s' cannot be auto-increment because it is not the primary key.", columnName);
        }
        this.columnNameQualified = tableName + "_" + columnName;
        this.columnName = columnName;
        this.columnDataType = columnDataType;
        this.isPrimaryKeyColumn = isPrimaryKeyColumn;
        this.isAutoIncrementColumn = isAutoIncrementColumn;
        this.isForeignKeyToParentColumn = isForeignKeyToParentColumn;
        this.customSerializer = customSerializer;
        this.mappedFieldBlueprint = mappedFieldBlueprint;
        this.columnIndex = columnIndex;
    }

    void moveColumnToIndex(int newColumnIndex)
    {
        this.columnIndex = newColumnIndex;
    }
}
