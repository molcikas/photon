package photon.tests.databaseintegrations.h2;

import org.junit.Before;
import org.junit.Test;
import photon.Photon;
import photon.PhotonConnection;
import photon.blueprints.SortDirection;
import photon.tests.databaseintegrations.h2.setup.RecipeDbSetup;
import photon.tests.entities.recipe.Recipe;
import photon.tests.entities.recipe.RecipeIngredient;
import photon.tests.entities.recipe.RecipeInstruction;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RecipeSaveTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = RecipeDbSetup.setupDatabase();
    }

    @Test
    public void aggregate_save_saveNewRecipeNoChildren_insertsRecipe()
    {
        registerRecipeAggregate();

        Recipe recipe = new Recipe(
            UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000011"),
            "My Recipe",
            "This is my recipe.",
            10,
            11,
            12,
            true,
            false,
            true,
            "http://www.example.com/food/myrecipe",
            Collections.emptyList(),
            null
        );

        try (PhotonConnection connection = photon.open())
        {
            connection
                .save(recipe);
        }

        try (PhotonConnection connection = photon.open())
        {
            Recipe fetchedRecipe = connection
                .query(Recipe.class)
                .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000011"));

            assertNotNull(fetchedRecipe);
            assertEquals(recipe.getRecipeId(), fetchedRecipe.getRecipeId());
            assertEquals(recipe.getIngredients().size(), fetchedRecipe.getIngredients().size());
            assertEquals(0, fetchedRecipe.getInstructions().size());

            recipe.setInstructions(Collections.emptyList());
            assertEquals(recipe, fetchedRecipe);
        }
    }

    @Test
    public void aggregate_save_saveNewRecipeWithInstructions_insertsRecipe()
    {
        registerRecipeAggregate();

        Recipe recipe = new Recipe(
            UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000011"),
            "My Recipe",
            "This is my recipe.",
            10,
            11,
            12,
            true,
            false,
            true,
            "http://www.example.com/food/myrecipe",
            Collections.emptyList(),
            Arrays.asList(
                new RecipeInstruction(
                    UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000012"),
                    1,
                    "Cook all the food."
                ),
                new RecipeInstruction(
                    UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000013"),
                    2,
                    "Eat it."
                )
            )
        );

        try (PhotonConnection connection = photon.open())
        {
            connection
                .save(recipe);
        }

        try (PhotonConnection connection = photon.open())
        {
            Recipe fetchedRecipe = connection
                .query(Recipe.class)
                .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000011"));

            assertNotNull(fetchedRecipe);
            assertEquals(recipe.getRecipeId(), fetchedRecipe.getRecipeId());
            assertEquals(recipe.getIngredients().size(), fetchedRecipe.getIngredients().size());
            assertEquals(recipe.getInstructions().size(), fetchedRecipe.getInstructions().size());
            assertEquals(recipe, fetchedRecipe);
        }
    }

    @Test
    public void aggregate_save_saveNewRecipeWithIngredientsAndInstructions_insertsRecipe()
    {
        registerRecipeAggregate();

        Recipe recipe = new Recipe(
            UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000011"),
            "My Recipe",
            "This is my recipe.",
            10,
            11,
            12,
            true,
            false,
            true,
            "http://www.example.com/food/myrecipe",
            Arrays.asList(
                new RecipeIngredient(
                    true,
                    "1/2",
                    "teaspoon",
                    null,
                    "salt",
                    null,
                    0
                ),
                new RecipeIngredient(
                    true,
                    "1/4",
                    "tablespoon",
                    null,
                    "tumeric",
                    "dried",
                    1
                )
            ),
            Arrays.asList(
                new RecipeInstruction(
                    UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000012"),
                    1,
                    "Cook all the food."
                ),
                new RecipeInstruction(
                    UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000013"),
                    2,
                    "Eat it."
                )
            )
        );

        try (PhotonConnection connection = photon.open())
        {
            connection
                .save(recipe);
        }

        try (PhotonConnection connection = photon.open())
        {
            Recipe fetchedRecipe = connection
                .query(Recipe.class)
                .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000011"));

            assertNotNull(fetchedRecipe);
            assertEquals(recipe.getRecipeId(), fetchedRecipe.getRecipeId());
            assertEquals(recipe.getIngredients().size(), fetchedRecipe.getIngredients().size());
            assertEquals(recipe.getInstructions().size(), fetchedRecipe.getInstructions().size());
            assertEquals(recipe, fetchedRecipe);
        }
    }

    @Test
    public void aggregate_save_overwriteExistingRecipe_insertsRecipe()
    {
        registerRecipeAggregate();

        // A recipe with this id already exists in the database. Any traces if the old recipe
        // should be removed when saving this new recipe.

        Recipe recipe = new Recipe(
            UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000010"),
            "My Recipe",
            "This is my recipe.",
            10,
            11,
            12,
            true,
            false,
            true,
            "http://www.example.com/food/myrecipe",
            Arrays.asList(
                new RecipeIngredient(
                    true,
                    "1/2",
                    "teaspoon",
                    null,
                    "salt",
                    null,
                    0
                ),
                new RecipeIngredient(
                    true,
                    "1/4",
                    "tablespoon",
                    null,
                    "tumeric",
                    "dried",
                    1
                )
            ),
            Arrays.asList(
                new RecipeInstruction(
                    UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000012"),
                    1,
                    "Cook all the food."
                ),
                new RecipeInstruction(
                    UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000013"),
                    2,
                    "Eat it."
                )
            )
        );

        try (PhotonConnection connection = photon.open())
        {
            connection
                .save(recipe);
        }

        try (PhotonConnection connection = photon.open())
        {
            Recipe fetchedRecipe = connection
                .query(Recipe.class)
                .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000010"));

            assertNotNull(fetchedRecipe);
            assertEquals(recipe.getRecipeId(), fetchedRecipe.getRecipeId());
            assertEquals(recipe.getIngredients().size(), fetchedRecipe.getIngredients().size());
            assertEquals(recipe.getInstructions().size(), fetchedRecipe.getInstructions().size());
            assertEquals(recipe, fetchedRecipe);
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
