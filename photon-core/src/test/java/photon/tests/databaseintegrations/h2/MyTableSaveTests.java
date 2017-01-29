package photon.tests.databaseintegrations.h2;

import org.junit.Before;
import org.junit.Test;
import photon.Photon;
import photon.PhotonConnection;
import photon.tests.databaseintegrations.h2.setup.MyTableDbSetup;
import photon.tests.entities.mytable.MyOtherTable;
import photon.tests.entities.mytable.MyTable;

import static org.junit.Assert.*;

public class MyTableSaveTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = MyTableDbSetup.setupDatabase();
    }

    @Test
    public void aggregate_save_simpleEntity_savesEntity()
    {
        registerMyTableOnlyAggregate();

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTable = new MyTable(2, "MySavedValue", null);
            connection
                .aggregate(MyTable.class)
                .save(myTable);
        }

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTableRetrieved = connection
                .aggregate(MyTable.class)
                .fetchById(2);

            assertNotNull(myTableRetrieved);
            assertEquals(2, myTableRetrieved.getId());
            assertEquals("MySavedValue", myTableRetrieved.getMyvalue());
        }
    }

    @Test
    public void aggregate_save_insertSimpleEntity_savesEntity()
    {
        registerMyTableOnlyAggregate();

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTable = new MyTable(1111, "MyInsertedSavedValue", null);
            connection
                .aggregate(MyTable.class)
                .save(myTable);
        }

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTableRetrieved = connection
                .aggregate(MyTable.class)
                .fetchById(1111);

            assertNotNull(myTableRetrieved);
            assertEquals(1111, myTableRetrieved.getId());
            assertEquals("MyInsertedSavedValue", myTableRetrieved.getMyvalue());
        }
    }

    @Test
    public void aggregate_save_updateEntityWithChild_savesEntity()
    {
        registerMyTableAggregate();

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTable = new MyTable(3, "MySavedValue", new MyOtherTable(3, "MyOtherSavedValue"));

            connection
                .aggregate(MyTable.class)
                .save(myTable);
        }

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTableRetrieved = connection
                .aggregate(MyTable.class)
                .fetchById(3);

            assertNotNull(myTableRetrieved);
            assertEquals(3, myTableRetrieved.getId());
            assertEquals("MySavedValue", myTableRetrieved.getMyvalue());

            MyOtherTable myOtherTableRetrieved = myTableRetrieved.getMyOtherTable();
            assertNotNull(myOtherTableRetrieved);
            assertEquals(3, myOtherTableRetrieved.getId());
            assertEquals("MyOtherSavedValue", myOtherTableRetrieved.getMyOtherValueWithDiffName());
        }
    }

    private void registerMyTableOnlyAggregate()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .register();
    }

    private void registerMyTableAggregate()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .withChild(MyOtherTable.class)
                .withId("id")
                .withForeignKeyToParent("id")
                .withFieldToColmnnMapping("myOtherValueWithDiffName", "myothervalue")
                .addAsChild("myOtherTable")
            .register();
    }
}
