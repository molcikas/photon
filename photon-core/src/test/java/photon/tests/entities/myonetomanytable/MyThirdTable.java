package photon.tests.entities.myonetomanytable;

public class MyThirdTable
{
    private Integer id;

    private Integer parent;

    private String val;

    public Integer getId()
    {
        return id;
    }

    public Integer getParent()
    {
        return parent;
    }

    public String getVal()
    {
        return val;
    }

    private MyThirdTable()
    {
    }

    public MyThirdTable(Integer parent, String val)
    {
        this.parent = parent;
        this.val = val;
    }
}
