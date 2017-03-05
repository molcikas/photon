package photon.tests.databaseintegrations.h2.myonetomanytable;

import photon.Photon;
import photon.PhotonConnection;
import photon.tests.databaseintegrations.h2.H2TestUtil;

public class MyOneToManyTableDbSetup
{
    public static Photon setupDatabase()
    {
        Photon photon = new Photon(H2TestUtil.h2Url, H2TestUtil.h2User, H2TestUtil.h2Password);

        try(PhotonConnection connection = photon.open())
        {
            connection.query("DROP TABLE IF EXISTS `myonetomanytable`").executeUpdate();
            connection.query("CREATE TABLE `myonetomanytable` (\n" +
                "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                "  `myvalue` varchar(255) DEFAULT 'oops',\n" +
                "  PRIMARY KEY (`id`)\n" +
                ")").executeUpdate();
            connection.query("insert into `myonetomanytable` (`id`, `myvalue`) values (1, 'my1dbvalue')").executeUpdate();
            connection.query("insert into `myonetomanytable` (`id`, `myvalue`) values (2, 'my2dbvalue')").executeUpdate();
            connection.query("insert into `myonetomanytable` (`id`, `myvalue`) values (3, 'my3dbvalue')").executeUpdate();
            connection.query("insert into `myonetomanytable` (`id`, `myvalue`) values (4, 'my4dbvalue')").executeUpdate();
            connection.query("insert into `myonetomanytable` (`id`, `myvalue`) values (5, 'my5dbvalue')").executeUpdate();
            connection.query("insert into `myonetomanytable` (`id`, `myvalue`) values (6, 'my6dbvalue')").executeUpdate();

            connection.query("DROP TABLE IF EXISTS `mymanytable`").executeUpdate();
            connection.query("CREATE TABLE `mymanytable` (\n" +
                "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                "  `parent` int(11) NOT NULL,\n" +
                "  `myothervalue` varchar(255) DEFAULT 'oops',\n" +
                "  PRIMARY KEY (`id`),\n" +
                "  CONSTRAINT `MyOneToManyTable_MyManyTable` FOREIGN KEY (`parent`) REFERENCES `myonetomanytable` (`id`)\n" +
                ")").executeUpdate();
            connection.query("insert into `mymanytable` (`id`, `parent`, `myothervalue`) values (1, 3, 'my31otherdbvalue')").executeUpdate();
            connection.query("insert into `mymanytable` (`id`, `parent`, `myothervalue`) values (2, 4, 'my41otherdbvalue')").executeUpdate();
            connection.query("insert into `mymanytable` (`id`, `parent`, `myothervalue`) values (3, 4, 'my42otherdbvalue')").executeUpdate();
            connection.query("insert into `mymanytable` (`id`, `parent`, `myothervalue`) values (4, 5, 'my51otherdbvalue')").executeUpdate();
            connection.query("insert into `mymanytable` (`id`, `parent`, `myothervalue`) values (5, 5, 'my52otherdbvalue')").executeUpdate();
            connection.query("insert into `mymanytable` (`id`, `parent`, `myothervalue`) values (6, 5, 'my53otherdbvalue')").executeUpdate();
            connection.query("insert into `mymanytable` (`id`, `parent`, `myothervalue`) values (7, 6, 'my62otherdbvalue')").executeUpdate();
            connection.query("insert into `mymanytable` (`id`, `parent`, `myothervalue`) values (8, 6, 'my62otherdbvalue')").executeUpdate();
            connection.query("insert into `mymanytable` (`id`, `parent`, `myothervalue`) values (9, 6, 'my63otherdbvalue')").executeUpdate();

            connection.query("DROP TABLE IF EXISTS `mythirdtable`").executeUpdate();
            connection.query("CREATE TABLE `mythirdtable` (\n" +
                "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                "  `parent` int(11) NOT NULL,\n" +
                "  `val` varchar(255) DEFAULT 'oops',\n" +
                "  PRIMARY KEY (`id`),\n" +
                "  CONSTRAINT `MyManyTable_MyThirdTable` FOREIGN KEY (`parent`) REFERENCES `mymanytable` (`id`)\n" +
                ")").executeUpdate();
            connection.query("insert into `mythirdtable` (`id`, `parent`, `val`) values (1, 7, 'thirdtableval1')").executeUpdate();
            connection.query("insert into `mythirdtable` (`id`, `parent`, `val`) values (2, 8, 'thirdtableval2')").executeUpdate();
            connection.query("insert into `mythirdtable` (`id`, `parent`, `val`) values (3, 9, 'thirdtableval3')").executeUpdate();
            connection.query("insert into `mythirdtable` (`id`, `parent`, `val`) values (4, 9, 'thirdtableval4')").executeUpdate();
        }

        return photon;
    }
}
