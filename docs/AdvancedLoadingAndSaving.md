# Advanced Loading and Saving

## Change Tracking

By default, photon tracks the state of each entity in each aggregate and only saves changes when `save()` is called. There is no concept of "flushing" changes. All queries (including inserts and updates) are always executed immediately.

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

Aggregates do not need to be tracked by Photon in order for them to save correctly. If an untracked aggregate is saved, the entire aggregate will be re-saved (as if the entire aggregate had changed).

## Lazy Loading

Aggregates are loaded as whole units. Photon does not support "[lazy loading](http://www.mehdi-khalili.com/orm-anti-patterns-part-3-lazy-loading)" because an aggregate should not be used to control the loading of other aggregates. All entities in an aggregate are eager loaded. Therefore, it is important to keep your aggregates small. See [Effective Aggregate Design](https://vaughnvernon.co/?p=838) for more information on these design concepts.

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
