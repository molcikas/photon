package com.github.molcikas.photon.blueprints;

import com.github.molcikas.photon.exceptions.PhotonException;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class EntityBlueprint
{
    private Class entityClass;
    private EntityClassDiscriminator entityClassDiscriminator;
    private List<FieldBlueprint> fields;
    private TableBlueprint rootTableBlueprint;

    EntityBlueprint(
        Class entityClass,
        EntityClassDiscriminator entityClassDiscriminator,
        List<FieldBlueprint> fields,
        TableBlueprint rootTableBlueprint)
    {
        this.entityClass = entityClass;
        this.entityClassDiscriminator = entityClassDiscriminator;
        this.fields = fields;
        this.rootTableBlueprint = rootTableBlueprint;
    }

    public Class getEntityClass()
    {
        return entityClass;
    }

    public List<FieldBlueprint> getFieldsWithChildEntities()
    {
        return fields
            .stream()
            .filter(f -> f.getFieldType() == FieldType.Entity || f.getFieldType() == FieldType.EntityList)
            .collect(Collectors.toList());
    }

    public List<FieldBlueprint> getForeignKeyListFields()
    {
        return fields
            .stream()
            .filter(f -> f.getFieldType() == FieldType.ForeignKeyList)
            .collect(Collectors.toList());
    }

    public Constructor getEntityConstructor(Map<String, Object> entityValues)
    {
        Class classToConstruct = null;

        if(entityClassDiscriminator != null)
        {
            classToConstruct = entityClassDiscriminator.getClassForEntity(entityValues);
        }
        if(classToConstruct == null)
        {
            classToConstruct = entityClass;
        }

        try
        {
            Constructor constructor = classToConstruct.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        }
        catch (Exception ex)
        {
            throw new PhotonException(
                String.format("Error getting constructor for entity class '%s'. Make sure the class has a parameterless constructor (private is ok).", classToConstruct),
                ex
            );
        }
    }

    public Field getReflectedField(String fieldName)
    {
        return fields
            .stream()
            .filter(f -> f.getFieldName().equals(fieldName))
            .map(FieldBlueprint::getReflectedField)
            .findFirst()
            .orElse(null);
    }

    public String getEntityClassName()
    {
        return entityClass.getName();
    }

    public FieldBlueprint getFieldForColumnName(String columnName)
    {
        return fields
            .stream()
            .filter(f -> StringUtils.equals(f.getMappedColumnName(), columnName))
            .findFirst()
            .orElse(null);
    }

    public List<FieldBlueprint> getCompoundCustomValueMapperFields()
    {
        return fields
            .stream()
            .filter(f -> f.getFieldType() == FieldType.CompoundCustomValueMapper)
            .collect(Collectors.toList());
    }

    public TableBlueprint getRootTableBlueprint()
    {
        return rootTableBlueprint;
    }
}
