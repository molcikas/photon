package photon.blueprints;

import org.apache.commons.lang3.StringUtils;
import photon.exceptions.PhotonException;

public class ColumnBlueprint
{
    private final String columnName;
    private final Integer columnDataType;
    private final boolean isPrimaryKeyColumn;
    private final boolean isForeignKeyToParentColumn;

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

    public boolean isForeignKeyToParentColumn()
    {
        return isForeignKeyToParentColumn;
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
        boolean isForeignKeyToParentColumn,
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
        this.columnName = columnName;
        this.columnDataType = columnDataType;
        this.isPrimaryKeyColumn = isPrimaryKeyColumn;
        this.isForeignKeyToParentColumn = isForeignKeyToParentColumn;
        this.mappedFieldBlueprint = mappedFieldBlueprint;
        this.columnIndex = columnIndex;
    }

    void moveColumnToIndex(int newColumnIndex)
    {
        this.columnIndex = newColumnIndex;
    }
}
