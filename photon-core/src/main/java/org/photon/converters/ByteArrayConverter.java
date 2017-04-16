package org.photon.converters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.UUID;

public class ByteArrayConverter implements Converter<byte[]>
{

    public byte[] convert(Object val) throws ConverterException {
        if (val == null) return null;

        if (val instanceof Blob) {
            Blob b = (Blob)val;
            InputStream stream=null;
            try {
                try {
                    stream = b.getBinaryStream();
                    return IOUtils.toByteArray(stream);
                } finally {
                    if(stream!=null) {
                        try {
                            stream.close();
                        } catch (Throwable ignore){
                            // ignore stream.close errors
                        }
                    }
                    try {
                        b.free();
                    } catch (Throwable ignore){
                        // ignore blob.free errors
                    }
                }
            } catch (SQLException e) {
                throw new ConverterException("Error converting Blob to byte[]", e);
            } catch (IOException e) {
                throw new ConverterException("Error converting Blob to byte[]", e);
            }
        }

        if (val instanceof byte[]){
            return (byte[])val;
        }

        if(val instanceof UUID)
        {
            UUID uuid = (UUID) val;
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());
            return bb.array();
        }

        throw new RuntimeException("could not convert " + val.getClass().getName() + " to byte[]");
    }
}
