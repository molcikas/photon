package com.github.molcikas.photon.tests.unit.entities.shape;

import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode
public class Shape
{
    private Integer id;

    private Integer drawingId;

    private String type;

    private String color;

    private List<ShapeColorHistory> colorHistory;

    public Integer getId()
    {
        return id;
    }

    public String getType()
    {
        return type;
    }

    public String getColor()
    {
        return color;
    }

    public List<ShapeColorHistory> getColorHistory()
    {
        return colorHistory;
    }

    protected Shape()
    {
    }

    public Shape(Integer id, String type, String color, Integer drawingId)
    {
        this.id = id;
        this.type = type;
        this.color = color;
        this.drawingId = drawingId;
    }


    public Shape(Integer id,
                 String type,
                 String color,
                 Integer drawingId,
                 List<ShapeColorHistory> colorHistory)
    {
        this.id = id;
        this.type = type;
        this.color = color;
        this.drawingId = drawingId;
        this.colorHistory = colorHistory;
    }

    public void setColor(String color)
    {
        this.color = color;
    }
}
