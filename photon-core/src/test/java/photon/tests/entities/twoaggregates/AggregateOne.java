package photon.tests.entities.twoaggregates;

import java.util.List;
import java.util.UUID;

public class AggregateOne
{
    private UUID aggregateOneId;

    private String myValue;

    private List<UUID> aggregateTwos;

    public UUID getAggregateOneId()
    {
        return aggregateOneId;
    }

    public String getMyValue()
    {
        return myValue;
    }

    public List<UUID> getAggregateTwos()
    {
        return aggregateTwos;
    }

    private AggregateOne()
    {
    }

    public AggregateOne(UUID aggregateOneId, String myValue, List<UUID> aggregateTwos)
    {
        this.aggregateOneId = aggregateOneId;
        this.myValue = myValue;
        this.aggregateTwos = aggregateTwos;
    }
}
