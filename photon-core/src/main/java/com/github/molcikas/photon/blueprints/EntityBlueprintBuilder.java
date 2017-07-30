package com.github.molcikas.photon.blueprints;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.exceptions.PhotonException;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The builder for creating aggregate entity blueprints.
 */
public class EntityBlueprintBuilder
{
    private final Photon photon;
    private final EntityBlueprintBuilder parentBuilder;
    private final Class entityClass;
    private final String aggregateBlueprintName;
    private final boolean registerBlueprintForSaving;
    private final List<MappedClassBlueprint> mappedClasses;
    private EntityClassDiscriminator entityClassDiscriminator;
    private final List<String> ignoredFields;
    private final Map<String, EntityBlueprint> childEntities;
    private final Map<String, Converter> customFieldHydraters;

    private final TableBlueprintBuilder tableBlueprintBuilder;

    public EntityBlueprintBuilder(Class entityClass, Photon photon)
    {
        this(entityClass, null, true, null, photon);
    }

    public EntityBlueprintBuilder(Class entityClass, EntityBlueprintBuilder parentBuilder, Photon photon)
    {
        this(entityClass, null, false, parentBuilder, photon);
    }

    public EntityBlueprintBuilder(Class entityClass, String aggregateBlueprintName, boolean registerBlueprintForSaving, Photon photon)
    {
        this(entityClass, aggregateBlueprintName,
            registerBlueprintForSaving, null, photon);
    }

    private EntityBlueprintBuilder(Class entityClass, String aggregateBlueprintName, boolean registerBlueprintForSaving, EntityBlueprintBuilder parentBuilder, Photon photon)
    {
        this.entityClass = entityClass;
        this.aggregateBlueprintName = aggregateBlueprintName;
        this.registerBlueprintForSaving = registerBlueprintForSaving;
        this.parentBuilder = parentBuilder;
        this.photon = photon;
        this.mappedClasses = new ArrayList<>();
        this.ignoredFields = new ArrayList<>();
        this.childEntities = new HashMap<>();
        this.customFieldHydraters = new HashMap<>();
        this.tableBlueprintBuilder = new TableBlueprintBuilder(photon.getOptions());
    }

