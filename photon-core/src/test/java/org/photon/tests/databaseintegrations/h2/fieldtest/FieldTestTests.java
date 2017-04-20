package org.photon.tests.databaseintegrations.h2.fieldtest;

import org.junit.Before;
import org.junit.Test;
import org.photon.Photon;
import org.photon.PhotonTransaction;
import org.photon.tests.entities.fieldtest.FieldTest;
import org.photon.tests.entities.fieldtest.TestEnum;

import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;

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

            assertEquals("2017-03-19 09:28:17", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(fieldTest.getDate()));
            assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1489933698000L), ZoneId.systemDefault()), fieldTest.getZonedDateTime());
            assertEquals(LocalDate.ofEpochDay(17244), fieldTest.getLocalDate());
            assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(1489933700000L), ZoneId.systemDefault()), fieldTest.getLocalDateTime());
            assertEquals(Instant.ofEpochMilli(1489933701000L), fieldTest.getInstant());

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
