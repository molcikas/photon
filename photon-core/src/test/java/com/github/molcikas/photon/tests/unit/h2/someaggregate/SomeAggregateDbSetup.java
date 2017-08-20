package com.github.molcikas.photon.tests.unit.h2.someaggregate;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.tests.unit.h2.H2TestUtil;

public class SomeAggregateDbSetup
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
            String sql = H2TestUtil.readResourceFile("setup-some-aggregate.sql");

            for(String statement : sql.split(";"))
            {
                transaction.query(statement.trim()).executeUpdate();
            }

            transaction.commit();
        }

        return photon;
    }
}
