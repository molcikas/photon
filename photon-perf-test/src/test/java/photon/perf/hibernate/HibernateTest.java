package photon.perf.hibernate;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Before;
import org.junit.Test;
import photon.perf.RecipeDbSetup;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;

public class HibernateTest
{
    private EntityManagerFactory entityManagerFactory;

    @Before
    public void setupDatabase()
    {
        RecipeDbSetup.setupDatabase();

        entityManagerFactory = Persistence.createEntityManagerFactory("app");
    }

    @Test
    public void hibernatePerformanceTest()
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
        long updateTime = 0;
        long deleteTime = 0;

        for(int i = 0; i < 10000; i++)
        {
            stopWatch.reset();
            stopWatch.start();

            EntityManager entityManager;

            entityManager = entityManagerFactory.createEntityManager();
            entityManager.getTransaction().begin();

            RecipeEntity recipe1 = new RecipeEntity(
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
                new HashSet<RecipeIngredientEntity>(Arrays.asList(
                    new RecipeIngredientEntity(
                        recipeIngredient1Id,
                        recipeId,
                        true,
                        "1/2",
                        "teaspoon",
                        null,
                        "salt",
                        null,
                        0
                    ),
                    new RecipeIngredientEntity(
                        recipeIngredient2Id,
                        recipeId,
                        true,
                        "1/4",
                        "tablespoon",
                        null,
                        "tumeric",
                        "dried",
                        1
                    ),
                    new RecipeIngredientEntity(
                        recipeIngredient3Id,
                        recipeId,
                        true,
                        "1/5",
                        "tablespoon",
                        null,
                        "tumeric",
                        "dried",
                        2
                    ),
                    new RecipeIngredientEntity(
                        recipeIngredient4Id,
                        recipeId,
                        true,
                        "1/6",
                        "tablespoon",
                        null,
                        "tumeric",
                        "dried",
                        3
                    ),
                    new RecipeIngredientEntity(
                        recipeIngredient5Id,
                        recipeId,
                        true,
                        "1/7",
                        "tablespoon",
                        null,
                        "tumeric",
                        "dried",
                        4
                    ),
                    new RecipeIngredientEntity(
                        recipeIngredient6Id,
                        recipeId,
                        true,
                        "1/8",
                        "tablespoon",
                        null,
                        "tumeric",
                        "dried",
                        5
                    )
                )),
                new HashSet<RecipeInstructionEntity>(Arrays.asList(
                    new RecipeInstructionEntity(
                        recipeInstruction1Id,
                        recipeId,
                        1,
                        "Get all the ingredients."
                    ),
                    new RecipeInstructionEntity(
                        recipeInstruction2Id,
                        recipeId,
                        2,
                        "Cook all the food."
                    ),
                    new RecipeInstructionEntity(
                        recipeInstruction3Id,
                        recipeId,
                        3,
                        "Eat it."
                    )
                ))
            );

            entityManager.persist(recipe1);
            entityManager.getTransaction().commit();
            entityManager.close();

            insertTime += stopWatch.getNanoTime();
            stopWatch.reset();
            stopWatch.start();

            entityManager = entityManagerFactory.createEntityManager();
            entityManager.getTransaction().begin();

            RecipeEntity recipe2 = (RecipeEntity) entityManager
                .createQuery("FROM RecipeEntity WHERE recipeId = :recipeId")
                .setParameter("recipeId", recipeId)
                .getResultList()
                .get(0);
            recipe2.setName("My renamed recipe.");
            Iterator<RecipeIngredientEntity> ingredientsIterator = recipe2.getIngredients().iterator();
            ingredientsIterator.next().setName("New First Recipe Ingredient Name");
            recipe2.getIngredients().remove(ingredientsIterator.next());
            recipe2.getInstructions().iterator().next().setDescription("New Step 1 Description");

            entityManager.merge(recipe2);
            entityManager.getTransaction().commit();
            entityManager.close();

            updateTime += stopWatch.getNanoTime();
            stopWatch.reset();
            stopWatch.start();

            entityManager = entityManagerFactory.createEntityManager();
            entityManager.getTransaction().begin();

            RecipeEntity recipe3 = (RecipeEntity) entityManager
                .createQuery("FROM RecipeEntity WHERE recipeId = :recipeId")
                .setParameter("recipeId", recipeId)
                .getResultList()
                .get(0);

            entityManager.remove(recipe3);
            entityManager.getTransaction().commit();
            entityManager.close();

            deleteTime += stopWatch.getNanoTime();
        }

        System.out.println(String.format("Inserts: %s, Updates: %s, Deletes: %s", insertTime / 1000, updateTime / 1000, deleteTime / 1000));
    }
}
