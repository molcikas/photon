package com.github.molcikas.photon.blueprints;

import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.exceptions.PhotonException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The builder for creating aggregate entity blueprints.
 */
public class AggregateEntityBlueprintBuilder
{
    private final Photon photon;
    private final EntityBlueprintConstructorService entityBlueprintConstructorService;
    private final AggregateEntityBlueprintBuilder parentBuilder;
    private final Class entityClass;
    private String tableName;
    private String idFieldName;
    private boolean isPrimaryKeyAutoIncrement;
    private String foreignKeyToParent;
    private String orderByColumnName;
    private SortDirection orderByDirection;
    private final Map<String, Integer> customColumnDataTypes;
    private final List<String> ignoredFields;
    private final Map<String, EntityFieldValueMapping> customDatabaseColumns;
    private final Map<String, AggregateEntityBlueprint> childEntities;
    private final Map<String, String> customFieldToColumnMappings;
    private final Map<String, ForeignKeyListBlueprint> foreignKeyListBlueprints;
    private final Map<String, Converter> customToFieldValueConverters;
    private final Map<String, Converter> customToDatabaseValueConverters;

    /**
     * Constructor. This should not be called directly. Use photon.registerAggregate() to create a builder.
     *
     * @param entityClass - the entity class
     * @param photon - the photon object
     * @param entityBlueprintConstructorService - the service for constructing entity blueprints
     */
    public AggregateEntityBlueprintBuilder(Class entityClass, Photon photon, EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        this(entityClass, null, photon, entityBlueprintConstructorService);
    }

    private AggregateEntityBlueprintBuilder(Class entityClass, AggregateEntityBlueprintBuilder parentBuilder, Photon photon,
                                           EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        this.entityClass = entityClass;
        this.photon = photon;
        this.entityBlueprintConstructorService = entityBlueprintConstructorService;
        this.parentBuilder = parentBuilder;
        this.isPrimaryKeyAutoIncrement = false;
        this.customColumnDataTypes = new HashMap<>();
        this.ignoredFields = new ArrayList<>();
        this.customDatabaseColumns = new HashMap<>();
        this.childEntities = new HashMap<>();
        this.customFieldToColumnMappings = new HashMap<>();
        this.foreignKeyListBlueprints = new HashMap<>();
        this.customToFieldValueConverters = new HashMap<>();
        this.customToDatabaseValueConverters = new HashMap<>();
    }

