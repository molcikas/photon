package com.github.molcikas.photon.tests.unit.h2.myonetomanytable;

import org.junit.Before;
import org.junit.Test;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.tests.unit.entities.myonetomanytable.MyThirdTable;
import com.github.molcikas.photon.tests.unit.entities.myonetomanytable.MyManyTable;
import com.github.molcikas.photon.tests.unit.entities.myonetomanytable.MyOneToManyTable;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class MyOneToManyTableSaveTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = MyOneToManyTableDbSetup.setupDatabase();
    }

    @Test
    public void aggregateSave_insertWithMultipleChildren_savesEntity()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = new MyOneToManyTable(
                null,
                "MyOneToManyTableValue",
                Arrays.asList(
                    new MyManyTable(0, "My1ManyValue", null),
                    new MyManyTable(0, "My2ManyValue", null),
                    new MyManyTable(null, "My3ManyValue", Arrays.asList(
                        new MyThirdTable(null, "MyThirdTableVal1"),
                        new MyThirdTable(null, "MyThirdTableVal2")
                    )),
                    new MyManyTable(null, "My4ManyValue", null)
                )
            );

            transaction.save(myOneToManyTable);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .fetchById(7);

            assertNotNull(myOneToManyTable);
            assertEquals(Integer.valueOf(7), myOneToManyTable.getId());
            assertEquals("MyOneToManyTableValue", myOneToManyTable.getMyvalue());
            assertEquals(4, myOneToManyTable.getMyManyTables().size());

            MyManyTable myManyTable = myOneToManyTable.getMyManyTables().get(2);
            assertEquals(Integer.valueOf(12), myManyTable.getId());
            assertEquals(Integer.valueOf(7), myManyTable.getParent());
            assertEquals("My3ManyValue", myManyTable.getMyOtherValueWithDiffName());
            assertEquals(2, myManyTable.getMyThirdTables().size());

            MyThirdTable myThirdTable = myManyTable.getMyThirdTables().get(1);
            assertEquals(Integer.valueOf(6), myThirdTable.getId());
            assertEquals(Integer.valueOf(12), myThirdTable.getParent());
            assertEquals("MyThirdTableVal2", myThirdTable.getVal());
        }
    }

    @Test
    public void aggregateSave_deleteEntityWithChildren_savesAggregate()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            myOneToManyTable.getMyManyTables().remove(1);

            transaction.save(myOneToManyTable);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            assertNotNull(myOneToManyTable);
            assertEquals(Integer.valueOf(6), myOneToManyTable.getId());
            assertEquals("my6dbvalue", myOneToManyTable.getMyvalue());
            assertEquals(2, myOneToManyTable.getMyManyTables().size());

            MyManyTable myManyTable = myOneToManyTable.getMyManyTables().get(1);
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
    public void aggregateSave_excludeChild_doesNotSaveChild()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            myOneToManyTable.setMyvalue("MyNewValue");
            myOneToManyTable.getMyManyTables().clear();

            transaction.saveWithExcludedFields(myOneToManyTable, "myManyTables");
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            assertNotNull(myOneToManyTable);
            assertEquals(Integer.valueOf(6), myOneToManyTable.getId());
            assertEquals("MyNewValue", myOneToManyTable.getMyvalue());
            assertEquals(3, myOneToManyTable.getMyManyTables().size());
        }
    }

    @Test
    public void aggregateSave_excludeGrandchild_doesNotSaveGrandchild()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            myOneToManyTable.getMyManyTables().get(2).getMyThirdTables().clear();

            transaction.saveWithExcludedFields(myOneToManyTable, "myManyTables.myThirdTables");
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            assertNotNull(myOneToManyTable);
            assertEquals(Integer.valueOf(6), myOneToManyTable.getId());
            assertEquals(3, myOneToManyTable.getMyManyTables().size());

            MyManyTable myManyTable = myOneToManyTable.getMyManyTables().get(2);
            assertEquals(2, myManyTable.getMyThirdTables().size());
        }
    }

    @Test
    public void aggregate_delete_deleteThreeLevelAggregate_deletesAggregate()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            transaction.delete(myOneToManyTable);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            assertNull(myOneToManyTable);
        }
    }

    private void registerMyOneToManyTableAggregate()
    {
        photon.registerAggregate(MyOneToManyTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .withChild("myManyTables", MyManyTable.class)
                .withId("id")
                .withPrimaryKeyAutoIncrement()
                .withForeignKeyToParent("parent")
                .withDatabaseColumn("myothervalue", "myOtherValueWithDiffName")
                .withChild("myThirdTables", MyThirdTable.class)
                    .withId("id")
                    .withPrimaryKeyAutoIncrement()
                    .withForeignKeyToParent("parent")
                    .addAsChild()
            .addAsChild()
            .register();
    }
}
