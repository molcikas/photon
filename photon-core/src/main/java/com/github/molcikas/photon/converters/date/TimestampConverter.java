package com.github.molcikas.photon.converters.date;

import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.converters.ConverterException;

import java.sql.Timestamp;
import java.time.*;
import java.util.Date;

public class TimestampConverter implements Converter<Timestamp>
{
    public Timestamp convert(Object val) throws ConverterException
    {
        if (val == null)
        {
            return null;
        }

        if(Date.class.isAssignableFrom(val.getClass()))
        {
            return new Timestamp(((Date)val).getTime());
        }

        if(ZonedDateTime.class.isAssignableFrom(val.getClass()))
        {
            return new Timestamp(((ZonedDateTime)val).toEpochSecond() * 1000);
        }

        if(LocalDate.class.isAssignableFrom(val.getClass()))
        {
            LocalDate localDate = (LocalDate) val;
            return new Timestamp(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond() * 1000);
        }

        if(LocalDateTime.class.isAssignableFrom(val.getClass()))
        {
            LocalDateTime localDateTime = (LocalDateTime) val;
            return new Timestamp(localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000);
        }

        if(Instant.class.isAssignableFrom(val.getClass()))
        {
            return new Timestamp(((Instant)val).getEpochSecond() * 1000);
        }

        if (val instanceof Number)
        {
            return new Timestamp(((Number) val).longValue());
        }

        throw new ConverterException("Cannot convert type " + val.getClass().toString() + " to java.sql.Timestamp");
    }
}
