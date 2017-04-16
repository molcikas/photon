package org.photon.perf.photon;

import java.util.UUID;

public class RecipeIngredient
{
    private UUID recipeIngredientId;

    private boolean isRequired;

    private String quantity;

    private String quantityUnit;

    private String quantityDetail;

    private String name;

    private String preparation;

    private Integer orderBy;

    public UUID getRecipeIngredientId()
    {
        return recipeIngredientId;
    }

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

    public void setName(String name)
    {
        this.name = name;
    }

    private RecipeIngredient()
    {
    }

    public RecipeIngredient(UUID recipeIngredientId, boolean isRequired, String quantity, String quantityUnit, String quantityDetail, String name, String preparation, Integer orderBy)
    {
        this.recipeIngredientId = recipeIngredientId;
        this.isRequired = isRequired;
        this.quantity = quantity;
        this.quantityUnit = quantityUnit;
        this.quantityDetail = quantityDetail;
        this.name = name;
        this.preparation = preparation;
        this.orderBy = orderBy;
    }
}
