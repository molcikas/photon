package photon.tests.mssql;

import org.junit.Before;
import org.junit.Test;
import photon.Photon;
import photon.PhotonConnection;
import photon.tests.H2TestUtil;
import photon.tests.base.MyTableTests;
import photon.tests.entities.mytable.MyTable;

import static org.junit.Assert.*;

public class MSSQLMyTableTests extends MyTableTests
{
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
            connection.query("insert into `mytable` (`id`, `myvalue`) values (1, 'my1dbvalue')").executeUpdate();
            connection.query("insert into `mytable` (`id`, `myvalue`) values (2, 'my2dbvalue')").executeUpdate();
            connection.query("insert into `mytable` (`id`, `myvalue`) values (3, 'my3dbvalue')").executeUpdate();
            connection.query("insert into `mytable` (`id`, `myvalue`) values (4, 'my4dbvalue')").executeUpdate();
        }
    }

    @Test
    public void aggregateQuery_fetchById_simple_returnsEntity()
    {
        super.aggregateQuery_fetchById_simple_returnsEntity();
    }

    @Test
    public void aggregateQuery_fetchById_idNotInDatabase_returnsNull()
    {
        super.aggregateQuery_fetchById_idNotInDatabase_returnsNull();
    }

    @Test
    public void aggregateQuery_fetchByIds_returnsAggregates()
    {
        super.aggregateQuery_fetchByIds_returnsAggregates();
    }
}
