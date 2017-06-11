package com.github.molcikas.photon.tests.unit.h2.recipe;

import com.github.molcikas.photon.blueprints.ColumnDataType;
import org.junit.Before;
import org.junit.Test;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.tests.unit.entities.recipe.Recipe;
import com.github.molcikas.photon.tests.unit.entities.recipe.RecipeIngredient;
import com.github.molcikas.photon.tests.unit.entities.recipe.RecipeInstruction;

import java.util.UUID;

import static org.junit.Assert.assertNull;

public class RecipeDeleteTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = RecipeDbSetup.setupDatabase();
    }

    @Test
    public void aggregate_delete_existingRecipe_deletesRecipe()
    {
        registerRecipeAggregate();

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Recipe recipe1 = transaction.query(Recipe.class).fetchById(UUID.fromString("3E04169A-A9B6-11E6-AB83-0A0027000010"));

            transaction.delete(recipe1);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Recipe recipe1 = transaction.query(Recipe.class).fetchById(UUID.fromString("3E04169A-A9B6-11E6-AB83-0A0027000010"));

            assertNull(recipe1);
            transaction.commit();
        }
    }

    private void registerRecipeAggregate()
    {
        registerRecipeAggregate("recipeingredient.orderBy");
    }

    private void registerRecipeAggregate(String orderBySql)
    {
        photon.registerAggregate(Recipe.class)
            .withId("recipeId")
            .withChild(RecipeInstruction.class)
                .withId("recipeInstructionId", ColumnDataType.BINARY)
                .withForeignKeyToParent("recipeId")
                .withDatabaseColumn("recipeId", ColumnDataType.BINARY)
                .withOrderBySql("stepNumber")
            .addAsChild("instructions")
            .withChild(RecipeIngredient.class)
                .withId("recipeIngredientId")
                .withForeignKeyToParent("recipeId")
                .withDatabaseColumn("recipeIngredientId", ColumnDataType.BINARY)
                .withDatabaseColumn("recipeId", ColumnDataType.BINARY)
                .withOrderBySql(orderBySql)
                .addAsChild("ingredients")
            .register();
    }
}
