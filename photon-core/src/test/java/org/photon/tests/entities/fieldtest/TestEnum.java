package org.photon.tests.entities.fieldtest;

public enum TestEnum
{
    VALUE_ZERO,
    VALUE_ONE,
    VALUE_TWO,
    VALUE_THREE;

    @Override
    public String toString()
    {
        return "Hopefully nothing relies on toString() returning the enum value because it's not!";
    }
}
