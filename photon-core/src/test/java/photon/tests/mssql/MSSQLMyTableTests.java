package photon.tests.mssql;

import org.junit.Before;
import org.junit.Test;
import photon.Photon;
import photon.PhotonConnection;
import photon.tests.H2TestUtil;
import photon.tests.entities.mytable.MyTable;

import static org.junit.Assert.*;

public class MSSQLMyTableTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = new Photon(H2TestUtil.getH2Url("MSSQLServer"), H2TestUtil.h2User, H2TestUtil.h2Password);

        try(PhotonConnection connection = photon.open())
        {
            connection.query("DROP TABLE IF EXISTS `mytable`").executeUpdate();
            connection.query("CREATE TABLE `mytable` (\n" +
                "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                "  `myvalue` varchar(255) DEFAULT 'oops',\n" +
                "  PRIMARY KEY (`id`)\n" +
                ")").executeUpdate();
            connection.query("insert into `mytable` (`id`, `myvalue`) values (1, 'mydbvalue')").executeUpdate();
        }
    }

    @Test
    public void aggregateQuery_simple_returnsEntity()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .register();

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTable = connection
                .aggregateQuery(MyTable.class)
                .fetchById(1);

            assertNotNull(myTable);
            assertEquals(1, myTable.getId(), 1);
            assertEquals("mydbvalue", myTable.getMyvalue());
        }
    }

    @Test
    public void aggregateQuery_idNotInDatabase_returnsNull()
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
}
