package com.github.molcikas.photon.tests.unit.h2.someaggregate;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.blueprints.ColumnDataType;
import com.github.molcikas.photon.tests.unit.entities.someaggregate.SomeAggregate;
import com.github.molcikas.photon.tests.unit.entities.someaggregate.SomeClass;
import org.junit.Before;
import org.junit.Test;

public class SomeAggregateTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = SomeAggregateDbSetup.setupDatabase();
    }

    @Test
    public void update_entityWithOnlyId_doesNothing()
    {
        registerAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            SomeAggregate someAggregate = transaction
                .query(SomeAggregate.class)
                .fetchById(1);

            transaction.save(someAggregate);
        }
    }

    private void registerAggregate()
    {
        photon
            .registerAggregate(SomeAggregate.class)
            .withChild("fieldOne", SomeClass.class)
                .withForeignKeyToParent("someAggregateId", ColumnDataType.INTEGER)
                .addAsChild()
            .register();
    }
}
