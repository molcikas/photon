package com.github.molcikas.photon.tests.unit.h2.myonetomanytable;

import com.github.molcikas.photon.blueprints.ColumnDataType;
import com.github.molcikas.photon.exceptions.PhotonException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.tests.unit.entities.myonetomanytable.MyThirdTable;
import com.github.molcikas.photon.tests.unit.entities.myonetomanytable.MyManyTable;
import com.github.molcikas.photon.tests.unit.entities.myonetomanytable.MyOneToManyTable;

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

    private void registerMyOneToManyTableAggregate()
    {
        photon.registerAggregate(MyOneToManyTable.class)
            .withId("id")
            .withPrimaryKeyAutoIncrement()
            .withChild(MyManyTable.class)
                .withId("id", true)
                .withForeignKeyToParent("parent")
                .withDatabaseColumn("myothervalue", "myOtherValueWithDiffName", ColumnDataType.VARCHAR)
                .withChild(MyThirdTable.class)
                    .withId("id")
                    .withPrimaryKeyAutoIncrement()
                    .withForeignKeyToParent("parent")
                    .addAsChild("myThirdTables")
                .addAsChild("myManyTables")
            .register();
    }
}
