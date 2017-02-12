package photon.tests.databaseintegrations.h2.recipe;

import org.junit.Before;
import org.junit.Test;
import photon.Photon;
import photon.PhotonConnection;
import photon.blueprints.SortDirection;
import photon.tests.entities.recipe.Recipe;
import photon.tests.entities.recipe.RecipeIngredient;
import photon.tests.entities.recipe.RecipeInstruction;

import java.sql.Types;
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

        try (PhotonConnection connection = photon.open())
        {
            Recipe recipe1 = connection.query(Recipe.class).fetchById(UUID.fromString("3E04169A-A9B6-11E6-AB83-0A0027000010"));

            connection.delete(recipe1);
        }

        try (PhotonConnection connection = photon.open())
        {
            Recipe recipe1 = connection.query(Recipe.class).fetchById(UUID.fromString("3E04169A-A9B6-11E6-AB83-0A0027000010"));

            assertNull(recipe1);
        }
    }

    private void registerRecipeAggregate()
    {
        registerRecipeAggregate(SortDirection.Ascending);
    }

    private void registerRecipeAggregate(SortDirection ingredientSortDirection)
    {
        photon.registerAggregate(Recipe.class)
            .withId("recipeId")
            .withChild(RecipeInstruction.class)
                .withId("recipeInstructionId")
                .withColumnDataType("recipeInstructionId", Types.BINARY)
                .withForeignKeyToParent("recipeId")
                .withColumnDataType("recipeId", Types.BINARY)
                .withOrderBy("stepNumber")
            .addAsChild("instructions")
            .withChild(RecipeIngredient.class)
                .withId("recipeIngredientId")
                .withColumnDataType("recipeIngredientId", Types.BINARY)
                .withForeignKeyToParent("recipeId")
                .withColumnDataType("recipeId", Types.BINARY)
                .withOrderBy("orderBy", ingredientSortDirection)
                .addAsChild("ingredients")
            .register();
    }
}