package com.github.molcikas.photon.tests.integration;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.options.PhotonOptions;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PostgresIntegrationTest
{
    private Photon photon;

    @SneakyThrows
    @Before
    public void setup()
    {
        String url = "jdbc:postgresql://localhost:15432/";
        photon = new Photon(url, "postgres", "bears", PhotonOptions.postgresOptions().build());

        try(PhotonTransaction transaction = photon.beginAutoCommitTransaction())
        {
            transaction.executeUpdate("CREATE DATABASE photon_test_db");
        }
        catch(PhotonException ex)
        {
            if(ex.getCause() == null || !ex.getCause().getMessage().equals("ERROR: database \"photon_test_db\" already exists"))
            {
                throw ex;
            }
        }

        url = "jdbc:postgresql://localhost:15432/photon_test_db";
        photon = new Photon(url, "postgres", "bears", PhotonOptions.postgresOptions().build());

        photon
            .registerAggregate(PhotonTestTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            transaction
                .query(
                "DROP TABLE IF EXISTS \"PhotonTestTable\";\n" +
                    "CREATE TABLE \"PhotonTestTable\"\n" +
                    "(\n" +
                    "    id SERIAL PRIMARY KEY,\n" +
                    "    \"uuidColumn\" uuid NOT NULL,\n" +
                    "    \"dateColumn\" timestamp without time zone NOT NULL,\n" +
                    "    \"varcharColumn\" character varying(32) COLLATE pg_catalog.\"default\" NOT NULL\n" +
                    ")\n" +
                    "WITH (\n" +
                    "    OIDS = FALSE\n" +
                    ")\n" +
                    "TABLESPACE pg_default;"
                ).executeInsert();

            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            transaction
                .query("INSERT INTO \"PhotonTestTable\" (\"uuidColumn\", \"dateColumn\", \"varcharColumn\") VALUES ('8ED1E1BD-253E-4469-B4CB-71E1217825B7', to_timestamp(1489915698), 'Test String')")
                .executeInsert();

            transaction.commit();
        }
    }

    @Test
    public void fetchExistingAggregateById_populatesValues()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            PhotonTestTable photonTestTable = transaction.query(PhotonTestTable.class).fetchById(1);

            // Note: Unlike MySQL and SQL Server, Postgres defaults to UTC when given epoch times with to_timestamp(). No need to apply an offset here.

            assertNotNull(photonTestTable);
            assertEquals(1, photonTestTable.getId());
            assertEquals(UUID.fromString("8ED1E1BD-253E-4469-B4CB-71E1217825B7"), photonTestTable.getUuidColumn());
            assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochSecond(1489915698), ZoneId.systemDefault()), photonTestTable.getDateColumn());
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
