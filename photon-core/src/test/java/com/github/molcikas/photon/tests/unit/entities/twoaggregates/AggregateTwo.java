package com.github.molcikas.photon.tests.unit.entities.twoaggregates;

import java.util.UUID;

public class AggregateTwo
{
    private UUID aggregateTwoId;

    private String myValue;

    public UUID getAggregateTwoId()
    {
        return aggregateTwoId;
    }

    public String getMyValue()
    {
        return myValue;
    }

    private AggregateTwo()
    {
    }
}
