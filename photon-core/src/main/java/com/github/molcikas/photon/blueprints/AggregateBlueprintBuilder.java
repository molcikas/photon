package com.github.molcikas.photon.blueprints;

import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.exceptions.PhotonException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The builder for creating aggregate entity blueprints.
 */
public class AggregateBlueprintBuilder
{
    private final Photon photon;
    private final EntityBlueprintConstructorService entityBlueprintConstructorService;
    private final AggregateBlueprintBuilder parentBuilder;
    private final Class entityClass;
    private final String aggregateBlueprintName;
    private final boolean registerBlueprintForSaving;
    private final List<MappedClassBlueprint> mappedClasses;
    private EntityClassDiscriminator entityClassDiscriminator;
    private String tableName;
    private String idFieldName;
    private boolean isPrimaryKeyAutoIncrement;
    private String foreignKeyToParent;
    private String orderBySql;
    private final Map<String, ColumnDataType> customColumnDataTypes;
    private final List<String> ignoredFields;
    private final Map<String, EntityFieldValueMapping> customDatabaseColumns;
    private final Map<List<String>, CompoundEntityFieldValueMapping> customCompoundDatabaseColumns;
    private final Map<String, AggregateEntityBlueprint> childEntities;
    private final Map<String, String> customFieldToColumnMappings;
    private final Map<String, ForeignKeyListBlueprint> foreignKeyListBlueprints;
    private final Map<String, Converter> customToFieldValueConverters;
    private final Map<String, Converter> customToDatabaseValueConverters;

    public AggregateBlueprintBuilder(Class entityClass, Photon photon, EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        this(entityClass, null, true, null, photon, entityBlueprintConstructorService);
    }

    public AggregateBlueprintBuilder(Class entityClass, AggregateBlueprintBuilder parentBuilder, Photon photon, EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        this(entityClass, null, false, parentBuilder, photon, entityBlueprintConstructorService);
    }

    public AggregateBlueprintBuilder(Class entityClass, String aggregateBlueprintName, boolean registerBlueprintForSaving, Photon photon, EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        this(entityClass, aggregateBlueprintName,
            registerBlueprintForSaving, null, photon, entityBlueprintConstructorService);
    }

    private AggregateBlueprintBuilder(Class entityClass, String aggregateBlueprintName, boolean registerBlueprintForSaving, AggregateBlueprintBuilder parentBuilder, Photon photon, EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        this.entityClass = entityClass;
        this.aggregateBlueprintName = aggregateBlueprintName;
        this.registerBlueprintForSaving = registerBlueprintForSaving;
        this.parentBuilder = parentBuilder;
        this.photon = photon;
        this.entityBlueprintConstructorService = entityBlueprintConstructorService;
        this.isPrimaryKeyAutoIncrement = false;
        this.mappedClasses = new ArrayList<>();
        this.customColumnDataTypes = new HashMap<>();
        this.ignoredFields = new ArrayList<>();
        this.customDatabaseColumns = new HashMap<>();
        this.customCompoundDatabaseColumns = new HashMap<>();
        this.childEntities = new HashMap<>();
        this.customFieldToColumnMappings = new HashMap<>();
        this.foreignKeyListBlueprints = new HashMap<>();
        this.customToFieldValueConverters = new HashMap<>();
        this.customToDatabaseValueConverters = new HashMap<>();
    }

    /**
     * Adds a super or sub class's fields to the entity, and auto-maps them to database columns. This can be used
     * to simply include fields in a sub or super class, or can be combined with withClassDiscriminator() to implement
     * single-table inheritance.
     *
     * @param mappedClass - the super or sub class whose fields will all be mapped into the entity
     * @return - builder for chaining
     */
    public AggregateBlueprintBuilder withMappedClass(Class mappedClass)
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
    public AggregateBlueprintBuilder withMappedClass(Class mappedClass, List<String> includedFields)
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
    public AggregateBlueprintBuilder withClassDiscriminator(EntityClassDiscriminator entityClassDiscriminator)
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
    public AggregateBlueprintBuilder withTableName(String tableName)
    {
        this.tableName = tableName;
        return this;
    }

