package com.github.molcikas.photon.blueprints.entity;

import com.github.molcikas.photon.blueprints.table.TableValue;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class EntityBlueprintAndKey
{
    private final EntityBlueprint entityBlueprint;
    private final TableValue primaryKey;
}
