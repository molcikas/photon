package photon.query;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PhotonQueryResultRow
{
    private final Map<String, Object> values;
    private Object firstValue;

    public void addValue(String columnName, Object value)
    {
        if(values.isEmpty())
        {
            firstValue = value;
        }
        values.put(columnName, value);
    }

    public Object getValue(String columnName)
    {
        return values.get(columnName);
    }

    public Set<Map.Entry<String, Object>> getValues()
    {
        return values.entrySet();
    }

    public Object getFirstValue()
    {
        return firstValue;
    }

    public PhotonQueryResultRow()
    {
        values = new HashMap<>();
    }
}
