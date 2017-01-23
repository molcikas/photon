package photon.tests.databaseintegrations.h2;

import org.junit.Before;
import org.junit.Test;
import photon.Photon;
import photon.PhotonConnection;
import photon.blueprints.SortDirection;
import photon.tests.databaseintegrations.h2.setup.MyTableDbSetup;
import photon.tests.entities.mytable.MyOtherTable;
import photon.tests.entities.mytable.MyTable;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class MyTableTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = MyTableDbSetup.setupDatabase();
    }

    @Test
    public void aggregateQuery_fetchById_simple_returnsEntity()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .register();

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTable = connection
                .aggregateQuery(MyTable.class)
                .fetchById(2);

            assertNotNull(myTable);
            assertEquals(2, myTable.getId());
            assertEquals("my2dbvalue", myTable.getMyvalue());
        }
    }

    @Test
    public void aggregateQuery_fetchById_idNotInDatabase_returnsNull()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .register();

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTable = connection
                .aggregateQuery(MyTable.class)
                .fetchById(7);

            assertNull(myTable);
        }
    }

    @Test
    public void aggregateQuery_fetchByIdsAndSortRootDescending_returnsAggregates()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .withOrderBy("id", SortDirection.Descending)
            .register();

        try(PhotonConnection connection = photon.open())
        {
            List<MyTable> myTables = connection
                .aggregateQuery(MyTable.class)
                .fetchByIds(Arrays.asList(2, 4));

            assertNotNull(myTables);
            assertEquals(2, myTables.size());
            assertEquals(4, myTables.get(0).getId());
            assertEquals("my4dbvalue", myTables.get(0).getMyvalue());
            assertEquals(2, myTables.get(1).getId());
            assertEquals("my2dbvalue", myTables.get(1).getMyvalue());
        }
    }

    @Test
    public void aggregateQuery_fetchById_oneToOneWithNoMatch_returnsNullChild()
    {
        registerMyTableAggregate();

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTable = connection
                .aggregateQuery(MyTable.class)
                .fetchById(2);

            assertNotNull(myTable);
            assertEquals(2, myTable.getId());
            assertEquals("my2dbvalue", myTable.getMyvalue());

            assertNull(myTable.getMyOtherTable());
        }
    }

    @Test
    public void aggregateQuery_fetchById_oneToOneWithMatch_returnsChild()
    {
        registerMyTableAggregate();

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTable = connection
                .aggregateQuery(MyTable.class)
                .fetchById(5);

            assertNotNull(myTable);
            assertEquals(5, myTable.getId());
            assertEquals("my5dbvalue", myTable.getMyvalue());

            MyOtherTable myOtherTable = myTable.getMyOtherTable();
            assertNotNull(myOtherTable);
            assertEquals(5, myOtherTable.getId());
            assertEquals("my5otherdbvalue", myOtherTable.getMyOtherValueWithDiffName());
        }
    }

    @Test
    public void aggregateQuery_fetchByIds_oneToOnesWithMatches_returnsChild()
    {
        registerMyTableAggregate();

        try(PhotonConnection connection = photon.open())
        {
            List<MyTable> myTables = connection
                .aggregateQuery(MyTable.class)
                .fetchByIds(Arrays.asList(1, 2, 3, 4));

            assertNotNull(myTables);
            assertEquals(4, myTables.size());

            assertEquals(1, myTables.get(0).getId());
            assertNull(myTables.get(0).getMyOtherTable());

            assertEquals(2, myTables.get(1).getId());
            assertNull(myTables.get(1).getMyOtherTable());

            assertEquals(3, myTables.get(2).getId());
            assertEquals(3, myTables.get(2).getMyOtherTable().getId());

            assertEquals(4, myTables.get(3).getId());
            assertEquals(4, myTables.get(3).getMyOtherTable().getId());
        }
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
