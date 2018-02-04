package com.github.molcikas.photon.blueprints.entity;

import com.github.molcikas.photon.blueprints.table.TableKey;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class FieldBlueprintAndKey
{
    private final FieldBlueprint fieldBlueprint;
    private final TableKey primaryKey;
}
