package com.github.molcikas.photon.query;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

@AllArgsConstructor
@Getter
public class GetParameterValuesResult
{
    private boolean skipped;
    private boolean changed;
    private Map<String, ParameterValue> values;

    public static GetParameterValuesResult skipped()
    {
        return new GetParameterValuesResult(true, false, Collections.emptyMap());
    }

    public static GetParameterValuesResult unchanged()
    {
        return new GetParameterValuesResult(false, false, Collections.emptyMap());
    }
}