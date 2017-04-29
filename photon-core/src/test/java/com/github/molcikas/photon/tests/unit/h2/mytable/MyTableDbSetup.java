package com.github.molcikas.photon.tests.unit.h2.mytable;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.tests.unit.h2.H2TestUtil;

public class MyTableDbSetup
{
    public static Photon setupDatabase()
    {
        Photon photon = new Photon(H2TestUtil.h2Url, H2TestUtil.h2User, H2TestUtil.h2Password);
        return setupDatabase(photon);
    }

    public static Photon setupDatabase(Photon photon)
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            transaction.query("DROP TABLE IF EXISTS `mytable`").executeUpdate();
            transaction.query("CREATE TABLE `mytable` (\n" +
                "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                "  `myvalue` varchar(255) DEFAULT 'oops',\n" +
                "  PRIMARY KEY (`id`)\n" +
                ")").executeUpdate();
            transaction.query("insert into `mytable` (`id`, `myvalue`) values (1, 'my1dbvalue')").executeUpdate();
            transaction.query("insert into `mytable` (`id`, `myvalue`) values (2, 'my2dbvalue')").executeUpdate();
            transaction.query("insert into `mytable` (`id`, `myvalue`) values (3, 'my3dbvalue')").executeUpdate();
            transaction.query("insert into `mytable` (`id`, `myvalue`) values (4, 'my4dbvalue')").executeUpdate();
            transaction.query("insert into `mytable` (`id`, `myvalue`) values (5, 'my5dbvalue')").executeUpdate();
            transaction.query("insert into `mytable` (`id`, `myvalue`) values (6, NULL)").executeUpdate();

            transaction.query("DROP TABLE IF EXISTS `myothertable`").executeUpdate();
            transaction.query("CREATE TABLE `myothertable` (\n" +
                "  `id` int(11) NOT NULL,\n" +
                "  `myothervalue` varchar(255) DEFAULT 'oops',\n" +
                "  PRIMARY KEY (`id`),\n" +
                "  CONSTRAINT `MyOtherTable_MyTable` FOREIGN KEY (`id`) REFERENCES `mytable` (`id`)\n" +
                ")").executeUpdate();
            transaction.query("insert into `myothertable` (`id`, `myothervalue`) values (3, 'my3otherdbvalue')").executeUpdate();
            transaction.query("insert into `myothertable` (`id`, `myothervalue`) values (4, 'my4otherdbvalue')").executeUpdate();
            transaction.query("insert into `myothertable` (`id`, `myothervalue`) values (5, 'my5otherdbvalue')").executeUpdate();

            transaction.commit();
        }

        return photon;
    }
}
