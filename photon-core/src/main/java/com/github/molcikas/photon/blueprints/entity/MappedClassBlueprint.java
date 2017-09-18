package com.github.molcikas.photon.blueprints.entity;

import com.github.molcikas.photon.exceptions.PhotonException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MappedClassBlueprint
{
    private final Class mappedClass;
    private final boolean includeAllFields;
    private final List<String> includedFields;

    public Class getMappedClass()
    {
        return mappedClass;
    }

    public MappedClassBlueprint(Class mappedClass, boolean includeAllFields, List<String> includedFields)
    {
        if(mappedClass == null)
        {
            throw new PhotonException("Mapped class cannot be null.");
        }

        this.mappedClass = mappedClass;
        this.includeAllFields = includeAllFields;
        this.includedFields = new ArrayList<>(includedFields != null ? includedFields : Collections.emptyList());
    }

    public List<Field> getIncludedFields()
    {
        List<Field> fieldsToInclude = Arrays.asList(mappedClass.getDeclaredFields());

        if(includeAllFields)
        {
            return fieldsToInclude;
        }

        return fieldsToInclude
            .stream()
            .filter(f -> includedFields.contains(f.getName()))
            .collect(Collectors.toList());
    }
}
