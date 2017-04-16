package org.photon.converters.date;

import org.photon.converters.ConverterException;
import org.photon.converters.Converter;

import java.time.Instant;
import java.util.Date;

public class InstantConverter implements Converter<Instant>
{
    public Instant convert(Object val) throws ConverterException
    {
        if (val == null)
        {
            return null;
        }

        if(Instant.class.isAssignableFrom(val.getClass()))
        {
            return (Instant) val;
        }

        if(Date.class.isAssignableFrom(val.getClass()))
        {
            return Instant.ofEpochMilli(((Date) val).getTime());
        }

        if (val instanceof Number)
        {
            return Instant.ofEpochMilli(((Number) val).longValue());
        }

        throw new ConverterException("Cannot convert type " + val.getClass().toString() + " to java.util.Instant");
    }
}
