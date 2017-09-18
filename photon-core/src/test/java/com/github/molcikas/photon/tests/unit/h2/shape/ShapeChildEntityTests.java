package com.github.molcikas.photon.tests.unit.h2.shape;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.blueprints.table.ColumnDataType;
import com.github.molcikas.photon.tests.unit.entities.shape.Circle;
import com.github.molcikas.photon.tests.unit.entities.shape.CornerCoordinates;
import com.github.molcikas.photon.tests.unit.entities.shape.Rectangle;
import com.github.molcikas.photon.tests.unit.entities.shape.Shape;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ShapeChildEntityTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = ShapeDbSetup.setupDatabase();
    }

    @Test
    public void withMappedClass_selectExistingRows_createsAggregate()
    {
        registerAggregate();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Rectangle rectangle = transaction
                .query(Rectangle.class)
                .fetchById(2);

            assertNotNull(rectangle);
            assertEquals(Integer.valueOf(2), rectangle.getId());
            assertEquals("blue", rectangle.getColor());

            assertEquals(4, rectangle.getCorners().size());
            assertEquals(Integer.valueOf(0), rectangle.getCorners().get(0).getX());
            assertEquals(Integer.valueOf(7), rectangle.getCorners().get(1).getX());
            assertEquals(Integer.valueOf(8), rectangle.getCorners().get(3).getY());
        }
    }

    @Test
    public void withMappedClass_updateExistingRows_updatesAggregate()
    {
        registerAggregate();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Rectangle rectangle = transaction
                .query(Rectangle.class)
                .fetchById(2);

            rectangle.getCorners().remove(1);
            rectangle.getCorners().get(2).setX(3);
            transaction.save(rectangle);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Rectangle rectangle = transaction
                .query(Rectangle.class)
                .fetchById(2);

            assertEquals(3, rectangle.getCorners().size());
            assertEquals(Integer.valueOf(0), rectangle.getCorners().get(0).getX());
            assertEquals(Integer.valueOf(8), rectangle.getCorners().get(1).getY());
            assertEquals(Integer.valueOf(3), rectangle.getCorners().get(2).getX());
        }
    }

    private void registerAggregate()
    {
        photon.registerAggregate(Shape.class)
            .withMappedClass(Circle.class)
            .withMappedClass(Rectangle.class)
            .withClassDiscriminator(valuesMap ->
            {
                String type = (String) valuesMap.get("Shape_type");
                switch (type)
                {
                    case "circle":
                        return Circle.class;
                    case "rectangle":
                        return Rectangle.class;
                    default:
                        return null;
                }
            })
            .withChild("corners", CornerCoordinates.class)
                .withForeignKeyToParent("shapeId", ColumnDataType.INTEGER)
                .addAsChild()
            .register();
    }

}
