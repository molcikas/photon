package photon.blueprints;

import org.apache.commons.lang3.StringUtils;
import photon.converters.Converter;
import photon.exceptions.PhotonException;

import java.lang.reflect.Field;
import java.util.Collection;

public class FieldBlueprint
{
    private final Field reflectedField;
    private final String fieldName;
    private final Class fieldClass;
    private final FieldType fieldType;
    private final Converter customToFieldValueConverter;
    private final Converter customToDatabaseValueConverter;

    private final String mappedColumnName;
    private final AggregateEntityBlueprint childEntityBlueprint;
    private final ForeignKeyListBlueprint foreignKeyListBlueprint;

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

    public FieldType getFieldType()
    {
        return fieldType;
    }

    public String getMappedColumnName()
    {
        return mappedColumnName;
    }

    public AggregateEntityBlueprint getChildEntityBlueprint()
    {
        return childEntityBlueprint;
    }

    public ForeignKeyListBlueprint getForeignKeyListBlueprint()
    {
        return foreignKeyListBlueprint;
    }

    public Converter getCustomToFieldValueConverter()
    {
        return customToFieldValueConverter;
    }

    public Converter getCustomToDatabaseValueConverter()
    {
        return customToDatabaseValueConverter;
    }

    FieldBlueprint(Field reflectedField, String mappedColumnName, AggregateEntityBlueprint childEntityBlueprint,
                   ForeignKeyListBlueprint foreignKeyListBlueprint, Converter customToFieldValueConverter, Converter customToDatabaseValueConverter)
    {
        if(reflectedField == null)
        {
            throw new PhotonException("The reflected field for a field blueprint cannot be null.");
        }

        reflectedField.setAccessible(true);
        this.reflectedField = reflectedField;
        this.fieldName = reflectedField.getName();
        this.fieldClass = reflectedField.getType();
        this.customToFieldValueConverter = customToFieldValueConverter;
        this.customToDatabaseValueConverter = customToDatabaseValueConverter;

        if(foreignKeyListBlueprint != null)
        {
            this.fieldType = FieldType.ForeignKeyList;
            this.mappedColumnName = null;
            this.childEntityBlueprint = null;
            this.foreignKeyListBlueprint = foreignKeyListBlueprint;

            if(StringUtils.isBlank(foreignKeyListBlueprint.getForeignTableName()) ||
                StringUtils.isBlank(foreignKeyListBlueprint.getForeignTableKeyColumnName()) ||
                StringUtils.isBlank(foreignKeyListBlueprint.getForeignTableJoinColumnName()) ||
                foreignKeyListBlueprint.getFieldListItemClass() == null ||
                foreignKeyListBlueprint.getForeignTableKeyColumnType() == null)
            {
                throw new PhotonException(String.format("The foreign key list data for '%s' must be non-null.", fieldName));
            }

            if(!Collection.class.isAssignableFrom(fieldClass))
            {
                throw new PhotonException(String.format("The field '%s' must be a Collection since it is a foreign key list field.", fieldName));
            }
        }
        else if(childEntityBlueprint != null)
        {
            if(Collection.class.isAssignableFrom(this.fieldClass))
            {
                this.fieldType = FieldType.EntityList;
            }
            else
            {
                this.fieldType = FieldType.Entity;
            }

            this.mappedColumnName = mappedColumnName;
            this.childEntityBlueprint = childEntityBlueprint;
            this.foreignKeyListBlueprint = null;
        }
        else
        {
            this.fieldType = FieldType.Primitive;
            this.mappedColumnName = mappedColumnName;
            this.childEntityBlueprint = null;
            this.foreignKeyListBlueprint = null;
        }
    }
}
