package com.github.molcikas.photon.blueprints.entity;

import com.github.molcikas.photon.PhotonUtils;
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
    private final Converter customHydrater;
    private final EntityFieldValueMapping entityFieldValueMapping;
    private final CompoundEntityFieldValueMapping compoundEntityFieldValueMapping;

    private final EntityBlueprint childEntityBlueprint;
    private final ForeignKeyListBlueprint foreignKeyListBlueprint;

    private boolean isVersionField = false;

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

    public EntityBlueprint getChildEntityBlueprint()
    {
        return childEntityBlueprint;
    }

    public ForeignKeyListBlueprint getForeignKeyListBlueprint()
    {
        return foreignKeyListBlueprint;
    }

    public Converter getCustomHydrater()
    {
        return customHydrater;
    }

    public EntityFieldValueMapping getEntityFieldValueMapping()
    {
        return entityFieldValueMapping;
    }

    public CompoundEntityFieldValueMapping getCompoundEntityFieldValueMapping()
    {
        return compoundEntityFieldValueMapping;
    }

    public boolean isVersionField()
    {
        return isVersionField;
    }

    public FieldBlueprint(Field reflectedField,
                          EntityBlueprint childEntityBlueprint,
                          ForeignKeyListBlueprint foreignKeyListBlueprint,
                          Converter customHydrater,
                          EntityFieldValueMapping entityFieldValueMapping,
                          CompoundEntityFieldValueMapping compoundEntityFieldValueMapping)
    {
        if(reflectedField == null && entityFieldValueMapping == null && compoundEntityFieldValueMapping == null)
        {
            throw new PhotonException("The reflected field and value mapping for a field cannot both be null.");
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

        this.customHydrater = customHydrater;

        if(entityFieldValueMapping != null)
        {
            if(reflectedField != null || childEntityBlueprint != null || foreignKeyListBlueprint != null || compoundEntityFieldValueMapping != null)
            {
                throw new PhotonException("A field has a custom entity field value mapping, therefore the field cannot have a compound mapping, reflected field, child entity, or foreign key list.");
            }
            this.fieldType = FieldType.CustomValueMapper;
            this.childEntityBlueprint = null;
            this.foreignKeyListBlueprint = null;
            this.entityFieldValueMapping = entityFieldValueMapping;
            this.compoundEntityFieldValueMapping = null;
        }
        else if(compoundEntityFieldValueMapping != null)
        {
            if(reflectedField != null || childEntityBlueprint != null || foreignKeyListBlueprint != null)
            {
                throw new PhotonException("A field has a custom compound entity field value mapping, therefore the field cannot have a reflected field, child entity, or foreign key list.");
            }
            this.fieldType = FieldType.CompoundCustomValueMapper;
            this.childEntityBlueprint = null;
            this.foreignKeyListBlueprint = null;
            this.entityFieldValueMapping = null;
            this.compoundEntityFieldValueMapping = compoundEntityFieldValueMapping;
        }
        else if(foreignKeyListBlueprint != null)
        {
            this.fieldType = FieldType.ForeignKeyList;
            this.childEntityBlueprint = null;
            this.foreignKeyListBlueprint = foreignKeyListBlueprint;
            this.entityFieldValueMapping = null;
            this.compoundEntityFieldValueMapping = null;

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
                throw new PhotonException("The field '%s' must be a Collection since it is a foreign key list field.", fieldName);
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

            this.childEntityBlueprint = childEntityBlueprint;
            this.foreignKeyListBlueprint = null;
            this.entityFieldValueMapping = null;
            this.compoundEntityFieldValueMapping = null;
        }
        else
        {
            this.fieldType = FieldType.Primitive;
            this.childEntityBlueprint = null;
            this.foreignKeyListBlueprint = null;
            this.entityFieldValueMapping = null;
            this.compoundEntityFieldValueMapping = null;
        }
    }

    public void setAsVersionField()
    {
        if(reflectedField == null || fieldClass == null)
        {
            throw new PhotonException("Field '%s' cannot be a version field because it does not map to a real field on the entity.", fieldName);
        }
        if(!PhotonUtils.isNumericType(fieldClass))
        {
            throw new PhotonException("Field '%s' cannot be a version field because it is not a number field.", fieldName);
        }
        isVersionField = true;
    }
}
