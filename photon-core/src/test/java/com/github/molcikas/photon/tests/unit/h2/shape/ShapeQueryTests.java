package com.github.molcikas.photon.tests.unit.h2.shape;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.tests.unit.entities.shape.Circle;
import com.github.molcikas.photon.tests.unit.entities.shape.Rectangle;
import com.github.molcikas.photon.tests.unit.entities.shape.Shape;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

public class ShapeQueryTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = ShapeDbSetup.setupDatabase();
    }

    @Test
    public void withMappedClass_selectExistingRow_fetchesObject()
    {
        registerAggregate();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Circle circle = transaction
                .query("SELECT * FROM shape WHERE id = 1")
                .withMappedClass(Shape.class)
                .fetch(Circle.class);

            assertNotNull(circle);
            assertEquals(Integer.valueOf(1), circle.getId());
            assertEquals("red", circle.getColor());
            assertEquals(3, circle.getRadius());
        }
    }

    @Test
    public void withMappedClass_selectExistingRows_fetchesList()
    {
        registerAggregate();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            List<Shape> shapes = transaction
                .query("SELECT * FROM shape WHERE id IN (1, 2)")
                .withMappedClass(Circle.class)
                .withMappedClass(Rectangle.class)
                .withClassDiscriminator(valuesMap ->
                {
                    String type = (String) valuesMap.get("type");
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
                .fetchList(Shape.class);

            assertEquals(2, shapes.size());

            Circle circle = (Circle) shapes.get(0);
            assertEquals(Integer.valueOf(1), circle.getId());
            assertEquals("red", circle.getColor());
            assertEquals(3, circle.getRadius());

            Rectangle rectangle = (Rectangle) shapes.get(1);
            assertEquals(Integer.valueOf(2), rectangle.getId());
            assertEquals("blue", rectangle.getColor());
            assertEquals(7, rectangle.getWidth());
            assertEquals(8, rectangle.getHeight());
        }
    }

    private void registerAggregate()
    {
        photon.registerAggregate(Shape.class)
            .withMappedClass(Circle.class)
            .withMappedClass(Rectangle.class)
            .withClassDiscriminator(valuesMap ->
            {
                String type = (String) valuesMap.get("type");
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
            .register();
    }

// TODO FUTURE?:
//        photon.registerAggregate(Shape.class)
//            .isMappedToTable(false)
//            .withUnion(Circle.class)
//                .withId("id")
//                .addUnion()
//            .withUnion(Rectangle.class)
//                .withId("id")
//                .addUnion()
//            .register();

}
