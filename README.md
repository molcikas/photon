# Photon [![Build Status](https://travis-ci.org/molcikas/photon.svg?branch=master)](https://travis-ci.org/molcikas/photon) [![Code Coverage](https://img.shields.io/codecov/c/github/molcikas/photon/master.svg)](https://codecov.io/github/molcikas/photon?branch=master)

Photon is a unique Java ORM that aims to combine the best aspects of traditional and micro ORMs. Photon supports some of the features of traditional ORMs, such as eager loading and change tracking, but it also feels and acts like a micro ORM. When you want to do a query, you write real SQL, not a custom query language that isn't quite SQL.

Traditional ORMs can be complex and cryptic. Since they hide the SQL they are executing behind leaky abstractions, diagnosing and troubleshooting issues requires deep knowledge about the inner workings of the ORM. Photon queries are just plain SQL, so you always know exactly what query the ORM is running.

Photon is the only Java ORM that supports a fluent API. Other ORMs require the object-to-table mapping to be specified in XML or using annotations. XML is error prone and difficult to maintain. Annotations clutter domain entities with persistence details and require the entities to be modeled after the database tables rather than the business domain. A fluent API allows entities to remain free from the details of how they are persisted.

Micro ORMs give developers greater control of the SQL but can be cumbersome to use, especially when loading and saving clusters of entities ("[aggregates](https://martinfowler.com/bliki/DDD_Aggregate.html)" in DDD terms). Photon gives developers the ability to specify relationships between entities in an aggregate so that you don't have to write cumbersome queries to do basic CRUD operations.

The goal of Photon is to capture the best of the micro and tradition ORMs by giving developers control over the SQL executed while still providing an easy way to do routine CRUD operations on aggregates.

## Getting Started

### Installation

