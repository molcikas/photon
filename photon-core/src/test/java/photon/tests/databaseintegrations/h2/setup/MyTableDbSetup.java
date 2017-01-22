package photon.tests.databaseintegrations.h2.setup;

import photon.Photon;
import photon.PhotonConnection;
import photon.tests.databaseintegrations.h2.H2TestUtil;

public class MyTableDbSetup
{
    public static Photon setupDatabase()
    {
        Photon photon = new Photon(H2TestUtil.getH2Url("MySQL"), H2TestUtil.h2User, H2TestUtil.h2Password);

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
            connection.query("insert into `mytable` (`id`, `myvalue`) values (5, 'my5dbvalue')").executeUpdate();

            connection.query("DROP TABLE IF EXISTS `myothertable`").executeUpdate();
            connection.query("CREATE TABLE `myothertable` (\n" +
                "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                "  `myothervalue` varchar(255) DEFAULT 'oops',\n" +
                "  PRIMARY KEY (`id`),\n" +
                "  CONSTRAINT `MyOtherTable_MyTable` FOREIGN KEY (`id`) REFERENCES `mytable` (`id`)\n" +
                ")").executeUpdate();
            connection.query("insert into `myothertable` (`id`, `myothervalue`) values (3, 'my3otherdbvalue')").executeUpdate();
            connection.query("insert into `myothertable` (`id`, `myothervalue`) values (4, 'my4otherdbvalue')").executeUpdate();
            connection.query("insert into `myothertable` (`id`, `myothervalue`) values (5, 'my5otherdbvalue')").executeUpdate();
        }

        return photon;
    }
}
