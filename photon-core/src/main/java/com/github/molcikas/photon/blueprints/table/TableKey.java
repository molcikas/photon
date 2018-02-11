package com.github.molcikas.photon.blueprints.table;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

public class TableKey
{
    @Getter
    private final Object key;

    public TableKey(Object key)
    {
        this.key = key;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableKey other = (TableKey) o;

        if (Objects.equals(key, other.key))
        {
            return true;
        }

        return key instanceof byte[] && other.key instanceof byte[] && Arrays.equals((byte[]) key, (byte[]) other.key);
    }

    @Override
    public int hashCode()
    {
        if (key instanceof byte[])
        {
            byte[] keyArray = (byte[]) key;
            int hash = keyArray.length;
            int position = 0;
            for (byte b : keyArray)
            {
                hash += b << (position % 4);
                position++;
            }
            return hash;
        }

        return Objects.hash(key);
    }
}
