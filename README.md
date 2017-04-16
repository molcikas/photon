# Photon
A micro ORM that supports aggregates and a fluent API to allow aggregates to be modeled from the business domain rather than the database tables without the trade-off of having separate models for the domain and database.

Traditional ORMs hide the SQL they are executing behind complicated modeling and custom querying languages. This often leads to poorly constructed and slow queries that are difficult to diagnose and troubleshoot. Also, most traditional ORMs (especially JVM ones) require the object-database mapping to either be specified in XML or using annotations. XML is error prone and difficult to maintain. Annotations clutter domain entities with persistence logic and require the entities to be structured similarly to the database tables, preventing them from being modeled purely from the business domain.

Micro ORMs give developers greater control of the SQL but can be cumbersome to use, especially when loading and saving aggregates with multiple entities.

The goal of photon is to capture the best of both worlds by giving developers control over the SQL executed but still provide an easy way to do basic CRUD operations on aggregates, while also allowing entities to be free from the details of how they are persisted.

## Getting Started

### Initializing Photon

Simply construct a `Photon` object with a `DataSource` (which can be retrieved from connection poolers like `HikariCP`) or a JDBC url, username, and password. Then, register your aggregates using `registerAggregate()`. It is recommended to use the constructed `Photon` object as a singleton in your application.

### Registering Aggregates

Immediately after constructing the `Photon` object, register each aggregate using the fluent API. For example:

```java
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
        .withCustomToFieldValueConverter("quantity", val -> val != null ? Fraction.getFraction((String) val) : null)
        .withOrderBy("orderBy", ingredientSortDirection)
        .addAsChild("ingredients")
    .register();
```

By default, each field (public or private) is mapped to a database column of the same name and data type. Child objects are ignored by default, so use `withChild()` if an entity contains a child entity as part of the aggregate.

### Creating and Committing Transactions

Every SQL command and query must be executed in the context of a transaction. The transaction must have an explicit `commit()` for changes to saved in the database. Otherwise, the transaction is automatically rolled back, which can be useful in read-only transactions to ensure nothing was accidentally changed.

```java
try (PhotonTransaction transaction = photon.beginTransaction())
{
    // ... Queries, inserts, updates, deletes, and other orchestration logic ...
    
    transaction.commit(); // Omitting the commit causes the transaction to be rolled back.
}
```

### Querying and Updating Aggregates

Aggregates must be queried by ID and must be loaded and saved as whole units. Lazy loading is not supported. This helps ensure that the invariants are properly enforced by the aggregate and that the aggregate is not corrupted in the database due to only partially saving it.

```java
try(PhotonTransaction transaction = photon.beginTransaction())
{
    MyTable myTable = new MyTable(2, "MySavedValue", null);
    transaction.insert(myTable); // Use "save" instead of "insert" if updating an aggregate.
    transaction.commit();
}

try(PhotonTransaction transaction = photon.beginTransaction())
{
    MyTable myTableRetrieved = transaction
        .query(MyTable.class)
        .fetchById(2);

    assertEquals(2, myTableRetrieved.getId());
    assertEquals("MySavedValue", myTableRetrieved.getMyvalue());
}
```

Photon provides an easy interface for fetching aggregates by an IDs query:

```java
List<MyTable> myTables = transaction
    .query(MyTable.class)
    .fetchByIdsQuery("SELECT mytable.id FROM mytable JOIN myothertable ON myothertable.id = mytable.id WHERE myothervalue IN (:myOtherValues)")
    .addParameter("myOtherValues", Arrays.asList("my4otherdbvalue", "my5otherdbvalue"))
    .fetchList();
```

### View Models using Custom SQL Queries

User interfaces often need only need a few fields from an aggregate, or need pieces of data from multiple aggregates. Photon makes it easy to construct custom view models using plain SQL.

```java
try(PhotonTransaction transaction = photon.beginTransaction())
{
    String sql =
        "SELECT id, myvalue " +
        "FROM mytable " +
        "WHERE id IN (:ids) " +
        "ORDER BY id DESC ";

    List<MyTable> myTables = transaction
        .query(sql)
        .addParameter("ids", Arrays.asList(2, 4))
        .fetchList(MyTable.class);

    assertEquals(2, myTables.size());
    assertEquals(4, myTables.get(0).getId());
    assertEquals("my4dbvalue", myTables.get(0).getMyvalue());
    assertEquals(2, myTables.get(1).getId());
    assertEquals("my2dbvalue", myTables.get(1).getMyvalue());
}
```

Queries results are automatically mapped to the view model fields by name. Unlike aggregates, view model classes do not need to be pre-registered with Photon.

It's also possible to fetch scalar values and lists using plain SQL:

```java
try(PhotonTransaction transaction = photon.beginTransaction())
{
    String sql =
        "SELECT myvalue " +
        "FROM mytable " +
        "WHERE id >= :id ";

    List<String> myValues = transaction
        .query(sql)
        .addParameter("id", 3)
        .fetchScalarList(String.class);
}
```

## Limitations

* Currently does not support composite keys in aggregates.
* Currently does not automatically map fields from super classes. This can be done manually using `withDatabaseColumn()` in Photon's fluent API.
* Testing has only be done on MySQL and H2, but should work with any database with a JDBC driver.

## Performance

Photon uses JDBC batching to maximize performance for inserts, updates, and deletes. For selects, each entity in the aggregate is queried so that all of the rows for all aggregate instances are retrieved in a single query, which cuts down significantly on the number of database queries needed.

Aggregate CRUD comparison with hibernate (see [source code](https://github.com/molcikas/photon/tree/master/photon-perf-test/src/test/java/photon/perf) for testing details).

ORM | 10k Inserts | 10k Selects | 10k Updates | 10k Deletes
--- | --- | --- | --- | ---
Photon | 1387 ms | 1729 ms | 3588 ms | 2588 ms
Hibernate | 1882 ms | 1783 ms | 2378 ms | 2172 ms

Photon performance is comparable to Hibernate. Photon is faster with inserts because it uses JDBC batching, but slower on updates because it is session-less.

Hibernate stores the initial state of each entity in its session state. When a transaction is committed, only the fields that changed during the session are updated. This gives Hibernate a performance advantage with updates, but it comes at a price. Entity instances must be attached to the Hibernate session so that Hibernate knows what changes occur during the session, and must be detached (or the session must be closed) before they can be garbage collected. Because the entire aggregate isn't saved, there is a chance that the aggregate was updated by another application, and the save puts the aggregate into an indeterminate (and likely invalid) state in the database.

## Acknowledgements

This project was inspired by [sql2o](https://github.com/aaberg/sql2o), another great JVM micro ORM.