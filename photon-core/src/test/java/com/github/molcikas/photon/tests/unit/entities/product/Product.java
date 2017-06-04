package com.github.molcikas.photon.tests.unit.entities.product;

import org.apache.commons.lang3.math.Fraction;

public class Product
{
    private int id;

    private Fraction quantity;

    public int getId()
    {
        return id;
    }

    public Fraction getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Fraction quantity)
    {
        this.quantity = quantity;
    }

    private Product()
    {
    }

    public Product(int id, Fraction quantity)
    {
        this.id = id;
        this.quantity = quantity;
    }
}
