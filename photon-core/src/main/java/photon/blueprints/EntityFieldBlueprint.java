package photon.blueprints;

public class EntityFieldBlueprint
{
    private final String fieldName;
    private final Class fieldClass;
    private final String columnName;
    private final EntityBlueprint childEntityBlueprint;

    public String getFieldName()
    {
        return fieldName;
    }

    public Class getFieldClass()
    {
        return fieldClass;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public EntityBlueprint getChildEntityBlueprint()
    {
        return childEntityBlueprint;
    }

    public EntityFieldBlueprint(String fieldName, Class fieldClass, String columnName, EntityBlueprint childEntityBlueprint)
    {
        this.fieldName = fieldName;
        this.fieldClass = fieldClass;
        this.columnName = columnName;
        this.childEntityBlueprint = childEntityBlueprint;
    }
}
