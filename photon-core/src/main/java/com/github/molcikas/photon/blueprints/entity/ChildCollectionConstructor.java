package com.github.molcikas.photon.blueprints.entity;

import java.util.Collection;

public interface ChildCollectionConstructor<F, E, P>
{
    Collection<E> toCollection(F fieldValue, P parentEntityInstance);

    F toFieldValue(Collection<E> collection, P parentEntityInstance);
}
