package com.github.molcikas.photon.tests.integration;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MySqlIntegrationTest
{
    private Photon photon;

    @Before
    public void setup()
    {
        String url = "jdbc:mysql://localhost:3306/PhotonTestDb";
        photon = new Photon(url, "root", "bears");

        photon
            .registerAggregate(PhotonTestTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            transaction.query(
                "DROP TABLE IF EXISTS PhotonTestTable"
            ).executeInsert();

            transaction.query(
                "CREATE TABLE PhotonTestTable( " +
                "`id` int NOT NULL AUTO_INCREMENT, " +
                "`uuidColumn` binary(16) NOT NULL, " +
                "`dateColumn` datetime NOT NULL, " +
                "`varcharColumn` varchar(50) NOT NULL, " +
                "PRIMARY KEY (`id`) " +
                ") "
            ).executeInsert();

            transaction.query("INSERT INTO PhotonTestTable VALUES (DEFAULT, UNHEX('8ED1E1BD253E4469B4CB71E1217825B7'), FROM_UNIXTIME(1489915698), 'Test String')").executeInsert();

            transaction.commit();
        }
    }

    @Test
    public void fetchExistingAggregateById_populatesValues()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            PhotonTestTable photonTestTable = transaction.query(PhotonTestTable.class).fetchById(1);

            assertNotNull(photonTestTable);
            assertEquals(1, photonTestTable.getId());
            assertEquals(UUID.fromString("8ED1E1BD-253E-4469-B4CB-71E1217825B7"), photonTestTable.getUuidColumn());
            assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1489915698000L), ZoneId.systemDefault()), photonTestTable.getDateColumn());
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
