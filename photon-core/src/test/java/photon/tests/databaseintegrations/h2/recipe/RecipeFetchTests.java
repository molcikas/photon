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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RecipeFetchTests
{
    private Photon photon;

    @Before
    public void setupDatabase()
    {
        photon = RecipeDbSetup.setupDatabase();
    }

    @Test
    public void aggregate_fetchById_validSingleAggregateAndQuery_returnsCorrectAggregate()
    {
        registerRecipeAggregate();

        try (PhotonConnection connection = photon.open())
        {
            Recipe recipe = connection
                .query(Recipe.class)
                .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000010"));

            assertNotNull(recipe);
            assertEquals(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000010"), recipe.getRecipeId());
            assertEquals("Spaghetti with Lentil and Tomato Sauce", recipe.getName());
            assertEquals("We found this recipe in a clearance vegetarian cookbook and loved it. It has plenty of flavor and makes a ton of food, be prepared for lots of leftovers. Lentils are a great way to infuse protein into a vegan diet, and theyre super cheap and easy to cook. Unlike almost all other dry beans, lentils can be soaked and ready for cooking in 30 minutes instead of 8 hours.", recipe.getDescription());
            assertEquals(30, recipe.getPrepTime());
            assertEquals(60, recipe.getCookTime());
            assertEquals(4, recipe.getServings());
            assertEquals(true, recipe.isVegetarian());
            assertEquals(false, recipe.isVegan());
            assertEquals(true, recipe.isPublished());
            assertEquals("The Easy Way Vegetarian Cookbook", recipe.getCredit());
            assertEquals(17, recipe.getIngredients().size());
            assertEquals(6, recipe.getInstructions().size());

            RecipeIngredient recipeIngredient = recipe.getIngredients().get(12);
            assertEquals(true, recipeIngredient.isRequired());
            assertEquals("2.0000", recipeIngredient.getQuantity());
            assertEquals("teaspoon", recipeIngredient.getQuantityUnit());
            assertEquals(null, recipeIngredient.getQuantityDetail());
            assertEquals("Rosemary", recipeIngredient.getName());
            assertEquals("finely chopped", recipeIngredient.getPreparation());
            assertEquals((Integer) 12, recipeIngredient.getOrderBy());

            RecipeInstruction recipeInstruction = recipe.getInstructions().get(3);
            assertEquals(UUID.fromString("d2a094a6-a94b-11e6-ab83-0a0027000010"), recipeInstruction.getRecipeInstructionId());
            assertEquals(4, recipeInstruction.getStepNumber());
            assertEquals("Meanwhile, cook the spaghetti or linguine noodles in a separate pot.", recipeInstruction.getDescription());
        }
    }

    @Test
    public void aggregate_fetchById_SingleAggregateWithEmptyChildLists_returnsAggregateWithEmptyChildLists()
    {
        registerRecipeAggregate();

        try (PhotonConnection connection = photon.open())
        {
            Recipe recipe = connection
                .query(Recipe.class)
                .fetchById(UUID.fromString("28d8b2d4-90a7-467c-93a1-59d1493c0d15"));

            assertNotNull(recipe);
            assertEquals(UUID.fromString("28d8b2d4-90a7-467c-93a1-59d1493c0d15"), recipe.getRecipeId());
            assertEquals("AA", recipe.getName());
            assertEquals(0, recipe.getIngredients().size());
            assertEquals(0, recipe.getInstructions().size());
        }
    }

    @Test
    public void aggregate_fetchByIds_validAggregateAndQuery_returnsCorrectAggregates()
    {
        registerRecipeAggregate();

        try (PhotonConnection connection = photon.open())
        {
            List<Recipe> recipes = connection
                .query(Recipe.class)
                .fetchByIds(Arrays.asList(UUID.fromString("3e0378c5-a9b6-11e6-ab83-0a0027000010"), UUID.fromString("3e03cb62-a9b6-11e6-ab83-0a0027000010")));

            assertNotNull(recipes);

            assertEquals(2, recipes.size());

            Recipe recipe1 = recipes.get(0);
            assertEquals("Onion Gravy", recipe1.getName());
            assertEquals(8, recipe1.getIngredients().size());
            assertEquals(6, recipe1.getInstructions().size());

            RecipeIngredient recipeIngredient1 = recipe1.getIngredients().get(3);
            assertEquals(true, recipeIngredient1.isRequired());
            assertEquals("3.0000", recipeIngredient1.getQuantity());
            assertEquals("tablespoon", recipeIngredient1.getQuantityUnit());
            assertEquals(null, recipeIngredient1.getQuantityDetail());
            assertEquals("onions", recipeIngredient1.getName());
            assertEquals("finely chopped", recipeIngredient1.getPreparation());
            assertEquals((Integer) 3, recipeIngredient1.getOrderBy());

            Recipe recipe2 = recipes.get(1);
            assertEquals("Caesar Salad", recipe2.getName());
            assertEquals(11, recipe2.getIngredients().size());
            assertEquals(3, recipe2.getInstructions().size());

            RecipeInstruction recipeInstruction2 = recipe2.getInstructions().get(1);
            assertEquals(UUID.fromString("3771a1b6-1ebd-4e14-bec0-a9bd90c6e02c"), recipeInstruction2.getRecipeInstructionId());
            assertEquals(2, recipeInstruction2.getStepNumber());
            assertEquals("Heat a large skillet to medium-high heat. Add the olive oil. Mince and add the remaining garlic. Add the bread cubes. Cook them until they turn crusty and brown (like croutons!).", recipeInstruction2.getDescription());
        }
    }

    @Test
    public void aggregate_fetchById_validAggregateAndQueryWithDescendingSort_returnsCorrectAggregate()
    {
        registerRecipeAggregate(SortDirection.Descending);

        try (PhotonConnection connection = photon.open())
        {
            Recipe recipe = connection
                .query(Recipe.class)
                .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000010"));

            assertNotNull(recipe);
            assertEquals(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000010"), recipe.getRecipeId());
            assertEquals("Spaghetti with Lentil and Tomato Sauce", recipe.getName());
            assertEquals(17, recipe.getIngredients().size());
            assertEquals(6, recipe.getInstructions().size());

            for(int i = 0; i < recipe.getIngredients().size(); i++)
            {
                assertEquals((Integer) (recipe.getIngredients().size() - i - 1), recipe.getIngredients().get(i).getOrderBy());
            }

            for(int i = 0; i < recipe.getInstructions().size(); i++)
            {
                assertEquals(i + 1, recipe.getInstructions().get(i).getStepNumber());
            }
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
