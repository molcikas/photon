package photon.perf;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import photon.Photon;
import photon.PhotonConnection;

import javax.sql.DataSource;

public class RecipeDbSetup
{
    private static final String h2Url = "jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1";
    private static final String h2User = "sa";
    private static final String h2Password = "";

    public static DataSource createDataSource()
    {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(h2Url);
        hikariConfig.setUsername(h2User);
        hikariConfig.setPassword(h2Password);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return new HikariDataSource(hikariConfig);
    }

    public static void setupDatabase()
    {
        Photon photon = new Photon(h2Url, h2User, h2Password);

        try(PhotonConnection connection = photon.open())
        {
            connection.query("DROP TABLE IF EXISTS recipe").executeUpdate();
            connection.query("CREATE TABLE recipe (\n" +
                "  recipeId binary(16) NOT NULL,\n" +
                "  name varchar(50) NOT NULL,\n" +
                "  description text NOT NULL,\n" +
                "  prepTime int(10) unsigned NOT NULL,\n" +
                "  cookTime int(10) unsigned NOT NULL,\n" +
                "  servings int(10) unsigned NOT NULL,\n" +
                "  isVegetarian tinyint(1) NOT NULL,\n" +
                "  isVegan tinyint(1) NOT NULL,\n" +
                "  isPublished tinyint(1) NOT NULL,\n" +
                "  credit varchar(512) DEFAULT NULL,\n" +
                "  PRIMARY KEY (recipeId)\n" +
                ")").executeUpdate();

            connection.query("DROP TABLE IF EXISTS recipeingredient").executeUpdate();
            connection.query("CREATE TABLE recipeingredient (\n" +
                "  recipeIngredientId binary(16) NOT NULL,\n" +
                "  recipeId binary(16) NOT NULL,\n" +
                "  isRequired tinyint(1) NOT NULL,\n" +
                "  quantity varchar(64) DEFAULT NULL,\n" +
                "  quantityUnit varchar(64) DEFAULT NULL,\n" +
                "  quantityDetail varchar(64) DEFAULT NULL,\n" +
                "  name varchar(128) NOT NULL,\n" +
                "  preparation varchar(128) DEFAULT NULL,\n" +
                "  orderBy int(11) NOT NULL,\n" +
                "  PRIMARY KEY (recipeIngredientId),\n" +
                "  CONSTRAINT RecipeIngredient_Recipe FOREIGN KEY (recipeId) REFERENCES recipe (recipeId) ON DELETE NO ACTION ON UPDATE CASCADE\n" +
                ")").executeUpdate();

            connection.query("DROP TABLE IF EXISTS recipeinstruction").executeUpdate();
            connection.query("CREATE TABLE recipeinstruction (\n" +
                "  recipeInstructionId binary(16) NOT NULL,\n" +
                "  recipeId binary(16) NOT NULL,\n" +
                "  stepNumber int(10) unsigned NOT NULL,\n" +
                "  description text NOT NULL,\n" +
                "  PRIMARY KEY (recipeInstructionId),\n" +
                "  CONSTRAINT RecipeInstruction_Recipe FOREIGN KEY (recipeId) REFERENCES recipe (recipeId) ON DELETE NO ACTION ON UPDATE CASCADE\n" +
                ")").executeUpdate();
        }
    }
}
