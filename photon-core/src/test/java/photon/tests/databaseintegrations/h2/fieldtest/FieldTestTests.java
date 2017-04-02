package photon.tests.databaseintegrations.h2.fieldtest;

import org.junit.Before;
import org.junit.Test;
import photon.Photon;
import photon.PhotonConnection;
import photon.tests.entities.fieldtest.FieldTest;
import photon.tests.entities.fieldtest.TestEnum;

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

        try(PhotonConnection connection = photon.open())
        {
            FieldTest fieldTest = connection
                .query(FieldTest.class)
                .fetchById(1);

            assertNotNull(fieldTest);
            assertEquals(1, fieldTest.getId());

            assertEquals(fieldTest.getDate(), new Date(1489933697000L));
            assertEquals(fieldTest.getZonedDateTime(), ZonedDateTime.ofInstant(Instant.ofEpochMilli(1489933698000L), ZoneId.systemDefault()));
            assertEquals(fieldTest.getLocalDate(), LocalDate.ofEpochDay(17244));
            assertEquals(fieldTest.getLocalDateTime(), LocalDateTime.ofInstant(Instant.ofEpochMilli(1489933700000L), ZoneId.systemDefault()));
            assertEquals(fieldTest.getInstant(), Instant.ofEpochMilli(1489933701000L));

            assertEquals(fieldTest.getTestEnumNumber(), TestEnum.VALUE_ZERO);
            assertEquals(fieldTest.getTestEnumString(), TestEnum.VALUE_ONE);
        }
    }

    @Test
    public void queryAggregate_nullValues_fetchesAggregateWithAllNullValues()
    {
        FieldTestDbSetup.registerAggregate(photon);

        try(PhotonConnection connection = photon.open())
        {
            FieldTest fieldTest = connection
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

        try(PhotonConnection connection = photon.open())
        {
            FieldTest fieldTest = new FieldTest(3, date, zonedDateTime, localDate, localDateTime, instant, TestEnum.VALUE_ONE, TestEnum.VALUE_TWO);
            connection.save(fieldTest);
        }

        try(PhotonConnection connection = photon.open())
        {
            FieldTest fieldTest = connection
                .query(FieldTest.class)
                .fetchById(3);

            assertNotNull(fieldTest);
            assertEquals(3, fieldTest.getId());

            assertEquals(fieldTest.getDate(), date);
            assertEquals(fieldTest.getZonedDateTime(), zonedDateTime);
            assertEquals(fieldTest.getLocalDate(), localDate);
            assertEquals(fieldTest.getLocalDateTime(), localDateTime);
            assertEquals(fieldTest.getInstant(), instant);

            assertEquals(fieldTest.getTestEnumNumber(), TestEnum.VALUE_ONE);
            assertEquals(fieldTest.getTestEnumString(), TestEnum.VALUE_TWO);
        }
    }
}
