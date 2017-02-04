package photon.tests.databaseintegrations.h2;

import org.junit.Before;
import org.junit.Test;
import photon.Photon;
import photon.PhotonConnection;
import photon.tests.databaseintegrations.h2.setup.MyOneToManyTableDbSetup;
import photon.tests.entities.myonetomanytable.MyManyTable;
import photon.tests.entities.myonetomanytable.MyOneToManyTable;

import java.sql.Types;

import static org.junit.Assert.*;

public class MyOneToManyTableFetchTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = MyOneToManyTableDbSetup.setupDatabase();
    }

    @Test
    public void aggregate_fetchById_oneToOneWithMatch_returnsChild()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonConnection connection = photon.open())
        {
            MyOneToManyTable myOneToManyTable = connection
                .query(MyOneToManyTable.class)
                .fetchById(5);

            assertNotNull(myOneToManyTable);
            assertEquals(Integer.valueOf(5), myOneToManyTable.getId());
            assertEquals("my5dbvalue", myOneToManyTable.getMyvalue());
            assertEquals(3, myOneToManyTable.getMyManyTables().size());

            MyManyTable myManyTable = myOneToManyTable.getMyManyTables().get(0);
            assertNotNull(myManyTable);
            assertEquals(Integer.valueOf(5), myManyTable.getParent());
            assertEquals("my51otherdbvalue", myManyTable.getMyOtherValueWithDiffName());
        }
    }

    private void registerMyOneToManyTableAggregate()
    {
        photon.registerAggregate(MyOneToManyTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .withChild(MyManyTable.class)
                .withId("id")
                .withColumnDataType("id", Types.INTEGER)
                .withPrimaryKeyAutoIncrement()
                .withForeignKeyToParent("parent")
                .withFieldToColmnnMapping("myOtherValueWithDiffName", "myothervalue")
                .addAsChild("myManyTables")
            .register();
    }
}
