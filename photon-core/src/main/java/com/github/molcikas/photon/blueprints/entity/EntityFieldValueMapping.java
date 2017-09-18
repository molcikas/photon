package com.github.molcikas.photon.blueprints.entity;

import java.util.Map;

/**
 * Interface for custom mapping for getting and setting field values on an entity for a given database column.
 *
 * @param <E> - The entity class type
 * @param <F> - The field class type
 */
public interface EntityFieldValueMapping<E, F>
{
    /**
     * Get the field value from the entity instance that maps to the database column.
     *
     * @param entityInstance - The entity instance
     * @return - The field value
     */
    F getFieldValue(E entityInstance);

    /**
     * Set the field value(s) on a given entity instance that maps to the database column. The value can be applied
     * directly to the entity instance, or the method can return a map of values that will be applied to the entity
     * instance (to avoid having to write reflection code directly in this method).
     *
     * @param entityInstance - The entity instance
     * @param fieldValue - The field value
     * @return - The field values to set on the entity. The key is the field name and the value is the field value. If
     *           the value was applied directly to the entity instance, then null or an empty map can be returned.
     */
    Map<String, Object> setFieldValue(E entityInstance, F fieldValue);
}
