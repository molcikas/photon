package com.github.molcikas.photon.blueprints;

import org.apache.commons.lang3.StringUtils;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.Photon;
import com.github.molcikas.photon.exceptions.PhotonException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public AggregateEntityBlueprintBuilder(Class entityClass, Photon photon, EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        this(entityClass, null, photon, entityBlueprintConstructorService);
    }

    public AggregateEntityBlueprintBuilder(Class entityClass, AggregateEntityBlueprintBuilder parentBuilder, Photon photon,
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

    public AggregateEntityBlueprintBuilder withTableName(String tableName)
    {
        this.tableName = tableName;
        return this;
    }

    public AggregateEntityBlueprintBuilder withId(String idFieldName)
    {
        this.idFieldName = idFieldName;
        return this;
    }

    public AggregateEntityBlueprintBuilder withPrimaryKeyAutoIncrement()
    {
        this.isPrimaryKeyAutoIncrement = true;
        return this;
    }

    public AggregateEntityBlueprintBuilder withForeignKeyToParent(String foreignKeyToParent)
    {
        this.foreignKeyToParent = foreignKeyToParent;
        return this;
    }

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

    public AggregateEntityBlueprintBuilder withColumnDataType(String columnName, Integer columnDataType)
    {
        customColumnDataTypes.put(columnName, columnDataType);
        return this;
    }

    public AggregateEntityBlueprintBuilder withIgnoredField(String fieldName)
    {
        ignoredFields.add(fieldName);
        return this;
    }

    public AggregateEntityBlueprintBuilder withFieldToColumnMapping(String fieldName, String columnName)
    {
        customFieldToColumnMappings.put(fieldName, columnName);
        return this;
    }

    public AggregateEntityBlueprintBuilder withDatabaseColumn(String columnName, Integer columnDataType, EntityFieldValueMapping entityFieldValueMapping)
    {
        customColumnDataTypes.put(columnName, columnDataType);
        customDatabaseColumns.put(columnName, entityFieldValueMapping);
        return this;
    }

    public AggregateEntityBlueprintBuilder withCustomToFieldValueConverter(String fieldName, Converter customToFieldValueConverter)
    {
        customToFieldValueConverters.put(fieldName, customToFieldValueConverter);
        return this;
    }

    public AggregateEntityBlueprintBuilder withCustomToDatabaseValueConverter(String columnName, Converter customToDatabaseValueConverter)
    {
        customToDatabaseValueConverters.put(columnName, customToDatabaseValueConverter);
        return this;
    }

    public AggregateEntityBlueprintBuilder withOrderBy(String orderByColumnName)
    {
        return withOrderBy(orderByColumnName, SortDirection.Ascending);
    }

    public AggregateEntityBlueprintBuilder withOrderBy(String orderByColumnName, SortDirection orderByDirection)
    {
        this.orderByColumnName = orderByColumnName;
        this.orderByDirection = orderByDirection;
        return this;
    }

    public AggregateEntityBlueprintBuilder withChild(Class childClass)
    {
        return new AggregateEntityBlueprintBuilder(childClass, this, photon, entityBlueprintConstructorService);
    }

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
