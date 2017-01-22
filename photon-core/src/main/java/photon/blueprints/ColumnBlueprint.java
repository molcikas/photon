package photon.blueprints;

public class ColumnBlueprint
{
    private final String columnName;
    private final Integer columnDataType;
    private final boolean isPrimaryKeyColumn;
    private final EntityFieldBlueprint mappedEntityFieldBlueprint;

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

    public EntityFieldBlueprint getMappedEntityFieldBlueprint()
    {
        return mappedEntityFieldBlueprint;
    }

    public ColumnBlueprint(String columnName, Integer columnDataType, boolean isPrimaryKeyColumn, EntityFieldBlueprint mappedEntityFieldBlueprint)
    {
        this.columnName = columnName;
        this.columnDataType = columnDataType;
        this.isPrimaryKeyColumn = isPrimaryKeyColumn;
        this.mappedEntityFieldBlueprint = mappedEntityFieldBlueprint;
    }
}
