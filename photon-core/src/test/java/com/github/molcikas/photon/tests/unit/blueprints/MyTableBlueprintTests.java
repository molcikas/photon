package com.github.molcikas.photon.tests.unit.blueprints;

import org.junit.Assert;
import org.junit.Test;
import com.github.molcikas.photon.datasource.GenericDataSource;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.tests.unit.entities.mytable.MyOtherTable;
import com.github.molcikas.photon.tests.unit.entities.mytable.MyTable;

import static org.junit.Assert.*;

public class MyTableBlueprintTests
{
    @Test
    public void registerEntity_childWithNoForeignKeyToParent_ThrowsException()
    {
        Photon photon = new Photon(new GenericDataSource("", "", ""));

        try
        {
            photon.registerAggregate(MyTable.class)
                .withId("id")
                .withChild("myOtherTable", MyOtherTable.class)
                    .withId("id")
                    .addAsChild()
                .register();

            Assert.fail("Failed to throw PhotonException.");
        }
        catch(PhotonException ex)
        {
            assertTrue(ex.getMessage().contains("foreign key"));
        }
    }

    // TODO: Aggregate root entity must have primary key.

    // TODO: Entity without a primary key field cannot have child entities.

    // TODO: Entity without a primary key field cannot have foreign key list field.

    // TODO: Cannot set withForeignKeyToParent to the primary key and also set primaryKeyAutoIncrement to true.
}
