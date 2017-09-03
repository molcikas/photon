package com.github.molcikas.photon.tests.unit.entities.product;

import org.apache.commons.lang3.math.Fraction;

public class Product
{
    private int theProductId;

    private Fraction quantity;

    public int getTheProductId()
    {
        return theProductId;
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

    public Product(int theProductId, Fraction quantity)
    {
        this.theProductId = theProductId;
        this.quantity = quantity;
    }
}
