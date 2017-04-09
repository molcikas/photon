package photon.query;

import photon.blueprints.EntityBlueprintConstructorService;

public class PhotonSqlParameter
{
    private final int index;
    private final String name;
    private Object value;
    private Integer dataType;

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

    public Integer getDataType()
    {
        return dataType;
    }

    public PhotonSqlParameter(int index, String name)
    {
        this.index = index;
        this.name = name;
    }

    public void assignValue(Object value)
    {
        this.value = value;
        this.dataType = value != null ? EntityBlueprintConstructorService.defaultColumnDataTypeForField(value.getClass()) : null;
    }

    public void assignValue(Object value, Integer dataType)
    {
        this.value = value;
        this.dataType = dataType;
    }
}
