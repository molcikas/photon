package com.github.molcikas.photon.tests.unit.h2.myonetomanytable;

import com.github.molcikas.photon.blueprints.entity.ChildCollectionConstructor;
import com.github.molcikas.photon.blueprints.table.ColumnDataType;
import com.github.molcikas.photon.tests.unit.entities.myonetomanytable.MyOneToManyMapTable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.tests.unit.entities.myonetomanytable.MyThirdTable;
import com.github.molcikas.photon.tests.unit.entities.myonetomanytable.MyManyTable;
import com.github.molcikas.photon.tests.unit.entities.myonetomanytable.MyOneToManyTable;

import java.util.*;
import java.util.stream.Collectors;

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

    @Test
    public void aggregateSave_fieldAsMap_savesAggregate()
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
                        return fieldValue.values();
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
            Map<Integer, MyManyTable> map = new LinkedHashMap<>();

            map.put(1, new MyManyTable(0, "My1ManyValue", null));
            map.put(2, new MyManyTable(0, "My2ManyValue", null));

            MyOneToManyMapTable myOneToManyTable = new MyOneToManyMapTable(
                null,
                "MyOneToManyTableValue",
                map
            );

            transaction.save(myOneToManyTable);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyMapTable myOneToManyTable = transaction
                .query(MyOneToManyMapTable.class)
                .fetchById(7);

            assertNotNull(myOneToManyTable);
            assertEquals(Integer.valueOf(7), myOneToManyTable.getMyOneToManyMapTableId());
            assertEquals("MyOneToManyTableValue", myOneToManyTable.getMyvalue());
            assertEquals(2, myOneToManyTable.getMyManyTables().size());

            MyManyTable myManyTable = myOneToManyTable.getMyManyTables().get(11);
            assertEquals(Integer.valueOf(11), myManyTable.getId());
            assertEquals(Integer.valueOf(7), myManyTable.getParent());
            assertEquals("My2ManyValue", myManyTable.getMyOtherValueWithDiffName());

            myOneToManyTable.getMyManyTables().remove(11);
            transaction.save(myOneToManyTable);
            transaction.commit();

            myOneToManyTable = transaction
                .query(MyOneToManyMapTable.class)
                .fetchById(7);

            assertEquals(1, myOneToManyTable.getMyManyTables().size());
            assertEquals(Integer.valueOf(10), myOneToManyTable.getMyManyTables().values().iterator().next().getId());
        }
    }

    @Test
    public void aggregateSave_withTrackingAndNoChildrenChanged_doesNotDeleteOrphans()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            transaction.query("insert into `mymanytable` (`id`, `parent`, `myothervalue`) values (10, 6, 'my64otherdbvalue')").executeUpdate();

            // This should NOT delete the orphan we just inserted.
            transaction.save(myOneToManyTable);

            MyOneToManyTable myOneToManyTableFetched = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            assertNotNull(myOneToManyTableFetched);
            assertEquals(4, myOneToManyTableFetched.getMyManyTables().size());
        }
    }

    @Test
    public void aggregateSave_withNoTrackingAndNoChildrenChanged_deletesOrphans()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .noTracking()
                .fetchById(6);

            transaction.query("insert into `mymanytable` (`id`, `parent`, `myothervalue`) values (10, 6, 'my64otherdbvalue')").executeUpdate();

            // This should delete the orphan we just inserted.
            transaction.save(myOneToManyTable);

            MyOneToManyTable myOneToManyTableFetched = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            assertNotNull(myOneToManyTableFetched);
            assertEquals(3, myOneToManyTableFetched.getMyManyTables().size());
        }
    }

    @Test
    public void aggregateSave_withTrackingAndChildrenChanged_deletesOrphanAndInsertsNewChild()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            myOneToManyTable.getMyManyTables().remove(1);

            List<MyThirdTable> myThirdTables = Arrays.asList(
                new MyThirdTable(0, "NewThird1"),
                new MyThirdTable(0, "NewThird2")
            );
            myOneToManyTable.getMyManyTables().add(1, new MyManyTable(null, "NewVal", myThirdTables));

            transaction.save(myOneToManyTable);

            MyOneToManyTable myOneToManyTableFetched = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            assertNotNull(myOneToManyTableFetched);
            assertEquals(3, myOneToManyTableFetched.getMyManyTables().size());

            MyManyTable myManyTableFetched = myOneToManyTableFetched.getMyManyTables().get(2);
            assertEquals("NewVal", myManyTableFetched.getMyOtherValueWithDiffName());
            assertEquals(2, myManyTableFetched.getMyThirdTables().size());
            assertEquals(new Integer(6), myManyTableFetched.getMyThirdTables().get(1).getId());
            assertEquals("NewThird2", myManyTableFetched.getMyThirdTables().get(1).getVal());
        }
    }

    @Test
    public void aggregateSave_withTrackingAndRemoveAfterSave_tracksChangesThroughMultipleSaves()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            List<MyThirdTable> myThirdTables = Arrays.asList(
                new MyThirdTable(0, "NewThird1"),
                new MyThirdTable(0, "NewThird2")
            );
            myOneToManyTable.getMyManyTables().add(1, new MyManyTable(null, "NewVal", myThirdTables));

            transaction.save(myOneToManyTable);

            myOneToManyTable.getMyManyTables().remove(1);
            transaction.save(myOneToManyTable);

            MyOneToManyTable myOneToManyTableFetched = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            assertNotNull(myOneToManyTableFetched);
            assertEquals(
                Arrays.asList(7, 8, 9),
                myOneToManyTableFetched.getMyManyTables().stream().map(MyManyTable::getId).collect(Collectors.toList()));
            assertEquals(
                Arrays.asList("my61otherdbvalue", "my62otherdbvalue", "my63otherdbvalue"),
                myOneToManyTableFetched.getMyManyTables().stream().map(MyManyTable::getMyOtherValueWithDiffName).collect(Collectors.toList()));
        }
    }

    @Test
    public void aggregateSave_withTrackingAndAddBackAfterSave_tracksChangesThroughMultipleSaves()
    {
        registerMyOneToManyTableAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            MyManyTable myManyTable = myOneToManyTable.getMyManyTables().remove(0);
            transaction.save(myOneToManyTable);

            myOneToManyTable.getMyManyTables().add(0, myManyTable);
            transaction.save(myOneToManyTable);

            MyOneToManyTable myOneToManyTableFetched = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            assertNotNull(myOneToManyTableFetched);
            assertEquals(
                Arrays.asList(7, 8, 9),
                myOneToManyTableFetched.getMyManyTables().stream().map(MyManyTable::getId).collect(Collectors.toList()));
            assertEquals(
                Arrays.asList("my61otherdbvalue", "my62otherdbvalue", "my63otherdbvalue"),
                myOneToManyTableFetched.getMyManyTables().stream().map(MyManyTable::getMyOtherValueWithDiffName).collect(Collectors.toList()));
        }
    }

    @Test
    public void aggregateSave_withTrackingAndDelete_tracksDelete()
    {
        registerMyOneToManyTableAggregate();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            MyOneToManyTable myOneToManyTable = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            transaction.delete(myOneToManyTable);
            transaction.delete(myOneToManyTable);

            MyOneToManyTable myOneToManyTableFetched = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            assertNull(myOneToManyTableFetched);

            transaction.save(myOneToManyTable);

            myOneToManyTableFetched = transaction
                .query(MyOneToManyTable.class)
                .fetchById(6);

            assertNotNull(myOneToManyTableFetched);
            assertEquals(
                Arrays.asList(7, 8, 9),
                myOneToManyTableFetched.getMyManyTables().stream().map(MyManyTable::getId).collect(Collectors.toList()));
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
