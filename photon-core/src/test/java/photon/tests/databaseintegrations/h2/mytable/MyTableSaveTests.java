package photon.tests.databaseintegrations.h2.mytable;

import org.junit.Before;
import org.junit.Test;
import photon.Photon;
import photon.PhotonConnection;
import photon.converters.Converter;
import photon.converters.ConverterException;
import photon.tests.entities.mytable.MyOtherTable;
import photon.tests.entities.mytable.MyTable;

import static org.junit.Assert.*;

public class MyTableSaveTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = MyTableDbSetup.setupDatabase();
    }

    @Test
    public void aggregate_save_simpleEntity_savesEntity()
    {
        registerMyTableOnlyAggregate();

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTable = new MyTable(2, "MySavedValue", null);
            connection
                .save(myTable);
        }

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTableRetrieved = connection
                .query(MyTable.class)
                .fetchById(2);

            assertNotNull(myTableRetrieved);
            assertEquals(2, myTableRetrieved.getId());
            assertEquals("MySavedValue", myTableRetrieved.getMyvalue());
        }
    }

    @Test
    public void aggregate_save_insertSimpleEntity_savesEntity()
    {
        registerMyTableOnlyAggregate();

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTable = new MyTable(1111, "MyInsertedSavedValue", null);
            connection
                .save(myTable);
        }

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTableRetrieved = connection
                .query(MyTable.class)
                .fetchById(1111);

            assertNotNull(myTableRetrieved);
            assertEquals(1111, myTableRetrieved.getId());
            assertEquals("MyInsertedSavedValue", myTableRetrieved.getMyvalue());
        }
    }

    @Test
    public void aggregate_save_insertSimpleEntityWithAutoIncrement_savesEntity()
    {
        registerMyTableOnlyWithAutoIncrementAggregate();

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTable = new MyTable(0, "MyAutoIncrementedInsertedSavedValue", null);
            connection
                .save(myTable);
        }

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTableRetrieved = connection
                .query(MyTable.class)
                .fetchById(7);

            assertNotNull(myTableRetrieved);
            assertEquals(7, myTableRetrieved.getId());
            assertEquals("MyAutoIncrementedInsertedSavedValue", myTableRetrieved.getMyvalue());
        }
    }

    @Test
    public void aggregate_save_updateEntityWithChild_savesEntity()
    {
        registerMyTableAggregate();

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTable = new MyTable(3, "MySavedValue", new MyOtherTable(3, "MyOtherSavedValue"));

            connection
                .save(myTable);
        }

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTableRetrieved = connection
                .query(MyTable.class)
                .fetchById(3);

            assertNotNull(myTableRetrieved);
            assertEquals(3, myTableRetrieved.getId());
            assertEquals("MySavedValue", myTableRetrieved.getMyvalue());

            MyOtherTable myOtherTableRetrieved = myTableRetrieved.getMyOtherTable();
            assertNotNull(myOtherTableRetrieved);
            assertEquals(3, myOtherTableRetrieved.getId());
            assertEquals("MyOtherSavedValue", myOtherTableRetrieved.getMyOtherValueWithDiffName());
        }
    }

    @Test
    public void aggregate_save_insertAutoIncrementEntityAndChild_savesEntity()
    {
        registerMyTableWithAutoIncrementAggregate();

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTable = new MyTable(0, "MySavedValueAutoInc", new MyOtherTable(0, "MyOtherSavedValueAutoInc"));

            connection
                .save(myTable);
        }

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTableRetrieved = connection
                .query(MyTable.class)
                .fetchById(7);

            assertNotNull(myTableRetrieved);
            assertEquals(7, myTableRetrieved.getId());
            assertEquals("MySavedValueAutoInc", myTableRetrieved.getMyvalue());

            MyOtherTable myOtherTableRetrieved = myTableRetrieved.getMyOtherTable();
            assertNotNull(myOtherTableRetrieved);
            assertEquals(7, myOtherTableRetrieved.getId());
            assertEquals("MyOtherSavedValueAutoInc", myOtherTableRetrieved.getMyOtherValueWithDiffName());
        }
    }

    @Test
    public void aggregate_save_insertWithCustomToFieldValueConverter_savesAndRetrievesEntity()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .withCustomToFieldValueConverter("myvalue", new Converter()
            {
                @Override
                public Object convert(Object val) throws ConverterException
                {
                    return ((String) val).toUpperCase();
                }

                @Override
                public Object toDatabaseParam(Object val)
                {
                    return null;
                }
            })
            .register();

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTable = new MyTable(0, "MySavedValueAutoInc", new MyOtherTable(0, "MyOtherSavedValueAutoInc"));

            connection
                .save(myTable);
        }

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTableRetrieved = connection
                .query(MyTable.class)
                .fetchById(7);

            assertNotNull(myTableRetrieved);
            assertEquals(7, myTableRetrieved.getId());
            assertEquals("MYSAVEDVALUEAUTOINC", myTableRetrieved.getMyvalue());

            MyTable myOtherTableRaw = connection.query("SELECT * FROM MyTable WHERE id = 7").fetch(MyTable.class);
            assertEquals("MySavedValueAutoInc", myOtherTableRaw.getMyvalue());
        }
    }

    @Test
    public void aggregate_save_insertWithCustomToDatabaseValueConverter_savesAndRetrievesEntity()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .withCustomToDatabaseValueConverter("myvalue", new Converter()
            {
                @Override
                public Object convert(Object val) throws ConverterException
                {
                    return ((String) val).toUpperCase();
                }

                @Override
                public Object toDatabaseParam(Object val)
                {
                    return null;
                }
            })
            .register();

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTable = new MyTable(0, "MySavedValueAutoInc", new MyOtherTable(0, "MyOtherSavedValueAutoInc"));

            connection
                .save(myTable);
        }

        try(PhotonConnection connection = photon.open())
        {
            MyTable myTableRetrieved = connection
                .query(MyTable.class)
                .fetchById(7);

            assertNotNull(myTableRetrieved);
            assertEquals(7, myTableRetrieved.getId());
            assertEquals("MYSAVEDVALUEAUTOINC", myTableRetrieved.getMyvalue());

            MyTable myOtherTableRaw = connection.query("SELECT * FROM MyTable WHERE id = 7").fetch(MyTable.class);
            assertEquals("MYSAVEDVALUEAUTOINC", myOtherTableRaw.getMyvalue());
        }
    }

    private void registerMyTableOnlyAggregate()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .register();
    }

    private void registerMyTableOnlyWithAutoIncrementAggregate()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .register();
    }

    private void registerMyTableWithAutoIncrementAggregate()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .withChild(MyOtherTable.class)
                .withId("id")
                .withForeignKeyToParent("id")
                .withFieldToColumnMapping("myOtherValueWithDiffName", "myothervalue")
                .addAsChild("myOtherTable")
            .register();
    }

    private void registerMyTableAggregate()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .withChild(MyOtherTable.class)
                .withId("id")
                .withForeignKeyToParent("id")
                .withFieldToColumnMapping("myOtherValueWithDiffName", "myothervalue")
                .addAsChild("myOtherTable")
            .register();
    }
}
