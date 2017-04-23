package com.github.molcikas.photon.perf.hibernate;

import javax.persistence.*;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "recipe")
public class RecipeEntity
{
    @Id
    @Column(columnDefinition = "BINARY(16)", length = 16)
    public UUID recipeId;

    @Column
    public String name;

    @Column
    public String description;

    @Column
    public int prepTime;

    @Column
    public int cookTime;

    @Column
    public int servings;

    @Column
    public boolean isVegetarian;

    @Column
    public boolean isVegan;

    @Column
    public boolean isPublished;

    @Column
    public String credit;

    @OneToMany(mappedBy = "recipeId", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("name ASC")
    public Set<RecipeIngredientEntity> ingredients;

    @OneToMany(mappedBy = "recipeId", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNumber ASC")
    public Set<RecipeInstructionEntity> instructions;

    public Set<RecipeIngredientEntity> getIngredients()
    {
        return ingredients;
    }

    public Set<RecipeInstructionEntity> getInstructions()
    {
        return instructions;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    protected RecipeEntity()
    {
    }

    public RecipeEntity(UUID recipeId, String name, String description, int prepTime, int cookTime, int servings, boolean isVegetarian, boolean isVegan, boolean isPublished, String credit, Set<RecipeIngredientEntity> ingredients, Set<RecipeInstructionEntity> instructions)
    {
        this.recipeId = recipeId;
        this.name = name;
        this.description = description;
        this.prepTime = prepTime;
        this.cookTime = cookTime;
        this.servings = servings;
        this.isVegetarian = isVegetarian;
        this.isVegan = isVegan;
        this.isPublished = isPublished;
        this.credit = credit;
        this.ingredients = ingredients;
        this.instructions = instructions;
    }
}
