package com.github.molcikas.photon.perf.photon;

import com.github.molcikas.photon.blueprints.ColumnDataType;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Before;
import org.junit.Test;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.PhotonTransaction;
import com.github.molcikas.photon.blueprints.SortDirection;
import com.github.molcikas.photon.perf.RecipeDbSetup;

import java.util.Arrays;
import java.util.UUID;

public class PhotonTest
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        RecipeDbSetup.setupDatabase();

        photon = new Photon(RecipeDbSetup.createDataSource());

        photon.registerAggregate(Recipe.class)
            .withId("recipeId")
            .withChild(RecipeInstruction.class)
                .withId("recipeInstructionId", ColumnDataType.BINARY)
                .withForeignKeyToParent("recipeId")
                .withDatabaseColumn("recipeId", ColumnDataType.BINARY)
                .withOrderBy("stepNumber")
                .addAsChild("instructions")
            .withChild(RecipeIngredient.class)
                .withId("recipeIngredientId", ColumnDataType.BINARY)
                .withForeignKeyToParent("recipeId")
                .withDatabaseColumn("recipeId", ColumnDataType.BINARY)
                .withOrderBy("orderBy", SortDirection.Ascending)
                .addAsChild("ingredients")
            .register();
    }

    @Test
    public void photonPerformanceTest()
    {
        System.out.println("Warming up...");

        runPerformanceTest();

        for(int i = 0; i < 5; i++)
        {
            System.out.println(String.format("Running test iteration %s.", i + 1));
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            runPerformanceTest();
            long totalTime = stopWatch.getTime();
            System.out.println(String.format("Test finished after %s ms.", totalTime));
        }
    }

    private void runPerformanceTest()
    {
        UUID recipeId = UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000010");
        UUID recipeIngredient1Id = UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000011");
        UUID recipeIngredient2Id = UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000012");
        UUID recipeIngredient3Id = UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000013");
        UUID recipeIngredient4Id = UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000014");
        UUID recipeIngredient5Id = UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000015");
        UUID recipeIngredient6Id = UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000016");
        UUID recipeInstruction1Id = UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000021");
        UUID recipeInstruction2Id = UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000022");
        UUID recipeInstruction3Id = UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000023");

        StopWatch stopWatch = new StopWatch();
        long insertTime = 0;
        long selectTime = 0;
        long updateTime = 0;
        long deleteTime = 0;

        for(int i = 0; i < 10000; i++)
        {
            stopWatch.reset();
            stopWatch.start();

            try (PhotonTransaction transaction = photon.beginTransaction())
            {
                Recipe recipe = new Recipe(
                    recipeId,
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
                            recipeIngredient1Id,
                            true,
                            "1/2",
                            "teaspoon",
                            null,
                            "salt",
                            null,
                            0
                        ),
                        new RecipeIngredient(
                            recipeIngredient2Id,
                            true,
                            "1/4",
                            "tablespoon",
                            null,
                            "tumeric",
                            "dried",
                            1
                        ),
                        new RecipeIngredient(
                            recipeIngredient3Id,
                            true,
                            "1/5",
                            "tablespoon",
                            null,
                            "tumeric",
                            "dried",
                            2
                        ),
                        new RecipeIngredient(
                            recipeIngredient4Id,
                            true,
                            "1/6",
                            "tablespoon",
                            null,
                            "tumeric",
                            "dried",
                            3
                        ),
                        new RecipeIngredient(
                            recipeIngredient5Id,
                            true,
                            "1/7",
                            "tablespoon",
                            null,
                            "tumeric",
                            "dried",
                            4
                        ),
                        new RecipeIngredient(
                            recipeIngredient6Id,
                            true,
                            "1/8",
                            "tablespoon",
                            null,
                            "tumeric",
                            "dried",
                            5
                        )
                    ),
                    Arrays.asList(
                        new RecipeInstruction(
                            recipeInstruction1Id,
                            1,
                            "Get all the ingredients."
                        ),
                        new RecipeInstruction(
                            recipeInstruction2Id,
                            2,
                            "Cook all the food."
                        ),
                        new RecipeInstruction(
                            recipeInstruction3Id,
                            3,
                            "Eat it."
                        )
                    )
                );

                transaction.insert(recipe);
                transaction.commit();
            }

            insertTime += stopWatch.getNanoTime();
            stopWatch.reset();
            stopWatch.start();

            try (PhotonTransaction transaction = photon.beginTransaction())
            {
                Recipe recipe = transaction
                    .query(Recipe.class)
                    .fetchById(recipeId);
            }

            selectTime += stopWatch.getNanoTime();
            stopWatch.reset();
            stopWatch.start();

            try (PhotonTransaction transaction = photon.beginTransaction())
            {
                Recipe recipe = transaction
                    .query(Recipe.class)
                    .fetchById(recipeId);

                recipe.setName("My renamed recipe.");
                recipe.getIngredients().get(0).setName("New First Recipe Ingredient Name");
                recipe.getIngredients().remove(1);
                recipe.getInstructions().get(0).setDescription("New Step 1 Description");

                transaction.save(recipe);
                transaction.commit();
            }

            updateTime += stopWatch.getNanoTime();
            stopWatch.reset();
            stopWatch.start();

            try (PhotonTransaction transaction = photon.beginTransaction())
            {
                Recipe recipe = transaction
                    .query(Recipe.class)
                    .fetchById(recipeId);

                transaction.delete(recipe);
                transaction.commit();
            }

            deleteTime += stopWatch.getNanoTime();
        }

        System.out.println(String.format("Inserts: %s, Selects: %s, Updates: %s, Deletes: %s", insertTime / 1000000, selectTime / 1000000, updateTime / 1000000, deleteTime / 1000000));
    }
}
