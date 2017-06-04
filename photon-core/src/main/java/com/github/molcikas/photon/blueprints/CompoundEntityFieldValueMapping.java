package com.github.molcikas.photon.blueprints;

import java.util.Map;

/**
 * Interface for custom mapping a group of database values to and from one or more entity fields.
 *
 * @param <E> - The entity class type
 */
public interface CompoundEntityFieldValueMapping<E>
{
    /**
     * Get the database values from a given entity instance.
     *
     * @param entityInstance - The entity instance
     * @return - The database values. The key is the column name and the value is the database value.
     */
    Map<String, Object> getDatabaseValues(E entityInstance);

    /**
     * Set a given set of database values on a given entity instance.
     *
     * @param entityInstance - The entity instance
     * @param databaseValues - The database values. The key is the column name and the value is the database value.
     * @return - The field values to set on the entity. The key is the field name and the value is the field value. If
     *           the values were applied directly to the entity instance, then null or an empty map can be returned.
     */
    Map<String, Object> setFieldValues(E entityInstance, Map<String, Object> databaseValues);
}
