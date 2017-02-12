package photon.tests.databaseintegrations.h2.myonetomanytable;

import org.junit.Before;
import org.junit.Test;
import photon.Photon;
import photon.PhotonConnection;
import photon.tests.entities.myonetomanytable.MyManyTable;
import photon.tests.entities.myonetomanytable.MyOneToManyTable;
import photon.tests.entities.myonetomanytable.MyThirdTable;

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
                    new MyManyTable(0, "My1ManyValue", null),
                    new MyManyTable(0, "My2ManyValue", null),
                    new MyManyTable(null, "My3ManyValue", Arrays.asList(
                        new MyThirdTable(null, "MyThirdTableVal1"),
                        new MyThirdTable(null, "MyThirdTableVal2")
                    )),
                    new MyManyTable(null, "My4ManyValue", null)
                )
            );

            connection
                .save(myOneToManyTable);
        }

        try(PhotonConnection connection = photon.open())
        {
            MyOneToManyTable myOneToManyTable = connection
                .query(MyOneToManyTable.class)
                .fetchById(7);

            assertNotNull(myOneToManyTable);
            assertEquals(Integer.valueOf(7), myOneToManyTable.getId());
            assertEquals("MyOneToManyTableValue", myOneToManyTable.getMyvalue());
            assertEquals(4, myOneToManyTable.getMyManyTables().size());

            MyManyTable myManyTable = myOneToManyTable.getMyManyTables().get(2);
            assertEquals(Integer.valueOf(12), myManyTable.getId());
            assertEquals(Integer.valueOf(7), myManyTable.getParent());
            assertEquals("My3ManyValue", myManyTable.getMyOtherValueWithDiffName());
            assertEquals(2, myManyTable.getMyThirdTables().size());

            MyThirdTable myThirdTable = myManyTable.getMyThirdTables().get(1);
            assertEquals(Integer.valueOf(6), myThirdTable.getId());
            assertEquals(Integer.valueOf(12), myThirdTable.getParent());
            assertEquals("MyThirdTableVal2", myThirdTable.getVal());
        }
    }

    @Test
    public void aggregate_save_deleteEntityWithChildren_savesAggregate()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonConnection connection = photon.open())
        {
            MyOneToManyTable myOneToManyTable = connection
                .query(MyOneToManyTable.class)
                .fetchById(6);

            myOneToManyTable.getMyManyTables().remove(1);

            connection.save(myOneToManyTable);
        }

        try(PhotonConnection connection = photon.open())
        {
            MyOneToManyTable myOneToManyTable = connection
                .query(MyOneToManyTable.class)
                .fetchById(6);

            assertNotNull(myOneToManyTable);
            assertEquals(Integer.valueOf(6), myOneToManyTable.getId());
            assertEquals("my6dbvalue", myOneToManyTable.getMyvalue());
            assertEquals(2, myOneToManyTable.getMyManyTables().size());

            MyManyTable myManyTable = myOneToManyTable.getMyManyTables().get(1);
            assertEquals(Integer.valueOf(9), myManyTable.getId());
            assertEquals(Integer.valueOf(6), myManyTable.getParent());
            assertEquals("my63otherdbvalue", myManyTable.getMyOtherValueWithDiffName());
            assertEquals(2, myManyTable.getMyThirdTables().size());

            MyThirdTable myThirdTable = myManyTable.getMyThirdTables().get(0);
            assertEquals(Integer.valueOf(3), myThirdTable.getId());
            assertEquals(Integer.valueOf(9), myThirdTable.getParent());
            assertEquals("thirdtableval3", myThirdTable.getVal());
        }
    }

    @Test
    public void aggregate_delete_deleteThreeLevelAggregate_deletesAggregate()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonConnection connection = photon.open())
        {
            MyOneToManyTable myOneToManyTable = connection
                .query(MyOneToManyTable.class)
                .fetchById(6);

            connection.delete(myOneToManyTable);
        }

        try(PhotonConnection connection = photon.open())
        {
            MyOneToManyTable myOneToManyTable = connection
                .query(MyOneToManyTable.class)
                .fetchById(6);

            assertNull(myOneToManyTable);
        }
    }

    private void registerMyOneToManyTableAggregate()
    {
        photon.registerAggregate(MyOneToManyTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .withChild(MyManyTable.class)
                .withId("id")
                .withPrimaryKeyAutoIncrement()
                .withForeignKeyToParent("parent")
                .withFieldToColumnMapping("myOtherValueWithDiffName", "myothervalue")
                .withChild(MyThirdTable.class)
                    .withId("id")
                    .withPrimaryKeyAutoIncrement()
                    .withForeignKeyToParent("parent")
                .addAsChild("myThirdTables")
            .addAsChild("myManyTables")
            .register();
    }
}
