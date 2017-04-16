package photon.tests.databaseintegrations.h2.recipe;

import org.apache.commons.lang3.math.Fraction;
import photon.Photon;
import photon.PhotonTransaction;
import photon.blueprints.SortDirection;
import photon.converters.Converter;
import photon.converters.ConverterException;
import photon.tests.databaseintegrations.h2.H2TestUtil;
import photon.tests.entities.recipe.Recipe;
import photon.tests.entities.recipe.RecipeIngredient;
import photon.tests.entities.recipe.RecipeInstruction;

import java.sql.Types;

public class RecipeDbSetup
{
    public static Photon setupDatabase()
    {
        Photon photon = new Photon(H2TestUtil.h2Url, H2TestUtil.h2User, H2TestUtil.h2Password);

        try(PhotonTransaction transaction = photon.beginTransaction())
        {
            transaction.query("DROP TABLE IF EXISTS recipe").executeUpdate();
            transaction.query("CREATE TABLE recipe (\n" +
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
            transaction.query("insert into recipe (recipeId, name, description, prepTime, cookTime, servings, isVegetarian, isVegan, isPublished, credit) values\n" +
                "(X'28D8B2D490A7467C93A159D1493C0D15','AA','BB','1','2','3','0','0','0',NULL),\n" +
                "(X'28D8B2D490A7467C93A159D1493C0D16','A','B','1','2','3','0','0','0',NULL),\n" +
                "(X'3DFFC3B3A9B611E6AB830A0027000010','Vegetable Fried Rice','Yummy cashews add a load of benefits to this simple recipe for fried rice.','20','15','6','1','1','1',NULL),\n" +
                "(X'3E0378C5A9B611E6AB830A0027000010','Onion Gravy','This is a very quick and easy vegetarian gravy recipe that doest use any strange ingredients. This recipe can be made vegan if you use vegan butter instead of margarine.','10','20','4','1','0','1','http://www.example.com/recipe/easy-vegetarian-gravy-226004'),\n" +
                "(X'3E038307A9B611E6AB830A0027000010','Spaghetti with Lentil and Tomato Sauce','We found this recipe in a clearance vegetarian cookbook and loved it. It has plenty of flavor and makes a ton of food, be prepared for lots of leftovers. Lentils are a great way to infuse protein into a vegan diet, and theyre super cheap and easy to cook. Unlike almost all other dry beans, lentils can be soaked and ready for cooking in 30 minutes instead of 8 hours.','30','60','4','1','0','1','The Easy Way Vegetarian Cookbook'),\n" +
                "(X'3E03CB62A9B611E6AB830A0027000010','Caesar Salad','This anchovy-free Caesar salad comes with homemade croutons and dressing.','35','20','6','1','0','1',NULL),\n" +
                "(X'3E0403AAA9B611E6AB830A0027000010','Mexican Rice and Bean Casserole','This casserole is low-cost, easy-to-make, and delicious. If you like Mexican food, chances are youll love this recipe.','20','45','6','1','0','1',NULL),\n" +
                "(X'3E040B3DA9B611E6AB830A0027000010','Slow Cooker Rice and Bean Casserole','This recipe is quick, easy, cheap, and tasty.','15','240','6','1','0','1',NULL),\n" +
                "(X'3E04169AA9B611E6AB830A0027000010','Mexican Spaghetti','Who needs meat sauce when you have this recipe? This is one vegetarian dish that definitely does not taste vegetarian.','15','30','8','1','1','1',NULL),\n" +
                "(X'3E041EB8A9B611E6AB830A0027000010','Vegetable Chop Suey','Cashews and an array of tasty vegetables make this recipe delicious.','20','10','6','1','1','1',NULL),\n" +
                "(X'3E042916A9B611E6AB830A0027000010','Vegan Chili','Zucchini and two kinds of beans make this recipe meaty without having any meat. Leave off the cheese and make it vegan!','10','30','8','1','1','1',NULL),\n" +
                "(X'3E0431C1A9B611E6AB830A0027000010','Texas Black Bean Burgers','These burgers are packed with flavor and taste just as good as a regular hamburger.','30','45','4','1','1','0',NULL)").executeUpdate();

            transaction.query("DROP TABLE IF EXISTS recipeingredient").executeUpdate();
            transaction.query("CREATE TABLE recipeingredient (\n" +
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
            transaction.query("insert into recipeingredient (recipeIngredientId, recipeId, isRequired, quantity, quantityUnit, quantityDetail, name, preparation, orderBy) values\n" +
                "(X'0AA82E11E97346BFA537C7EE34C65D87',X'3E03CB62A9B611E6AB830A0027000010','1',NULL,NULL,'','salt','to taste','8'),\n" +
                "(X'0CB17026B38A4C1B990AF626495581B9',X'3DFFC3B3A9B611E6AB830A0027000010','1','1/2',NULL,NULL,'red bell pepper','deseeded and diced','6'),\n" +
                "(X'154EE28CFF694ED189A76686BC38AAF2',X'28D8B2D490A7467C93A159D1493C0D16','0','1/3',NULL,NULL,'a',NULL,'0'),\n" +
                "(X'18896540C5EB4B1186964D83DF56EDD7',X'3E03CB62A9B611E6AB830A0027000010','1','6.0000','tablespoon',NULL,'parmesan cheese','grated','6'),\n" +
                "(X'1AE20A84876D4A30BDFE4BB8C5B25BC9',X'3E03CB62A9B611E6AB830A0027000010','1','6.0000','clove',NULL,'garlic','peeled','2'),\n" +
                "(X'264F77E1EF3846C98217A7C2032B70F2',X'3E03CB62A9B611E6AB830A0027000010','1','1.0000','head',NULL,'romaine lettuce','cut into pieces','7'),\n" +
                "(X'26DAD97BD5E94FB59F48D60A06C74F65',X'3E03CB62A9B611E6AB830A0027000010','1','0.7500','cup',NULL,'mayonnaise',NULL,'4'),\n" +
                "(X'290F2956734741508406A93C2082F672',X'3E03CB62A9B611E6AB830A0027000010','1','4.0000','cup',NULL,'slightly stale bread',NULL,'9'),\n" +
                "(X'3F9AA520DF69418E9BBDEF29FCB43193',X'3E03CB62A9B611E6AB830A0027000010','1','1.0000','tablespoon',NULL,'lemon juice',NULL,'3'),\n" +
                "(X'569FB967127C45A0AF98352425B18EB1',X'3DFFC3B3A9B611E6AB830A0027000010','0',NULL,NULL,NULL,'salt',NULL,'8'),\n" +
                "(X'596FA363A9B611E6AB830A0027000010',X'3E042916A9B611E6AB830A0027000010','1','0.3300','cup',NULL,'sugar',NULL,'10'),\n" +
                "(X'596FA493A9B611E6AB830A0027000010',X'3E042916A9B611E6AB830A0027000010','0','0.5000','cup',NULL,'shredded cheddar cheese',NULL,'9'),\n" +
                "(X'596FA5C0A9B611E6AB830A0027000010',X'3E0378C5A9B611E6AB830A0027000010','1','3.0000','tablespoon',NULL,'margarine',NULL,'2'),\n" +
                "(X'596FA6BBA9B611E6AB830A0027000010',X'3E0378C5A9B611E6AB830A0027000010','1','3.0000','tablespoon',NULL,'onions','finely chopped','3'),\n" +
                "(X'596FA7B6A9B611E6AB830A0027000010',X'3E0378C5A9B611E6AB830A0027000010','1','2.0000','clove',NULL,'garlic','minced','1'),\n" +
                "(X'596FA8B2A9B611E6AB830A0027000010',X'3E0378C5A9B611E6AB830A0027000010','1','3.0000','tablespoon',NULL,'flour',NULL,'0'),\n" +
                "(X'596FAA9EA9B611E6AB830A0027000010',X'3E0378C5A9B611E6AB830A0027000010','1','2.0000','tablespoon',NULL,'soy sauce',NULL,'6'),\n" +
                "(X'596FAB87A9B611E6AB830A0027000010',X'3E0378C5A9B611E6AB830A0027000010','1','1.0000','cup',NULL,'water',NULL,'7'),\n" +
                "(X'596FAC6AA9B611E6AB830A0027000010',X'3E0378C5A9B611E6AB830A0027000010','1',NULL,NULL,NULL,'salt',NULL,'5'),\n" +
                "(X'596FAD8FA9B611E6AB830A0027000010',X'3E0378C5A9B611E6AB830A0027000010','1',NULL,NULL,NULL,'pepper',NULL,'4'),\n" +
                "(X'596FAF54A9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1','1.0000','cup',NULL,'Lentils',NULL,'7'),\n" +
                "(X'596FB139A9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1','2.0000','tablespoon',NULL,'Olive Oil',NULL,'8'),\n" +
                "(X'59700B9CA9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1','1.0000',NULL,'Large','Onion',NULL,'9'),\n" +
                "(X'59700DABA9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1','2.0000',NULL,NULL,'Garlic Cloves','crushed','6'),\n" +
                "(X'59700F3EA9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1','2.0000',NULL,NULL,'Carrots','chopped','1'),\n" +
                "(X'597010C7A9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1','2.0000',NULL,NULL,'Celery Stalks','chopped','2'),\n" +
                "(X'5970123BA9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1','1.0000','can','28 oz.','Diced Tomatoes',NULL,'3'),\n" +
                "(X'597013ABA9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1','0.6700','cup',NULL,'Vegetable Stock',NULL,'15'),\n" +
                "(X'59701502A9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1','1.0000',NULL,NULL,'Red Bell Pepper','seeded and chopped','11'),\n" +
                "(X'5970165DA9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1','2.0000','tablespoon',NULL,'Tomato Paste',NULL,'14'),\n" +
                "(X'597017C6A9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1','2.0000','teaspoon',NULL,'Rosemary','finely chopped','12'),\n" +
                "(X'5970194FA9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1','1.0000','teaspoon',NULL,'Dried Oregano',NULL,'4'),\n" +
                "(X'59701AA2A9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1','10.0000','ounce',NULL,'Dried Spaghetti or Linguine',NULL,'5'),\n" +
                "(X'59701BD6A9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1','1.0000','tablespoon',NULL,'Basil Leaves','tor','0'),\n" +
                "(X'59701CB9A9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1',NULL,NULL,NULL,'Salt',NULL,'13'),\n" +
                "(X'59701D94A9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1',NULL,NULL,NULL,'Pepper',NULL,'10'),\n" +
                "(X'59701E6CA9B611E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1',NULL,NULL,NULL,'Vegetarian Parmesan-Style Cheese','to serve','16'),\n" +
                "(X'59702D0CA9B611E6AB830A0027000010',X'3E0403AAA9B611E6AB830A0027000010','1','1.0000','can','15 oz.','pinto beans','drained and rinsed','4'),\n" +
                "(X'59702F58A9B611E6AB830A0027000010',X'3E0403AAA9B611E6AB830A0027000010','1','1.0000','cup',NULL,'rice','cooked','5'),\n" +
                "(X'597030EFA9B611E6AB830A0027000010',X'3E0403AAA9B611E6AB830A0027000010','1','1.0000',NULL,NULL,'green pepper','chopped','3'),\n" +
                "(X'59703289A9B611E6AB830A0027000010',X'3E0403AAA9B611E6AB830A0027000010','1','1.0000','cup',NULL,'frozen cor',NULL,'1'),\n" +
                "(X'59703388A9B611E6AB830A0027000010',X'3E0403AAA9B611E6AB830A0027000010','1','1.0000','cup',NULL,'shredded Mexican cheese',NULL,'7'),\n" +
                "(X'59703467A9B611E6AB830A0027000010',X'3E0403AAA9B611E6AB830A0027000010','1','0.7500','cup',NULL,'sour cream',NULL,'8'),\n" +
                "(X'5970353FA9B611E6AB830A0027000010',X'3E0403AAA9B611E6AB830A0027000010','1','2.0000','tablespoon',NULL,'green onions','chopped','2'),\n" +
                "(X'59703613A9B611E6AB830A0027000010',X'3E0403AAA9B611E6AB830A0027000010','1','0.1200','teaspoon',NULL,'black pepper',NULL,'0'),\n" +
                "(X'597036F2A9B611E6AB830A0027000010',X'3E0403AAA9B611E6AB830A0027000010','0',NULL,NULL,NULL,'salt',NULL,'6'),\n" +
                "(X'597037CEA9B611E6AB830A0027000010',X'3E040B3DA9B611E6AB830A0027000010','1','1.0000','cup',NULL,'rice','uncooked','5'),\n" +
                "(X'59703988A9B611E6AB830A0027000010',X'3E040B3DA9B611E6AB830A0027000010','1','0.5000','cup',NULL,'onion','chopped','4'),\n" +
                "(X'59703A64A9B611E6AB830A0027000010',X'3E040B3DA9B611E6AB830A0027000010','1','0.6700','cup',NULL,'frozen cor',NULL,'2'),\n" +
                "(X'59703B38A9B611E6AB830A0027000010',X'3E040B3DA9B611E6AB830A0027000010','1','2.0000','clove',NULL,'garlic','minced','3'),\n" +
                "(X'59703C10A9B611E6AB830A0027000010',X'3E040B3DA9B611E6AB830A0027000010','1','1.0000','can','14.5 oz.','stewed tomatoes',NULL,'7'),\n" +
                "(X'59703D44A9B611E6AB830A0027000010',X'3E040B3DA9B611E6AB830A0027000010','1','1.0000','can','15 oz.','chili beans (spicy)',NULL,'0'),\n" +
                "(X'59703E4AA9B611E6AB830A0027000010',X'3E040B3DA9B611E6AB830A0027000010','1','1.0000','can','4.5 oz.','chopped green chilies',NULL,'1'),\n" +
                "(X'59703F17A9B611E6AB830A0027000010',X'3E040B3DA9B611E6AB830A0027000010','1','0.5000','cup',NULL,'shredded cheddar cheese',NULL,'6'),\n" +
                "(X'59703FE1A9B611E6AB830A0027000010',X'3E040B3DA9B611E6AB830A0027000010','1','1.0000','cup',NULL,'vegetable broth',NULL,'9'),\n" +
                "(X'597040B9A9B611E6AB830A0027000010',X'3E040B3DA9B611E6AB830A0027000010','1','0.2500','teaspoon',NULL,'tumeric',NULL,'8'),\n" +
                "(X'5970418DA9B611E6AB830A0027000010',X'3E04169AA9B611E6AB830A0027000010','1','1.0000','cup',NULL,'textured vegetable protein (TVP)','rehydrated (see TVPs package directions)','11'),\n" +
                "(X'59704379A9B611E6AB830A0027000010',X'3E04169AA9B611E6AB830A0027000010','1','1.0000',NULL,NULL,'medium yellow onion','chopped','6'),\n" +
                "(X'59704451A9B611E6AB830A0027000010',X'3E04169AA9B611E6AB830A0027000010','1','1.0000','jar','26 oz.','spaghetti sauce',NULL,'10'),\n" +
                "(X'5970451EA9B611E6AB830A0027000010',X'3E04169AA9B611E6AB830A0027000010','1','1.0000','can','14.5 oz.','black beans','drained and rinsed','0'),\n" +
                "(X'597045EFA9B611E6AB830A0027000010',X'3E04169AA9B611E6AB830A0027000010','1','1.0000','cup',NULL,'frozen cor','thawed','5'),\n" +
                "(X'597046C0A9B611E6AB830A0027000010',X'3E04169AA9B611E6AB830A0027000010','1','1.0000','can','4 oz.','chopped green chilies',NULL,'3'),\n" +
                "(X'59704789A9B611E6AB830A0027000010',X'3E04169AA9B611E6AB830A0027000010','1','1.0000','tablespoon',NULL,'olive oil',NULL,'7'),\n" +
                "(X'59704857A9B611E6AB830A0027000010',X'3E04169AA9B611E6AB830A0027000010','1',NULL,NULL,NULL,'spaghetti noodles',NULL,'9'),\n" +
                "(X'5970491DA9B611E6AB830A0027000010',X'3E04169AA9B611E6AB830A0027000010','1','1.0000','tablespoon',NULL,'chili powder',NULL,'2'),\n" +
                "(X'597049E6A9B611E6AB830A0027000010',X'3E04169AA9B611E6AB830A0027000010','1','0.2500','cup',NULL,'salsa',NULL,'8'),\n" +
                "(X'59704AADA9B611E6AB830A0027000010',X'3E04169AA9B611E6AB830A0027000010','1','1.0000','can','14.5 oz.','diced tomatoes','undrained','4'),\n" +
                "(X'59704C4BA9B611E6AB830A0027000010',X'3E04169AA9B611E6AB830A0027000010','1','0.2500','teaspoon',NULL,'black pepper',NULL,'1'),\n" +
                "(X'59704D18A9B611E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','1','1.0000',NULL,NULL,'onion','chopped','8'),\n" +
                "(X'59704E88A9B611E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','1','5.0000','ounce',NULL,'bean sprouts',NULL,'0'),\n" +
                "(X'59704F6EA9B611E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','1','3.0000','ounce',NULL,'garlic','minced','6'),\n" +
                "(X'59705046A9B611E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','1','1.0000',NULL,NULL,'green pepper','diced','7'),\n" +
                "(X'5970511AA9B611E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','1','1.0000',NULL,NULL,'red bell pepper','diced','9'),\n" +
                "(X'597051F6A9B611E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','1','1.0000',NULL,NULL,'zucchini','diced','14'),\n" +
                "(X'597052CAA9B611E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','1','3.0000','ounce',NULL,'brocculi florets',NULL,'2'),\n" +
                "(X'5970539EA9B611E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','1','1.0000',NULL,NULL,'carrot','julienned','4'),\n" +
                "(X'59705CC9A9B611E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','1','0.5000','cup',NULL,'vegetable stock',NULL,'13'),\n" +
                "(X'59705E9CA9B611E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','1','2.0000','teaspoon',NULL,'brown sugar',NULL,'3'),\n" +
                "(X'59705F74A9B611E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','1','0.5000','cup',NULL,'cashews',NULL,'5'),\n" +
                "(X'59706048A9B611E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','1','2.0000','tablespoon',NULL,'soy sauce',NULL,'11'),\n" +
                "(X'5970612EA9B611E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','1','2.0000','tablespoon',NULL,'vegetable oil',NULL,'12'),\n" +
                "(X'59706206A9B611E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','0',NULL,NULL,NULL,'black pepper',NULL,'1'),\n" +
                "(X'597062DAA9B611E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','1','0.7500','cup',NULL,'rice','uncooked','10'),\n" +
                "(X'597063AFA9B611E6AB830A0027000010',X'3E042916A9B611E6AB830A0027000010','1','1.0000','can','28 oz.','crushed tomatoes',NULL,'2'),\n" +
                "(X'5970648EA9B611E6AB830A0027000010',X'3E042916A9B611E6AB830A0027000010','1','1.0000','can','6 oz.','tomato paste',NULL,'11'),\n" +
                "(X'59706570A9B611E6AB830A0027000010',X'3E042916A9B611E6AB830A0027000010','1','2.0000','can','15 oz.','pinto beans','drained and rinsed','7'),\n" +
                "(X'59706727A9B611E6AB830A0027000010',X'3E042916A9B611E6AB830A0027000010','1','1.0000','can','16 oz.','kidney beans','drained and rinsed','5'),\n" +
                "(X'597067FBA9B611E6AB830A0027000010',X'3E042916A9B611E6AB830A0027000010','1','1.0000','can','15 oz.','yello hominy','drained','13'),\n" +
                "(X'597068D3A9B611E6AB830A0027000010',X'3E042916A9B611E6AB830A0027000010','1','1.0000','can','4 oz.','chopped green chilies',NULL,'1'),\n" +
                "(X'59706ACAA9B611E6AB830A0027000010',X'3E042916A9B611E6AB830A0027000010','1','1.0000',NULL,NULL,'medium onion','chopped','6'),\n" +
                "(X'59706C0FA9B611E6AB830A0027000010',X'3E042916A9B611E6AB830A0027000010','1','2.0000','can',NULL,'zucchini','chopped','14'),\n" +
                "(X'59706CEBA9B611E6AB830A0027000010',X'3E042916A9B611E6AB830A0027000010','1','1.5000','cup',NULL,'water',NULL,'12'),\n" +
                "(X'59706DCDA9B611E6AB830A0027000010',X'3E042916A9B611E6AB830A0027000010','1','1.0000','teaspoon',NULL,'cumin',NULL,'3'),\n" +
                "(X'59706E9BA9B611E6AB830A0027000010',X'3E042916A9B611E6AB830A0027000010','1','1.0000','teaspoon',NULL,'salt',NULL,'8'),\n" +
                "(X'59706F68A9B611E6AB830A0027000010',X'3E042916A9B611E6AB830A0027000010','1','2.0000','teaspoon',NULL,'chili powder',NULL,'0'),\n" +
                "(X'59707032A9B611E6AB830A0027000010',X'3E042916A9B611E6AB830A0027000010','1','0.5000','teaspoon',NULL,'garlic powder',NULL,'4'),\n" +
                "(X'5E388901DF744F92982335A63A2DEE5C',X'3DFFC3B3A9B611E6AB830A0027000010','1','1','teaspoon',NULL,'fresh ginger','minced','2'),\n" +
                "(X'6693FE39EC6049C89A86A45CB9DEF0A8',X'3DFFC3B3A9B611E6AB830A0027000010','1','3/4','cup',NULL,'cashews',NULL,'1'),\n" +
                "(X'717FE01988484F118EC69E9834A93A88',X'3DFFC3B3A9B611E6AB830A0027000010','1','1',NULL,NULL,'green onion','finely chopped','4'),\n" +
                "(X'71F2D8F7354C41F8B783A4DFD673796A',X'3DFFC3B3A9B611E6AB830A0027000010','1','2','tablespoon',NULL,'vegetable oil',NULL,'10'),\n" +
                "(X'73BB16BE2CF34449A26174DB76D2DC87',X'3E03CB62A9B611E6AB830A0027000010','1','0.2500','cup',NULL,'olive oil',NULL,'5'),\n" +
                "(X'8948352F21564B678F297553F76894DA',X'3DFFC3B3A9B611E6AB830A0027000010','1','1/2','cup',NULL,'frozen corn or peas',NULL,'3'),\n" +
                "(X'8BE27DE2A7694234B153C40D92506E4C',X'3E03CB62A9B611E6AB830A0027000010','1','1.0000','teaspoon',NULL,'dijon mustard',NULL,'1'),\n" +
                "(X'986BD8F5E0B24266A0714ACDB7017803',X'3E03CB62A9B611E6AB830A0027000010','1',NULL,NULL,NULL,'black pepper','to taste','0'),\n" +
                "(X'9A71A25750D14995BA55E964E2BB8AAD',X'3DFFC3B3A9B611E6AB830A0027000010','1',NULL,NULL,NULL,'black pepper','to taste','0'),\n" +
                "(X'A65C66E822A9426E9000ED5CFDC5E957',X'28D8B2D490A7467C93A159D1493C0D16','0','333/1000',NULL,NULL,'d',NULL,'3'),\n" +
                "(X'B2DD5DBA8BD944AABC8CAFA0763A3F8C',X'28D8B2D490A7467C93A159D1493C0D16','0',NULL,NULL,NULL,'c',NULL,'2'),\n" +
                "(X'B5B36B5C66CF4F548C123BDD3FD3E894',X'3DFFC3B3A9B611E6AB830A0027000010','1','2','teaspoon',NULL,'soy sauce, thick soy sauce, vegetarian oyster sauce, or a combination thereof',NULL,'9'),\n" +
                "(X'BE53DD295468423A846161BA196B8631',X'3DFFC3B3A9B611E6AB830A0027000010','1','3 1/2','cup',NULL,'rice','previously cooked','7'),\n" +
                "(X'C33A3C2BE272430BA88815383985D406',X'28D8B2D490A7467C93A159D1493C0D16','0','3 5/6',NULL,NULL,'b',NULL,'1'),\n" +
                "(X'DF5EBAB85E6A4694B539B7610CA8DB86',X'3DFFC3B3A9B611E6AB830A0027000010','1','1',NULL,NULL,'medium onion','diced','5'),\n" +
                "(X'E01AE10E897C49E897CE38D89460610C',X'3E03CB62A9B611E6AB830A0027000010','1','1.0000','teaspoon',NULL,'worcestershire sauce',NULL,'10');").executeUpdate();

            transaction.query("DROP TABLE IF EXISTS recipeinstruction").executeUpdate();
            transaction.query("CREATE TABLE recipeinstruction (\n" +
                "  recipeInstructionId binary(16) NOT NULL,\n" +
                "  recipeId binary(16) NOT NULL,\n" +
                "  stepNumber int(10) unsigned NOT NULL,\n" +
                "  description text NOT NULL,\n" +
                "  PRIMARY KEY (recipeInstructionId),\n" +
                "  CONSTRAINT RecipeInstruction_Recipe FOREIGN KEY (recipeId) REFERENCES recipe (recipeId) ON DELETE NO ACTION ON UPDATE CASCADE\n" +
                ")").executeUpdate();
            transaction.query("insert into recipeInstruction (recipeInstructionId, recipeId, stepNumber, description) values\n" +
                "(X'3771A1B61EBD4E14BEC0A9BD90C6E02C',X'3E03CB62A9B611E6AB830A0027000010','2','Heat a large skillet to medium-high heat. Add the olive oil. Mince and add the remaining garlic. Add the bread cubes. Cook them until they turn crusty and brown (like croutons!).'),\n" +
                "(X'3AD748177FF549029A0133D55083FE52',X'3DFFC3B3A9B611E6AB830A0027000010','4','Stir in soy sauce and pepper. Stir in cashews. Stir in green onion. Taste and add seasoning if desired.'),\n" +
                "(X'4C61288ACF58458FB5017D6872661D7A',X'3DFFC3B3A9B611E6AB830A0027000010','3','Add rice in the middle of the pan. Stir until heated through (about 2 minutes);, using a spatula to turn and move the rice around the pan.'),\n" +
                "(X'7C399CBE33364F5589C39CD404B3A615',X'3E03CB62A9B611E6AB830A0027000010','1','To create the dressing, mince 3 gloves of garlic and put it into a bowl. Add the mayo, 2 tablespoons of the cheese, lemon juice, Worcestershire sauce, and mustard. Add salt and pepper to taste. Refrigerate until ready to eat.'),\n" +
                "(X'B17C4CFF6D814C2E9D80800CEE796BC3',X'28D8B2D490A7467C93A159D1493C0D16','1','gggg'),\n" +
                "(X'B228B7CD7B6142D8B385B0A17FE41ECF',X'3E03CB62A9B611E6AB830A0027000010','3','In a large bowl, add the lettuce and toss in the dressing. Add the remaining cheese. Add the bread cubes.'),\n" +
                "(X'BDD28F56385A450DA76770513FFA8B65',X'3DFFC3B3A9B611E6AB830A0027000010','2','Push the onion to the sides of the wok. Add the frozen corn or peas to the middle and stir-fry for 1 minute. Push to the sides, add red bell pepper, and stir-fry for 1 minute.'),\n" +
                "(X'D29929DCA94B11E6AB830A0027000010',X'3E0403AAA9B611E6AB830A0027000010','1','Preheat oven to 350°F. Spray a 2-quart baking dish with cooking spray to coat.'),\n" +
                "(X'D29976EAA94B11E6AB830A0027000010',X'3E0403AAA9B611E6AB830A0027000010','2','In a large bowl, add beans, rice, 3/4 cup cheese, sour cream, corn, green pepper, green onions, black pepper, chili powder, and salt (optional);. Stir until mixed.'),\n" +
                "(X'D299C52DA94B11E6AB830A0027000010',X'3E0403AAA9B611E6AB830A0027000010','3','Pour the mixture into the baking dish. Bake for 30 minutes. Add remaining cheese and bake for an additional 10 minutes.'),\n" +
                "(X'D29A1BBBA94B11E6AB830A0027000010',X'3E040B3DA9B611E6AB830A0027000010','1','Spray large skillet with cooking spray. Heat to medium-high on the stove. Add rice, stir it continuously until it turns light brown (be careful, the rice will burn very easily);. Add the rice to the slow cooker.'),\n" +
                "(X'D29A7569A94B11E6AB830A0027000010',X'3E040B3DA9B611E6AB830A0027000010','2','Add all remaining ingredients to the slow cooker except the cheese and stewed tomatoes. Drain the stewed tomatoes into the slow cooker. Place the stewed tomatoes on a cutting board and cut them into bit-sized pieces. Add them to the slow cooker.'),\n" +
                "(X'D29AC6FCA94B11E6AB830A0027000010',X'3E040B3DA9B611E6AB830A0027000010','3','Cover and cook on low for at least 4 hours. Stir, add cheese, then cook until the cheese is melted (about 10 minutes);.'),\n" +
                "(X'D29B1DDFA94B11E6AB830A0027000010',X'3E04169AA9B611E6AB830A0027000010','1','In a large skillet, add olive oil and heat over medium-high. Add onion and cook until soft.'),\n" +
                "(X'D29B6949A94B11E6AB830A0027000010',X'3E04169AA9B611E6AB830A0027000010','2','Cook spaghetti noodles in a separate pot.'),\n" +
                "(X'D29BB141A94B11E6AB830A0027000010',X'3E04169AA9B611E6AB830A0027000010','3','In a large saucepan, combine all ingredients except the spaghetti noodles. Bring to a boil. Reduce heat and simmer uncovered for about 30 minutes.'),\n" +
                "(X'D29BFC64A94B11E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','1','In a large wok, add oil and heat on medium-high.'),\n" +
                "(X'D29C44C6A94B11E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','2','Cook the rice in a rice cooker or in a separate pot.'),\n" +
                "(X'D29C9FB6A94B11E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','3','Reduce heat to medium. add onion and garlic. Stir for about 2 minutes (careful to not let the garlic sit as it will burn);.'),\n" +
                "(X'D29CEBB8A94B11E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','4','Add broccoli, zucchini, carrots, and peppers. Stir for about 3 more minutes.'),\n" +
                "(X'D29D38AAA94B11E6AB830A0027000010',X'3E041EB8A9B611E6AB830A0027000010','5','Add all remaining ingredients. Stir fry for several minutes. For softer vegetables, cover and let simmer for about 10 minutes (recommended);.'),\n" +
                "(X'D29D8E5CA94B11E6AB830A0027000010',X'3E042916A9B611E6AB830A0027000010','1','In a large pot, add all ingredients except the cheese. Bring to a boil. Reduce heat, cover, and simmer for 30 minutes. Top with cheese if desired (not vegan obviously);.'),\n" +
                "(X'D29DDC74A94B11E6AB830A0027000010',X'3E0378C5A9B611E6AB830A0027000010','1','Put margarine in a pot and saute the onions and garlic over med-high heat.'),\n" +
                "(X'D29E24F3A94B11E6AB830A0027000010',X'3E0378C5A9B611E6AB830A0027000010','2','Reduce heat back to medium after onions and garlic have become lden brown.'),\n" +
                "(X'D29E6C65A94B11E6AB830A0027000010',X'3E0378C5A9B611E6AB830A0027000010','3','Make a roux by gradually adding the flour, while continuously stirring to avoid lumps.'),\n" +
                "(X'D29EB59CA94B11E6AB830A0027000010',X'3E0378C5A9B611E6AB830A0027000010','4','Still stirring, add soy sauce and water to the mixture.'),\n" +
                "(X'D29F050BA94B11E6AB830A0027000010',X'3E0378C5A9B611E6AB830A0027000010','5','Add salt and pepper to taste.'),\n" +
                "(X'D29F50F4A94B11E6AB830A0027000010',X'3E0378C5A9B611E6AB830A0027000010','6','Once the gravy has reached desired thickness, turn off the stove and you are done!'),\n" +
                "(X'D29F9CD9A94B11E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','1','Put the lentils in a saucepan and cover with cold water. Add extra water as the lentils will absorb a lot of it. Boil and simmer for 30 minutes or until tender.'),\n" +
                "(X'D29FF0DFA94B11E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','2','Meanwhile, heat the oil in a large saucepan. Add the onion, garlic, carrots, and celery. Cover and cook over low heat for 5 minutes.'),\n" +
                "(X'D2A04976A94B11E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','3','Stir in the tomatoes, red bell pepper, tomato paste, rosemary, and oregano. Cover and simmer for 20 minutes, until the sauce is thickened and the vegetables are softened (check the carrots);.'),\n" +
                "(X'D2A094A6A94B11E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','4','Meanwhile, cook the spaghetti or linguine noodles in a separate pot.'),\n" +
                "(X'D2A30BB1A94B11E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','5','Drain the lentils and add them. Cook for 5 minutes. Season with salt and pepper.'),\n" +
                "(X'D2A36443A94B11E6AB830A0027000010',X'3E038307A9B611E6AB830A0027000010','6','Serve the sauce over noodles. Sprinkle with basil and parmesan-style cheese.'),\n" +
                "(X'F018A06DC36343BC8C62340E8A3F011A',X'3DFFC3B3A9B611E6AB830A0027000010','1','Heat oil in preheated wok on medium-high heat. Add ginger, stir until fragrant. Add onion, stir-fry for about 2 minutes until softened.')").executeUpdate();

            transaction.commit();
        }

        return photon;
    }

    public static void registerRecipeAggregate(Photon photon)
    {
        registerRecipeAggregate(photon, SortDirection.Ascending);
    }

    public static void registerRecipeAggregate(Photon photon, SortDirection ingredientSortDirection)
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
            .withColumnDataType("quantity", Types.VARCHAR)
            .withCustomToFieldValueConverter("quantity", new Converter()
            {
                @Override
                public Object convert(Object val) throws ConverterException
                {
                    return val != null ? Fraction.getFraction((String) val) : null;
                }
            })
            .withOrderBy("orderBy", ingredientSortDirection)
            .addAsChild("ingredients")
            .register();
    }
}
