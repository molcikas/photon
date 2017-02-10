package photon.blueprints;

import org.apache.commons.lang3.StringUtils;
import photon.exceptions.PhotonException;

import java.util.Arrays;
import java.util.List;

public class ForeignKeyListBlueprint
{
    private final String foreignTableName;
    private final String foreignTableJoinColumnName;
    private final String foreignTableKeyColumnName;
    private final Integer foreignTableKeyColumnType;
    private final Class fieldListItemClass;

    private String selectSql;
    private String insertSql;
    private String deleteSql;

    public String getForeignTableName()
    {
        return foreignTableName;
    }

    public String getForeignTableJoinColumnName()
    {
        return foreignTableJoinColumnName;
    }

    public String getForeignTableKeyColumnName()
    {
        return foreignTableKeyColumnName;
    }

    public Integer getForeignTableKeyColumnType()
    {
        return foreignTableKeyColumnType;
    }

    public Class getFieldListItemClass()
    {
        return fieldListItemClass;
    }

    public String getSelectSql()
    {
        return selectSql;
    }

    public String getInsertSql()
    {
        return insertSql;
    }

    public String getDeleteSql()
    {
        return deleteSql;
    }

    public ForeignKeyListBlueprint(
        String foreignTableName,
        String foreignTableJoinColumnName,
        String foreignTableKeyColumnName,
        Integer foreignTableKeyColumnType,
        Class fieldListItemClass)
    {
        this.foreignTableName = foreignTableName;
        this.foreignTableJoinColumnName = foreignTableJoinColumnName;
        this.foreignTableKeyColumnName = foreignTableKeyColumnName;
        this.foreignTableKeyColumnType = foreignTableKeyColumnType;
        this.fieldListItemClass = fieldListItemClass;
    }

    public List<String> getSelectColumnNames()
    {
        return Arrays.asList(foreignTableKeyColumnName, foreignTableJoinColumnName);
    }

    public void setSelectSql(String selectSql)
    {
        if(StringUtils.isBlank(selectSql))
        {
            throw new PhotonException("Select SQL cannot be blank.");
        }
        this.selectSql = selectSql;
    }

    public void setInsertSql(String insertSql)
    {
        if(StringUtils.isBlank(insertSql))
        {
            throw new PhotonException("Insert SQL cannot be blank.");
        }
        this.insertSql = insertSql;
    }

    public void setDeleteSql(String deleteSql)
    {
        if(StringUtils.isBlank(deleteSql))
        {
            throw new PhotonException("Delete SQL cannot be blank.");
        }
        this.deleteSql = deleteSql;
    }
}
