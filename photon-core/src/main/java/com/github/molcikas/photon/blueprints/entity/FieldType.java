package com.github.molcikas.photon.blueprints.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum FieldType
{
    Primitive(false),
    Entity(true),
    EntityList(true),
    CustomValueMapper(false),
    CompoundCustomValueMapper(false),
    FlattenedCollection(false);

    @Getter
    private final boolean hasChildEntities;
}
