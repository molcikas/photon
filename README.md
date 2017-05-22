# Photon [![Build Status](https://travis-ci.org/molcikas/photon.svg?branch=master)](https://travis-ci.org/molcikas/photon)
A micro ORM that gives developers control over the SQL executed while also providing an easy way to do basic CRUD operations on entities.

Traditional ORMs hide the SQL they are executing. This often leads to poorly constructed and slow queries that are difficult to diagnose and troubleshoot. They usually require the object-to-table mapping to either be specified in XML or using annotations. XML is error prone and difficult to maintain. Annotations clutter domain entities with persistence logic and require the entities to be structured similarly to the database tables. This prevents them from being modeled purely from the business domain.

Micro ORMs give developers greater control of the SQL but can be cumbersome to use, especially when loading and saving entities with sub-entities ("aggregates" in DDD terms).

The goal of photon is to capture the best of both worlds by giving developers control over the SQL executed while still providing an easy way to do routine CRUD operations on aggregates. It also allows entities to remain free from the details of how they are persisted.

Photon does not have its own query language or query functions. Photon automatically does the selects, inserts, updates, and deletes for your entities based on how you define their shapes. For choosing which entities to select, or when selecting custom read models, you write plain SQL `SELECT` statements.

## Getting Started

### Initializing Photon

Construct a `Photon` object with a `DataSource` (which can be retrieved from connection poolers like `HikariCP`) or a JDBC url, username, and password. Then, register your aggregates using `registerAggregate()`.

### Registering Aggregates

Immediately after constructing the `Photon` object, register each aggregate by describing how the root entity and sub entities are mapped to database tables. For example:

```java
photon.registerAggregate(Recipe.class)
    .withId("recipeId")
    .withChild(RecipeInstruction.class)
        .withId("recipeInstructionId", Types.BINARY)
        .withForeignKeyToParent("recipeId")
        .withDatabaseColumn("recipeId", Types.BINARY)
        .withOrderBy("stepNumber")
        .addAsChild("instructions")
    .withChild(RecipeIngredient.class)
        .withId("recipeIngredientId", Types.BINARY)
        .withForeignKeyToParent("recipeId")
        .withDatabaseColumn("recipeId", Types.BINARY)
        .withDatabaseColumn("quantity", Types.VARCHAR)
        .withCustomToFieldValueConverter("quantity", val -> val != null ? Fraction.getFraction((String) val) : null)
        .withOrderBy("orderBy", ingredientSortDirection)
        .addAsChild("ingredients")
    .register();
```

By default, each class field (public or private) is mapped to a database column of the same name equivalent data type. Use `withChild()` for child entities that should be mapped to a database table.

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

### Read Models using Custom SQL Queries

User interfaces often only need a few fields from an aggregate, or need pieces of data from multiple aggregates. Photon makes it easy to construct custom read models using plain SQL.

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

Query results are automatically mapped to the read model fields by name. Unlike aggregates, view model classes do not need to be pre-registered with Photon.

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

Photon does not maintain a cache of entities (often referred to as the ORM's "session") and their pending changes ("dirty checking"). Entity instances do not need to attached to Photon in order for them to save correctly, and there is no concept of "flushing" changes. Queries are always executed immediately.

Aggregates are loaded and saved as whole units. Photon does not track pending changes for entities and does not do "[lazy loading](http://www.mehdi-khalili.com/orm-anti-patterns-part-3-lazy-loading)". Therefore, it is important to keep your aggregates small and to avoid using aggregates as read models. See [Effective Aggregate Design](https://vaughnvernon.co/?p=838) for more information on these design concepts.

## Limitations

* Currently does not support composite primary keys in aggregates.
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
