package com.github.molcikas.photon.tests.integration.sqlserver.entities;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
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
        photon = new Photon(url, null, null);

        photon
            .registerAggregate(MySqlServerTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            transaction.query(
                "IF OBJECT_ID('dbo.MySqlServerTable', 'U') IS NOT NULL\n" +
                "   DROP TABLE [dbo].[MySqlServerTable]"
            ).executeInsert();

            transaction.query(
                "CREATE TABLE [dbo].[MySqlServerTable](\n" +
                "\t[id] [int] IDENTITY(1,1) NOT NULL,\n" +
                "\t[uuidColumn] [uniqueidentifier] NOT NULL,\n" +
                "\t[dateColumn] [datetime] NOT NULL,\n" +
                "\t[varcharColumn] [varchar](50) NOT NULL,\n" +
                " CONSTRAINT [PK_MySqlServerTable] PRIMARY KEY CLUSTERED \n" +
                "(\n" +
                "\t[id] ASC\n" +
                ")WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]\n" +
                ") ON [PRIMARY]"
            ).executeInsert();

            transaction.query("INSERT INTO MySqlServerTable VALUES ('8ED1E1BD-253E-4469-B4CB-71E1217825B7', DATEADD(SECOND, 1489915698, '1970-01-01'), 'Test String')").executeInsert();

            transaction.commit();
        }
    }

    @Test
    public void fetchExistingAggregateById_populatesValues()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MySqlServerTable mySqlServerTable = transaction.query(MySqlServerTable.class).fetchById(1);

            // The database does not store a time zone, so we assume the date is in the system's time zone. But to make these tests
            // compare epoch times but still work with any system time zone, we have to offset the epoch to the system's time zone.
            int currentUtcOffset = TimeZone.getDefault().getOffset(new Date().getTime());

            assertNotNull(mySqlServerTable);
            assertEquals(1, mySqlServerTable.getId());
            assertEquals(UUID.fromString("8ED1E1BD-253E-4469-B4CB-71E1217825B7"), mySqlServerTable.getUuidColumn());
            assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1489915698000L - currentUtcOffset), ZoneId.systemDefault()), mySqlServerTable.getDateColumn());
            assertEquals("Test String", mySqlServerTable.getVarcharColumn());
        }
    }

    @Test
    public void insertAggregateAndFetch_insertsAggregateAndPopulatesValues()
    {
        MySqlServerTable mySqlServerTable = new MySqlServerTable(2, UUID.randomUUID(), ZonedDateTime.now(), "My Test String");

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            transaction.insert(mySqlServerTable);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MySqlServerTable mySqlServerTableFetched = transaction.query(MySqlServerTable.class).fetchById(2);

            assertEquals(mySqlServerTable, mySqlServerTableFetched);
        }
    }
}
