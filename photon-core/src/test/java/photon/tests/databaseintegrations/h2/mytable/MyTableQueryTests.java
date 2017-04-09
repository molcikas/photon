package photon.tests.databaseintegrations.h2.mytable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import photon.Photon;
import photon.PhotonConnection;
import photon.converters.Converter;
import photon.converters.ConverterException;
import photon.exceptions.PhotonException;
import photon.query.PhotonQuery;
import photon.tests.entities.mytable.MyTable;

import java.sql.Types;
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
        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id = :id ";

            MyTable myTable = connection
                .query(sql)
                .addParameter("id", 2)
                .fetch(MyTable.class);

            assertNotNull(myTable);
            assertEquals(2, myTable.getId());
            assertEquals("my2dbvalue", myTable.getMyvalue());
        }
    }

    @Test
    public void query_fetch_customParameterDataType_returnsEntity()
    {
        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id = :id ";

            MyTable myTable = connection
                .query(sql)
                .addParameter("id", "2", Types.INTEGER)
                .fetch(MyTable.class);

            assertNotNull(myTable);
            assertEquals(2, myTable.getId());
            assertEquals("my2dbvalue", myTable.getMyvalue());
        }
    }

    @Test
    public void query_fetch_customToFieldConverter_returnsEntity()
    {
        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id = :id ";

            MyTable myTable = connection
                .query(sql)
                .addParameter("id", "2", Types.INTEGER)
                .withCustomToFieldValueConverter("myvalue", new Converter()
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
        }
    }

    @Test
    public void query_fetchScalar_fetchString_fetchesString()
    {
        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "SELECT myvalue " +
                "FROM mytable " +
                "WHERE id = :id ";

            String myValue = connection
                .query(sql)
                .addParameter("id", 2)
                .fetchScalar(String.class);

            assertEquals("my2dbvalue", myValue);
        }
    }

    @Test
    public void query_fetchScalar_fetchNumber_fetchesNumber()
    {
        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "SELECT COUNT(*) " +
                "FROM mytable ";

            Long count = connection
                .query(sql)
                .fetchScalar(Long.class);

            assertEquals(Long.valueOf(6L), count);
        }
    }

    @Test
    public void query_fetchScalar_noResult_returnsNull()
    {
        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "SELECT myvalue " +
                "FROM mytable " +
                "WHERE id = :id ";

            String myValue = connection
                .query(sql)
                .addParameter("id", 9999)
                .fetchScalar(String.class);

            assertNull(myValue);
        }
    }

    @Test
    public void query_fetchScalarList_fetchStringsList_returnsStringsList()
    {
        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "SELECT myvalue " +
                "FROM mytable " +
                "WHERE id >= :id ";

            List<String> myValues = connection
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
        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "SELECT myvalue " +
                "FROM mytable " +
                "WHERE id >= :id ";

            List<String> myValues = connection
                .query(sql)
                .addParameter("id", 9999)
                .fetchScalarList(String.class);

            assertEquals(0, myValues.size());
        }
    }

    @Test
    public void query_fetchList_simpleEntities_returnsEntities()
    {
        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id IN (2, 4) " +
                "ORDER BY id DESC ";

            List<MyTable> myTables = connection
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
        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id IN (:ids) " +
                "ORDER BY id DESC ";

            List<MyTable> myTables = connection
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
        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id = :id ";

            try
            {
                MyTable myTable = connection
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
        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "UPDATE mytable " +
                "SET myvalue = :newvalue " +
                "WHERE id = :id ";

            int updateCount = connection
                .query(sql)
                .addParameter("id", 2)
                .addParameter("newvalue", "MyNewValue")
                .executeUpdate();

            assertEquals(1, updateCount);
        }

        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id = :id ";

            MyTable myTable = connection
                .query(sql)
                .addParameter("id", 2)
                .fetch(MyTable.class);

            assertNotNull(myTable);
            assertEquals(2, myTable.getId());
            assertEquals("MyNewValue", myTable.getMyvalue());
        }
    }

    @Test
    public void query_insert_insertSimpleEntityWithAutoIncrement_savesEntity()
    {
        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "INSERT INTO mytable (myvalue) " +
                "VALUES ('MyAutoIncrementedInsertedSavedValue') ";

            PhotonQuery query = connection.query(sql);
            int updateCount = query.executeInsert();
            List<Long> newKeys = query.getGeneratedKeys();

            assertEquals(1, updateCount);
            assertEquals(1, newKeys.size());
            assertEquals((Long) 7L, newKeys.get(0));
        }

        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id = :id ";

            MyTable myTable = connection
                .query(sql)
                .addParameter("id", 7)
                .fetch(MyTable.class);

            assertNotNull(myTable);
            assertEquals(7, myTable.getId());
            assertEquals("MyAutoIncrementedInsertedSavedValue", myTable.getMyvalue());
        }
    }

    @Test
    public void query_insert_nullParameter_InsertsValueAsSqlNull()
    {
        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "INSERT INTO mytable VALUES (:id, :myvalue)";

            int rowsInserted = connection
                .query(sql)
                .addParameter("id", 7)
                .addParameter("myvalue", null)
                .executeInsert();

            assertEquals(1, rowsInserted);
        }

        try(PhotonConnection connection = photon.open())
        {
            String sql =
                "SELECT * " +
                "FROM mytable " +
                "WHERE id = 7 ";

            MyTable myTable = connection
                .query(sql)
                .fetch(MyTable.class);

            assertNotNull(myTable);
            assertNull(myTable.getMyvalue());
        }
    }
}
