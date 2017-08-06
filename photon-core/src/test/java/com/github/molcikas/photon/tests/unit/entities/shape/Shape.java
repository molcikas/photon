package com.github.molcikas.photon.tests.unit.entities.shape;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public abstract class Shape
{
    private Integer id;

    private String type;

    private String color;

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

    protected Shape()
    {
    }

    public Shape(Integer id, String type, String color)
    {
        this.id = id;
        this.type = type;
        this.color = color;
    }

    public void setColor(String color)
    {
        this.color = color;
    }
}
