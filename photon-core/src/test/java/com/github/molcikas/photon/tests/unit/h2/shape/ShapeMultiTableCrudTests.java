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

import static org.junit.Assert.*;

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

    // TODO: Delete orphans, joined table children

    private void registerAggregates()
    {
        photon.registerAggregate(Rectangle.class)
            .withMappedClass(Shape.class)
            .withJoinedTable("Shape")
                .withDatabaseColumn("type")
                .withDatabaseColumn("color")
                .addJoinedTable()
            .register();

        photon.registerAggregate(Circle.class)
            .withMappedClass(Shape.class)
            .withJoinedTable("Shape")
                .withDatabaseColumn("type")
                .withDatabaseColumn("color")
            .addJoinedTable()
            .register();

        photon.registerAggregate(Shape.class)
            .register();
    }
}
