package com.github.molcikas.photon.tests.unit.entities.mytable;

public class MyOtherTable
{
    private int id;

    private String myOtherValueWithDiffName;

    public int getId()
    {
        return id;
    }

    public String getMyOtherValueWithDiffName()
    {
        return myOtherValueWithDiffName;
    }

    private MyOtherTable()
    {
    }

    public MyOtherTable(int id, String myOtherValueWithDiffName)
    {
        this.id = id;
        this.myOtherValueWithDiffName = myOtherValueWithDiffName;
    }
}
