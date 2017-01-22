package photon.tests.entities.recipe;

import java.util.UUID;

public class RecipeInstruction
{
    private UUID recipeInstructionId;

    private int stepNumber;

    private String description;

    public UUID getRecipeInstructionId()
    {
        return recipeInstructionId;
    }

    public int getStepNumber()
    {
        return stepNumber;
    }

    public String getDescription()
    {
        return description;
    }
}
