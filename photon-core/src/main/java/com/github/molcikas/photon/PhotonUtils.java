package com.github.molcikas.photon;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PhotonUtils
{
    private static final Set<Class<?>> primitiveNumbers = Stream
        .of(int.class, long.class, float.class, double.class, byte.class, short.class)
        .collect(Collectors.toSet());

    public static boolean isNumericType(Class<?> cls)
    {
        if (cls.isPrimitive())
        {
            return primitiveNumbers.contains(cls);
        }
        else
        {
            return Number.class.isAssignableFrom(cls);
        }
    }
}
