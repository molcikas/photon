package photon.tests.entities.recipe;

import java.util.Collections;
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
        return Collections.unmodifiableList(ingredients);
    }

    public List<RecipeInstruction> getInstructions()
    {
        return Collections.unmodifiableList(instructions);
    }
}
