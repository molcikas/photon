package com.github.molcikas.photon.converters;

/**
 * Represents a converter.
 */
public interface Converter<T> {

    T convert(Object val) throws ConverterException;
}
