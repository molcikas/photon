package com.github.molcikas.photon.tests.unit.h2.product;

import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.blueprints.ColumnDataType;
import com.github.molcikas.photon.blueprints.CompoundEntityFieldValueMapping;
import com.github.molcikas.photon.blueprints.DatabaseColumnDefinition;
import com.github.molcikas.photon.tests.unit.entities.product.Product;
import org.apache.commons.lang3.math.Fraction;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ProductTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = ProductDbSetup.setupDatabase();
    }

    @Test
    public void query_simpleEntityWithoutId_returnsEntity()
    {
        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            String sql =
                "SELECT theProductId " +
                "FROM product " +
                "WHERE theProductId = :theProductId ";

            Product product = transaction.query(sql).addParameter("theProductId", 1).fetch(Product.class);

            assertNotNull(product);
            assertEquals(1, product.getTheProductId());
        }
    }

    @Test
    public void fetchById_simpleEntity_returnsEntity()
    {
        registerAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            Product product = transaction
                .query(Product.class)
                .fetchById(1);

            assertNotNull(product);
            assertEquals(1, product.getTheProductId());
            assertEquals(Fraction.getFraction(2, 3), product.getQuantity());
        }
    }

    @Test
    public void insert_simpleEntity_insertsEntity()
    {
        registerAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            Product product = new Product(0, Fraction.getFraction(3, 4));
            transaction.insert(product);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            Product product = transaction
                .query(Product.class)
                .fetchById(2);

            assertNotNull(product);
            assertEquals(2, product.getTheProductId());
            assertEquals(Fraction.getFraction(3, 4), product.getQuantity());
        }
    }

    @Test
    public void update_simpleEntity_updatesEntity()
    {
        registerAggregate();

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            Product product = transaction
                .query(Product.class)
                .fetchById(1);

            product.setQuantity(Fraction.getFraction(5, 6));
            transaction.save(product);
            transaction.commit();
        }

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            Product product = transaction
                .query(Product.class)
                .fetchById(1);

            assertNotNull(product);
            assertEquals(1, product.getTheProductId());
            assertEquals(Fraction.getFraction(5, 6), product.getQuantity());
        }
    }

    private void registerAggregate()
    {
        photon.registerAggregate(Product.class)
            .withId("theProductId")
            .withPrimaryKeyAutoIncrement()
            .withDatabaseColumns(
                Arrays.asList(
                    new DatabaseColumnDefinition("numerator"),
                    new DatabaseColumnDefinition("denominator", ColumnDataType.INTEGER)
                ),
                new CompoundEntityFieldValueMapping<Product>()
                {
                    @Override
                    public Map<String, Object> getDatabaseValues(Product entityInstance)
                    {
                        Map<String, Object> values = new HashMap<>();
                        values.put("numerator", entityInstance.getQuantity().getNumerator());
                        values.put("denominator", entityInstance.getQuantity().getDenominator());
                        return values;
                    }

                    @Override
                    public Map<String, Object> setFieldValues(Product entityInstance, Map<String, Object> databaseValues)
                    {
                        Map<String, Object> values = new HashMap<>();
                        values.put(
                            "quantity",
                            Fraction.getFraction((int) databaseValues.get("Product_numerator"), (int) databaseValues.get("Product_denominator"))
                        );
                        return values;
                    }
                }
            )
            .register();
    }
}
