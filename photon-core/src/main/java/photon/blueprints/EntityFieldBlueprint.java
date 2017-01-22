package photon.blueprints;

public class EntityFieldBlueprint
{
    private final String fieldName;
    private final Class fieldClass;
    private final EntityBlueprint childEntityBlueprint;

    public String getFieldName()
    {
        return fieldName;
    }

    public Class getFieldClass()
    {
        return fieldClass;
    }

    public EntityBlueprint getChildEntityBlueprint()
    {
        return childEntityBlueprint;
    }

    public EntityFieldBlueprint(String fieldName, Class fieldClass, EntityBlueprint childEntityBlueprint)
    {
        this.fieldName = fieldName;
        this.fieldClass = fieldClass;
        this.childEntityBlueprint = childEntityBlueprint;
    }
}