    /**
     * Adds a super or sub class's fields to the entity, and auto-maps them to database columns. This can be used
     * to simply include fields in a sub or super class, or can be combined with withClassDiscriminator() to implement
     * single-table inheritance.
     *
     * @param mappedClass - the super or sub class whose fields will all be mapped into the entity
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withMappedClass(Class mappedClass)
    {
        mappedClasses.add(new MappedClassBlueprint(mappedClass, true, null));
        return this;
    }

    /**
     * Adds a super or sub class's fields to the entity, and auto-maps them to database columns. This can be used
     * to simply include fields in a sub or super class, or can be combined with withClassDiscriminator() to implement
     * single-table inheritance.
     *
     * @param mappedClass - the super or sub class whose fields will be mapped into the entity
     * @param includedFields - the list of fields on the super or sub class to map
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withMappedClass(Class mappedClass, List<String> includedFields)
    {
        mappedClasses.add(new MappedClassBlueprint(mappedClass, false, includedFields));
        return this;
    }

    /**
     * A custom discriminator that can be used to dynamically set the type of entity to construct. Typically, this
     * is used to implement single-table inheritance. The column values are provided to the discriminator.
     *
     * @param entityClassDiscriminator - the discriminator
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withClassDiscriminator(EntityClassDiscriminator entityClassDiscriminator)
    {
        this.entityClassDiscriminator = entityClassDiscriminator;
        return this;
    }

    /**
     * The database table name for the entity. This only needs to be set if the entity name is different than the
     * table name.
     * @param tableName - table name
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withTableName(String tableName)
    {
        tableBlueprintBuilder.withTableName(tableName);
        return this;
    }

    /**
     * The id field for the entity. This only needs to be set if the id field is not "id" or "tableNameId" (where
     * tableName is the name of the table.
     * @param idFieldName - the id field name
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withId(String idFieldName)
    {
        tableBlueprintBuilder.withId(idFieldName);
        return this;
    }

    /**
     * Sets the id field and primary key auto increment in a single method. See the individual setters for details.
     * @param idFieldName - the id field name
     * @param isPrimaryKeyAutoIncrement - whether the primary key is auto incrementing (a.k.a. identity column)
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withId(String idFieldName, boolean isPrimaryKeyAutoIncrement)
    {
        tableBlueprintBuilder.withId(idFieldName, isPrimaryKeyAutoIncrement);
        return this;
    }

    /**
     * Sets the id field, column data type, and primary key auto increment in a single method. See the individual
     * setters for details.
     * @param idFieldName - the id field name
     * @param columnDataType - the column data type for the primary key column
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withId(String idFieldName, ColumnDataType columnDataType)
    {
        tableBlueprintBuilder.withId(idFieldName, columnDataType);
        return this;
    }

    /**
     * Sets the id field, column data type, and primary key auto increment in a single method. See the individual
     * setters for details.
     * @param idFieldName - the id field name
     * @param columnDataType - the column data type for the primary key column
     * @param isPrimaryKeyAutoIncrement - whether the primary key is auto incrementing (a.k.a. identity column)
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withId(String idFieldName, ColumnDataType columnDataType, boolean isPrimaryKeyAutoIncrement)
    {
        tableBlueprintBuilder.withId(idFieldName, columnDataType, isPrimaryKeyAutoIncrement);
        return this;
    }

    /**
     * Sets the primary key as auto incrementing (a.k.a. identity column).
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withPrimaryKeyAutoIncrement()
    {
        tableBlueprintBuilder.withPrimaryKeyAutoIncrement();
        return this;
    }

    /**
     * Sets the column which is a foreign key to the parent entity.
     *
     * If the entity does not have children, then it is not required that this column is mapped to a field on the
     * entity. However, if it is not mapped, then all rows will be deleted and re-inserted on every save since there
     * would be no way to map the entities to existing rows. If the entity has children, then the foreign key to
     * parent must be mapped to a field.
     *
     * @param foreignKeyToParent - the foreign key to parent column
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withForeignKeyToParent(String foreignKeyToParent)
    {
        tableBlueprintBuilder.withForeignKeyToParent(foreignKeyToParent);
        return this;
    }

    /**
     * Sets the column which is a foreign key to the parent entity.
     *
     * If the entity does not have children, then it is not required that this column is mapped to a field on the
     * entity. However, if it is not mapped, then all rows will be deleted and re-inserted on every save since there
     * would be no way to map the entities to existing rows. If the entity has children, then the foreign key to
     * parent must be mapped to a field.
     *
     * @param foreignKeyToParent - the foreign key to parent column
     * @param columnDataType - the column data type for the the foreign key to parent column
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withForeignKeyToParent(String foreignKeyToParent, ColumnDataType columnDataType)
    {
        tableBlueprintBuilder.withForeignKeyToParent(foreignKeyToParent, columnDataType);
        return this;
    }

    /**
     * Sets up a many-to-many relationship, mapping an aggregate to a list of other aggregates.
     *
     * @param fieldName - the field containing the list of foreign aggregate ids
     * @param foreignTableName - the many-to-many intermediate table
     * @param foreignTableJoinColumnName - the foreign table column that joins back to the aggregate
     * @param foreignTableKeyColumnName - the foreign table column that joins to the foreign aggregate
     * @param foreignTableKeyColumnType - the column data type for the foreign table key column
     * @param fieldListItemClass - the class type for the items in the field list.
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withForeignKeyListToOtherAggregate(
        String fieldName,
        String foreignTableName,
        String foreignTableJoinColumnName,
        String foreignTableKeyColumnName,
        ColumnDataType foreignTableKeyColumnType,
        Class fieldListItemClass)
    {
        tableBlueprintBuilder.withForeignKeyListToOtherAggregate(
            fieldName,
            foreignTableName,
            foreignTableJoinColumnName,
            foreignTableKeyColumnName,
            foreignTableKeyColumnType,
            fieldListItemClass
        );
        return this;
    }

    /**
     * Sets the column data type for a database column. This only needs to be called if the column is being mapped to
     * a non-default type.
     *
     * @param columnName - the database column name
     * @param columnDataType - the database column data type.
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withDatabaseColumn(String columnName, ColumnDataType columnDataType)
    {
        tableBlueprintBuilder.withDatabaseColumn(columnName, columnDataType);
        return this;
    }

    /**
     * Create a custom field-to-column name mapping. This only needs to be called if the column name is not the same
     * as the field name.
     *
     * @param columnName - the database column name
     * @param fieldName - the entity field name
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withDatabaseColumn(String columnName, String fieldName)
    {
        tableBlueprintBuilder.withDatabaseColumn(columnName, fieldName);
        return this;
    }

    /**
     * Create a custom field-to-column name mapping. This only needs to be called if the column name is not the same
     * as the field name.
     *
     * @param columnName - the database column name
     * @param fieldName - the entity field name
     * @param columnDataType - the column data type
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withDatabaseColumn(String columnName, String fieldName, ColumnDataType columnDataType)
    {
        tableBlueprintBuilder.withDatabaseColumn(columnName, fieldName, columnDataType);
        return this;
    }

    /**
     * Creates a database column that is mapped to and from an entity field via a custom value mapper. This method should
     * be used if a database value does not map directly to a field (e.g. if the value consists of multiple fields
     * or a field on a child entity).
     *
     * @param columnName - the database column name
     * @param columnDataType - the database column data type
     * @param entityFieldValueMapping - the mapper that maps the entity value to and from the database column value
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withDatabaseColumn(String columnName, ColumnDataType columnDataType,
                                                     EntityFieldValueMapping entityFieldValueMapping)
    {
        tableBlueprintBuilder.withDatabaseColumn(columnName, columnDataType, entityFieldValueMapping);
        return this;
    }

    /**
     * Creates database columns that are mapped to and from one or more fields via a custom value mapper. This method
     * should be used if a field(s) maps to more than one database column.
     *
     * @param columnDefinitions - the database columns
     * @param compoundEntityFieldValueMapping - the mapper that maps field value(s) to and from the database values
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withDatabaseColumns(
        List<DatabaseColumnDefinition> columnDefinitions,
        CompoundEntityFieldValueMapping compoundEntityFieldValueMapping)
    {
        tableBlueprintBuilder.withDatabaseColumns(columnDefinitions, compoundEntityFieldValueMapping);
        return this;
    }

    /**
     * Sets a custom converter for hydrating a database value into an entity field value.
     *
     * @param fieldName - the entity field name
     * @param fieldHydrater - the converter for doing the field hydration
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withFieldHydrater(String fieldName, Converter fieldHydrater)
    {
        customFieldHydraters.put(fieldName, fieldHydrater);
        return this;
    }

    /**
     * Sets a custom converter for serializing an entity field value into a database value.
     *
     * @param columnName - the database column name
     * @param databaseColumnSerializer - the converter for serializing values into the database
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withDatabaseColumnSerializer(String columnName, Converter databaseColumnSerializer)
    {
        tableBlueprintBuilder.withDatabaseColumnSerializer(columnName, databaseColumnSerializer);
        return this;
    }

    /**
     * Ignore a field and prevent it from being auto-mapped to a database column.
     *
     * @param fieldName - the entity field name
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withIgnoredField(String fieldName)
    {
        ignoredFields.add(fieldName);
        return this;
    }

    /**
     * Sets the database order to use for sorting the database entities. Defaults to id ascending. If this entity is
     * not the aggregate root, it is highly recommend to include the table column with each column to avoid
     * ambiguity errors (e.g. "MyTable.myColumn DESC" vs. "myColumn DESC").
     *
     * @param orderBySql - The SQL order by clause (excluding the ORDER BY keywords)
     * @return - builder for chaining
     */
    public EntityBlueprintBuilder withOrderBySql(String orderBySql)
    {
        tableBlueprintBuilder.withOrderBySql(orderBySql);
        return this;
    }

