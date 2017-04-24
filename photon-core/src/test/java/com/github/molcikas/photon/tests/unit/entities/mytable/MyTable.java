package com.github.molcikas.photon.tests.unit.entities.mytable;

public class MyTable
{
    private int id;
    private String myvalue;
    private MyOtherTable myOtherTable;

    public int getId()
    {
        return id;
    }

    public String getMyvalue()
    {
        return myvalue;
    }

    public MyOtherTable getMyOtherTable()
    {
        return myOtherTable;
    }

    private MyTable()
    {
    }

    public MyTable(int id, String myvalue, MyOtherTable myOtherTable)
    {
        this.id = id;
        this.myvalue = myvalue;
        this.myOtherTable = myOtherTable;
    }
}
