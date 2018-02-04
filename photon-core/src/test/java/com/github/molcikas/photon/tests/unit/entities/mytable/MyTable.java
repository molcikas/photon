package com.github.molcikas.photon.tests.unit.entities.mytable;

public class MyTable
{
    private Integer id;
    private String myvalue;
    private int version;

    private MyOtherTable myOtherTable;

    public Integer getId()
    {
        return id;
    }

    public String getMyvalue()
    {
        return myvalue;
    }

    public int getVersion()
    {
        return version;
    }

    public MyOtherTable getMyOtherTable()
    {
        return myOtherTable;
    }

    private MyTable()
    {
    }

    public MyTable(Integer id, String myvalue, MyOtherTable myOtherTable)
    {
        this.id = id;
        this.myvalue = myvalue;
        this.version = 1;
        this.myOtherTable = myOtherTable;
    }

    public void setMyvalue(String myvalue)
    {
        this.myvalue = myvalue;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }
}