    /**
     * Creates a builder that is used to build the blueprint for a child entity.
     *
     * @param childClass - the child entity class
     * @return - the child builder
     */
    public EntityBlueprintBuilder withChild(Class childClass)
    {
        return new EntityBlueprintBuilder(childClass, this, photon);
    }

    /**
     * Completes the builder and registers it as a child of the parent entity.
     *
     * @param fieldName - the field name on the parent that references the child entity.
     * @return - the parent builder for chaining
     */
    public EntityBlueprintBuilder addAsChild(String fieldName)
    {
        if(parentBuilder == null)
        {
            throw new PhotonException(String.format("Cannot add child to field '%s' because there is no parent entity.", fieldName));
        }
        if(StringUtils.isBlank(tableBlueprintBuilder.getForeignKeyToParent()))
        {
            throw new PhotonException(String.format("Cannot add child to parent field '%s' because the child does not have a foreign key to parent set.", fieldName));
        }
        parentBuilder.addChild(fieldName, build());
        return parentBuilder;
    }

    /**
     * Completes the builder and registers the entity with Photon.
     */
    public void register()
    {
        if(photon == null)
        {
            throw new PhotonException("Cannot register entityBlueprint because it is not the aggregate root.");
        }
        photon.registerBuiltAggregateBlueprint(aggregateBlueprintName, registerBlueprintForSaving, build());
    }

