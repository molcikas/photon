package org.photon.blueprints;

import org.apache.commons.lang3.StringUtils;
import org.photon.converters.Converter;
import org.photon.exceptions.PhotonException;

public class ColumnBlueprint
{
    private final String columnName;
    private final Integer columnDataType;
    private final boolean isPrimaryKeyColumn;
    private final boolean isAutoIncrementColumn;
    private final boolean isForeignKeyToParentColumn;
    private final Converter customToDatabaseValueConverter;

    // Reference to the entity field that this database column is mapped to. This can (but does not have to)
    // be null if this column is an unmapped primary key or a foreign key to the parent.
    private final FieldBlueprint mappedFieldBlueprint;

    private int columnIndex;

    public String getColumnName()
    {
        return columnName;
    }

    public Integer getColumnDataType()
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

    public Converter getCustomToDatabaseValueConverter()
    {
        return customToDatabaseValueConverter;
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
        String columnName,
        Integer columnDataType,
        boolean isPrimaryKeyColumn,
        boolean isAutoIncrementColumn,
        boolean isForeignKeyToParentColumn,
        Converter customToDatabaseValueConverter,
        FieldBlueprint mappedFieldBlueprint,
        int columnIndex)
    {
        if(StringUtils.isBlank(columnName))
        {
            throw new PhotonException(String.format("Column names cannot be blank."));
        }
        if(columnDataType == null)
        {
            throw new PhotonException(String.format("The data type for column '%s' was null.", columnName));
        }
        if(isAutoIncrementColumn && !isPrimaryKeyColumn)
        {
            throw new PhotonException(String.format("The column '%s' cannot be auto-increment because it is not the primary key.", columnName));
        }
        this.columnName = columnName;
        this.columnDataType = columnDataType;
        this.isPrimaryKeyColumn = isPrimaryKeyColumn;
        this.isAutoIncrementColumn = isAutoIncrementColumn;
        this.isForeignKeyToParentColumn = isForeignKeyToParentColumn;
        this.customToDatabaseValueConverter = customToDatabaseValueConverter;
        this.mappedFieldBlueprint = mappedFieldBlueprint;
        this.columnIndex = columnIndex;
    }

    void moveColumnToIndex(int newColumnIndex)
    {
        this.columnIndex = newColumnIndex;
    }
}
