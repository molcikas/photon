package com.github.molcikas.photon.tests.unit.h2.shape;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.blueprints.ColumnDataType;
import com.github.molcikas.photon.blueprints.JoinType;
import com.github.molcikas.photon.tests.unit.entities.shape.*;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ShapeAndDrawingCrudTests
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
            Drawing drawing = transaction.query(Drawing.class).fetchById(1);

            assertNotNull(drawing);
            assertEquals(4, drawing.getShapes().size());
            assertTrue(drawing.getShapes().get(0) instanceof Circle);
            assertTrue(drawing.getShapes().get(1) instanceof Rectangle);
            assertTrue(drawing.getShapes().get(2) instanceof Circle);
            assertTrue(drawing.getShapes().get(3) instanceof Rectangle);

            assertEquals("yellow", drawing.getShapes().get(2).getColorHistory().get(1).getColorName());
            assertEquals(new Integer(7), ((Rectangle)drawing.getShapes().get(3)).getCorners().get(1).getX());
        }
    }

    @Test
    public void withJoinedTable_deleteChildAndInsertOther_deletesChildAndInsertsNew()
    {
        registerAggregates();

        Rectangle rectangle = new Rectangle(null, "skyblue", null, 11, 12, new ArrayList<>());

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            Drawing drawing = transaction.query(Drawing.class).fetchById(1);

            drawing.getShapes().remove(2);
            drawing.getShapes().add(rectangle);

            transaction.save(drawing);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            Drawing drawing = transaction.query(Drawing.class).fetchById(1);

            assertNotNull(drawing);
            assertEquals(4, drawing.getShapes().size());
            assertTrue(drawing.getShapes().get(2) instanceof Rectangle);
            assertTrue(drawing.getShapes().get(3) instanceof Rectangle);

            assertEquals("gray", drawing.getShapes().get(2).getColorHistory().get(1).getColorName());
            assertEquals(new Integer(7), ((Rectangle)drawing.getShapes().get(2)).getCorners().get(1).getX());

            assertEquals(rectangle, drawing.getShapes().get(3));
        }
    }

    private void registerAggregates()
    {
        photon
            .registerAggregate(Drawing.class)
            .withChild(Shape.class)
                .withPrimaryKeyAutoIncrement()
                .withForeignKeyToParent("drawingId")
                .withJoinedTable(Circle.class, JoinType.LeftOuterJoin)
                    .withPrimaryKeyAutoIncrement()
                    .addJoinedTable()
                .withJoinedTable(Rectangle.class, JoinType.LeftOuterJoin)
                    .withPrimaryKeyAutoIncrement()
                    .addJoinedTable()
                .withChild(ShapeColorHistory.class)
                    .withPrimaryKeyAutoIncrement()
                    .withParentTable("Shape", "shapeId")
                    .addAsChild("colorHistory")
                .withChild(CornerCoordinates.class)
                    .withParentTable("Rectangle")
                    .withForeignKeyToParent("shapeId", ColumnDataType.INTEGER)
                    .addAsChild("corners")
                .withClassDiscriminator(valueMap ->
                {
                    if(valueMap.get("Circle_id") != null)
                    {
                        return Circle.class;
                    }
                    else if(valueMap.get("Rectangle_id") != null)
                    {
                        return Rectangle.class;
                    }
                    else
                    {
                        return Shape.class;
                    }
                })
                .addAsChild("shapes")
            .register();
    }
}