    /**
     * The database table name for the entity. This only needs to be set if the entity name is different than the
     * table name.
     * @param tableName - table name
     * @return - builder for chaining
     */
    public AggregateEntityBlueprintBuilder withTableName(String tableName)
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
    public AggregateEntityBlueprintBuilder withId(String idFieldName)
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
    public AggregateEntityBlueprintBuilder withId(String idFieldName, boolean isPrimaryKeyAutoIncrement)
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
     * @param isPrimaryKeyAutoIncrement - whether the primary key is auto incrementing (a.k.a. identity column)
     * @return - builder for chaining
     */
    public AggregateEntityBlueprintBuilder withId(String idFieldName, Integer columnDataType, boolean isPrimaryKeyAutoIncrement)
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
    public AggregateEntityBlueprintBuilder withPrimaryKeyAutoIncrement()
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
    public AggregateEntityBlueprintBuilder withForeignKeyToParent(String foreignKeyToParent)
    {
        this.foreignKeyToParent = foreignKeyToParent;
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
    public AggregateEntityBlueprintBuilder withForeignKeyListToOtherAggregate(
        String fieldName,
        String foreignTableName,
        String foreignTableJoinColumnName,
        String foreignTableKeyColumnName,
        Integer foreignTableKeyColumnType,
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
     * @param columnDataType - the database column data type. Use java.sql.Types.
     * @return - builder for chaining
     */
    public AggregateEntityBlueprintBuilder withColumnDataType(String columnName, Integer columnDataType)
    {
        customColumnDataTypes.put(columnName, columnDataType);
        return this;
    }

    /**
     * Ignore a field and prevent it from being auto-mapped to a database column.
     *
     * @param fieldName - the entity field name
     * @return - builder for chaining
     */
    public AggregateEntityBlueprintBuilder withIgnoredField(String fieldName)
    {
        ignoredFields.add(fieldName);
        return this;
    }

    /**
     * Create a custom field-to-column name mapping. This only needs to be called if the column name is not the same
     * as the field name.
     *
     * @param fieldName - the entity field name
     * @param columnName - the database column name
     * @return - builder for chaining
     */
    public AggregateEntityBlueprintBuilder withFieldToColumnMapping(String fieldName, String columnName)
    {
        customFieldToColumnMappings.put(fieldName, columnName);
        return this;
    }

    /**
     * Create a custom field-to-column name mapping. This only needs to be called if the column name is not the same
     * as the field name.
     *
     * @param fieldName - the entity field name
     * @param columnName - the database column name
     * @param columnDataType - the column data type
     * @return - builder for chaining
     */
    public AggregateEntityBlueprintBuilder withFieldToColumnMapping(String fieldName, String columnName, Integer columnDataType)
    {
        customFieldToColumnMappings.put(fieldName, columnName);
        customColumnDataTypes.put(columnName, columnDataType);
        return this;
    }

    /**
     * Creates a database column that is mapped to and from a field value via a custom value mapper. This method should
     * be used if a database row value does not map directly to a field (e.g. if the value consists of multiple fields
     * or a field on a child entity).
     *
     * @param columnName - the database column name
     * @param columnDataType - the database column data type
     * @param entityFieldValueMapping - the mapper that maps the entity field value to and from the database column value
     * @return - builder for chaining
     */
    public AggregateEntityBlueprintBuilder withDatabaseColumn(String columnName, Integer columnDataType, EntityFieldValueMapping entityFieldValueMapping)
    {
        customColumnDataTypes.put(columnName, columnDataType);
        customDatabaseColumns.put(columnName, entityFieldValueMapping);
        return this;
    }

    /**
     * Sets a custom value converter for converting a database value to an entity field value.
     *
     * @param fieldName - the entity field name
     * @param customToFieldValueConverter - the converter
     * @return - builder for chaining
     */
    public AggregateEntityBlueprintBuilder withCustomToFieldValueConverter(String fieldName, Converter customToFieldValueConverter)
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
    public AggregateEntityBlueprintBuilder withCustomToDatabaseValueConverter(String columnName, Converter customToDatabaseValueConverter)
    {
        customToDatabaseValueConverters.put(columnName, customToDatabaseValueConverter);
        return this;
    }

    /**
     * Sets the database column to use for sorting the database entities. Defaults to ascending order.
     *
     * @param orderByColumnName - The database column name
     * @return - builder for chaining
     */
    public AggregateEntityBlueprintBuilder withOrderBy(String orderByColumnName)
    {
        return withOrderBy(orderByColumnName, SortDirection.Ascending);
    }

    /**
     * Sets the database column to use for sorting the database entities. Defaults to ascending order.
     *
     * @param orderByColumnName - The database column name
     * @param orderByDirection - The sort direction, ascending or descending.
     * @return - builder for chaining
     */
    public AggregateEntityBlueprintBuilder withOrderBy(String orderByColumnName, SortDirection orderByDirection)
    {
        this.orderByColumnName = orderByColumnName;
        this.orderByDirection = orderByDirection;
        return this;
    }

    /**
     * Creates a builder that is used to build the blueprint for a child entity.
     *
     * @param childClass - the child entity class
     * @return - the child builder
     */
    public AggregateEntityBlueprintBuilder withChild(Class childClass)
    {
        return new AggregateEntityBlueprintBuilder(childClass, this, photon, entityBlueprintConstructorService);
    }

    /**
     * Completes the builder and registers it as a child of the parent entity.
     *
     * @param fieldName - the field name on the parent that references the child entity.
     * @return - the parent builder for chaining
     */
    public AggregateEntityBlueprintBuilder addAsChild(String fieldName)
    {
        if(parentBuilder == null)
        {
            throw new PhotonException(String.format("Cannot add entity to field '%s' as a child because it does not have a parent entity.", fieldName));
        }
        if(StringUtils.isBlank(foreignKeyToParent))
        {
            throw new PhotonException(String.format("Cannot add entity to parent field '%s' because the entity does not have a foreign key to parent set.", fieldName));
        }
        parentBuilder.addChild(fieldName, buildEntity());
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
        photon.registerAggregate(buildEntity());
    }

    private AggregateEntityBlueprint buildEntity()
    {
        return new AggregateEntityBlueprint(
            entityClass,
            tableName,
            idFieldName,
            isPrimaryKeyAutoIncrement,
            foreignKeyToParent,
            orderByColumnName,
            orderByDirection,
            customColumnDataTypes,
            ignoredFields,
            customDatabaseColumns,
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
