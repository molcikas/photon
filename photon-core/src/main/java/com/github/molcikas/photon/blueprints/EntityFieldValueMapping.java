package com.github.molcikas.photon.blueprints;

import java.util.Map;

public interface EntityFieldValueMapping<T>
{
    T getFieldValue(Map<String, Object> entityValues);
    void setFieldValue(Map<String, Object> entityValues, T fieldValue);
}
