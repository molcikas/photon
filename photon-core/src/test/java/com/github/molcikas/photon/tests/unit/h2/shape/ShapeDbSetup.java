package com.github.molcikas.photon.tests.unit.h2.shape;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.tests.unit.h2.H2TestUtil;

public class ShapeDbSetup
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
            transaction.query("DROP TABLE IF EXISTS `drawing`").executeUpdate();
            transaction.query("CREATE TABLE `drawing` (\n" +
                "`id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                "`name` varchar(255) NULL,\n" +
                "PRIMARY KEY (`id`)\n" +
                ")").executeUpdate();

            transaction.query("insert into `drawing` (`id`) values (1)").executeUpdate();

            transaction.query("DROP TABLE IF EXISTS `shape`").executeUpdate();
            transaction.query("CREATE TABLE `shape` (\n" +
                "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                "  `type` varchar(255) NOT NULL,\n" +
                "  `color` varchar(255) NOT NULL,\n" +
                "  `radius` int(11) NULL,\n" +
                "  `width` int(11) NULL,\n" +
                "  `height` int(11) NULL,\n" +
                "  `drawingId` int(11) DEFAULT NULL,\n" +
                "  CONSTRAINT `shape_drawing` FOREIGN KEY (`drawingId`) REFERENCES `drawing` (`id`),\n" +
                "  PRIMARY KEY (`id`)\n" +
                ")").executeUpdate();

            transaction.query("insert into `shape` (`id`, `type`, `color`, `radius`, `width`, `height`) values (1, 'circle', 'red', 3, NULL, NULL)").executeUpdate();
            transaction.query("insert into `shape` (`id`, `type`, `color`, `radius`, `width`, `height`) values (2, 'rectangle', 'blue', NULL, 7, 8)").executeUpdate();

            transaction.query("DROP TABLE IF EXISTS `cornercoordinates`").executeUpdate();
            transaction.query("CREATE TABLE `cornercoordinates` (\n" +
                "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                "  `shapeId` int(11) NOT NULL,\n" +
                "  `x` int(11) NULL,\n" +
                "  `y` int(11) NULL,\n" +
                "  PRIMARY KEY (`id`),\n" +
                "  CONSTRAINT `CornerCoordinates_Shape` FOREIGN KEY (`shapeId`) REFERENCES `shape` (`id`)\n" +
                ")").executeUpdate();

            transaction.query("insert into `cornercoordinates` (`id`, `shapeId`, `x`, `y`) values (1, 2, 0, 0)").executeUpdate();
            transaction.query("insert into `cornercoordinates` (`id`, `shapeId`, `x`, `y`) values (2, 2, 7, 0)").executeUpdate();
            transaction.query("insert into `cornercoordinates` (`id`, `shapeId`, `x`, `y`) values (3, 2, 0, 8)").executeUpdate();
            transaction.query("insert into `cornercoordinates` (`id`, `shapeId`, `x`, `y`) values (4, 2, 7, 8)").executeUpdate();

            transaction.commit();
        }

        return photon;
    }
}
