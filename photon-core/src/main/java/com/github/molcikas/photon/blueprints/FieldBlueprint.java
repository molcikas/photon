package com.github.molcikas.photon.blueprints;

import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.exceptions.PhotonException;

import java.lang.reflect.Field;
import java.util.Collection;

public class FieldBlueprint
{
    private final Field reflectedField;
    private final String fieldName;
    private final Class fieldClass;
    private final FieldType fieldType;
    private final Converter customToFieldValueConverter;
    private final EntityFieldValueMapping entityFieldValueMapping;

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

    public EntityFieldValueMapping getEntityFieldValueMapping()
    {
        return entityFieldValueMapping;
    }

    FieldBlueprint(Field reflectedField, String mappedColumnName, AggregateEntityBlueprint childEntityBlueprint,
                   ForeignKeyListBlueprint foreignKeyListBlueprint, Converter customToFieldValueConverter, EntityFieldValueMapping entityFieldValueMapping)
    {
        if(reflectedField == null && entityFieldValueMapping == null)
        {
            throw new PhotonException("The reflected field and entity field value mapping for a field blueprint cannot both be null.");
        }

        if(reflectedField != null)
        {
            reflectedField.setAccessible(true);
            this.reflectedField = reflectedField;
            this.fieldName = reflectedField.getName();
            this.fieldClass = reflectedField.getType();
        }
        else
        {
            this.reflectedField = null;
            this.fieldName = null;
            this.fieldClass = null;
        }

        this.customToFieldValueConverter = customToFieldValueConverter;

        if(entityFieldValueMapping != null)
        {
            if(StringUtils.isBlank(mappedColumnName))
            {
                throw new PhotonException("A field with a custom entity field value mapping must have a mapped column name.");
            }
            if(reflectedField != null || childEntityBlueprint != null || foreignKeyListBlueprint != null)
            {
                throw new PhotonException(String.format("The field for column '%s' has a custom entity field value mapping, therefore the field cannot have a reflected field, child entity, or foreign key list.", mappedColumnName));
            }
            this.fieldType = FieldType.CustomValueMapper;
            this.mappedColumnName = mappedColumnName;
            this.childEntityBlueprint = null;
            this.foreignKeyListBlueprint = null;
            this.entityFieldValueMapping = entityFieldValueMapping;
        }
        else if(foreignKeyListBlueprint != null)
        {
            this.fieldType = FieldType.ForeignKeyList;
            this.mappedColumnName = null;
            this.childEntityBlueprint = null;
            this.foreignKeyListBlueprint = foreignKeyListBlueprint;
            this.entityFieldValueMapping = null;

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
            this.entityFieldValueMapping = null;
        }
        else
        {
            this.fieldType = FieldType.Primitive;
            this.mappedColumnName = mappedColumnName;
            this.childEntityBlueprint = null;
            this.foreignKeyListBlueprint = null;
            this.entityFieldValueMapping = null;
        }
    }
}
