package com.github.molcikas.photon.blueprints.table;

import lombok.Getter;

import javax.xml.bind.DatatypeConverter;
import java.util.Arrays;
import java.util.Objects;

public class TableValue
{
    @Getter
    private final Object value;

    public TableValue(Object value)
    {
        this.value = value;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableValue other = (TableValue) o;

        if (Objects.equals(value, other.value))
        {
            return true;
        }

        return value instanceof byte[] && other.value instanceof byte[] && Arrays.equals((byte[]) value, (byte[]) other.value);
    }

    @Override
    public int hashCode()
    {
        if (value instanceof byte[])
        {
            byte[] keyArray = (byte[]) value;
            int hash = keyArray.length;
            int position = 0;
            for (byte b : keyArray)
            {
                hash += b << (position % 4);
                position++;
            }
            return hash;
        }

        return Objects.hash(value);
    }

    @Override
    public String toString()
    {
        if(value == null)
        {
            return "(null)";
        }

        if(value instanceof byte[])
        {
            return "(" + DatatypeConverter.printHexBinary((byte[]) value) + ")";
        }

        return "(" + value + ")";
    }
}
