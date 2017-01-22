package photon.tests.base;

import org.junit.Before;
import org.junit.Test;
import photon.Photon;
import photon.PhotonConnection;
import photon.tests.H2TestUtil;
import photon.tests.entities.mytable.MyTable;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class MyTableTests
{
    protected Photon photon;

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

    public void aggregateQuery_fetchByIds_returnsAggregates()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .register();

        try(PhotonConnection connection = photon.open())
        {
            List<MyTable> myTables = connection
                .aggregateQuery(MyTable.class)
                .fetchByIds(Arrays.asList(2, 4));

            assertNotNull(myTables);
            assertEquals(2, myTables.size());
            assertEquals(2, myTables.get(0).getId());
            assertEquals("my2dbvalue", myTables.get(0).getMyvalue());
            assertEquals(4, myTables.get(1).getId());
            assertEquals("my4dbvalue", myTables.get(1).getMyvalue());
        }
    }
}
