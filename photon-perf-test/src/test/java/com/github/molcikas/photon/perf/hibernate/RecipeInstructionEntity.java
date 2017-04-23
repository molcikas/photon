package com.github.molcikas.photon.perf.hibernate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "recipeinstruction")
public class RecipeInstructionEntity
{
    @Id
    @Column(columnDefinition = "BINARY(16)", length = 16)
    public UUID recipeInstructionId;

    @Column(columnDefinition = "BINARY(16)", length = 16)
    public UUID recipeId;

    @Column
    public int stepNumber;

    @Column
    public String description;

    public void setDescription(String description)
    {
        this.description = description;
    }

    protected RecipeInstructionEntity()
    {
    }

    public RecipeInstructionEntity(UUID recipeInstructionId, UUID recipeId, int stepNumber, String description)
    {
        this.recipeInstructionId = recipeInstructionId;
        this.recipeId = recipeId;
        this.stepNumber = stepNumber;
        this.description = description;
    }
}
