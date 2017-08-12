package com.github.molcikas.photon.tests.unit.h2.shape;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.tests.unit.entities.shape.Circle;
import com.github.molcikas.photon.tests.unit.entities.shape.Rectangle;
import com.github.molcikas.photon.tests.unit.entities.shape.Shape;
import com.github.molcikas.photon.tests.unit.entities.shape.ShapeColorHistory;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class ShapeMultiTableCrudTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = ShapeMultiTableDbSetup.setupDatabase();
    }

    @Test
    public void withJoinedTable_selectExistingRow_createsAggregate()
    {
        registerAggregates();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            Rectangle rectangle = transaction.query(Rectangle.class).fetchById(2);

            Rectangle expected = new Rectangle(2, "blue", 7, 8, null);
            assertEquals(expected, rectangle);
        }
    }

    @Test
    public void withJoinedTable_insertAndSelectAggregate_createsAndFetchesAggregate()
    {
        registerAggregates();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Circle circle = new Circle(3, "blue", 4);

            transaction.save(circle);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Circle circle = transaction
                .query(Circle.class)
                .fetchById(3);

            assertNotNull(circle);
            assertEquals(Circle.class, circle.getClass());
            assertEquals(Integer.valueOf(3), circle.getId());
            assertEquals("blue", circle.getColor());
            assertEquals(4, circle.getRadius());
        }
    }

    @Test
    public void withJoinedTable_updateExistingRow_updatesAggregate()
    {
        registerAggregates();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Circle circle = transaction
                .query(Circle.class)
                .fetchById(1);

            circle.setColor("orange");

            transaction.save(circle);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Circle circle = transaction
                .query(Circle.class)
                .fetchById(1);

            assertNotNull(circle);
            assertEquals(Circle.class, circle.getClass());
            assertEquals(Integer.valueOf(1), circle.getId());
            assertEquals("orange", circle.getColor());
            assertEquals(3, circle.getRadius());
        }
    }

    @Test
    public void withJoinedTable_deleteExistingRow_deletesAggregate()
    {
        registerAggregates();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Circle circle = transaction
                .query(Circle.class)
                .fetchById(1);

            transaction.delete(circle);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Circle circle = transaction
                .query(Circle.class)
                .fetchById(1);

            assertNull(circle);

            Shape shape = transaction
                .query(Shape.class)
                .fetchById(1);

            assertNull(shape);
        }
    }

    @Test
    public void withJoinedTable_deleteExistingRowWithChildren_deletesAggregateAndChildren()
    {
        registerAggregatesWithShapeColorHistory();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            int count = transaction
                .query("SELECT COUNT(*) FROM shapecolorhistory WHERE shapeId = 3")
                .fetchScalar(Integer.class);
            assertEquals(2, count);

            Circle circle = transaction
                .query(Circle.class)
                .fetchById(3);

            transaction.delete(circle);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Circle circle = transaction
                .query(Circle.class)
                .fetchById(3);
            assertNull(circle);

            Shape shape = transaction
                .query(Shape.class)
                .fetchById(3);
            assertNull(shape);

            int count = transaction
                .query("SELECT COUNT(*) FROM shapecolorhistory WHERE shapeId = 3")
                .fetchScalar(Integer.class);
            assertEquals(0, count);
        }
    }

    @Test
    public void withJoinedTable_removeChild_updatesAggregateAndRemovesChild()
    {
        registerAggregatesWithShapeColorHistory();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Circle circle = transaction
                .query(Circle.class)
                .fetchById(3);

            circle.setRadius(333);

            circle.getColorHistory().remove(0);
            circle.getColorHistory().get(0).setColorName("bluegreen");
            circle.getColorHistory().add(new ShapeColorHistory(0, 0, ZonedDateTime.now(), "black"));

            transaction.save(circle);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Circle circle = transaction
                .query(Circle.class)
                .fetchById(3);

            assertNotNull(circle);
            assertEquals(333, circle.getRadius());
            assertEquals(2, circle.getColorHistory().size());
            assertEquals(2, circle.getColorHistory().get(0).getId());
            assertEquals("bluegreen", circle.getColorHistory().get(0).getColorName());
            assertEquals(3, circle.getColorHistory().get(1).getId());
            assertEquals("black", circle.getColorHistory().get(1).getColorName());
        }
    }

    // TODO: Drawing that contains shapes of different types. Will need to add left joining.
    // TODO: Allow joined tables to specify parent (for multi-level inheritance).

    private void registerAggregates()
    {
        photon.registerAggregate(Rectangle.class)
            .withPrimaryKeyAutoIncrement()
            .withMappedClass(Shape.class)
            .withJoinedTable("Shape")
                .withPrimaryKeyAutoIncrement()
                .withDatabaseColumn("type")
                .withDatabaseColumn("color")
                .addJoinedTable()
            .register();

        photon.registerAggregate(Circle.class)
            .withPrimaryKeyAutoIncrement()
            .withMappedClass(Shape.class)
            .withJoinedTable("Shape")
                .withPrimaryKeyAutoIncrement()
                .withDatabaseColumn("type")
                .withDatabaseColumn("color")
            .addJoinedTable()
            .register();

        photon.registerAggregate(Shape.class)
            .withPrimaryKeyAutoIncrement()
            .register();
    }

    private void registerAggregatesWithShapeColorHistory()
    {
        photon.registerAggregate(Rectangle.class)
            .withPrimaryKeyAutoIncrement()
            .withMappedClass(Shape.class)
            .withJoinedTable("Shape")
                .withPrimaryKeyAutoIncrement()
                .withDatabaseColumn("type")
                .withDatabaseColumn("color")
                .addJoinedTable()
            .register();

        photon.registerAggregate(Circle.class)
            .withPrimaryKeyAutoIncrement()
            .withMappedClass(Shape.class)
            .withJoinedTable("Shape")
                .withPrimaryKeyAutoIncrement()
                .withDatabaseColumn("type")
                .withDatabaseColumn("color")
                .addJoinedTable()
            .withChild(ShapeColorHistory.class)
                .withPrimaryKeyAutoIncrement()
                .withParentTable("Shape", "shapeId")
                .addAsChild("colorHistory")
            .register();

        photon.registerAggregate(Shape.class)
            .register();
    }
}
