package com.github.molcikas.photon.tests.unit.h2.mytable;

import com.github.molcikas.photon.blueprints.table.ColumnDataType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.converters.ConverterException;
import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.query.PhotonQuery;
import com.github.molcikas.photon.tests.unit.entities.mytable.MyTable;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class MyTableQueryTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = MyTableDbSetup.setupDatabase();
    }

    @Test
    public void query_fetch_simpleEntity_returnsEntity()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id = :id ";

            MyTable myTable = transaction
                .query(sql)
                .addParameter("id", 2)
                .fetch(MyTable.class);

            assertNotNull(myTable);
            assertEquals(2, myTable.getId());
            assertEquals("my2dbvalue", myTable.getMyvalue());

            transaction.commit();
        }
    }

    @Test
    public void query_fetch_sameParameterTwice_returnsEntity()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id >= :id AND myvalue = :myvalue AND id <= :id ";

            MyTable myTable = transaction
                .query(sql)
                .addParameter("id", 2)
                .addParameter("myvalue", "my2dbvalue")
                .fetch(MyTable.class);

            assertNotNull(myTable);
            assertEquals(2, myTable.getId());
            assertEquals("my2dbvalue", myTable.getMyvalue());

            transaction.commit();
        }
    }

    @Test
    public void query_fetch_customParameterDataType_returnsEntity()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id = :id ";

            MyTable myTable = transaction
                .query(sql)
                .addParameter("id", "2", ColumnDataType.INTEGER)
                .fetch(MyTable.class);

            assertNotNull(myTable);
            assertEquals(2, myTable.getId());
            assertEquals("my2dbvalue", myTable.getMyvalue());

            transaction.commit();
        }
    }

    @Test
    public void query_fetch_customFieldHydrater_returnsEntity()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id = :id ";

            MyTable myTable = transaction
                .query(sql)
                .addParameter("id", "2", ColumnDataType.INTEGER)
                .withFieldHydrater("myvalue", new Converter()
                {
                    @Override
                    public Object convert(Object val) throws ConverterException
                    {
                        return ((String) val).toUpperCase();
                    }
                })
                .fetch(MyTable.class);

            assertNotNull(myTable);
            assertEquals(2, myTable.getId());
            assertEquals("MY2DBVALUE", myTable.getMyvalue());

            transaction.commit();
        }
    }

    @Test
    public void query_fetchScalar_fetchString_fetchesString()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT myvalue " +
                "FROM mytable " +
                "WHERE id = :id ";

            String myValue = transaction
                .query(sql)
                .addParameter("id", 2)
                .fetchScalar(String.class);

            assertEquals("my2dbvalue", myValue);

            transaction.commit();
        }
    }

    @Test
    public void query_fetchScalar_fetchNumber_fetchesNumber()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT COUNT(*) " +
                "FROM mytable ";

            Long count = transaction
                .query(sql)
                .fetchScalar(Long.class);

            assertEquals(Long.valueOf(6L), count);

            transaction.commit();
        }
    }

    @Test
    public void query_fetchScalar_noResult_returnsNull()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT myvalue " +
                "FROM mytable " +
                "WHERE id = :id ";

            String myValue = transaction
                .query(sql)
                .addParameter("id", 9999)
                .fetchScalar(String.class);

            assertNull(myValue);
        }
    }

    @Test
    public void query_fetchScalarList_fetchStringsList_returnsStringsList()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT myvalue " +
                "FROM mytable " +
                "WHERE id >= :id ";

            List<String> myValues = transaction
                .query(sql)
                .addParameter("id", 3)
                .fetchScalarList(String.class);

            assertEquals(4, myValues.size());
            assertEquals("my3dbvalue", myValues.get(0));
            assertEquals("my5dbvalue", myValues.get(2));
        }
    }

    @Test
    public void query_fetchScalarList_noResults_returnsEmptyList()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT myvalue " +
                "FROM mytable " +
                "WHERE id >= :id ";

            List<String> myValues = transaction
                .query(sql)
                .addParameter("id", 9999)
                .fetchScalarList(String.class);

            assertEquals(0, myValues.size());
        }
    }

    @Test
    public void query_fetchList_simpleEntities_returnsEntities()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id IN (2, 4) " +
                "ORDER BY id DESC ";

            List<MyTable> myTables = transaction
                .query(sql)
                .fetchList(MyTable.class);

            assertNotNull(myTables);
            assertEquals(2, myTables.size());
            assertEquals(4, myTables.get(0).getId());
            assertEquals("my4dbvalue", myTables.get(0).getMyvalue());
            assertEquals(2, myTables.get(1).getId());
            assertEquals("my2dbvalue", myTables.get(1).getMyvalue());
        }
    }

    @Test
    public void query_fetchListWithListParameter_simpleEntities_returnsEntities()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT id, myvalue " +
                "FROM mytable " +
                "WHERE id IN (:ids) " +
                "ORDER BY id DESC ";

            List<MyTable> myTables = transaction
                .query(sql)
                .addParameter("ids", Arrays.asList(2, 4))
                .fetchList(MyTable.class);

            assertNotNull(myTables);
            assertEquals(2, myTables.size());
            assertEquals(4, myTables.get(0).getId());
            assertEquals("my4dbvalue", myTables.get(0).getMyvalue());
            assertEquals(2, myTables.get(1).getId());
            assertEquals("my2dbvalue", myTables.get(1).getMyvalue());
        }
    }

    @Test
    public void query_fetch_parameterNotInList_throwsException()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id = :id ";

            try
            {
                MyTable myTable = transaction
                    .query(sql)
                    .addParameter("NotARealParameter", 2)
                    .fetch(MyTable.class);

                Assert.fail("Failed to throw PhotonException");
            }
            catch(PhotonException ex)
            {
                assertTrue(ex.getMessage().toLowerCase().contains("parameter"));
            }
        }
    }

    @Test
    public void query_update_simpleEntity_updatesEntity()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "UPDATE mytable " +
                "SET myvalue = :newvalue " +
                "WHERE id = :id ";

            int updateCount = transaction
                .query(sql)
                .addParameter("id", 2)
                .addParameter("newvalue", "MyNewValue")
                .executeUpdate();

            assertEquals(1, updateCount);

            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id = :id ";

            MyTable myTable = transaction
                .query(sql)
                .addParameter("id", 2)
                .fetch(MyTable.class);

            assertNotNull(myTable);
            assertEquals(2, myTable.getId());
            assertEquals("MyNewValue", myTable.getMyvalue());

            transaction.commit();
        }
    }

    @Test
    public void query_insert_insertSimpleEntityWithAutoIncrement_savesEntity()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "INSERT INTO mytable (myvalue) " +
                "VALUES ('MyAutoIncrementedInsertedSavedValue') ";

            PhotonQuery query = transaction.query(sql, true);
            int updateCount = query.executeInsert();
            List<Long> newKeys = query.getGeneratedKeys();

            assertEquals(1, updateCount);
            assertEquals(1, newKeys.size());
            assertEquals((Long) 7L, newKeys.get(0));

            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id = :id ";

            MyTable myTable = transaction
                .query(sql)
                .addParameter("id", 7)
                .fetch(MyTable.class);

            assertNotNull(myTable);
            assertEquals(7, myTable.getId());
            assertEquals("MyAutoIncrementedInsertedSavedValue", myTable.getMyvalue());

            transaction.commit();
        }
    }

    @Test
    public void query_insert_nullParameter_InsertsValueAsSqlNull()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "INSERT INTO mytable (id, myvalue) VALUES (:id, :myvalue)";

            int rowsInserted = transaction
                .query(sql)
                .addParameter("id", 7)
                .addParameter("myvalue", null)
                .executeInsert();

            assertEquals(1, rowsInserted);

            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id = 7 ";

            MyTable myTable = transaction
                .query(sql)
                .fetch(MyTable.class);

            assertNotNull(myTable);
            assertNull(myTable.getMyvalue());

            transaction.commit();
        }
    }

    private static class MyTableInnerClass
    {
        private Long id;

        private String myvalue;

        public Long getId()
        {
            return id;
        }

        public String getMyvalue()
        {
            return myvalue;
        }

        protected MyTableInnerClass()
        {
        }
    }

    @Test
    public void query_innerClass_constructsObject()
    {
        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id = 1 ";

            MyTableInnerClass myTableInnerClass = transaction
                .query(sql)
                .fetch(MyTableInnerClass.class);

            assertNotNull(myTableInnerClass);
            assertEquals(new Long(1L), myTableInnerClass.getId());
            assertEquals("my1dbvalue", myTableInnerClass.getMyvalue());

            transaction.commit();
        }
    }
}
