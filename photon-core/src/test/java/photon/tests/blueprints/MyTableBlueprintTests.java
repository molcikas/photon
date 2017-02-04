package photon.tests.blueprints;

import org.junit.Assert;
import org.junit.Test;
import photon.GenericDataSource;
import photon.Photon;
import photon.exceptions.PhotonException;
import photon.tests.entities.mytable.MyOtherTable;
import photon.tests.entities.mytable.MyTable;

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
                .withChild(MyOtherTable.class)
                    .withId("id")
                    .addAsChild("myOtherTable")
                .register();

            Assert.fail("Failed to throw PhotonException.");
        }
        catch(PhotonException ex)
        {
            assertTrue(ex.getMessage().contains("foreign key"));
        }
    }

    // TODO: Entity without a primary key field cannot have child entities.

    // TODO: Cannot set withForeignKeyToParent to the primary key and also set primaryKeyAutoIncrement to true.
}
