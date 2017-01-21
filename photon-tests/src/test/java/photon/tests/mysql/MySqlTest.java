package photon.tests.mysql;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.junit.Test;
import photon.Photon;
import photon.transaction.PhotonTransaction;
import photon.blueprints.SortDirection;
import photon.tests.entities.Recipe;
import photon.tests.entities.RecipeIngredient;
import photon.tests.entities.RecipeInstruction;

import java.sql.Types;
import java.util.UUID;

public class MySqlTest
{
    @Test
    public void test()
    {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setDatabaseName("veganexp");
        dataSource.setUser("root");
        dataSource.setPassword("bears");
        dataSource.setServerName("localhost");

        Photon photon = new Photon(dataSource);

        photon.registerAggregate(Recipe.class)
            .withId("recipeId")
            .withChild(RecipeInstruction.class)
                .withId("recipeInstructionId")
                .withColumnDataType("recipeInstructionId", Types.BINARY)
                .withForeignKeyToParent("recipeId")
                .withColumnDataType("recipeId", Types.BINARY)
                .withOrderBy("stepNumber", SortDirection.Ascending)
                .addAsChild("instructions")
            .withChild(RecipeIngredient.class)
                .withId("recipeIngredientId")
                .withColumnDataType("recipeIngredientId", Types.BINARY)
                .withForeignKeyToParent("recipeId")
                .withColumnDataType("recipeId", Types.BINARY)
                .withOrderBy("orderBy")
                .addAsChild("ingredients")
            .register();

        System.out.println(photon);

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            Recipe recipe = transaction
                .aggregateQuery(Recipe.class)
                .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000010"));
            System.out.println(recipe);

            //transaction.saveAggregate(recipe);
        }
    }
}
