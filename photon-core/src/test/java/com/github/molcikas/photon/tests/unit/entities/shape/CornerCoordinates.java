package com.github.molcikas.photon.tests.unit.entities.shape;

public class CornerCoordinates
{
    private Integer id;

    private Integer x;

    private Integer y;

    public Integer getId()
    {
        return id;
    }

    public Integer getX()
    {
        return x;
    }

    public Integer getY()
    {
        return y;
    }

    private CornerCoordinates()
    {
    }

    public CornerCoordinates(Integer id, Integer x, Integer y)
    {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public void setX(Integer x)
    {
        this.x = x;
    }
}
