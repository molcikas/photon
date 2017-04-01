package photon.converters;

/**
 * Represents a converter.
 */
public interface Converter<T> {

    /**
     * Conversion from SQL to Java.
     */
    T convert(Object val) throws ConverterException;
}
