package photon.tests.databaseintegrations.h2.twoaggregates;

import photon.Photon;
import photon.PhotonConnection;
import photon.tests.databaseintegrations.h2.H2TestUtil;

public class TwoAggregatesDbSetup
{
    public static Photon setupDatabase()
    {
        Photon photon = new Photon(H2TestUtil.h2Url, H2TestUtil.h2User, H2TestUtil.h2Password);

        try(PhotonConnection connection = photon.open())
        {
            connection.query("DROP TABLE IF EXISTS `aggregateone`").executeUpdate();
            connection.query("CREATE TABLE `aggregateone` (\n" +
                "  `aggregateOneId` binary(16) NOT NULL,\n" +
                "  `myValue` varchar(255) DEFAULT 'oops',\n" +
                "  PRIMARY KEY (`aggregateOneId`)\n" +
                ")").executeUpdate();
            connection.query("insert into `aggregateone` (`aggregateOneId`, `myValue`) values (X'3DFFC3B3A9B611E6AB830A0027000010', 'agg1val0')").executeUpdate();
            connection.query("insert into `aggregateone` (`aggregateOneId`, `myValue`) values (X'3DFFC3B3A9B611E6AB830A0027000011', 'agg1val1')").executeUpdate();
            connection.query("insert into `aggregateone` (`aggregateOneId`, `myValue`) values (X'3DFFC3B3A9B611E6AB830A0027000012', 'agg1val2')").executeUpdate();

            connection.query("DROP TABLE IF EXISTS `aggregatetwo`").executeUpdate();
            connection.query("CREATE TABLE `aggregatetwo` (\n" +
                "  `aggregateOneId` binary(16) NOT NULL,\n" +
                "  `aggregateTwoId` binary(16) NOT NULL,\n" +
                "  `myValue` varchar(255) DEFAULT 'oops',\n" +
                "  PRIMARY KEY (`aggregateTwoId`)\n" +
                ")").executeUpdate();
            connection.query("insert into `aggregatetwo` (`aggregateTwoId`, `myValue`) values (X'3DFFC3B3A9B611E6AB830A0027000020', 'agg2val0')").executeUpdate();
            connection.query("insert into `aggregatetwo` (`aggregateTwoId`, `myValue`) values (X'3DFFC3B3A9B611E6AB830A0027000021', 'agg2val1')").executeUpdate();
            connection.query("insert into `aggregatetwo` (`aggregateTwoId`, `myValue`) values (X'3DFFC3B3A9B611E6AB830A0027000022', 'agg2val2')").executeUpdate();

            connection.query("DROP TABLE IF EXISTS `aggregatemapping`").executeUpdate();
            connection.query("CREATE TABLE `aggregatemapping` (\n" +
                "  `aggregateOneId` binary(16) NOT NULL,\n" +
                "  `aggregateTwoId` binary(16) NOT NULL,\n" +
                "  PRIMARY KEY (`aggregateOneId`, `aggregateTwoId`),\n" +
                "  CONSTRAINT `AggregateMapping_AggregateOne` FOREIGN KEY (`aggregateOneId`) REFERENCES `aggregateone` (`aggregateOneId`),\n" +
                "  CONSTRAINT `AggregateMapping_AggregateTwo` FOREIGN KEY (`aggregateTwoId`) REFERENCES `aggregatetwo` (`aggregateTwoId`)\n" +
                ")").executeUpdate();
            connection.query("insert into `aggregatemapping` (`aggregateOneId`, `aggregateTwoId`) values (X'3DFFC3B3A9B611E6AB830A0027000011', X'3DFFC3B3A9B611E6AB830A0027000021')").executeUpdate();
            connection.query("insert into `aggregatemapping` (`aggregateOneId`, `aggregateTwoId`) values (X'3DFFC3B3A9B611E6AB830A0027000012', X'3DFFC3B3A9B611E6AB830A0027000020')").executeUpdate();
            connection.query("insert into `aggregatemapping` (`aggregateOneId`, `aggregateTwoId`) values (X'3DFFC3B3A9B611E6AB830A0027000012', X'3DFFC3B3A9B611E6AB830A0027000021')").executeUpdate();
            connection.query("insert into `aggregatemapping` (`aggregateOneId`, `aggregateTwoId`) values (X'3DFFC3B3A9B611E6AB830A0027000012', X'3DFFC3B3A9B611E6AB830A0027000022')").executeUpdate();
        }

        return photon;
    }
}
