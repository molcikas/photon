package com.github.molcikas.photon.tests.unit.entities.shape;

import java.util.List;

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

    public Circle(Integer id, String color, Integer drawingId, int radius)
    {
        super(id, "circle", color, drawingId);
        this.radius = radius;
    }

    public Circle(
        Integer id,
        String color,
        Integer drawingId,
        int radius,
        List<ShapeColorHistory> colorHistory)
    {
        super(id, "circle", color, drawingId, colorHistory);
        this.radius = radius;
    }

    public void setRadius(int radius)
    {
        this.radius = radius;
    }
}
