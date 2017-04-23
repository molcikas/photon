package com.github.molcikas.photon.tests.databaseintegrations.h2.fieldtest;

import org.junit.Before;
import org.junit.Test;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.tests.entities.fieldtest.FieldTest;
import com.github.molcikas.photon.tests.entities.fieldtest.TestEnum;

import java.time.*;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class FieldTestTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = FieldTestDbSetup.setupDatabase();
    }

    @Test
    public void queryAggregate_withDates_fetchesAggregateWithCorrectValues()
    {
        FieldTestDbSetup.registerAggregate(photon);

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            FieldTest fieldTest = transaction
                .query(FieldTest.class)
                .fetchById(1);

            assertNotNull(fieldTest);
            assertEquals(1, fieldTest.getId());

            // The database does not store a time zone, so we assume the date is in the system's time zone. But to make these tests
            // compare epoch times but still work with any system time zone, we have to offset the epoch to the system's time zone.
            int currentUtcOffset = TimeZone.getDefault().getOffset(new Date().getTime());

            assertEquals(new Date(1489915697000L - currentUtcOffset), fieldTest.getDate());
            assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1489915698000L - currentUtcOffset), ZoneId.systemDefault()), fieldTest.getZonedDateTime());
            assertEquals(LocalDate.ofEpochDay(17244), fieldTest.getLocalDate());
            assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(1489915700000L - currentUtcOffset), ZoneId.systemDefault()), fieldTest.getLocalDateTime());
            assertEquals(Instant.ofEpochMilli(1489915701000L - currentUtcOffset), fieldTest.getInstant());

            assertEquals(TestEnum.VALUE_ZERO, fieldTest.getTestEnumNumber());
            assertEquals(TestEnum.VALUE_ONE, fieldTest.getTestEnumString());
        }
    }

    @Test
    public void queryAggregate_nullValues_fetchesAggregateWithAllNullValues()
    {
        FieldTestDbSetup.registerAggregate(photon);

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            FieldTest fieldTest = transaction
                .query(FieldTest.class)
                .fetchById(2);

            assertNotNull(fieldTest);
            assertEquals(2, fieldTest.getId());

            assertNull(fieldTest.getDate());
            assertNull(fieldTest.getZonedDateTime());
            assertNull(fieldTest.getLocalDate());
            assertNull(fieldTest.getLocalDateTime());
            assertNull(fieldTest.getInstant());

            assertNull(fieldTest.getTestEnumNumber());
            assertNull(fieldTest.getTestEnumString());
        }
    }

    @Test
    public void createAggregate_withDates_createsAggregateWithCorrectValues()
    {
        FieldTestDbSetup.registerAggregate(photon);

        Date date = new Date(1481122691000L);
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1481122692000L), ZoneId.systemDefault());
        LocalDate localDate = LocalDate.ofEpochDay(17184);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(1481122693000L), ZoneId.systemDefault());
        Instant instant = Instant.ofEpochMilli(1481122694000L);

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            FieldTest fieldTest = new FieldTest(3, date, zonedDateTime, localDate, localDateTime, instant, TestEnum.VALUE_ONE, TestEnum.VALUE_TWO);
            transaction.save(fieldTest);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            FieldTest fieldTest = transaction
                .query(FieldTest.class)
                .fetchById(3);

            assertNotNull(fieldTest);
            assertEquals(3, fieldTest.getId());

            assertEquals(date, fieldTest.getDate());
            assertEquals(zonedDateTime, fieldTest.getZonedDateTime());
            assertEquals(localDate, fieldTest.getLocalDate());
            assertEquals(localDateTime, fieldTest.getLocalDateTime());
            assertEquals(instant, fieldTest.getInstant());

            assertEquals(TestEnum.VALUE_ONE, fieldTest.getTestEnumNumber());
            assertEquals(TestEnum.VALUE_TWO, fieldTest.getTestEnumString());

            transaction.commit();
        }
    }
}
