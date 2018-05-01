# Photon [![Build Status](https://travis-ci.org/molcikas/photon.svg?branch=master)](https://travis-ci.org/molcikas/photon) [![Code Coverage](https://img.shields.io/codecov/c/github/molcikas/photon/master.svg)](https://codecov.io/github/molcikas/photon?branch=master)

Photon is a unique Java ORM that aims to combine the best aspects of traditional and micro ORMs. Photon supports some of the features of traditional ORMs, such as eager loading and change tracking, but it also feels and acts like a micro ORM. When you want to write a query, you use real SQL, not a custom query language that isn't quite SQL.

Traditional ORMs can be complex and cryptic. Since they hide the SQL they are executing behind leaky abstractions, diagnosing and troubleshooting issues requires deep knowledge about the inner workings of the ORM. Photon queries are just plain SQL, so you always know exactly what query the ORM is running.

Photon is the one of the only Java ORMs that supports a fluent API. Most other ORMs require the object-to-table mapping to be specified in XML or using annotations. XML is error prone and difficult to maintain. Annotations clutter domain entities with persistence details and require the entities to be modeled after the database tables rather than the business domain. A fluent API allows entities to remain free from the details of how they are persisted.

Micro ORMs give developers greater control of the SQL but can be cumbersome to use, especially when loading and saving clusters of entities ("[aggregates](https://martinfowler.com/bliki/DDD_Aggregate.html)" in DDD terms). Photon gives developers the ability to specify relationships between entities in an aggregate so that you don't have to write cumbersome queries to do basic CRUD operations like you would in most micro ORMs.

The goal of Photon is to capture the best of micro and tradition ORMs by giving developers control over the SQL executed while still providing an easy way to do routine CRUD operations on aggregates.

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

## Advanced Modeling and Mapping

1. [View Models](./docs/ViewModels.md). Also called DTO projections by other ORMs.
1. [Value Objects](./docs/ValueObjects.md). Photon makes it easy to use rich [value objects](https://en.wikipedia.org/wiki/Value_object) in your entities. Don't make everything a `String`!
1. [Advanced Object-Relational Mapping](./docs/AdvancedObjectRelationalMapping.md). Inheritance, collections, and optimistic concurrency.
1. [Advanced Loading and Saving](./docs/AdvancedLoadingAndSaving.md). Change tracking, (lack of) lazy loading, and partial loading/saving.

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

The `PhotonOptions` class has builders with recommended settings for each of these 4 databases.

### PostgreSQL

The PostgreSQL JDBC driver requires using `preparedStatement.setObject()` for UUID fields. If using PostgreSQL, be sure to set `defaultUuidDataType` to `null` in the `PhotonOptions`.

### SQL Server

The SQL Server JDBC Driver does not support getting generated identity keys from batch inserts. Photon does not currently support JDBC batch inserts, so there are currently no known compatibility issues.

### Oracle

The Oracle JDBC Driver does not support JDBC's `Statement.RETURN_GENERATED_KEYS`. If using Oracle, be sure to set `enableJdbcGetGeneratedKeys` to false in the `PhotonOptions`.

## Acknowledgements

This project was inspired by [sql2o](https://github.com/aaberg/sql2o), another great JVM micro ORM.

This project was also inspired by the leaders in Domain Design Driven, especially Eric Evans, Martin Fowler, and Vaughn Vernon.
