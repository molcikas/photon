package com.github.molcikas.photon.tests.unit.h2.product;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.tests.unit.h2.H2TestUtil;

public class ProductDbSetup
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
            transaction.query("DROP TABLE IF EXISTS `product`").executeUpdate();
            transaction.query("CREATE TABLE `product` (\n" +
                "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                "  `numerator` int(11) NOT NULL,\n" +
                "  `denominator` int(11) NOT NULL,\n" +
                "  PRIMARY KEY (`id`)\n" +
                ")").executeUpdate();
            transaction.query("insert into `product` (`id`, `numerator`, `denominator`) values (1, 2, 3)").executeUpdate();

            transaction.commit();
        }

        return photon;
    }
}
