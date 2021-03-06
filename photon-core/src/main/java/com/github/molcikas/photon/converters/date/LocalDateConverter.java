package com.github.molcikas.photon.converters.date;

import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.converters.ConverterException;

import java.time.LocalDate;
import java.util.Date;

public class LocalDateConverter implements Converter<LocalDate>
{
    private static final long MILLSECONDS_PER_DAY = 86400000;

    public LocalDate convert(Object val) throws ConverterException
    {
        if (val == null)
        {
            return null;
        }

        if(LocalDate.class.isAssignableFrom(val.getClass()))
        {
            return (LocalDate) val;
        }

        if(Date.class.isAssignableFrom(val.getClass()))
        {
            return LocalDate.ofEpochDay(((Date) val).getTime() / MILLSECONDS_PER_DAY);
        }

        if (val instanceof Number)
        {
            return LocalDate.ofEpochDay(((Number) val).longValue() / MILLSECONDS_PER_DAY);
        }

        throw new ConverterException("Cannot convert type " + val.getClass().toString() + " to java.util.LocalDate");
    }
}
