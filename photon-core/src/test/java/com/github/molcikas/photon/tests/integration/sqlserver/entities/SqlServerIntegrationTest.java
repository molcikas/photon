package com.github.molcikas.photon.tests.integration.sqlserver.entities;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class SqlServerIntegrationTest
{
    private Photon photon;

    @Before
    public void setup()
    {
        String url = "jdbc:sqlserver://localhost:1433";
        photon = new Photon(url, null, null);

        photon
            .registerAggregate(MySqlServerTable.class)
            .withId("id")
            .register();
    }

    @Ignore // TODO: Implement
    @Test
    public void fetchExistingAggregateById_populatesValues()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MySqlServerTable mySqlServerTable = transaction.query(MySqlServerTable.class).fetchById(1);
        }
    }
}
