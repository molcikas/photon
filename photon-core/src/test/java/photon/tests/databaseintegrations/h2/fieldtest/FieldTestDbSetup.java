package photon.tests.databaseintegrations.h2.fieldtest;

import photon.Photon;
import photon.PhotonConnection;
import photon.tests.databaseintegrations.h2.H2TestUtil;
import photon.tests.entities.fieldtest.FieldTest;

import java.sql.Types;

public class FieldTestDbSetup
{
    public static Photon setupDatabase()
    {
        Photon photon = new Photon(H2TestUtil.h2Url, H2TestUtil.h2User, H2TestUtil.h2Password);

        try(PhotonConnection connection = photon.open())
        {
            connection.query("DROP TABLE IF EXISTS `fieldtest`").executeUpdate();
            connection.query("CREATE TABLE `fieldtest` (\n" +
                "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                "  `date` DATETIME,\n" +
                "  `zonedDateTime` DATETIME,\n" +
                "  `localDate` DATETIME,\n" +
                "  `localDateTime` DATETIME,\n" +
                "  `instant` DATETIME,\n" +
                "  `testEnumNumber` int(11),\n" +
                "  `testEnumString` VARCHAR(255),\n" +
                "  PRIMARY KEY (`id`)\n" +
                ")").executeUpdate();
            connection.query("insert into `fieldtest` (`id`, `date`, `zonedDateTime`, `localDate`, `localDateTime`, `instant`, `testEnumNumber`, `testEnumString`) " +
                "values (1, PARSEDATETIME('2017-03-19 09-28-17', 'yyyy-MM-dd HH-mm-ss'), PARSEDATETIME('2017-03-19 09-28-18', 'yyyy-MM-dd HH-mm-ss'), PARSEDATETIME('2017-03-19 09-28-19', 'yyyy-MM-dd HH-mm-ss'), PARSEDATETIME('2017-03-19 09-28-20', 'yyyy-MM-dd HH-mm-ss'), PARSEDATETIME('2017-03-19 09-28-21', 'yyyy-MM-dd HH-mm-ss'), 0, 'VALUE_ONE')").executeUpdate();
            connection.query("insert into `fieldtest` (`id`, `date`, `zonedDateTime`, `localDate`, `localDateTime`, `instant`, `testEnumNumber`, `testEnumString`) " +
                "values (2, NULL, NULL, NULL, NULL, NULL, NULL, NULL)").executeUpdate();
        }

        return photon;
    }

    public static void registerAggregate(Photon photon)
    {
        photon.registerAggregate(FieldTest.class)
            .withId("id")
            .withColumnDataType("testEnumString", Types.VARCHAR)
            .register();
    }
}
