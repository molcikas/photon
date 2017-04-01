package photon.converters.date;

import photon.converters.Converter;
import photon.converters.ConverterException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class LocalDateTimeConverter implements Converter<LocalDateTime>
{
    public LocalDateTime convert(Object val) throws ConverterException
    {
        if (val == null)
        {
            return null;
        }

        if(LocalDateTime.class.isAssignableFrom(val.getClass()))
        {
            return (LocalDateTime) val;
        }

        if(Date.class.isAssignableFrom(val.getClass()))
        {
            Instant instant = Instant.ofEpochMilli(((Date) val).getTime());
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        }

        if (val instanceof Number)
        {
            Instant instant = Instant.ofEpochMilli(((Number) val).longValue());
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        }

        throw new ConverterException("Cannot convert type " + val.getClass().toString() + " to java.util.LocalDateTime");
    }
}
