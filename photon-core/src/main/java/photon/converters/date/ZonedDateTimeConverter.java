package photon.converters.date;

import photon.converters.Converter;
import photon.converters.ConverterException;

import java.time.*;
import java.util.Date;

public class ZonedDateTimeConverter implements Converter<ZonedDateTime>
{
    public ZonedDateTime convert(Object val) throws ConverterException
    {
        if (val == null)
        {
            return null;
        }

        if(ZonedDateTime.class.isAssignableFrom(val.getClass()))
        {
            return (ZonedDateTime) val;
        }

        if(Date.class.isAssignableFrom(val.getClass()))
        {
            Instant instant = Instant.ofEpochMilli(((Date) val).getTime());
            return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        }

        if (val instanceof Number)
        {
            Instant instant = Instant.ofEpochMilli(((Number) val).longValue());
            return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        }

        throw new ConverterException("Cannot convert type " + val.getClass().toString() + " to java.util.ZonedDateTime");
    }
}
