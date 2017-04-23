package com.github.molcikas.photon.perf.photon;

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

    public void setDescription(String description)
    {
        this.description = description;
    }

    private RecipeInstruction()
    {
    }

    public RecipeInstruction(UUID recipeInstructionId, int stepNumber, String description)
    {
        this.recipeInstructionId = recipeInstructionId;
        this.stepNumber = stepNumber;
        this.description = description;
    }
}
