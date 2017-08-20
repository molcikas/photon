package com.github.molcikas.photon.tests.unit.entities.someaggregate;

import java.util.List;

public class SomeAggregate
{
    private int id;

    private List<SomeClass> fieldOne;

    private SomeAggregate()
    {
    }

    public SomeAggregate(int id, List<SomeClass> fieldOne)
    {
        this.id = id;
        this.fieldOne = fieldOne;
    }
}
