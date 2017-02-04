package photon.tests.databaseintegrations.h2;

import org.junit.Before;
import org.junit.Test;
import photon.Photon;
import photon.PhotonConnection;
import photon.tests.databaseintegrations.h2.setup.MyOneToManyTableDbSetup;
import photon.tests.entities.myonetomanytable.MyManyTable;
import photon.tests.entities.myonetomanytable.MyOneToManyTable;

import java.sql.Types;
import java.util.Arrays;

import static org.junit.Assert.*;

public class MyOneToManyTableSaveTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = MyOneToManyTableDbSetup.setupDatabase();
    }

    @Test
    public void aggregate_save_insertWithMultipleChildren_savesEntity()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonConnection connection = photon.open())
        {
            MyOneToManyTable myOneToManyTable = new MyOneToManyTable(
                null,
                "MyOneToManyTableValue",
                Arrays.asList(
                    new MyManyTable(0, "My1ManyValue"),
                    new MyManyTable(0, "My2ManyValue"),
                    new MyManyTable(null, "My3ManyValue"),
                    new MyManyTable(null, "My4ManyValue")
                )
            );

            connection
                .save(myOneToManyTable);
        }

        try(PhotonConnection connection = photon.open())
        {
            MyOneToManyTable myOneToManyTableRetrieved = connection
                .query(MyOneToManyTable.class)
                .fetchById(6);

            assertNotNull(myOneToManyTableRetrieved);
            assertEquals(Integer.valueOf(6), myOneToManyTableRetrieved.getId());
            assertEquals("MyOneToManyTableValue", myOneToManyTableRetrieved.getMyvalue());
            assertEquals(4, myOneToManyTableRetrieved.getMyManyTables().size());

//            MyOtherTable myOtherTableRetrieved = myTableRetrieved.getMyOtherTable();
//            assertNotNull(myOtherTableRetrieved);
//            assertEquals(6, myOtherTableRetrieved.getId());
//            assertEquals("MyOtherSavedValueAutoInc", myOtherTableRetrieved.getMyOtherValueWithDiffName());
        }
    }

    private void registerMyOneToManyTableAggregate()
    {
        photon.registerAggregate(MyOneToManyTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .withChild(MyManyTable.class)
                .withId("id")
                .withColumnDataType("id", Types.INTEGER)
                .withPrimaryKeyAutoIncrement()
                .withForeignKeyToParent("parent")
                .withFieldToColmnnMapping("myOtherValueWithDiffName", "myothervalue")
                .addAsChild("myManyTables")
            .register();
    }
}
