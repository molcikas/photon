package com.github.molcikas.photon.tests.unit.entities.myonetomanytable;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
public class MyOneToManyMapTable
{
    @Getter
    private Integer myOneToManyMapTableId;

    @Getter
    private String myvalue;

    @Getter
    private Map<Integer, MyManyTable> myManyTables;

    private MyOneToManyMapTable()
    {
    }

    public void setMyvalue(String myvalue)
    {
        this.myvalue = myvalue;
    }
}