The latest JAR is available in the [Maven Central Repository](https://mvnrepository.com/artifact/com.github.molcikas/photon-core).

### Initializing Photon

Construct a `Photon` object with a `DataSource` (which can be retrieved from connection poolers like `HikariCP`) or a JDBC url, username, and password. Then, register your aggregates using `registerAggregate()`.

```java
HikariConfig hikariConfig = new HikariConfig();
hikariConfig.setJdbcUrl(databaseUrl);
hikariConfig.setUsername(databaseUser);
hikariConfig.setPassword(databasePassword);
hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

Photon photon = new Photon(new HikariDataSource(hikariConfig));
```

### Registering Aggregates

After constructing the `Photon` object, register each aggregate by describing how the root entity and sub entities are mapped to database tables. For example:

```java
photon.registerAggregate(Recipe.class)
    .withId("recipeId")
    .withChild("instructions", RecipeInstruction.class)
        .withForeignKeyToParent("recipeId")
        .withOrderBySql("stepNumber")
        .addAsChild()
    .withChild("ingredients", RecipeIngredient.class)
        .withForeignKeyToParent("recipeId")
        .withDatabaseColumn("quantity", ColumnDataType.INTEGER)
        .withFieldHydrater("quantity", val -> val != null ? Fraction.getFraction((String) val) : null)
        .withOrderBySql("RecipeIngredient.orderBy DESC")
        .addAsChild()
    .register();
```

To keep boilerplate to a minimum, by default, each field (public or private) is implicitly mapped to a database column of the same name and equivalent data type.

### Creating and Committing Transactions

Every SQL command and query must be executed in the context of a transaction. The transaction must have an explicit `commit()` for changes to saved in the database. Otherwise, the transaction is automatically rolled back (which can be useful in read-only transactions to ensure nothing was accidentally changed).

```java
try (PhotonTransaction transaction = photon.beginTransaction())
{
    // ... Queries, inserts, updates, deletes, and other orchestration logic ...
    
    transaction.commit(); // Omitting the commit causes the transaction to be rolled back.
}
```

### Querying and Updating [Aggregates](https://martinfowler.com/bliki/DDD_Aggregate.html)

Aggregates are queried by ID and should be loaded and saved as whole units. Lazy loading is not supported. This helps ensure that the invariants are properly enforced by the aggregate and ensures that the aggregate is saved in a consistent state into the database.

```java
try(PhotonTransaction transaction = photon.beginTransaction())
{
    MyTable myTable = new MyTable(2, "MySavedValue");
    transaction.insert(myTable); // Use "save" instead of "insert" if updating an aggregate.
    transaction.commit();
}
```

```java
try(PhotonTransaction transaction = photon.beginTransaction())
{
    MyTable myTable = transaction
        .query(MyTable.class)
        .fetchById(2);

    return myTable;
}
```

Photon provides an easy interface for fetching aggregates using a `SELECT` statement:

```java
try(PhotonTransaction transaction = photon.beginTransaction())
{
    String sql =
        "SELECT Product.id " +
        "FROM Product " +
        "JOIN ProductOrders ON ProductOrders.productId = Product.id " +
        "JOIN Orders ON Orders.id = ProductOrder.orderId " +
        "WHERE Orders.total > :orderTotal ";
    
    List<Product> productsInLargeOrders = transaction
        .query(MyTable.class)
        .whereIdIn(sql)
        .addParameter("orderTotal", 1000)
        .fetchList();
    
    return productsInLargeOrders;
}
```

## Beyond the Basics

### Constructing View Models using SQL

User interfaces often need to display data from many different tables. Or, they need to show summaries and aggregations of data, such as averages, counts, or sums. One method for getting this data is to select all the entities that contain the necessary data and construct the view model in code. But there are several problems with this approach. First, you end up selecting more data than you need from the database, which creates unnecessary latency in your query and load on your database and application servers. Second, your entity object graphs become too large because they have to support these queries. A better approach is to have separate models that are specifically for displaying and reporting.

Photon makes it easy to construct custom view models using plain SQL.

```java
try(PhotonTransaction transaction = photon.beginTransaction())
{
    String sql =
        "SELECT Product.id, Product.name, Order.orderDate " +
        "FROM Product " +
        "JOIN ProductOrders ON ProductOrders.productId = Product.id " +
        "JOIN Orders ON Orders.id = ProductOrder.orderId " +
        "WHERE Orders.orderDate > :orderDate " +
        "ORDER Orders.orderDate DESC ";

    List<ProductOrderDto> productOrdersAfterDate = transaction
        .query(sql)
        .addParameter("orderDate", new Date("2017/06/15"))
        .fetchList(ProductOrderDto.class);
    
    return productOrdersAfterDate;
}
```

Query results are automatically mapped to the view model fields by name. Unlike aggregates, view models do not need to be pre-registered with Photon.

It's also possible to fetch scalar values and lists using plain SQL:

```java
try(PhotonTransaction transaction = photon.beginTransaction())
{
    String sql =
        "SELECT DISTINCT name " +
        "FROM Product " +
        "WHERE CHAR_LENGTH(name) > :nameLength ";

    List<String> longProductNames = transaction
        .query(sql)
        .addParameter("nameLength", 8)
        .fetchScalarList(String.class);
    
    return longProductNames;
}
```

If you want to construct a view model that consists of root objects each containing a list of child objects, you could write multiple `SELECT` statements and aggregate the data yourself into a single DTO, but this quickly becomes tedious and error prone. Photon offers support for constructing aggregate view models so that you only have to write one `SELECT` statement. Aggregate view models use the same builder as regular aggregates.

```java
public class ProductOrdersDto
{
    public long productId;
    
    public String productName;
    
    public List<ProductOrderDto> productOrders;
}

public class ProductOrderDto
{
    public long orderId;
    
    public Date orderDate;
}
```

```java
// Register the view model aggregate. You can re-use the same DTO classes in multiple view model aggregates (e.g. if you want different
// view models with different sort orders), just give each aggregate view model a unique name.

photon
    .registerViewModelAggregate(ProductOrdersDto.class, "ProductOrdersMostRecentFirst")
    .withId("productId")
    .withChild("productOrders", ProductOrderDto.class)
        .withForeignKeyToParent("productOrderId")
        .withOrderBySql("Order.orderDate DESC")
        .addAsChild()
    .register();

// Create a query similar to querying for a regular aggregate.

try(PhotonTransaction transaction = photon.beginTransaction())
{
    String sql =
        "SELECT Product.id " +
        "FROM Product " +
        "JOIN ProductOrders ON ProductOrders.productId = Product.id " +
        "JOIN Orders ON Orders.id = ProductOrder.orderId " +
        "GROUP BY Product.id " +
        "HAVING COUNT(DISTINCT Orders.id) >= :minOrderCount ";

    List<ProductOrdersDto> productOrders = transaction
        .query(ProductOrdersDto.class, "ProductOrdersMostRecentFirst")
        .whereIdIn(sql)
        .addParameter("minOrderCount", 3)
        .fetchList();
    
    return productOrders;
}
```

### Working with Value Objects

ORMs typically very little support for [value objects](https://en.wikipedia.org/wiki/Value_object). They expect you to only use primitive values likes strings and integers in your entities. Photon provides several mechanisms to make it easier for you to use value objects.

If you have a value object in an entity, you can customize how that value object is hydrated from the database or serialized into the database.

```java
photon.registerAggregate(RecipeIngredient.class)
    // The quantity is a VARCHAR in the database but is converted to a Fraction value object when hydrated.
    .withDatabaseColumnSerializer("quantity", fraction -> fraction.getNumerator() + "/" + fraction.getDenominator())
    .withFieldHydrater("quantity", fractionString -> Fraction.getFraction(fractionString))
    .register();
```

If you have a common value object that is used in multiple entities in your application, and the value object maps to a single database column value using its `toString()` method, you can register a global custom converter for hydrating it.

```java
Photon.registerConverter(Fraction.class, fraction -> Fraction.getFraction(fraction));
```

If you have a value object that does not neatly map between an entity field and a database column, you can create a custom field value mapper.

```java
photon.registerAggregate(Product.class)
    .withDatabaseColumn("quantity", ColumnDataType.INTEGER, new EntityFieldValueMapping<MyTable, Integer>()
        {
            @Override
            public String getFieldValue(MyTable entityInstance)
            {
                return entityInstance.getQuantityInfo().getQuantity();
            }

            @Override
            public Map<String, Object> setFieldValue(MyTable entityInstance, Integer value)
            {
                entityInstance.getQuantityInfo().updateQuantity(value);
                return null;
            }
        }
    )
    .register();
```

If you have a value object that maps to multiple database columns, you can create a custom mapper for the value object.

```java
photon.registerAggregate(Product.class)
    .withDatabaseColumns(
        Arrays.asList(
            new DatabaseColumnDefinition("numerator", ColumnDataType.INTEGER),
            new DatabaseColumnDefinition("denominator", ColumnDataType.INTEGER)
        ),
        new CompoundEntityFieldValueMapping<Product>()
        {
            @Override
            public Map<String, Object> getDatabaseValues(Product entityInstance)
            {
                Map<String, Object> values = new HashMap<>();
                values.put("numerator", entityInstance.getQuantity().getNumerator());
                values.put("denominator", entityInstance.getQuantity().getDenominator());
                return values;
            }

            @Override
            public Map<String, Object> setFieldValues(Product entityInstance, Map<String, Object> values)
            {
                Map<String, Object> valuesToSet = new HashMap<>();
                valuesToSet.put(
                    "quantity",
                    Fraction.getFraction((int) values.get("numerator"), (int) values.get("denominator"))
                );
                return valuesToSet;
            }
        }
    )
    .register();
```

If you have a value object that is a list of items, you can add the list as a child of the entity and optionally omit the id (since value objects are not supposed to have an id) and the foreign key to the parent. However, if you omit the id, Photon won't be able to link each object in the list to its database row, so the entire set of rows will be deleted and re-inserted whenever the aggregate is saved.

```java
photon.registerAggregate(Recipe.class)
    .withChild("ingredients", RecipeIngredient.class)
        // The id and foreign key to parent must be specified here, but do not need to be in 
        // the RecipeIngredient class.
        .withId("recipeIngredientId")
        .withForeignKeyToParent("recipeId")
        .addAsChild()
    .register();
```

### Partial Aggregate Loading and Saving

While not recommended for most circumstances, Photon does support loading and saving partial aggregates. This can be useful if a simple update is needed and the overhead of loading and re-saving unmodified child entities would cause performance issues. 

```java
try(PhotonTransaction transaction = photon.beginTransaction())
{
    Recipe recipe = transaction
        .query(Recipe.class)
        .exclude("ingredients", "instructions") // Do not load the ingredient and instruction lists
        .fetchById(2);

    recipe.renameTo("Spaghetti and Meatballs");
    
    // Do not save the ingredient and instruction lists since we did not load them, otherwise this
    // recipe would lose all of its ingredients and instructions.
    transaction.saveWithExcludedFields(recipe, "ingredients", "instructions");
    
    transaction.commit();
}
```

It can also be useful for creating queries that only retrieve portions of an aggregate (although creating view models is preferred).

```java
try(PhotonTransaction transaction = photon.beginTransaction())
{
    Recipe recipe = transaction
        .query(Recipe.class)
        .exclude("ingredients") // Do not load the ingredient list
        .fetchById(2);
    
    return recipe;
```

### Inheritance

Photon supports mapping fields from super classes and sub classes.

```java
photon.registerAggregate(Circle.class)
    .withMappedClass(Shape.class)
    .register();
```

Photon supports single-table inheritance by combining `withMappedClass()` and `withClassDiscriminator()`.

```java
photon.registerAggregate(Shape.class)
    .withMappedClass(Circle.class)
    .withMappedClass(Rectangle.class)
    .withClassDiscriminator(valuesMap ->
    {
        String type = (String) valuesMap.get("type");
        switch (type)
        {
            case "circle":
                return Circle.class;
            case "rectangle":
                return Rectangle.class;
        }
    })
    .register();
```

Photon also supports multi-table inheritance using `withJoinedTable()`.

```java
photon.registerAggregate(Shape.class)
    .withPrimaryKeyAutoIncrement()
    .withJoinedTable(Circle.class, JoinType.LeftOuterJoin)
        .withPrimaryKeyAutoIncrement()
        .addAsJoinedTable()
    .withJoinedTable(Rectangle.class, JoinType.LeftOuterJoin)
        .withPrimaryKeyAutoIncrement()
        .addAsJoinedTable()
    .withChild("colorHistory", ShapeColorHistory.class)
        .withPrimaryKeyAutoIncrement()
        .withParentTable("Shape", "shapeId")
        .addAsChild()
    .withChild("corners", CornerCoordinates.class)
        .withParentTable("Rectangle")
        .withForeignKeyToParent("shapeId", ColumnDataType.INTEGER)
        .addAsChild()
    .withClassDiscriminator(valueMap ->
    {
        if(valueMap.get("Circle_id") != null)
        {
            return Circle.class;
        }
        else if(valueMap.get("Rectangle_id") != null)
        {
            return Rectangle.class;
        }
        else
        {
            return Shape.class;
        }
    })
    .register();
```

### Custom Child Field Types

If you want to use something other than a `List` or `Set` for a child list of entities, you can use `withChildCollectionConstructor`. Photon will call `toFieldValue` when hydrating your entity list and `toCollection` when persisting it to the database.

For example, if you have a `Recipe` entity and you want it to contain a map of ingredients with the ingredient name as the key (i.e. the `Recipe` entity has a field of `private Map<String, RecipeIngredient> ingredients`), then you could write a `withChildCollectionConstructor` like so:

```java
photon.registerAggregate(Recipe.class)
    .withChild("ingredients", RecipeIngredient.class)
        .withForeignKeyToParent("recipeId")
        .withChildCollectionConstructor(
            new ChildCollectionConstructor<Map<String, RecipeIngredient>, RecipeIngredient, Recipe>()
        {
            @Override
            public Collection<RecipeIngredient> toCollection(Map<String, RecipeIngredient> ingredientsMap, 
                                                             Recipe recipe)
            {
                return ingredientsMap.values();
            }

            @Override
            public Map<String, RecipeIngredient> toFieldValue(Collection<RecipeIngredient> ingredients, 
                                                              Recipe parentEntityInstance)
            {
                Map<String, RecipeIngredient> ingredientsMap = new HashMap<>();
                for(RecipeIngredient ingredient : ingredients)
                {
                    ingredientsMap.put(ingredient.getName(), ingredient);
                }
                return ingredientsMap;
            }
        })
        .addAsChild()
    .register();
```

### Many-to-Many Relationships and Flattened Collections

Many-to-many relationships don't make sense inside an aggregate. If an entity can be related to more than one entity, then each entity should be their own aggregate. For example, if you have an `Order` entity and an `OrderAddress` entity with a many-to-many relationship, meaning that an `Order` can have many `OrderAddresses` and an `OrderAddress` can be linked to many `Orders`, then the `Order` and `OrderAddress` entities should be in separate aggregates.

In a relational database, a many-to-many relationship is usually implemented by having an intermediate table that contains foreign keys to both tables. Each row in this table represents a relationship between the two tables. In Photon, you could add this intermediate table as a child entity for one or both aggregates. But since this entity would usually only have one field in it, Photon provides a way to represent the relationship as a list of primitive values (integers, longs, UUIDs, or any other primitive type).

```java
photon.registerAggregate(Order.class)
    .withFlattenedCollection("addresses", Integer.class, "OrderAddressAssignmentTable", "orderId", 
                             "orderAddressId", ColumnDataType.INTEGER)
    .register();
```

This will map the `List<Integer> addresses` field on `Order` to have the `orderAddressId` values for the aggregate. In other words, it will contain the `OrderAddress` ids for the `Order`.

`withFlattenedCollection` can also be used to flatten a child entity that only has a foreign key to the parent and one other column. For example, if a `Product` entity has a list of names, instead of having a `List<ProductName>` field where `ProductName` is an entity with `productId` and `name` fields, it could be flattened to be a `List<String>` of names.

In JPA 2.0, the equivalent implementation would be to have a list field decorated with `@ElementCollection` and `@CollectionTable` with a `@JoinColumn` with both `joinColumns` and `referencedColumnName` set.

### Optimistic Concurrency

Photon supports optimistic concurrency using an incrementing version number on the aggregate root.

```java
public class Recipe
{
    private UUID recipeId;
    
    private int version;
}
```

```java
photon.registerAggregate(Recipe.class)
    .withId("recipeId")
    .withVersionField("version")
    .register();
```

Photon will automatically increment the version number each time it is saved. If the version number in the database does not match the one being saved, a `PhotonOptimisticConcurrencyException` will be thrown. Note that you *cannot* use `photonTransaction.save()` to upsert aggregates with a version field because the `save()` will throw a `PhotonOptimisticConcurrencyException` if the update fails. You *must* use `insert()` to insert new aggregates and `save()` to update existing ones.

## Change Tracking

By default, photon tracks the state of each entity in each aggregate and only saves a row if at least one column changed. There is no concept of "flushing" changes. All queries (including inserts and updates) are always executed immediately.

If a queried aggregate will not be updated during a transaction, you can disable change tracking to reduce memory usage and improve performance.

```java
MyTable myTable = transaction
    .query(MyTable.class)
    .noTracking()
    .fetchById(2);
```

If you have a long-running transaction with aggregates falling out of scope and being garbage collected, you can clear the tracked state to reduce memory usage.

```java
try(PhotonTransaction transaction = photon.beginTransaction())
{
    Recipe recipe = transaction
        .query(Recipe.class)
        .fetchById(UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000010"));

    // ... later, when the recipe entity falls out of scope ...

    transaction.untrack(recipe);
}
```

Aggregates do not need to be tracked by Photon in order for them to save correctly. If an untracked aggregate is saved, every row for every entity in the aggregate will be re-saved. Therefore, it is recommended to have aggregates be tracked if they are going to be saved during the transaction.

## Lazy Loading

Aggregates are loaded as whole units. Photon does not support "[lazy loading](http://www.mehdi-khalili.com/orm-anti-patterns-part-3-lazy-loading)" because an aggregate should not be used to control the loading of other aggregates. All entities in an aggregate are eager loaded. Therefore, it is important to keep your aggregates small. See [Effective Aggregate Design](https://vaughnvernon.co/?p=838) for more information on these design concepts.

## Using Photon Alongside another ORM

While Photon is powerful enough to be used as the sole ORM on a project, it does provide ways to use it alongside another ORM. The `ExistingConnectionDataSource` can be used to have Photon share a database connection with another ORM.

```java
// During application initialization...
ExistingConnectionDataSource dataSource = new ExistingConnectionDataSource();
Photon photon = new Photon(dataSource);

// ... Later, when you need to run a query using photon ...
((ExistingConnectionDataSource) photon.getDataSource()).setConnection(existingConnection);
PhotonTransaction transaction = photon.beginTransaction();
// ... Do Photon queries as normal ...
```

If you want to ensure that Photon that does modify the state of the connection, you can wrap the connection with `new ReadOnlyConnection(existingConnection)`. Note that this only prevents the `Conection` itself from being modified, such as closing it, committing it, or changing the auto-commit state. You can still execute `INSERT`, `UPDATE`, and other SQL statements that modify database data (including DDL statements).

## Limitations

* Currently does not support composite primary keys.
* Currently does not support specifying database schemas (e.g. "dbo" for SQL Server).

## Compatibility

Photon requires Java 8. Photon should work with any database that has a JDBC driver. It has been tested with the following databases:
1. MySQL
1. PostgreSQL
1. SQL Server
1. Oracle

### PostgreSQL

The PostgreSQL JDBC driver requires using `preparedStatement.setObject()` for UUID fields. If using PostgreSQL, be sure to set `defaultUuidDataType` to `null` in the `PhotonOptions`.

```java
PhotonOptions photonOptions = new PhotonOptions("\"", "\"", DefaultTableName.ClassName, true, null, null);
photon = new Photon(url, user, password, photonOptions);
```

### SQL Server

The SQL Server JDBC Driver does not support getting generated identity keys from batch inserts, which are used by default in Photon. If using SQL Server, be sure to set `enableBatchInsertsForAutoIncrementEntities` to `false` in the `PhotonOptions`.

```java
PhotonOptions photonOptions = new PhotonOptions("[", "]", null, false, null);
Photon photon = new Photon(url, user, password, photonOptions);
```

### Oracle

The Oracle JDBC Driver does not support JDBC's `Statement.RETURN_GENERATED_KEYS`. If using Oracle, be sure to set `enableJdbcGetGeneratedKeys` to false in the `PhotonOptions`.

```java
PhotonOptions photonOptions = new PhotonOptions(null, null, null, null, false);
Photon photon = new Photon(url, user, password, photonOptions);
```

## Acknowledgements

This project was inspired by [sql2o](https://github.com/aaberg/sql2o), another great JVM micro ORM.

This project was also inspired by the leaders in Domain Design Driven, especially Eric Evans, Martin Fowler, and Vaughn Vernon.
