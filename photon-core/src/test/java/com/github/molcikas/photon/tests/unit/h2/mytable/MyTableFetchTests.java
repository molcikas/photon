package com.github.molcikas.photon.tests.unit.h2.mytable;

import com.github.molcikas.photon.blueprints.table.ColumnDataType;
import com.github.molcikas.photon.datasource.ExistingConnectionDataSource;
import com.github.molcikas.photon.datasource.ReadOnlyConnection;
import com.github.molcikas.photon.exceptions.PhotonException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.blueprints.entity.EntityFieldValueMapping;
import com.github.molcikas.photon.tests.unit.entities.mytable.MyOtherTable;
import com.github.molcikas.photon.tests.unit.entities.mytable.MyTable;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class MyTableFetchTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = MyTableDbSetup.setupDatabase();
    }

    @Test
    public void aggregate_fetchById_simpleEntity_returnsEntity()
    {
        registerMyTableOnlyAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = transaction
                .query(MyTable.class)
                .fetchById(2);

            assertNotNull(myTable);
            assertEquals(new Integer(2), myTable.getId());
            assertEquals("my2dbvalue", myTable.getMyvalue());
        }
    }

    @Test
    public void aggregate_fetchById_simpleEntityWithImplicitId_returnsEntity()
    {
        photon.registerAggregate(MyTable.class).register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = transaction
                .query(MyTable.class)
                .fetchById(2);

            assertNotNull(myTable);
            assertEquals(new Integer(2), myTable.getId());
            assertEquals("my2dbvalue", myTable.getMyvalue());
        }
    }

    @Test
    public void aggregate_fetchById_idNotInDatabase_returnsNull()
    {
        registerMyTableOnlyAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = transaction
                .query(MyTable.class)
                .fetchById(7);

            assertNull(myTable);
        }
    }

    @Test
    public void aggregate_fetchByIdsAndSortRootDescending_returnsAggregates()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .withOrderBySql("id DESC")
            .register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            List<MyTable> myTables = transaction
                .query(MyTable.class)
                .fetchByIds(Arrays.asList(2, 4));

            assertNotNull(myTables);
            assertEquals(2, myTables.size());
            assertEquals(new Integer(4), myTables.get(0).getId());
            assertEquals("my4dbvalue", myTables.get(0).getMyvalue());
            assertEquals(new Integer(2), myTables.get(1).getId());
            assertEquals("my2dbvalue", myTables.get(1).getMyvalue());
        }
    }

    @Test
    public void aggregate_fetchById_oneToOneWithNoMatch_returnsNullChild()
    {
        registerMyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = transaction
                .query(MyTable.class)
                .fetchById(2);

            assertNotNull(myTable);
            assertEquals(new Integer(2), myTable.getId());
            assertEquals("my2dbvalue", myTable.getMyvalue());

            assertNull(myTable.getMyOtherTable());
        }
    }

    @Test
    public void aggregate_fetchById_oneToOneWithMatch_returnsChild()
    {
        registerMyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = transaction
                .query(MyTable.class)
                .fetchById(5);

            assertNotNull(myTable);
            assertEquals(new Integer(5), myTable.getId());
            assertEquals("my5dbvalue", myTable.getMyvalue());

            MyOtherTable myOtherTable = myTable.getMyOtherTable();
            assertNotNull(myOtherTable);
            assertEquals(5, myOtherTable.getId());
            assertEquals("my5otherdbvalue", myOtherTable.getMyOtherValueWithDiffName());
        }
    }

    @Test
    public void aggregate_fetchByIds_oneToOnesWithMatches_returnsChild()
    {
        registerMyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            List<MyTable> myTables = transaction
                .query(MyTable.class)
                .fetchByIds(Arrays.asList(1, 2, 3, 4));

            assertNotNull(myTables);
            assertEquals(4, myTables.size());

            assertEquals(new Integer(1), myTables.get(0).getId());
            assertNull(myTables.get(0).getMyOtherTable());

            assertEquals(new Integer(2), myTables.get(1).getId());
            assertNull(myTables.get(1).getMyOtherTable());

            assertEquals(new Integer(3), myTables.get(2).getId());
            assertEquals(3, myTables.get(2).getMyOtherTable().getId());

            assertEquals(new Integer(4), myTables.get(3).getId());
            assertEquals(4, myTables.get(3).getMyOtherTable().getId());
        }
    }

    @Test
    public void aggregate_fetchById_ignoredField_returnsEntityWithIgnoredFieldNull()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .withIgnoredField("myvalue")
            .register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = transaction
                .query(MyTable.class)
                .fetchById(3);

            assertNotNull(myTable);
            assertEquals(new Integer(3), myTable.getId());
            assertNull(myTable.getMyvalue());
        }
    }

    @Test
    public void aggregate_fetchById_withCustomTableName_returnsEntityFromCustomTableName()
    {
        photon.registerAggregate(MyOtherTable.class)
            .withId("id")
            .withDatabaseColumn("myvalue", "myOtherValueWithDiffName")
            .withTableName("mytable")
            .register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOtherTable myOtherTable = transaction
                .query(MyOtherTable.class)
                .fetchById(3);

            assertNotNull(myOtherTable);
            assertEquals(3, myOtherTable.getId());
            assertEquals("my3dbvalue", myOtherTable.getMyOtherValueWithDiffName());
        }
    }

    @Test
    public void aggregate_fetchById_entityFieldValueMapping_returnsEntityWithMappedValue()
    {
        registerMyTableWithCustomFieldColumnMappingAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = transaction
                .query(MyTable.class)
                .fetchById(3);

            assertNotNull(myTable);
            assertEquals(new Integer(3), myTable.getId());
            assertEquals("my3dbvalue", myTable.getMyOtherTable().getMyOtherValueWithDiffName());
        }
    }

    @Test
    public void aggregate_fetchWhere_fetchByNonIdValue_fetchesAggregate()
    {
        registerMyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = transaction
                .query(MyTable.class)
                .where("myvalue = :myvalue")
                .addParameter("myvalue", "my3dbvalue")
                .fetch();

            assertNotNull(myTable);
            assertEquals(new Integer(3), myTable.getId());
            assertEquals("my3otherdbvalue", myTable.getMyOtherTable().getMyOtherValueWithDiffName());
        }
    }

    @Test
    public void aggregate_fetchWhere_fetchListByNonIdValue_fetchesList()
    {
        registerMyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            List<MyTable> myTables = transaction
                .query(MyTable.class)
                .where("myvalue = :myvalue1 OR myvalue = :myvalue2")
                .addParameter("myvalue1", "my3dbvalue")
                .addParameter("myvalue2", "my4dbvalue")
                .fetchList();

            assertNotNull(myTables);
            assertEquals(2, myTables.size());
            assertEquals(new Integer(3), myTables.get(0).getId());
            assertEquals("my3otherdbvalue", myTables.get(0).getMyOtherTable().getMyOtherValueWithDiffName());
            assertEquals(new Integer(4), myTables.get(1).getId());
            assertEquals("my4otherdbvalue", myTables.get(1).getMyOtherTable().getMyOtherValueWithDiffName());
        }
    }

    @Test
    public void aggregate_fetchByIdQuery_fetchByDataInSubEntity_fetchesAggregate()
    {
        registerMyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            List<MyTable> myTables = transaction
                .query(MyTable.class)
                .whereIdIn("SELECT mytable.id FROM mytable JOIN myothertable ON myothertable.id = mytable.id WHERE myothervalue = :myothervalue1 OR myothervalue = :myothervalue2")
                .addParameter("myothervalue1", "my4otherdbvalue")
                .addParameter("myothervalue2", "my5otherdbvalue")
                .fetchList();

            assertNotNull(myTables);
            assertEquals(2, myTables.size());
            assertEquals(new Integer(4), myTables.get(0).getId());
            assertEquals("my4otherdbvalue", myTables.get(0).getMyOtherTable().getMyOtherValueWithDiffName());
            assertEquals(new Integer(5), myTables.get(1).getId());
            assertEquals("my5otherdbvalue", myTables.get(1).getMyOtherTable().getMyOtherValueWithDiffName());
        }
    }

    @Test
    public void aggregate_fetchByIdQuery_fetchByDataInSubEntityList_fetchesAggregate()
    {
        registerMyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            List<MyTable> myTables = transaction
                .query(MyTable.class)
                .whereIdIn("SELECT mytable.id FROM mytable JOIN myothertable ON myothertable.id = mytable.id WHERE myothervalue IN (:myOtherValues)")
                .addParameter("myOtherValues", Arrays.asList("my4otherdbvalue", "my5otherdbvalue"))
                .fetchList();

            assertNotNull(myTables);
            assertEquals(2, myTables.size());
            assertEquals(new Integer(4), myTables.get(0).getId());
            assertEquals("my4otherdbvalue", myTables.get(0).getMyOtherTable().getMyOtherValueWithDiffName());
            assertEquals(new Integer(5), myTables.get(1).getId());
            assertEquals("my5otherdbvalue", myTables.get(1).getMyOtherTable().getMyOtherValueWithDiffName());

            transaction.commit();
        }
    }

    @Test
    public void registerViewModelAggregate_fetchAggregate_fetchesAggregate()
    {
        photon.registerViewModelAggregate(MyTable.class, "MyCustomName", false)
            .register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = transaction
                .query(MyTable.class, "MyCustomName")
                .fetchById(2);

            assertNotNull(myTable);
            assertEquals(new Integer(2), myTable.getId());
            assertEquals("my2dbvalue", myTable.getMyvalue());
        }
    }

    @Test
    public void registerViewModelAggregate_fetchAggregateWithWrongName_throwsEsception()
    {
        photon.registerViewModelAggregate(MyTable.class, "MyCustomName", false)
            .register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyTable myTable = transaction
                .query(MyTable.class, "MyWrongCustomName")
                .fetchById(2);

            Assert.fail("Failed to throw PhotonException.");
        }
        catch(PhotonException ex)
        {
            assertTrue(ex.getMessage().contains("MyWrongCustomName"));
        }
    }

    @Test
    public void existingConnection_queryEntity_returnsEntity()
    {
        Connection connection;
        try
        {
            connection = photon.getDataSource().getConnection();
        }
        catch (SQLException ex)
        {
            throw new RuntimeException(ex);
        }
        ExistingConnectionDataSource existingConnectionDataSource =
            new ExistingConnectionDataSource(new ReadOnlyConnection(connection));

        Photon photon2 = new Photon(existingConnectionDataSource);

        photon2.registerAggregate(MyTable.class)
            .withId("id")
            .register();

        try(PhotonTransaction transaction = photon2.beginTransaction())
        {
            MyTable myTable = transaction
                .query(MyTable.class)
                .fetchById(2);

            assertNotNull(myTable);
            assertEquals(new Integer(2), myTable.getId());
            assertEquals("my2dbvalue", myTable.getMyvalue());
        }
    }

    @Test
    public void existingConnection_saveWithImmutableConnection_doesNotCloseOrCommit()
    {
        Connection connection;
        try
        {
            connection = photon.getDataSource().getConnection();
        }
        catch (SQLException ex)
        {
            throw new RuntimeException(ex);
        }
        ExistingConnectionDataSource existingConnectionDataSource =
            new ExistingConnectionDataSource(new ReadOnlyConnection(connection));

        Photon photon2 = new Photon(existingConnectionDataSource);

        photon2.registerAggregate(MyTable.class)
            .withId("id")
            .register();

        try(PhotonTransaction transaction = photon2.beginTransaction())
        {
            MyTable myTable = new MyTable(7, "val", null);

            transaction.save(myTable);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon2.beginTransaction())
        {
            MyTable myTable = transaction
                .query(MyTable.class)
                .fetchById(2);

            assertNotNull(myTable);
            assertEquals(new Integer(2), myTable.getId());
            assertEquals("my2dbvalue", myTable.getMyvalue());

            myTable = transaction
                .query(MyTable.class)
                .fetchById(8);

            assertNull(myTable);
        }
    }

    private void registerMyTableOnlyAggregate()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .register();
    }

    private void registerMyTableAggregate()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .withChild("myOtherTable", MyOtherTable.class)
                .withId("id")
                .withForeignKeyToParent("id")
                .withDatabaseColumn("myothervalue", "myOtherValueWithDiffName")
                .addAsChild()
            .register();
    }

    private void registerMyTableWithCustomFieldColumnMappingAggregate()
    {
        photon.registerAggregate(MyTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .withIgnoredField("myvalue")
            .withDatabaseColumn("myvalue", ColumnDataType.VARCHAR, new EntityFieldValueMapping<MyTable, String>()
                {
                    @Override
                    public String getFieldValue(MyTable entityInstance)
                    {
                        return entityInstance.getMyOtherTable().getMyOtherValueWithDiffName();
                    }

                    @Override
                    public Map<String, Object> setFieldValue(MyTable entityInstance, String value)
                    {
                        MyOtherTable myOtherTable = new MyOtherTable(0, value);
                        Map<String, Object> valuesToSet = new HashMap<>();
                        valuesToSet.put("myOtherTable", myOtherTable);
                        return valuesToSet;
                    }
                }
            )
            .register();
    }
}
