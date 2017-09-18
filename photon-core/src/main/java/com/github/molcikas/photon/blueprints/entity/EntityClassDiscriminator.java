package com.github.molcikas.photon.blueprints.entity;

import java.util.Map;

/**
 * The interface for custom entity class discriminators.
 */
public interface EntityClassDiscriminator
{
    /**
     * Called after entity column values are retrieved but just before the entity is constructed. This allows
     * dynamically determining the entity class based on the column values. Typically, this is used to implement
     * single-table inheritance.
     *
     * @param valueMap - the column values for the entity instance
     * @return - the entity class to construct
     */
    Class getClassForEntity(Map<String, Object> valueMap);
}
