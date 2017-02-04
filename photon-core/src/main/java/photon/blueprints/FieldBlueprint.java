package photon.blueprints;

import java.lang.reflect.Field;

public class FieldBlueprint
{
    private final Field reflectedField;
    private final String fieldName;
    private final Class fieldClass;
    private final String columnName;
    private final EntityBlueprint childEntityBlueprint;

    public Field getReflectedField()
    {
        return reflectedField;
    }

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

    public FieldBlueprint(Field reflectedField, String columnName, EntityBlueprint childEntityBlueprint)
    {
        reflectedField.setAccessible(true);

        this.reflectedField = reflectedField;
        this.fieldName = reflectedField.getName();
        this.fieldClass = reflectedField.getType();
        this.columnName = columnName;
        this.childEntityBlueprint = childEntityBlueprint;
    }
}
