package photon.blueprints;

public class ColumnBlueprint
{
    public final String columnName;
    public final Integer columnDataType;
    public final boolean isPrimaryKeyColumn;
    public final EntityFieldBlueprint mappedEntityFieldBlueprint;

    public ColumnBlueprint(String columnName, Integer columnDataType, boolean isPrimaryKeyColumn, EntityFieldBlueprint mappedEntityFieldBlueprint)
    {
        this.columnName = columnName;
        this.columnDataType = columnDataType;
        this.isPrimaryKeyColumn = isPrimaryKeyColumn;
        this.mappedEntityFieldBlueprint = mappedEntityFieldBlueprint;
    }
}
