package com.github.molcikas.photon.tests.unit.entities.shape;

import java.time.ZonedDateTime;

public class ShapeColorHistory
{
    private int id;

    private int shapeId;

    private ZonedDateTime dateChanged;

    private String colorName;

    public int getId()
    {
        return id;
    }

    public int getShapeId()
    {
        return shapeId;
    }

    public ZonedDateTime getDateChanged()
    {
        return dateChanged;
    }

    public String getColorName()
    {
        return colorName;
    }

    private ShapeColorHistory()
    {
    }

    public ShapeColorHistory(int id, int shapeId, ZonedDateTime dateChanged, String colorName)
    {
        this.id = id;
        this.shapeId = shapeId;
        this.dateChanged = dateChanged;
        this.colorName = colorName;
    }

    public void setColorName(String colorName)
    {
        this.colorName = colorName;
    }
}
