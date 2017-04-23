package com.github.molcikas.photon.tests.databaseintegrations.h2.mytable;

import org.junit.Before;
import org.junit.Test;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.blueprints.EntityFieldValueMapping;
import com.github.molcikas.photon.blueprints.SortDirection;
import com.github.molcikas.photon.tests.entities.mytable.MyOtherTable;
import com.github.molcikas.photon.tests.entities.mytable.MyTable;

import java.lang.reflect.Field;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

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
            assertEquals(2, myTable.getId());
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
            .withOrderBy("id", SortDirection.Descending)
            .register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            List<MyTable> myTables = transaction
                .query(MyTable.class)
                .fetchByIds(Arrays.asList(2, 4));

            assertNotNull(myTables);
            assertEquals(2, myTables.size());
            assertEquals(4, myTables.get(0).getId());
            assertEquals("my4dbvalue", myTables.get(0).getMyvalue());
            assertEquals(2, myTables.get(1).getId());
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
            assertEquals(2, myTable.getId());
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
            assertEquals(5, myTable.getId());
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

            assertEquals(1, myTables.get(0).getId());
            assertNull(myTables.get(0).getMyOtherTable());

            assertEquals(2, myTables.get(1).getId());
            assertNull(myTables.get(1).getMyOtherTable());

            assertEquals(3, myTables.get(2).getId());
            assertEquals(3, myTables.get(2).getMyOtherTable().getId());

            assertEquals(4, myTables.get(3).getId());
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
            assertEquals(3, myTable.getId());
            assertNull(myTable.getMyvalue());
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
            assertEquals(3, myTable.getId());
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
            assertEquals(3, myTable.getId());
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
            assertEquals(3, myTables.get(0).getId());
            assertEquals("my3otherdbvalue", myTables.get(0).getMyOtherTable().getMyOtherValueWithDiffName());
            assertEquals(4, myTables.get(1).getId());
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
                .fetchByIdsQuery("SELECT mytable.id FROM mytable JOIN myothertable ON myothertable.id = mytable.id WHERE myothervalue = :myothervalue1 OR myothervalue = :myothervalue2")
                .addParameter("myothervalue1", "my4otherdbvalue")
                .addParameter("myothervalue2", "my5otherdbvalue")
                .fetchList();

            assertNotNull(myTables);
            assertEquals(2, myTables.size());
            assertEquals(4, myTables.get(0).getId());
            assertEquals("my4otherdbvalue", myTables.get(0).getMyOtherTable().getMyOtherValueWithDiffName());
            assertEquals(5, myTables.get(1).getId());
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
                .fetchByIdsQuery("SELECT mytable.id FROM mytable JOIN myothertable ON myothertable.id = mytable.id WHERE myothervalue IN (:myOtherValues)")
                .addParameter("myOtherValues", Arrays.asList("my4otherdbvalue", "my5otherdbvalue"))
                .fetchList();

            assertNotNull(myTables);
            assertEquals(2, myTables.size());
            assertEquals(4, myTables.get(0).getId());
            assertEquals("my4otherdbvalue", myTables.get(0).getMyOtherTable().getMyOtherValueWithDiffName());
            assertEquals(5, myTables.get(1).getId());
            assertEquals("my5otherdbvalue", myTables.get(1).getMyOtherTable().getMyOtherValueWithDiffName());

            transaction.commit();
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
