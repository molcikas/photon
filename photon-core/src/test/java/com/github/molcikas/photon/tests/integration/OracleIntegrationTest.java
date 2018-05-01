package com.github.molcikas.photon.tests.integration;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.options.PhotonOptions;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * For running these tests, you will need to download and install the Oracle JDBC driver (ojdbc6.jar) to src/test/libs.
 */
public class OracleIntegrationTest
{
    private Photon photon;

    @Before
    public void setup()
    {
        String url = "jdbc:oracle:thin:@localhost:1521:ORCL";
        photon = new Photon(url, "system", "bears2", PhotonOptions.oracleOptions().build());

        photon
            .registerAggregate(PhotonTestTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            transaction.query(
            "BEGIN\n" +
                "   EXECUTE IMMEDIATE 'DROP TABLE PHOTONTESTTABLE';\n" +
                "EXCEPTION\n" +
                "   WHEN OTHERS THEN\n" +
                "      IF SQLCODE != -942 THEN\n" +
                "         RAISE;\n" +
                "      END IF;\n" +
                "END;"
            ).executeInsert();

            transaction.query(
            "CREATE TABLE PHOTONTESTTABLE( " +
                "ID NUMBER GENERATED AS IDENTITY, " +
                "UUIDCOLUMN RAW(16) NOT NULL, " +
                "DATECOLUMN DATE NOT NULL, " +
                "VARCHARCOLUMN VARCHAR2(50) NOT NULL, " +
                "CONSTRAINT PHOTONTESTTABLE_PK PRIMARY KEY (ID) " +
                ")"
            ).executeInsert();

            transaction.query("INSERT INTO PhotonTestTable VALUES (DEFAULT, '8ED1E1BD253E4469B4CB71E1217825B7', DATE '1970-01-01' + 1489915698/24/60/60, 'Test String')").executeInsert();

            transaction.commit();
        }
    }

    // TODO: This test fails if run during standard time instead of DST. The time is off by one hour.
    @Test
    public void fetchExistingAggregateById_populatesValues()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            PhotonTestTable photonTestTable = transaction.query(PhotonTestTable.class).fetchById(1);

            // The database does not store a time zone, so we assume the date is in the system's time zone. But to make these tests
            // compare epoch times but still work with any system time zone, we have to offset the epoch to the system's time zone.
            int currentUtcOffset = TimeZone.getDefault().getOffset(new Date().getTime());

            assertNotNull(photonTestTable);
            assertEquals(1, photonTestTable.getId());
            assertEquals(UUID.fromString("8ED1E1BD-253E-4469-B4CB-71E1217825B7"), photonTestTable.getUuidColumn());
            assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1489915698000L - currentUtcOffset), ZoneId.systemDefault()), photonTestTable.getDateColumn());
            assertEquals("Test String", photonTestTable.getVarcharColumn());
        }
    }

    @Test
    public void insertAggregateAndFetch_insertsAggregateAndPopulatesValues()
    {
        PhotonTestTable photonTestTable = new PhotonTestTable(
            null,
            UUID.fromString("11111111-2222-3333-4444-555555555555"),
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(1493493022), ZoneId.systemDefault()),
            "My Test String"
        );

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            transaction.insert(photonTestTable);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            PhotonTestTable photonTestTableFetched = transaction.query(PhotonTestTable.class).fetchById(2);

            assertEquals(photonTestTable, photonTestTableFetched);
        }
    }
}
