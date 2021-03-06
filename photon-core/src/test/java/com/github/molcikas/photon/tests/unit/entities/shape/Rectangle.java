package com.github.molcikas.photon.tests.unit.entities.shape;

import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class Rectangle extends Shape
{
    private int width;

    private int height;

    private List<CornerCoordinates> corners;

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    public List<CornerCoordinates> getCorners()
    {
        return corners;
    }

    private Rectangle()
    {
    }

    public Rectangle(Integer id, String color, Integer drawingId, int width, int height, List<CornerCoordinates> corners)
    {
        super(id, "rectangle", color, drawingId);
        this.width = width;
        this.height = height;
        this.corners = corners;
    }
}
