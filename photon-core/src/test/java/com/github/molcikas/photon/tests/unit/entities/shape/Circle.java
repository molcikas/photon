package com.github.molcikas.photon.tests.unit.entities.shape;

public class Circle extends Shape
{
    private int radius;

    public int getRadius()
    {
        return radius;
    }

    private Circle()
    {
    }

    public Circle(Integer id, String color, int radius)
    {
        super(id, "circle", color);
        this.radius = radius;
    }
}
