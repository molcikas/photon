# Working with Value Objects

ORMs typically very little support for . They expect you to only use primitive values likes strings and integers in your entities. Photon provides several mechanisms to make it easier for you to use value objects.

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