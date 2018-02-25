package com.github.molcikas.photon.blueprints.entity;

import com.github.molcikas.photon.blueprints.table.ColumnDataType;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.exceptions.PhotonException;

import java.util.Arrays;
import java.util.List;

public class FlattenedCollectionBlueprint
{
    @Getter
    private final Class fieldClass;

    @Getter
    private final String tableName;

    @Getter
    private final String foreignKeyToParent;

    @Getter
    private final String foreignKeyToParentLowerCase;

    @Getter
    private final String columnName;

    @Getter
    private final String columnNameLowerCase;

    @Getter
    private final ColumnDataType columnDataType;

    @Getter
    private String selectSql;

    @Getter
    private String insertSql;

    @Getter
    private String deleteSql;

    @Getter
    private String deleteForeignKeysSql;

    public FlattenedCollectionBlueprint(
        Class fieldClass,
        String tableName,
        String foreignKeyToParent,
        String columnName,
        ColumnDataType columnDataType)
    {
        this.fieldClass = fieldClass;
        this.tableName = tableName;
        this.foreignKeyToParent = foreignKeyToParent;
        this.foreignKeyToParentLowerCase = foreignKeyToParent.toLowerCase();
        this.columnName = columnName;
        this.columnNameLowerCase = columnName.toLowerCase();
        this.columnDataType = columnDataType;
    }

    public List<String> getSelectColumnNames()
    {
        return Arrays.asList(columnName, foreignKeyToParent);
    }

    public List<String> getSelectColumnNamesLowerCase()
    {
        return Arrays.asList(columnNameLowerCase, foreignKeyToParentLowerCase);
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

    public void setDeleteForeignKeysSql(String deleteForeignKeysSql)
    {
        if(StringUtils.isBlank(deleteForeignKeysSql))
        {
            throw new PhotonException("Delete Foreign Keys SQL cannot be blank.");
        }
        this.deleteForeignKeysSql = deleteForeignKeysSql;
    }
}
