package photon.tests.entities.recipe;

public class RecipeIngredient
{
    private boolean isRequired;

    private String quantity;

    private String quantityUnit;

    private String quantityDetail;

    private String name;

    private String preparation;

    private Integer orderBy;

    public boolean isRequired()
    {
        return isRequired;
    }

    public String getQuantity()
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
