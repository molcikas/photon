package photon.tests.databaseintegrations.h2.recipe;

import org.apache.commons.lang3.math.Fraction;
import org.junit.Before;
import org.junit.Test;
import photon.Photon;
import photon.PhotonConnection;
import photon.tests.entities.recipe.Recipe;
import photon.tests.entities.recipe.RecipeIngredient;
import photon.tests.entities.recipe.RecipeInstruction;

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
        RecipeDbSetup.registerRecipeAggregate(photon);

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
        RecipeDbSetup.registerRecipeAggregate(photon);

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
        RecipeDbSetup.registerRecipeAggregate(photon);

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
                    Fraction.getFraction("1/2"),
                    "teaspoon",
                    null,
                    "salt",
                    null,
                    0
                ),
                new RecipeIngredient(
                    true,
                    Fraction.getFraction("1/4"),
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
        RecipeDbSetup.registerRecipeAggregate(photon);

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
                    Fraction.getFraction("1/2"),
                    "teaspoon",
                    null,
                    "salt",
                    null,
                    0
                ),
                new RecipeIngredient(
                    true,
                    Fraction.getFraction("1/4"),
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

    @Test
    public void aggregate_save_changeMultipleExistingRecipes_updatesRecipes()
    {
        RecipeDbSetup.registerRecipeAggregate(photon);

        try (PhotonConnection connection = photon.open())
        {
            Recipe recipe1 = connection.query(Recipe.class).fetchById(UUID.fromString("3E04169A-A9B6-11E6-AB83-0A0027000010"));
            Recipe recipe2 = connection.query(Recipe.class).fetchById(UUID.fromString("3E040B3D-A9B6-11E6-AB83-0A0027000010"));
            Recipe recipe3 = connection.query(Recipe.class).fetchById(UUID.fromString("3E0378C5-A9B6-11E6-AB83-0A0027000010"));

            recipe1.setName("New Recipe1 Name");
            recipe2.setPrepTime(12345);
            recipe3.setPrepTime(-377);

            recipe1.getIngredients().clear();
            recipe2.getInstructions().clear();

            connection.saveAll(Arrays.asList(recipe1, recipe2, recipe3));
        }

        try (PhotonConnection connection = photon.open())
        {
            Recipe recipe1 = connection.query(Recipe.class).fetchById(UUID.fromString("3E04169A-A9B6-11E6-AB83-0A0027000010"));
            Recipe recipe2 = connection.query(Recipe.class).fetchById(UUID.fromString("3E040B3D-A9B6-11E6-AB83-0A0027000010"));
            Recipe recipe3 = connection.query(Recipe.class).fetchById(UUID.fromString("3E0378C5-A9B6-11E6-AB83-0A0027000010"));

            assertEquals("New Recipe1 Name", recipe1.getName());
            assertEquals("Slow Cooker Rice and Bean Casserole", recipe2.getName());
            assertEquals("Onion Gravy", recipe3.getName());

            assertEquals(15, recipe1.getPrepTime());
            assertEquals(12345, recipe2.getPrepTime());
            assertEquals(-377, recipe3.getPrepTime());

            assertEquals(0, recipe1.getIngredients().size());
            assertEquals(3, recipe1.getInstructions().size());
            assertEquals(10, recipe2.getIngredients().size());
            assertEquals(0, recipe2.getInstructions().size());
            assertEquals(8, recipe3.getIngredients().size());
            assertEquals(6, recipe3.getInstructions().size());
        }
    }
}
