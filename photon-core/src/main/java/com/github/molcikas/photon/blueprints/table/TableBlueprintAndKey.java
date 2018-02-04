package com.github.molcikas.photon.blueprints.table;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class TableBlueprintAndKey
{
    private final TableBlueprint tableBlueprint;
    private final TableKey primaryKey;
}
