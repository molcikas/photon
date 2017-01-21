package photon.converters;

/**
 * Represents an exception thrown from a converter.
 */
public class ConverterException extends RuntimeException
{
    public ConverterException(String message) {
        super(message);
    }

    public ConverterException(String message, Throwable cause) {
        super(message, cause);
    }
}
