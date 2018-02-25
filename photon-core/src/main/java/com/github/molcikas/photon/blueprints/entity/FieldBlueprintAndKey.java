package com.github.molcikas.photon.blueprints.entity;

import com.github.molcikas.photon.blueprints.table.TableValue;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class FieldBlueprintAndKey
{
    private final FieldBlueprint fieldBlueprint;
    private final TableValue primaryKey;

    @Override
    public String toString()
    {
        return fieldBlueprint.getFieldClass().getSimpleName() + "." + fieldBlueprint.getFieldName() + ":" + primaryKey;
    }
}
