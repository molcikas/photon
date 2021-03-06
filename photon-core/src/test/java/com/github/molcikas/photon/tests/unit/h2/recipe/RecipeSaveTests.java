package com.github.molcikas.photon.tests.unit.h2.recipe;

import com.github.molcikas.photon.blueprints.table.ColumnDataType;
import org.apache.commons.lang3.math.Fraction;
import org.junit.Before;
import org.junit.Test;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.tests.unit.entities.recipe.RecipeIngredient;
import com.github.molcikas.photon.tests.unit.entities.recipe.RecipeInstruction;
import com.github.molcikas.photon.tests.unit.entities.recipe.Recipe;

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

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            transaction.save(recipe);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Recipe fetchedRecipe = transaction
                .query(Recipe.class)
                .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000011"));

            assertNotNull(fetchedRecipe);
            assertEquals(recipe.getRecipeId(), fetchedRecipe.getRecipeId());
            assertEquals(recipe.getIngredients().size(), fetchedRecipe.getIngredients().size());
            assertEquals(0, fetchedRecipe.getInstructions().size());

            recipe.setInstructions(Collections.emptyList());
            assertEquals(recipe, fetchedRecipe);

            transaction.commit();
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

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            transaction.save(recipe);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Recipe fetchedRecipe = transaction
                .query(Recipe.class)
                .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000011"));

            assertNotNull(fetchedRecipe);
            assertEquals(recipe.getRecipeId(), fetchedRecipe.getRecipeId());
            assertEquals(recipe.getIngredients().size(), fetchedRecipe.getIngredients().size());
            assertEquals(recipe.getInstructions().size(), fetchedRecipe.getInstructions().size());
            assertEquals(recipe, fetchedRecipe);

            transaction.commit();
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

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            transaction.save(recipe);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Recipe fetchedRecipe = transaction
                .query(Recipe.class)
                .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000011"));

            assertNotNull(fetchedRecipe);
            assertEquals(recipe.getRecipeId(), fetchedRecipe.getRecipeId());
            assertEquals(recipe.getIngredients().size(), fetchedRecipe.getIngredients().size());
            assertEquals(recipe.getInstructions().size(), fetchedRecipe.getInstructions().size());
            assertEquals(recipe, fetchedRecipe);

            transaction.commit();
        }
    }

    @Test
    public void aggregate_insert_insertRecipeWithIngredientsAndInstructions_insertsRecipe()
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

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            transaction.insert(recipe);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Recipe fetchedRecipe = transaction
                .query(Recipe.class)
                .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000011"));

            assertNotNull(fetchedRecipe);
            assertEquals(recipe.getRecipeId(), fetchedRecipe.getRecipeId());
            assertEquals(recipe.getName(), fetchedRecipe.getName());
            assertEquals(recipe.getIngredients().size(), fetchedRecipe.getIngredients().size());
            assertEquals(recipe.getIngredients().get(1).getName(), fetchedRecipe.getIngredients().get(1).getName());
            assertEquals(recipe.getInstructions().size(), fetchedRecipe.getInstructions().size());
            assertEquals(recipe.getInstructions().get(1).getDescription(), fetchedRecipe.getInstructions().get(1).getDescription());
            assertEquals(recipe, fetchedRecipe);

            transaction.commit();
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

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            transaction.save(recipe);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Recipe fetchedRecipe = transaction
                .query(Recipe.class)
                .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000010"));

            assertNotNull(fetchedRecipe);
            assertEquals(recipe.getRecipeId(), fetchedRecipe.getRecipeId());
            assertEquals(recipe.getIngredients().size(), fetchedRecipe.getIngredients().size());
            assertEquals(recipe.getInstructions().size(), fetchedRecipe.getInstructions().size());
            assertEquals(recipe, fetchedRecipe);

            transaction.commit();
        }
    }

    @Test
    public void aggregate_save_changeMultipleExistingRecipes_updatesRecipes()
    {
        RecipeDbSetup.registerRecipeAggregate(photon);

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Recipe recipe1 = transaction.query(Recipe.class).fetchById(UUID.fromString("3E04169A-A9B6-11E6-AB83-0A0027000010"));
            Recipe recipe2 = transaction.query(Recipe.class).fetchById(UUID.fromString("3E040B3D-A9B6-11E6-AB83-0A0027000010"));
            Recipe recipe3 = transaction.query(Recipe.class).fetchById(UUID.fromString("3E0378C5-A9B6-11E6-AB83-0A0027000010"));

            recipe1.setName("New Recipe1 Name");
            recipe2.setPrepTime(12345);
            recipe3.setPrepTime(-377);

            recipe1.getIngredients().clear();
            recipe2.getInstructions().clear();

            transaction.saveAll(recipe1, recipe2, recipe3);
            transaction.commit();
        }

        try (PhotonTransaction transaction = photon.beginTransaction())
        {
            Recipe recipe1 = transaction.query(Recipe.class).fetchById(UUID.fromString("3E04169A-A9B6-11E6-AB83-0A0027000010"));
            Recipe recipe2 = transaction.query(Recipe.class).fetchById(UUID.fromString("3E040B3D-A9B6-11E6-AB83-0A0027000010"));
            Recipe recipe3 = transaction.query(Recipe.class).fetchById(UUID.fromString("3E0378C5-A9B6-11E6-AB83-0A0027000010"));

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

            transaction.commit();
        }
    }

    @Test
    public void untrack_saveUnTrackedAggregate_resavesAllChanges()
    {
        RecipeDbSetup.registerRecipeAggregate(photon);

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            Recipe recipe = transaction
                .query(Recipe.class)
                .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000010"));

            int rowsUpdated = transaction.query("UPDATE recipe SET name = 'UpdatedName' WHERE recipeId = :recipeId")
                .addParameter("recipeId", recipe.getRecipeId(), ColumnDataType.BINARY)
                .executeUpdate();
            assertEquals(1, rowsUpdated);

            rowsUpdated = transaction.query("DELETE FROM recipeingredient WHERE recipeId = :recipeId")
                .addParameter("recipeId", recipe.getRecipeId(), ColumnDataType.BINARY)
                .executeUpdate();
            assertEquals(17, rowsUpdated);

            rowsUpdated = transaction.query("DELETE FROM recipeinstruction WHERE recipeId = :recipeId AND stepNumber NOT IN (2, 4)")
                .addParameter("recipeId", recipe.getRecipeId(), ColumnDataType.BINARY)
                .executeUpdate();
            assertEquals(4, rowsUpdated);

            transaction.untrack(recipe);

            // This should re-save the entire aggregate since it is untracked.
            transaction.save(recipe);

            Recipe recipeFetched = transaction
                .query(Recipe.class)
                .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000010"));

            assertNotNull(recipeFetched);
            assertEquals("Spaghetti with Lentil and Tomato Sauce", recipeFetched.getName());
            assertEquals(17, recipeFetched.getIngredients().size());
            assertEquals(6, recipeFetched.getInstructions().size());
        }
    }

    @Test
    public void track_saveTrackedAggregate_savesChangesOnly()
    {
        RecipeDbSetup.registerRecipeAggregate(photon);

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            Recipe recipe = transaction
                .query(Recipe.class)
                .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000010"));

            recipe.setPrepTime(777);
            recipe.getInstructions().get(1).setDescription("New Description 2");

            int rowsUpdated = transaction.query("UPDATE recipe SET name = 'UpdatedName' WHERE recipeId = :recipeId")
                .addParameter("recipeId", recipe.getRecipeId(), ColumnDataType.BINARY)
                .executeUpdate();
            assertEquals(1, rowsUpdated);

            rowsUpdated = transaction.query("DELETE FROM recipeingredient WHERE recipeId = :recipeId")
                .addParameter("recipeId", recipe.getRecipeId(), ColumnDataType.BINARY)
                .executeUpdate();
            assertEquals(17, rowsUpdated);

            rowsUpdated = transaction.query("DELETE FROM recipeinstruction WHERE recipeId = :recipeId AND stepNumber NOT IN (2, 4)")
                .addParameter("recipeId", recipe.getRecipeId(), ColumnDataType.BINARY)
                .executeUpdate();
            assertEquals(4, rowsUpdated);

            // This should only save things that have changed.
            transaction.save(recipe);

            Recipe recipeFetched = transaction
                .query(Recipe.class)
                .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000010"));

            assertNotNull(recipeFetched);
            assertEquals("UpdatedName", recipeFetched.getName());
            assertEquals(777, recipeFetched.getPrepTime());
            // Ingredients don't have the primary key in the entity, so if the list changes, we have to re-save the entire list.
            assertEquals(17, recipeFetched.getIngredients().size());
            assertEquals(2, recipeFetched.getInstructions().size());
            assertEquals("New Description 2", recipeFetched.getInstructions().get(0).getDescription());
        }
    }
}
