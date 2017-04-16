package org.photon.converters;

import java.util.Map;

/**
 * User: dimzon
 * Date: 4/24/14
 * Time: 12:53 AM
 */
public interface ConvertersProvider {
    void fill(Map<Class<?>, Converter<?>> mapToFill);
}
