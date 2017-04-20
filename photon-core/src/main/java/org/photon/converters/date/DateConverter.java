package org.photon.converters.date;

import org.photon.converters.Converter;
import org.photon.converters.ConverterException;

import java.time.*;
import java.util.Date;

public class DateConverter implements Converter<Date>
{
    public Date convert(Object val) throws ConverterException
    {
        if (val == null)
        {
            return null;
        }

        System.out.println("Converting " + val.getClass().getSimpleName() + " value to date: " + val.toString());

        if(Date.class.isAssignableFrom(val.getClass()))
        {
            System.out.println("Converting the date value to another date: " + ((Date)val).getTime());
            Date date = new Date(((Date)val).getTime());
            System.out.println("Converted the date to " + date.toString() + " epoch " + date.getTime());
            return date;
        }

        if(ZonedDateTime.class.isAssignableFrom(val.getClass()))
        {
            return new Date(((ZonedDateTime)val).toEpochSecond() * 1000);
        }

        if(LocalDate.class.isAssignableFrom(val.getClass()))
        {
            LocalDate localDate = (LocalDate) val;
            return new Date(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond() * 1000);
        }

        if(LocalDateTime.class.isAssignableFrom(val.getClass()))
        {
            LocalDateTime localDateTime = (LocalDateTime) val;
            return new Date(localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000);
        }

        if(Instant.class.isAssignableFrom(val.getClass()))
        {
            return new Date(((Instant)val).getEpochSecond() * 1000);
        }

        if (val instanceof Number)
        {
            return new Date(((Number) val).longValue());
        }

        throw new ConverterException("Cannot convert type " + val.getClass().toString() + " to java.util.Date");
    }
}
