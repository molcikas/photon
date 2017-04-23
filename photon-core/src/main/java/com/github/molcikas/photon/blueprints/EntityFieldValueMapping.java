package com.github.molcikas.photon.blueprints;

public interface EntityFieldValueMapping<E, F>
{
    F getFieldValueFromEntityInstance(E entityInstance);
    void setFieldValueOnEntityInstance(E entityInstance, F fieldValue);
}
