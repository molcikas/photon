package com.github.molcikas.photon.query;

import com.github.molcikas.photon.blueprints.table.ColumnBlueprint;
import com.github.molcikas.photon.blueprints.table.ColumnDataType;
import com.github.molcikas.photon.blueprints.table.TableValue;
import com.github.molcikas.photon.converters.Converter;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(doNotUseGetters = true)
public class ParameterValue
{
    private final TableValue value;

    @Getter
    private final ColumnDataType dataType;

    @Getter
    private final Converter customSerializer;

    public ParameterValue(Object value, ColumnDataType dataType, Converter customSerializer)
    {
        this.value = new TableValue(value);
        this.dataType = dataType;
        this.customSerializer = customSerializer;
    }

    public ParameterValue(Object value, ColumnBlueprint columnBlueprint)
    {
        this.value = new TableValue(value);
        this.dataType = columnBlueprint.getColumnDataType();
        this.customSerializer = columnBlueprint.getCustomSerializer();
    }

    public Object getRawValue()
    {
        return value.getValue();
    }
}