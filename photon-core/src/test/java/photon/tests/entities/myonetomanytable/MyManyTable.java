package photon.tests.entities.myonetomanytable;

import java.util.List;

public class MyManyTable
{
    private Integer id;

    private Integer parent;

    private String myOtherValueWithDiffName;

    private List<MyThirdTable> myThirdTables;

    public Integer getId()
    {
        return id;
    }

    public Integer getParent()
    {
        return parent;
    }

    public String getMyOtherValueWithDiffName()
    {
        return myOtherValueWithDiffName;
    }

    public List<MyThirdTable> getMyThirdTables()
    {
        return myThirdTables;
    }

    private MyManyTable()
    {
    }

    public MyManyTable(Integer parent, String myOtherValueWithDiffName, List<MyThirdTable> myThirdTables)
    {
        this.parent = parent;
        this.myOtherValueWithDiffName = myOtherValueWithDiffName;
        this.myThirdTables = myThirdTables;
    }
}
