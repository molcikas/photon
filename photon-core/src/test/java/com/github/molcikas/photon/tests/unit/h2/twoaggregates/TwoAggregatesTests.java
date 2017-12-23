package com.github.molcikas.photon.tests.unit.h2.twoaggregates;

import com.github.molcikas.photon.blueprints.table.ColumnDataType;
import org.junit.Before;
import org.junit.Test;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.tests.unit.entities.twoaggregates.AggregateOne;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class TwoAggregatesTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = TwoAggregatesDbSetup.setupDatabase();
    }

    @Test
    public void aggregate_fetchById_simpleEntity_returnsEntity()
    {
        registerAggregates();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            AggregateOne aggregateOne = transaction
                .query(AggregateOne.class)
                .fetchById(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000012"));

            assertNotNull(aggregateOne);
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000012"), aggregateOne.getAggregateOneId());
            assertEquals("agg1val2", aggregateOne.getMyValue());
            assertEquals(3, aggregateOne.getAggregateTwos().size());
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000020"), aggregateOne.getAggregateTwos().get(0));
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000021"), aggregateOne.getAggregateTwos().get(1));
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000022"), aggregateOne.getAggregateTwos().get(2));
        }
    }

    @Test
    public void aggregate_fetchByIds_simpleEntity_returnsEntity()
    {
        registerAggregates();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            List<AggregateOne> aggregateOnes = transaction
                .query(AggregateOne.class)
                .fetchByIds(Arrays.asList(
                    UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000011"),
                    UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000012"),
                    UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000010")
                ));

            assertNotNull(aggregateOnes);
            assertEquals(3, aggregateOnes.size());

            AggregateOne aggregateOne0 = aggregateOnes.get(0);
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000010"), aggregateOne0.getAggregateOneId());
            assertEquals(0, aggregateOne0.getAggregateTwos().size());

            AggregateOne aggregateOne1 = aggregateOnes.get(1);
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000011"), aggregateOne1.getAggregateOneId());
            assertEquals(1, aggregateOne1.getAggregateTwos().size());
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000021"), aggregateOne1.getAggregateTwos().get(0));

            AggregateOne aggregateOne2 = aggregateOnes.get(2);
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000012"), aggregateOne2.getAggregateOneId());
            assertEquals("agg1val2", aggregateOne2.getMyValue());
            assertEquals(3, aggregateOne2.getAggregateTwos().size());
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000020"), aggregateOne2.getAggregateTwos().get(0));
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000021"), aggregateOne2.getAggregateTwos().get(1));
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000022"), aggregateOne2.getAggregateTwos().get(2));
        }
    }

    @Test
    public void aggregate_save_simpleEntity_updatesEntity()
    {
        registerAggregates();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            List<AggregateOne> aggregateOnes = transaction
                .query(AggregateOne.class)
                .fetchByIds(Arrays.asList(
                    UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000011"),
                    UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000012"),
                    UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000010")
                ));

            aggregateOnes.get(0).getAggregateTwos().add(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000020"));
            aggregateOnes.get(0).getAggregateTwos().add(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000022"));
            aggregateOnes.get(0).getAggregateTwos().add(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000022")); // Duplicates should be ignored.
            aggregateOnes.get(1).getAggregateTwos().clear();
            aggregateOnes.get(1).getAggregateTwos().add(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000020"));
            aggregateOnes.get(2).getAggregateTwos().clear();

            transaction.saveAll(aggregateOnes);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            List<AggregateOne> aggregateOnes = transaction
                .query(AggregateOne.class)
                .fetchByIds(Arrays.asList(
                    UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000011"),
                    UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000012"),
                    UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000010")
                ));

            assertNotNull(aggregateOnes);
            assertEquals(3, aggregateOnes.size());

            AggregateOne aggregateOne0 = aggregateOnes.get(0);
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000010"), aggregateOne0.getAggregateOneId());
            assertEquals(2, aggregateOne0.getAggregateTwos().size());
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000020"), aggregateOne0.getAggregateTwos().get(0));
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000022"), aggregateOne0.getAggregateTwos().get(1));

            AggregateOne aggregateOne1 = aggregateOnes.get(1);
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000011"), aggregateOne1.getAggregateOneId());
            assertEquals(1, aggregateOne1.getAggregateTwos().size());
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000020"), aggregateOne1.getAggregateTwos().get(0));

            AggregateOne aggregateOne2 = aggregateOnes.get(2);
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000012"), aggregateOne2.getAggregateOneId());
            assertEquals("agg1val2", aggregateOne2.getMyValue());
            assertEquals(0, aggregateOne2.getAggregateTwos().size());

            transaction.commit();
        }
    }

    @Test
    public void aggregate_deleteAll_allButOneEntity_deletesEntities()
    {
        registerAggregates();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            List<AggregateOne> aggregateOnes = transaction
                .query(AggregateOne.class)
                .fetchByIds(Arrays.asList(
                    UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000012"),
                    UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000010")
                ));

            transaction.deleteAll(aggregateOnes);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            List<AggregateOne> aggregateOnes = transaction
                .query(AggregateOne.class)
                .fetchByIds(Arrays.asList(
                    UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000011"),
                    UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000012"),
                    UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000010")
                ));

            assertNotNull(aggregateOnes);
            assertEquals(1, aggregateOnes.size());

            AggregateOne aggregateOne1 = aggregateOnes.get(0);
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000011"), aggregateOne1.getAggregateOneId());
            assertEquals(1, aggregateOne1.getAggregateTwos().size());
            assertEquals(UUID.fromString("3DFFC3B3-A9B6-11E6-AB83-0A0027000021"), aggregateOne1.getAggregateTwos().get(0));

            transaction.commit();
        }
    }

    private void registerAggregates()
    {
        photon.registerAggregate(AggregateOne.class)
            .withId("aggregateOneId")
            .withFlattenedCollection("aggregateTwos", UUID.class, "aggregatemapping", "aggregateOneId", "aggregateTwoId", ColumnDataType.BINARY)
            .register();
    }
}
