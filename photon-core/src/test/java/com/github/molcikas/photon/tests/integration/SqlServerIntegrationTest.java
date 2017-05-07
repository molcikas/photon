package com.github.molcikas.photon.tests.integration;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.options.DefaultTableName;
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

public class SqlServerIntegrationTest
{
    private Photon photon;

    @Before
    public void setup()
    {
        String url = "jdbc:sqlserver://localhost\\SQLEXPRESS;databaseName=PhotonTestDb;integratedSecurity=true";
        PhotonOptions photonOptions = new PhotonOptions("[", "]", DefaultTableName.ClassName, false, null);
        photon = new Photon(url, null, null, photonOptions);

        photon
            .registerAggregate(PhotonTestTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            transaction.query(
                "IF OBJECT_ID('dbo.PhotonTestTable', 'U') IS NOT NULL\n" +
                "   DROP TABLE [dbo].[PhotonTestTable]"
            ).executeInsert();

            transaction.query(
                "CREATE TABLE [dbo].[PhotonTestTable](\n" +
                "\t[id] [int] IDENTITY(1,1) NOT NULL,\n" +
                "\t[uuidColumn] [uniqueidentifier] NOT NULL,\n" +
                "\t[dateColumn] [datetime] NOT NULL,\n" +
                "\t[varcharColumn] [varchar](50) NOT NULL,\n" +
                " CONSTRAINT [PK_PhotonTestTable] PRIMARY KEY CLUSTERED \n" +
                "(\n" +
                "\t[id] ASC\n" +
                ")WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]\n" +
                ") ON [PRIMARY]"
            ).executeInsert();

            transaction.query("INSERT INTO PhotonTestTable VALUES ('8ED1E1BD-253E-4469-B4CB-71E1217825B7', DATEADD(SECOND, 1489915698, '1970-01-01'), 'Test String')").executeInsert();

            transaction.commit();
        }
    }

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
