package com.github.molcikas.photon.tests.unit.h2.mytable;

import org.junit.Before;
import org.junit.Test;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.blueprints.EntityFieldValueMapping;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.converters.ConverterException;
import com.github.molcikas.photon.tests.unit.entities.mytable.MyOtherTable;
import com.github.molcikas.photon.tests.unit.entities.mytable.MyTable;

import java.lang.reflect.Field;
import java.sql.Types;

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

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = new MyTable(2, "MySavedValue", null);
            transaction.save(myTable);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTableRetrieved = transaction
                .query(MyTable.class)
                .fetchById(2);

            assertNotNull(myTableRetrieved);
            assertEquals(2, myTableRetrieved.getId());
            assertEquals("MySavedValue", myTableRetrieved.getMyvalue());
            transaction.commit();
        }
    }

    @Test
    public void aggregate_saveAndNoCommitWithReadTransaction_simpleEntity_savesEntity()
    {
        registerMyTableOnlyAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = new MyTable(2, "MySavedValue", null);
            transaction.save(myTable);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTableRetrieved = transaction
                .query(MyTable.class)
                .fetchById(2);

            assertNotNull(myTableRetrieved);
            assertEquals(2, myTableRetrieved.getId());
            assertEquals("MySavedValue", myTableRetrieved.getMyvalue());

            // Not committing, but that won't matter because this transaction does not make any changes.
        }
    }

    @Test
    public void aggregate_saveWithoutCommit_simpleEntity_doesNotSaveEntity()
    {
        registerMyTableOnlyAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = new MyTable(7, "MySavedValue", null);
            transaction.save(myTable);
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTableRetrieved = transaction
                .query(MyTable.class)
                .fetchById(7);

            assertNull(myTableRetrieved);
        }
    }

    @Test
    public void aggregate_save_insertSimpleEntity_savesEntity()
    {
        registerMyTableOnlyAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = new MyTable(1111, "MyInsertedSavedValue", null);
            transaction.save(myTable);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTableRetrieved = transaction
                .query(MyTable.class)
                .fetchById(1111);

            assertNotNull(myTableRetrieved);
            assertEquals(1111, myTableRetrieved.getId());
            assertEquals("MyInsertedSavedValue", myTableRetrieved.getMyvalue());
            transaction.commit();
        }
    }

    @Test
    public void aggregate_save_insertSimpleEntityWithAutoIncrement_savesEntity()
    {
        registerMyTableOnlyWithAutoIncrementAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = new MyTable(0, "MyAutoIncrementedInsertedSavedValue", null);
            transaction.save(myTable);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTableRetrieved = transaction
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

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = new MyTable(3, "MySavedValue", new MyOtherTable(3, "MyOtherSavedValue"));

            transaction.save(myTable);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTableRetrieved = transaction
                .query(MyTable.class)
                .fetchById(3);

            assertNotNull(myTableRetrieved);
            assertEquals(3, myTableRetrieved.getId());
            assertEquals("MySavedValue", myTableRetrieved.getMyvalue());

            MyOtherTable myOtherTableRetrieved = myTableRetrieved.getMyOtherTable();
            assertNotNull(myOtherTableRetrieved);
            assertEquals(3, myOtherTableRetrieved.getId());
            assertEquals("MyOtherSavedValue", myOtherTableRetrieved.getMyOtherValueWithDiffName());

            transaction.commit();
        }
    }

    @Test
    public void aggregate_save_insertAutoIncrementEntityAndChild_savesEntity()
    {
        registerMyTableWithAutoIncrementAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = new MyTable(0, "MySavedValueAutoInc", new MyOtherTable(0, "MyOtherSavedValueAutoInc"));

            transaction.save(myTable);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTableRetrieved = transaction
                .query(MyTable.class)
                .fetchById(7);

            assertNotNull(myTableRetrieved);
            assertEquals(7, myTableRetrieved.getId());
            assertEquals("MySavedValueAutoInc", myTableRetrieved.getMyvalue());

            MyOtherTable myOtherTableRetrieved = myTableRetrieved.getMyOtherTable();
            assertNotNull(myOtherTableRetrieved);
            assertEquals(7, myOtherTableRetrieved.getId());
            assertEquals("MyOtherSavedValueAutoInc", myOtherTableRetrieved.getMyOtherValueWithDiffName());

            transaction.commit();
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
            })
            .register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = new MyTable(0, "MySavedValueAutoInc", new MyOtherTable(0, "MyOtherSavedValueAutoInc"));

            transaction.save(myTable);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTableRetrieved = transaction
                .query(MyTable.class)
                .fetchById(7);

            assertNotNull(myTableRetrieved);
            assertEquals(7, myTableRetrieved.getId());
            assertEquals("MYSAVEDVALUEAUTOINC", myTableRetrieved.getMyvalue());

            MyTable myOtherTableRaw = transaction.query("SELECT * FROM MyTable WHERE id = 7").fetch(MyTable.class);
            assertEquals("MySavedValueAutoInc", myOtherTableRaw.getMyvalue());

            transaction.commit();
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
            })
            .register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = new MyTable(0, "MySavedValueAutoInc", new MyOtherTable(0, "MyOtherSavedValueAutoInc"));

            transaction.save(myTable);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTableRetrieved = transaction
                .query(MyTable.class)
                .fetchById(7);

            assertNotNull(myTableRetrieved);
            assertEquals(7, myTableRetrieved.getId());
            assertEquals("MYSAVEDVALUEAUTOINC", myTableRetrieved.getMyvalue());

            MyTable myOtherTableRaw = transaction.query("SELECT * FROM MyTable WHERE id = 7").fetch(MyTable.class);
            assertEquals("MYSAVEDVALUEAUTOINC", myOtherTableRaw.getMyvalue());

            transaction.commit();
        }
    }

    @Test
    public void aggregate_save_ignoredField_doesNotSaveIgnoredField()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .withIgnoredField("myvalue")
            .register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = new MyTable(0, "IShouldBeIgnored", null);


            transaction.save(myTable);
            transaction.commit();
        }

        photon.registerAggregate(MyTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = transaction
                .query(MyTable.class)
                .fetchById(7);

            assertNotNull(myTable);
            assertEquals(7, myTable.getId());
            assertEquals("oops", myTable.getMyvalue());

            transaction.commit();
        }
    }

    @Test
    public void aggregate_save_entityFieldValueMapping_savesEntityWithMappedValue()
    {
        registerMyTableWithCustomFieldColumnMappingAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = new MyTable(0, null, new MyOtherTable(0, "MySavedMappedEntityValue"));

            transaction.save(myTable);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = transaction
                .query(MyTable.class)
                .fetchById(7);

            assertNotNull(myTable);
            assertEquals(7, myTable.getId());
            assertEquals("MySavedMappedEntityValue", myTable.getMyOtherTable().getMyOtherValueWithDiffName());

            transaction.commit();
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

    private void registerMyTableWithCustomFieldColumnMappingAggregate()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .withIgnoredField("myvalue")
            .withDatabaseColumn("myvalue", Types.VARCHAR, new EntityFieldValueMapping<MyTable, String>()
                {
                    @Override
                    public String getFieldValueFromEntityInstance(MyTable entityInstance)
                    {
                        return entityInstance.getMyOtherTable().getMyOtherValueWithDiffName();
                    }

                    @Override
                    public void setFieldValueOnEntityInstance(MyTable entityInstance, String value)
                    {
                        try
                        {
                            MyOtherTable myOtherTable = new MyOtherTable(0, value);
                            Field field = MyTable.class.getDeclaredField("myOtherTable");
                            field.setAccessible(true);
                            field.set(entityInstance, myOtherTable);
                        }
                        catch(Exception ex)
                        {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            )
            .register();
    }
}
