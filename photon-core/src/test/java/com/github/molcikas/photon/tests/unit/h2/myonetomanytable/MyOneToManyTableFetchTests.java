package com.github.molcikas.photon.tests.unit.h2.myonetomanytable;

import com.github.molcikas.photon.blueprints.entity.ChildCollectionConstructor;
import com.github.molcikas.photon.blueprints.table.ColumnDataType;
import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.tests.unit.entities.myonetomanytable.MyOneToManyMapTable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.tests.unit.entities.myonetomanytable.MyThirdTable;
import com.github.molcikas.photon.tests.unit.entities.myonetomanytable.MyManyTable;
import com.github.molcikas.photon.tests.unit.entities.myonetomanytable.MyOneToManyTable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
    public void aggregateQuery_fetchByIdOneToOneWithMatch_returnsAggregate()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            assertNotNull(myOneToManyTable);
            assertEquals(Integer.valueOf(6), myOneToManyTable.getId());
            assertEquals("my6dbvalue", myOneToManyTable.getMyvalue());
            assertEquals(3, myOneToManyTable.getMyManyTables().size());

            MyManyTable myManyTable = myOneToManyTable.getMyManyTables().get(2);
            assertEquals(Integer.valueOf(9), myManyTable.getId());
            assertEquals(Integer.valueOf(6), myManyTable.getParent());
            assertEquals("my63otherdbvalue", myManyTable.getMyOtherValueWithDiffName());
            assertEquals(2, myManyTable.getMyThirdTables().size());

            MyThirdTable myThirdTable = myManyTable.getMyThirdTables().get(0);
            assertEquals(Integer.valueOf(3), myThirdTable.getId());
            assertEquals(Integer.valueOf(9), myThirdTable.getParent());
            assertEquals("thirdtableval3", myThirdTable.getVal());
        }
    }

    @Test
    public void aggregateQuery_fetchByIdNoChildren_returnsAggregateWithEmptyNotNullChildList()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .fetchById(1);

            assertNotNull(myOneToManyTable);
            assertNotNull(myOneToManyTable.getMyManyTables());
            assertEquals(0, myOneToManyTable.getMyManyTables().size());
        }
    }

    @Test
    public void aggregateQuery_fetchByIdExcludeChild_returnsAggregateWithNullChild()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .exclude("myManyTables")
                .fetchById(6);

            assertNotNull(myOneToManyTable);
            assertEquals(Integer.valueOf(6), myOneToManyTable.getId());
            assertEquals("my6dbvalue", myOneToManyTable.getMyvalue());
            assertEquals(0, myOneToManyTable.getMyManyTables().size());
        }
    }

    @Test
    public void aggregateQuery_fetchByIdExcludeGrandchild_returnsAggregateWithNullGrandchild()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .exclude("myManyTables.myThirdTables")
                .fetchById(6);

            assertNotNull(myOneToManyTable);
            assertEquals(Integer.valueOf(6), myOneToManyTable.getId());
            assertEquals("my6dbvalue", myOneToManyTable.getMyvalue());
            assertEquals(3, myOneToManyTable.getMyManyTables().size());

            MyManyTable myManyTable = myOneToManyTable.getMyManyTables().get(2);
            assertEquals(Integer.valueOf(9), myManyTable.getId());
            assertEquals(Integer.valueOf(6), myManyTable.getParent());
            assertEquals("my63otherdbvalue", myManyTable.getMyOtherValueWithDiffName());
            assertEquals(0, myManyTable.getMyThirdTables().size());
        }
    }

    @Test
    public void aggregateQuery_fetchByIdExcludeNonexistentChild_returnsAggregate()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .exclude("myManyTables.notARealField")
                .fetchById(6);

            Assert.fail("Failed to throw PhotonException");
        }
        catch (PhotonException ex)
        {
            assertTrue(ex.getMessage().contains("notARealField"));
        }
    }

    @Test
    public void aggregateQuery_fieldAsMap_returnsAggregate()
    {
        photon.registerAggregate(MyOneToManyMapTable.class)
            .withTableName("MyOneToManyTable")
            .withId("myOneToManyMapTableId")
            .withDatabaseColumn("id", "myOneToManyMapTableId")
            .withPrimaryKeyAutoIncrement()
            .withChild("myManyTables", MyManyTable.class)
                .withChildCollectionConstructor(new ChildCollectionConstructor<Map<Integer, MyManyTable>, MyManyTable, MyOneToManyMapTable>()
                {
                    @Override
                    public Collection<MyManyTable> toCollection(Map<Integer, MyManyTable> fieldValue, MyOneToManyMapTable parentEntityInstance)
                    {
                        return null;
                    }

                    @Override
                    public Map<Integer, MyManyTable> toFieldValue(Collection<MyManyTable> collection, MyOneToManyMapTable parentEntityInstance)
                    {
                        Map<Integer, MyManyTable> map = new HashMap<>();
                        for(MyManyTable myManyTable : collection)
                        {
                            map.put(myManyTable.getId(), myManyTable);
                        }
                        return map;
                    }
                })
                .withId("id", true)
                .withForeignKeyToParent("parent")
                .withDatabaseColumn("myothervalue", "myOtherValueWithDiffName", ColumnDataType.VARCHAR)
                .addAsChild()
            .register();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyMapTable myOneToManyTable = transaction
                .query(MyOneToManyMapTable.class)
                .fetchById(6);

            assertNotNull(myOneToManyTable);
            assertNotNull(myOneToManyTable.getMyManyTables());
            assertEquals(HashMap.class, myOneToManyTable.getMyManyTables().getClass());
            assertEquals(3, myOneToManyTable.getMyManyTables().size());

            MyManyTable myManyTable = myOneToManyTable.getMyManyTables().get(9);
            assertEquals(Integer.valueOf(9), myManyTable.getId());
            assertEquals(Integer.valueOf(6), myManyTable.getParent());
            assertEquals("my63otherdbvalue", myManyTable.getMyOtherValueWithDiffName());
        }
    }

    private void registerMyOneToManyTableAggregate()
    {
        photon.registerAggregate(MyOneToManyTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .withChild("myManyTables", MyManyTable.class)
                .withId("id", true)
                .withForeignKeyToParent("parent")
                .withDatabaseColumn("myothervalue", "myOtherValueWithDiffName", ColumnDataType.VARCHAR)
                .withChild("myThirdTables", MyThirdTable.class)
                    .withId("id")
                    .withPrimaryKeyAutoIncrement()
                    .withForeignKeyToParent("parent")
                    .addAsChild()
                .addAsChild()
            .register();
    }
}