    /**
     * The id field for the entity. This only needs to be set if the id field is not "id" or "tableNameId" (where
     * tableName is the name of the table.
     * @param idFieldName - the id field name
     * @return - builder for chaining
     */
    public AggregateBlueprintBuilder withId(String idFieldName)
    {
        this.idFieldName = idFieldName;
        return this;
    }

    /**
     * Sets the id field and primary key auto increment in a single method. See the individual setters for details.
     * @param idFieldName - the id field name
     * @param isPrimaryKeyAutoIncrement - whether the primary key is auto incrementing (a.k.a. identity column)
     * @return - builder for chaining
     */
    public AggregateBlueprintBuilder withId(String idFieldName, boolean isPrimaryKeyAutoIncrement)
    {
        this.idFieldName = idFieldName;
        this.isPrimaryKeyAutoIncrement = isPrimaryKeyAutoIncrement;
        return this;
    }

    /**
     * Sets the id field, column data type, and primary key auto increment in a single method. See the individual
     * setters for details.
     * @param idFieldName - the id field name
     * @param columnDataType - the column data type for the primary key column
     * @return - builder for chaining
     */
    public AggregateBlueprintBuilder withId(String idFieldName, ColumnDataType columnDataType)
    {
        this.idFieldName = idFieldName;
        this.customColumnDataTypes.put(idFieldName, columnDataType);
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
    public AggregateBlueprintBuilder withId(String idFieldName, ColumnDataType columnDataType, boolean isPrimaryKeyAutoIncrement)
    {
        this.idFieldName = idFieldName;
        this.customColumnDataTypes.put(idFieldName, columnDataType);
        this.isPrimaryKeyAutoIncrement = isPrimaryKeyAutoIncrement;
        return this;
    }

    /**
     * Sets the primary key as auto incrementing (a.k.a. identity column).
     * @return - builder for chaining
     */
    public AggregateBlueprintBuilder withPrimaryKeyAutoIncrement()
    {
        this.isPrimaryKeyAutoIncrement = true;
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
    public AggregateBlueprintBuilder withForeignKeyToParent(String foreignKeyToParent)
    {
        this.foreignKeyToParent = foreignKeyToParent;
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
    public AggregateBlueprintBuilder withForeignKeyToParent(String foreignKeyToParent, ColumnDataType columnDataType)
    {
        this.foreignKeyToParent = foreignKeyToParent;
        this.customColumnDataTypes.put(foreignKeyToParent, columnDataType);
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
    public AggregateBlueprintBuilder withForeignKeyListToOtherAggregate(
        String fieldName,
        String foreignTableName,
        String foreignTableJoinColumnName,
        String foreignTableKeyColumnName,
        ColumnDataType foreignTableKeyColumnType,
        Class fieldListItemClass)
    {
        foreignKeyListBlueprints.put(fieldName, new ForeignKeyListBlueprint(
            foreignTableName,
            foreignTableJoinColumnName,
            foreignTableKeyColumnName,
            foreignTableKeyColumnType,
            fieldListItemClass
        ));
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
    public AggregateBlueprintBuilder withDatabaseColumn(String columnName, ColumnDataType columnDataType)
    {
        customColumnDataTypes.put(columnName, columnDataType);
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
    public AggregateBlueprintBuilder withDatabaseColumn(String columnName, String fieldName)
    {
        customFieldToColumnMappings.put(fieldName, columnName);
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
    public AggregateBlueprintBuilder withDatabaseColumn(String columnName, String fieldName, ColumnDataType columnDataType)
    {
        customFieldToColumnMappings.put(fieldName, columnName);
        customColumnDataTypes.put(columnName, columnDataType);
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
    public AggregateBlueprintBuilder withDatabaseColumn(String columnName, ColumnDataType columnDataType,
                                                        EntityFieldValueMapping entityFieldValueMapping)
    {
        customColumnDataTypes.put(columnName, columnDataType);
        customDatabaseColumns.put(columnName, entityFieldValueMapping);
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
    public AggregateBlueprintBuilder withDatabaseColumns(
        List<DatabaseColumnDefinition> columnDefinitions,
        CompoundEntityFieldValueMapping compoundEntityFieldValueMapping)
    {
        if(columnDefinitions == null || columnDefinitions.isEmpty())
        {
            throw new PhotonException("Column definitions list cannot be null or empty.");
        }

        for(DatabaseColumnDefinition columnDefinition : columnDefinitions)
        {
            customColumnDataTypes.put(columnDefinition.getColumnName(), columnDefinition.getColumnDataType());
        }

        customCompoundDatabaseColumns.put(
            columnDefinitions.stream().map(DatabaseColumnDefinition::getColumnName).collect(Collectors.toList()),
            compoundEntityFieldValueMapping
        );

        return this;
    }

    /**
     * Sets a custom value converter for converting a database value to an entity field value.
     *
     * @param fieldName - the entity field name
     * @param customToFieldValueConverter - the converter
     * @return - builder for chaining
     */
    public AggregateBlueprintBuilder withCustomToFieldValueConverter(String fieldName, Converter customToFieldValueConverter)
    {
        customToFieldValueConverters.put(fieldName, customToFieldValueConverter);
        return this;
    }

    /**
     * Sets a custom value converter for converting an entity field value into a database value.
     *
     * @param columnName - the database column name
     * @param customToDatabaseValueConverter - the converter
     * @return - builder for chaining
     */
    public AggregateBlueprintBuilder withCustomToDatabaseValueConverter(String columnName, Converter customToDatabaseValueConverter)
    {
        customToDatabaseValueConverters.put(columnName, customToDatabaseValueConverter);
        return this;
    }

    /**
     * Ignore a field and prevent it from being auto-mapped to a database column.
     *
     * @param fieldName - the entity field name
     * @return - builder for chaining
     */
    public AggregateBlueprintBuilder withIgnoredField(String fieldName)
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
    public AggregateBlueprintBuilder withOrderBySql(String orderBySql)
    {
        this.orderBySql = orderBySql;
        return this;
    }

    /**
     * Creates a builder that is used to build the blueprint for a child entity.
     *
     * @param childClass - the child entity class
     * @return - the child builder
     */
    public AggregateBlueprintBuilder withChild(Class childClass)
    {
        return new AggregateBlueprintBuilder(childClass, this, photon, entityBlueprintConstructorService);
    }

    /**
     * Completes the builder and registers it as a child of the parent entity.
     *
     * @param fieldName - the field name on the parent that references the child entity.
     * @return - the parent builder for chaining
     */
    public AggregateBlueprintBuilder addAsChild(String fieldName)
    {
        if(parentBuilder == null)
        {
            throw new PhotonException(String.format("Cannot add child to field '%s' because there is no parent entity.", fieldName));
        }
        if(StringUtils.isBlank(foreignKeyToParent))
        {
            throw new PhotonException(String.format("Cannot add child to parent field '%s' because the child does not have a foreign key to parent set.", fieldName));
        }
        parentBuilder.addChild(fieldName, buildBlueprint());
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
        photon.registerBuiltAggregateBlueprint(aggregateBlueprintName, registerBlueprintForSaving, buildBlueprint());
    }

    private AggregateEntityBlueprint buildBlueprint()
    {
        return new AggregateEntityBlueprint(
            entityClass,
            mappedClasses,
            entityClassDiscriminator,
            tableName,
            idFieldName,
            isPrimaryKeyAutoIncrement,
            foreignKeyToParent,
            orderBySql,
            customColumnDataTypes,
            ignoredFields,
            customDatabaseColumns,
            customCompoundDatabaseColumns,
            customFieldToColumnMappings,
            childEntities,
            foreignKeyListBlueprints,
            customToFieldValueConverters,
            customToDatabaseValueConverters,
            photon.getOptions(),
            entityBlueprintConstructorService
        );
    }

    private void addChild(String fieldName, AggregateEntityBlueprint childEntityBlueprint)
    {
        if(childEntities.containsKey(fieldName))
        {
            throw new PhotonException(String.format("EntityBlueprint already contains a child for field %s", fieldName));
        }
        childEntities.put(fieldName, childEntityBlueprint);
    }
}
