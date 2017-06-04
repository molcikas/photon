# Photon [![Build Status](https://travis-ci.org/molcikas/photon.svg?branch=master)](https://travis-ci.org/molcikas/photon)
A micro ORM that gives developers the ability to write complex SELECT statements when performance is critical while also shielding them from having to hand-write routine CRUD SQL.

Traditional ORMs are complex and cryptic. They are often orders of magnitude slower than lower-level frameworks like JDBC. Since they hide the SQL they are executing, diagnosing and troubleshooting issues requires deep knowledge about how the ORM works. They usually require the object-to-table mapping to be specified in XML or using annotations. XML is error prone and difficult to maintain. Annotations clutter domain entities with persistence logic and require the entities to be modeled after the database tables rather than the business domain.

Micro ORMs give developers greater control of the SQL but can be cumbersome to use, especially when loading and saving clusters of entities ("[aggregates](https://martinfowler.com/bliki/DDD_Aggregate.html)" in DDD terms).

The goal of Photon is to capture the best of both worlds by giving developers control over the SQL executed while still providing an easy way to do routine CRUD operations on aggregates. It allows entities to remain free from the details of how they are persisted.

Photon does not require you to learn a custom query language or a custom set of query functions. Photon automatically does the selects, inserts, updates, and deletes for your aggregates based on how you define the aggregates. For choosing which rows/aggregates to select, or when selecting custom read models, you write plain SQL `SELECT` statements.

## Getting Started

### Installation

The latest JAR is available in the [Maven Central Repository](https://mvnrepository.com/artifact/com.github.molcikas/photon-core).

### Initializing Photon

Construct a `Photon` object with a `DataSource` (which can be retrieved from connection poolers like `HikariCP`) or a JDBC url, username, and password. Then, register your aggregates using `registerAggregate()`.

### Registering Aggregates

After constructing the `Photon` object, register each aggregate by describing how the root entity and sub entities are mapped to database tables. For example:

```java
photon.registerAggregate(Recipe.class)
    .withId("recipeId")
    .withChild(RecipeInstruction.class)
        .withId("recipeInstructionId", ColumnDataType.BINARY)
        .withForeignKeyToParent("recipeId", ColumnDataType.BINARY)
        .withOrderBy("stepNumber")
        .addAsChild("instructions")
    .withChild(RecipeIngredient.class)
        .withId("recipeIngredientId", ColumnDataType.BINARY)
        .withForeignKeyToParent("recipeId", ColumnDataType.BINARY)
        .withDatabaseColumn("quantity", ColumnDataType.VARCHAR)
        .withCustomToFieldValueConverter("quantity", val -> val != null ? Fraction.getFraction((String) val) : null)
        .withOrderBy("orderBy", ingredientSortDirection)
        .addAsChild("ingredients")
    .register();
```

By default, each class field (public or private) is mapped to a database column of the same name and equivalent data type. Use `withChild()` for child entities that should be mapped to a database table.

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
List<MyTable> myTables = transaction
    .query(MyTable.class)
    // Only return aggregates with an id in the result set for this SELECT statement
    .whereIdIn("SELECT mytable.id FROM mytable JOIN myothertable ON myothertable.id = mytable.id WHERE myothervalue IN (:myOtherValues)")
    .addParameter("myOtherValues", Arrays.asList("my4otherdbvalue", "my5otherdbvalue"))
    .fetchList();
```
    
### Constructing Read Models using SQL

User interfaces often only need a few fields from an aggregate, or need pieces of data from multiple aggregates. Photon makes it easy to construct custom read models using plain SQL.

```java
try(PhotonTransaction transaction = photon.beginTransaction())
{
    String sql =
        "SELECT Product.id, Product.name, Order.total AS orderTotal, Order.orderDate " +
        "FROM Product " +
        "JOIN ProductOrders ON ProductOrders.productId = Product.id " +
        "JOIN Orders ON Orders.id = ProductOrder.orderId " +
        "WHERE Orders.total > :orderTotal " +
        "ORDER Orders.orderDate DESC ";

    List<ProductOrderDto> expensiveProductOrders = transaction
        .query(sql)
        .addParameter("orderTotal", 1000)
        .fetchList(ProductOrderDto.class);
    
    return expensiveProductOrders;
}
```

Query results are automatically mapped to the read model fields by name. Unlike aggregates, read models do not need to be pre-registered with Photon.

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

## Working with Value Objects

Other ORMs offer very little support for [value objects](https://en.wikipedia.org/wiki/Value_object). They typically expect you to only use primitive values likes strings and integers in your entities. Photon provides several mechanisms to make it easier for you to use value objects in your entities.

If you have a custom value object for an entity, you can customize how that value object is converted to an entity field value and/or converted to a database value.

```java
photon.registerAggregate(RecipeIngredient.class)
    .withCustomToFieldValueConverter("quantity", val -> val != null ? Fraction.getFraction((String) val) : null)
    .register();
```

```java
photon.registerAggregate(MyTable.class)
    .withCustomToDatabaseValueConverter("myvalue", val -> ((String) val).toUpperCase())
    .register();
```

If you have a common value object that is used in multiple entities in your application, and the value object maps to a single database column value using its `toString()` method, you can register a global custom converter for it.

```java
Convert.registerConverter(Fraction.class, val -> val != null ? Fraction.getFraction((String) val) : null);
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

If you have a value object that contains a list of items, you can add it as a child of the entity and optionally omit the id (since value objects are not supposed to have an id) and the foreign key to the parent. However, if you omit the id, Photon won't be able to link each item to its database row, so the entire list will be deleted and re-inserted whenever the aggregate is saved.

```java
photon.registerAggregate(Recipe.class)
    .withChild(RecipeIngredient.class)
        // The id and foreign key to parent must be specified here, but do not need to be in the RecipeIngredient class.
        .withId("recipeIngredientId")
        .withForeignKeyToParent("recipeId")
        .addAsChild("ingredients")
    .register();
```

## Partial Aggregate Loading and Saving

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

It can also be useful for creating queries that only retrieve portions of an aggregate (although creating read models is preferred).

```java
try(PhotonTransaction transaction = photon.beginTransaction())
{
    Recipe recipe = transaction
        .query(Recipe.class)
        .exclude("ingredients") // Do not load the ingredient list
        .fetchById(2);
    
    return recipe;
```

## Inheritance

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
            default:
                return null;
        }
    })
    .register();
```

## Sessionless

Photon does not maintain any in-memory cache of entities (the "session") that can get stale or consume large amounts of memory. Entities do not need to be attached to Photon in order for them to save correctly, and there is no concept of "flushing" changes. Queries are always executed immediately.

Aggregates should be loaded and saved as whole units (unless this would cause significant performance issues). Photon does not track pending changes for entities and does not support "[lazy loading](http://www.mehdi-khalili.com/orm-anti-patterns-part-3-lazy-loading)". Therefore, it is important to keep your aggregates small and to avoid using aggregates as read models. See [Effective Aggregate Design](https://vaughnvernon.co/?p=838) for more information on these design concepts.

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
