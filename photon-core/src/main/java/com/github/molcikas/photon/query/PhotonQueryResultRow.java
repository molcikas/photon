package com.github.molcikas.photon.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PhotonQueryResultRow
{
    private final Map<String, Object> values;
    private Object firstValue;

    public void addValue(String columnName, Object value)
    {
        if(values.isEmpty())
        {
            firstValue = value;
        }
        values.put(columnName, value);
    }

    public Object getValue(String columnName)
    {
        return values.get(columnName);
    }

    public Set<Map.Entry<String, Object>> getValues()
    {
        return values.entrySet();
    }

    public Map<String, Object> getValuesMap()
    {
        return Collections.unmodifiableMap(values);
    }

    public Object getFirstValue()
    {
        return firstValue;
    }

    public PhotonQueryResultRow()
    {
        values = new HashMap<>();
    }
}
