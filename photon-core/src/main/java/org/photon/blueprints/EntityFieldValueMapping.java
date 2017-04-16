package org.photon.blueprints;

public interface EntityFieldValueMapping<E, F>
{
    F getFieldValueFromEntityInstance(E entityInstance);
    void setFieldValueOnEntityInstance(E entityInstance, F fieldValue);
}
