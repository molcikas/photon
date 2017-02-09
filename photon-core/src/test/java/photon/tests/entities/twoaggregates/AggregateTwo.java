package photon.tests.entities.twoaggregates;

import java.util.List;
import java.util.UUID;

public class AggregateTwo
{
    private UUID aggregateTwoId;

    private String myValue;

    private List<UUID> aggregateTwoIds;

    public UUID getAggregateTwoId()
    {
        return aggregateTwoId;
    }

    public String getMyValue()
    {
        return myValue;
    }

    public List<UUID> getAggregateTwoIds()
    {
        return aggregateTwoIds;
    }

    private AggregateTwo()
    {
    }

    public AggregateTwo(UUID aggregateTwoId, String myValue, List<UUID> aggregateTwoIds)
    {
        this.aggregateTwoId = aggregateTwoId;
        this.myValue = myValue;
        this.aggregateTwoIds = aggregateTwoIds;
    }
}
