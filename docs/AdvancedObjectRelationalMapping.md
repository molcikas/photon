# Advanced Object-Relational Mapping

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

## Custom Child Field Types

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

## Many-to-Many Relationships and Flattened Collections

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

## Optimistic Concurrency

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