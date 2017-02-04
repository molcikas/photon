package photon.query;

public class PhotonSqlParameter
{
    private final int index;
    private final String name;
    private Object value;

    public int getIndex()
    {
        return index;
    }

    public String getName()
    {
        return name;
    }

    public Object getValue()
    {
        return value;
    }

    public PhotonSqlParameter(int index, String name)
    {
        this.index = index;
        this.name = name;
    }

    public void assignValue(Object value)
    {
        this.value = value;
    }
}
