package photon.tests.entities.myonetomanytable;

import java.util.List;

public class MyOneToManyTable
{
    private Integer id;
    private String myvalue;
    private List<MyManyTable> myManyTables;

    public Integer getId()
    {
        return id;
    }

    public String getMyvalue()
    {
        return myvalue;
    }

    public List<MyManyTable> getMyManyTables()
    {
        return myManyTables;
    }

    private MyOneToManyTable()
    {
    }

    public MyOneToManyTable(Integer id, String myvalue, List<MyManyTable> myManyTables)
    {
        this.id = id;
        this.myvalue = myvalue;
        this.myManyTables = myManyTables;
    }
}
