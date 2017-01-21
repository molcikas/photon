package photon.tests.entities;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;

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
}
