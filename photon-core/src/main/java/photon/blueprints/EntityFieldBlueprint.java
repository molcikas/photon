package photon.blueprints;

public class EntityFieldBlueprint
{
    public final String fieldName;
    public final Class fieldClass;
    public final EntityBlueprint entityBlueprint;

    public EntityFieldBlueprint(String fieldName, Class fieldClass, EntityBlueprint entityBlueprint)
    {
        this.fieldName = fieldName;
        this.fieldClass = fieldClass;
        this.entityBlueprint = entityBlueprint;
    }
}
