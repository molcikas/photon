package com.github.molcikas.photon.blueprints.table;

import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

@AllArgsConstructor
public class TableKey
{
    private Object key;

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableKey other = (TableKey) o;

        if (key.equals(other.key))
        {
            return true;
        }

        return key instanceof byte[] && other.key instanceof byte[] && Arrays.equals((byte[]) key, (byte[]) other.key);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(key);
    }
}
