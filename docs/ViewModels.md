# Constructing View Models using SQL

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
