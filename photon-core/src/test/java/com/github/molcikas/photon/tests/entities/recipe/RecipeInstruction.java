package com.github.molcikas.photon.tests.entities.recipe;

import java.util.Objects;
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

    private RecipeInstruction()
    {
    }

    public RecipeInstruction(UUID recipeInstructionId, int stepNumber, String description)
    {
        this.recipeInstructionId = recipeInstructionId;
        this.stepNumber = stepNumber;
        this.description = description;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipeInstruction that = (RecipeInstruction) o;
        return stepNumber == that.stepNumber &&
            Objects.equals(recipeInstructionId, that.recipeInstructionId) &&
            Objects.equals(description, that.description);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(recipeInstructionId, stepNumber, description);
    }
}
