package photon.tests.entities.myonetomanytable;

public class MyManyTable
{
    private Integer parent;

    private String myOtherValueWithDiffName;

    public Integer getParent()
    {
        return parent;
    }

    public String getMyOtherValueWithDiffName()
    {
        return myOtherValueWithDiffName;
    }

    private MyManyTable()
    {
    }

    public MyManyTable(Integer parent, String myOtherValueWithDiffName)
    {
        this.parent = parent;
        this.myOtherValueWithDiffName = myOtherValueWithDiffName;
    }
}
