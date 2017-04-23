package com.github.molcikas.photon.perf.hibernate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "recipeingredient")
public class RecipeIngredientEntity
{
    @Id
    @Column(columnDefinition = "BINARY(16)", length = 16)
    public UUID recipeIngredientId;

    @Column(columnDefinition = "BINARY(16)", length = 16)
    public UUID recipeId;

    @Column
    public boolean isRequired;

    @Column
    public String quantity;

    @Column
    public String quantityUnit;

    @Column
    public String quantityDetail;

    @Column
    public String name;

    @Column
    public String preparation;

    @Column
    public int orderBy;

    public void setName(String name)
    {
        this.name = name;
    }

    protected RecipeIngredientEntity()
    {
    }

    public RecipeIngredientEntity(UUID recipeIngredientId, UUID recipeId, boolean isRequired, String quantity, String quantityUnit, String quantityDetail, String name, String preparation, int orderBy)
    {
        this.recipeIngredientId = recipeIngredientId;
        this.recipeId = recipeId;
        this.isRequired = isRequired;
        this.quantity = quantity;
        this.quantityUnit = quantityUnit;
        this.quantityDetail = quantityDetail;
        this.name = name;
        this.preparation = preparation;
        this.orderBy = orderBy;
    }
}
