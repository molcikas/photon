package com.github.molcikas.photon.blueprints.entity;

import com.github.molcikas.photon.blueprints.table.TableKey;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class EntityBlueprintAndKey
{
    private final EntityBlueprint entityBlueprint;
    private final TableKey primaryKey;
}
