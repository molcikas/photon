package photon.converters;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Used by sql2o to convert a value from the database into a {@link UUID}.
 */
public class UUIDConverter implements Converter<UUID>
{
    public UUID convert(Object val) throws ConverterException {
        if (val == null){
            return null;
        }

        if (val instanceof UUID){
            return (UUID)val;
        }

        if(val instanceof String){
            return UUID.fromString((String) val);
        }

        if(val instanceof byte[]) {
            ByteBuffer buffer = ByteBuffer.wrap((byte[]) val);
            return new UUID(buffer.getLong(), buffer.getLong());
        }

        throw new ConverterException("Cannot convert type " + val.getClass() + " " + UUID.class);
    }
}

