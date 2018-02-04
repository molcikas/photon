package com.github.molcikas.photon.blueprints;

import com.github.molcikas.photon.blueprints.table.TableKey;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class AggregateBlueprintAndKey
{
    private final AggregateBlueprint<?> aggregateBlueprint;
    private final TableKey primaryKey;
}
