package org.photon.perf.photon;

import java.util.List;
import java.util.UUID;

public class Recipe
{
    private UUID recipeId;

    private String name;

    private String description;

    private int prepTime;

    private int cookTime;

    private int servings;

    private boolean isVegetarian;

    private boolean isVegan;

    private boolean isPublished;

    private String credit;

    private List<RecipeIngredient> ingredients;

    private List<RecipeInstruction> instructions;

    public UUID getRecipeId()
    {
        return recipeId;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public int getPrepTime()
    {
        return prepTime;
    }

    public int getCookTime()
    {
        return cookTime;
    }

    public int getServings()
    {
        return servings;
    }

    public boolean isVegetarian()
    {
        return isVegetarian;
    }

    public boolean isVegan()
    {
        return isVegan;
    }

    public boolean isPublished()
    {
        return isPublished;
    }

    public String getCredit()
    {
        return credit;
    }

    public List<RecipeIngredient> getIngredients()
    {
        return ingredients;
    }

    public List<RecipeInstruction> getInstructions()
    {
        return instructions;
    }

    // Anemic setters are strongly discouraged in aggregates, but we need these for testing.

    public void setInstructions(List<RecipeInstruction> instructions)
    {
        this.instructions = instructions;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setPrepTime(int prepTime)
    {
        this.prepTime = prepTime;
    }

    public Recipe()
    {
    }

    public Recipe(UUID recipeId, String name, String description, int prepTime, int cookTime, int servings, boolean isVegetarian, boolean isVegan, boolean isPublished, String credit, List<RecipeIngredient> ingredients, List<RecipeInstruction> instructions)
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
