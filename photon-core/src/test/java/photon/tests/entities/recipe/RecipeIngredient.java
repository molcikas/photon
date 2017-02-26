package photon.tests.entities.recipe;

import org.apache.commons.lang3.math.Fraction;

import java.util.Objects;

public class RecipeIngredient
{
    private boolean isRequired;

    private Fraction quantity;

    private String quantityUnit;

    private String quantityDetail;

    private String name;

    private String preparation;

    private Integer orderBy;

    public boolean isRequired()
    {
        return isRequired;
    }

    public Fraction getQuantity()
    {
        return quantity;
    }

    public String getQuantityUnit()
    {
        return quantityUnit;
    }

    public String getQuantityDetail()
    {
        return quantityDetail;
    }

    public String getName()
    {
        return name;
    }

    public String getPreparation()
    {
        return preparation;
    }

    public Integer getOrderBy()
    {
        return orderBy;
    }

    private RecipeIngredient()
    {
    }

    public RecipeIngredient(boolean isRequired, Fraction quantity, String quantityUnit, String quantityDetail, String name, String preparation, Integer orderBy)
    {
        this.isRequired = isRequired;
        this.quantity = quantity;
        this.quantityUnit = quantityUnit;
        this.quantityDetail = quantityDetail;
        this.name = name;
        this.preparation = preparation;
        this.orderBy = orderBy;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipeIngredient that = (RecipeIngredient) o;
        return isRequired == that.isRequired &&
            Objects.equals(quantity, that.quantity) &&
            Objects.equals(quantityUnit, that.quantityUnit) &&
            Objects.equals(quantityDetail, that.quantityDetail) &&
            Objects.equals(name, that.name) &&
            Objects.equals(preparation, that.preparation) &&
            Objects.equals(orderBy, that.orderBy);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(isRequired, quantity, quantityUnit, quantityDetail, name, preparation, orderBy);
    }
}