    public EntityBlueprint build()
    {
        MultiValuedMap<Class, Field> reflectedFieldsMap = new HashSetValuedHashMap<>();
        reflectedFieldsMap.putAll(entityClass, Arrays.asList(entityClass.getDeclaredFields()));

        for(MappedClassBlueprint mappedClassBlueprint : mappedClasses)
        {
            List<Field> reflectedFieldsForMappedClass = mappedClassBlueprint.getIncludedFields();
            for(Field field : reflectedFieldsForMappedClass)
            {
                if(reflectedFieldsMap.values().stream().noneMatch(c -> c.getName().equals(field.getName())))
                {
                    reflectedFieldsMap.put(mappedClassBlueprint.getMappedClass(), field);
                }
            }
        }

        List<FieldBlueprint> fields = reflectedFieldsMap
            .entries()
            .stream()
            .filter(entry -> !ignoredFields.contains(entry.getValue().getName()))
            .map(entry -> new FieldBlueprint(
                entry.getValue(),
                Collections.singletonList(
                    tableBlueprintBuilder.getCustomFieldToColumnMappings().containsKey(entry.getValue().getName()) ?
                    tableBlueprintBuilder.getCustomFieldToColumnMappings().get(entry.getValue().getName()) :
                    entry.getValue().getName()),
                childEntities.get(entry.getValue().getName()),
                tableBlueprintBuilder.getForeignKeyListBlueprints().get(entry.getValue().getName()),
                customFieldHydraters.get(entry.getValue().getName()),
                null,
                null
            ))
            .collect(Collectors.toList());

        fields.addAll(
            tableBlueprintBuilder.getCustomDatabaseColumns().entrySet()
                .stream()
                .map(e -> new FieldBlueprint(
                    null,
                    Collections.singletonList(e.getKey()),
                    null,
                    null,
                    null,
                    e.getValue(),
                    null
                ))
                .collect(Collectors.toList())
        );

        fields.addAll(
            tableBlueprintBuilder.getCustomCompoundDatabaseColumns().entrySet()
                .stream()
                .map(e -> new FieldBlueprint(
                    null,
                    e.getKey(),
                    null,
                    null,
                    null,
                    null,
                    e.getValue()
                ))
                .collect(Collectors.toList())
        );

        TableBlueprint tableBlueprint = tableBlueprintBuilder.build(entityClass, fields);

        return new EntityBlueprint(
            entityClass,
            entityClassDiscriminator,
            fields,
            tableBlueprint
        );
    }

    private void addChild(String fieldName, EntityBlueprint childEntityBlueprint)
    {
        if(childEntities.containsKey(fieldName))
        {
            throw new PhotonException(String.format("EntityBlueprint already contains a child for field %s", fieldName));
        }
        childEntities.put(fieldName, childEntityBlueprint);
    }
}
