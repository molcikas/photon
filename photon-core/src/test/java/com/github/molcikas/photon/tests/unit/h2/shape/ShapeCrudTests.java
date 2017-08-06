package com.github.molcikas.photon.tests.unit.h2.shape;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.tests.unit.entities.shape.Circle;
import com.github.molcikas.photon.tests.unit.entities.shape.Rectangle;
import com.github.molcikas.photon.tests.unit.entities.shape.Shape;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ShapeCrudTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = ShapeDbSetup.setupDatabase();
    }

    @Test
    public void withMappedClassAndNoClassDiscriminator_selectExistingRow_createsAggregate()
    {
        photon.registerAggregate(Circle.class)
            .withMappedClass(Shape.class)
            .withTableName("shape")
            .register();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Circle circle = transaction
                .query(Circle.class)
                .fetchById(1);

            assertNotNull(circle);
            assertEquals(Circle.class, circle.getClass());
            assertEquals(Integer.valueOf(1), circle.getId());
            assertEquals("red", circle.getColor());
            assertEquals(3, circle.getRadius());
        }
    }



    @Test
    public void withMappedClass_selectExistingRow_createsAggregate()
    {
        registerAggregate();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Shape shape = transaction
                .query(Shape.class)
                .fetchById(1);

            assertNotNull(shape);
            assertEquals(Circle.class, shape.getClass());
            assertEquals(Integer.valueOf(1), shape.getId());
            assertEquals("red", shape.getColor());
            assertEquals(3, ((Circle) shape).getRadius());
        }
    }

    @Test
    public void withMappedClass_insertAndSelectAggregate_createsAndFetchesAggregate()
    {
        registerAggregate();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Circle circle = new Circle(2, "blue", 4);

            transaction.save(circle);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Shape shape = transaction
                .query(Shape.class)
                .fetchById(2);

            assertNotNull(shape);
            assertEquals(Circle.class, shape.getClass());
            assertEquals(Integer.valueOf(2), shape.getId());
            assertEquals("blue", shape.getColor());
            assertEquals(4, ((Circle) shape).getRadius());
        }
    }

    @Test
    public void withMappedClass_saveMultipleInstancesOfDifferentTypes_createsAndFetchesAggregate()
    {
        registerAggregate();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Circle circle = new Circle(null, "green", 4);
            Rectangle rectangle = new Rectangle(null, "orange", 5, 6, null);

            transaction.saveAll(circle, rectangle);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            List<Shape> shapes = transaction
                .query(Shape.class)
                .fetchByIds(3, 4);

            assertEquals(2, shapes.size());

            Circle circle = (Circle) shapes.get(0);
            assertEquals(Integer.valueOf(3), circle.getId());
            assertEquals("green", circle.getColor());
            assertEquals(4, circle.getRadius());

            Rectangle rectangle = (Rectangle) shapes.get(1);
            assertEquals(Integer.valueOf(4), rectangle.getId());
            assertEquals("orange", rectangle.getColor());
            assertEquals(5, rectangle.getWidth());
            assertEquals(6, rectangle.getHeight());
        }
    }

    @Test
    public void withMappedClassAndSomeFieldsIgnored_selectExistingRow_createsAggregate()
    {
        photon.registerAggregate(Shape.class)
            .withMappedClass(Circle.class)
            .withMappedClass(Rectangle.class, Collections.singletonList("height"))
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
            .register();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Shape shape = transaction
                .query(Shape.class)
                .fetchById(2);

            assertNotNull(shape);
            assertEquals(Rectangle.class, shape.getClass());
            assertEquals(Integer.valueOf(2), shape.getId());
            assertEquals("blue", shape.getColor());
            assertEquals(0, ((Rectangle) shape).getWidth());
            assertEquals(8, ((Rectangle) shape).getHeight());
        }
    }

    @Test
    public void withMappedClass_updateExistingRow_updatesAggregate()
    {
        registerAggregate();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Shape shape = transaction
                .query(Shape.class)
                .fetchById(1);

            shape.setColor("orange");

            transaction.save(shape);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Shape shape = transaction
                .query(Shape.class)
                .fetchById(1);

            assertNotNull(shape);
            assertEquals(Circle.class, shape.getClass());
            assertEquals(Integer.valueOf(1), shape.getId());
            assertEquals("orange", shape.getColor());
            assertEquals(3, ((Circle) shape).getRadius());
        }
    }

    @Test
    public void withMappedClass_deleteExistingRow_deletesAggregate()
    {
        registerAggregate();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Shape shape = transaction
                .query(Shape.class)
                .fetchById(1);

            transaction.delete(shape);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Shape shape = transaction
                .query(Shape.class)
                .fetchById(1);

            assertNull(shape);
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
            .register();
    }
}
