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
    private final TableValue primaryKey;

    @Override
    public String toString()
    {
        return tableBlueprint.getTableName() + ":" + primaryKey;
    }
}
