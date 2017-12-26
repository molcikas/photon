package com.github.molcikas.photon.blueprints.table;

import com.github.molcikas.photon.blueprints.entity.CompoundEntityFieldValueMapping;
import com.github.molcikas.photon.blueprints.entity.*;
import com.github.molcikas.photon.converters.Converter;
import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.options.PhotonOptions;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class TableBlueprintBuilder
{
    private final EntityBlueprintBuilder entityBlueprintBuilder;
    private final PhotonOptions photonOptions;
    private Class entityClass;
    private String tableName;
    private JoinType joinType;
    private boolean isPrimaryKeyAutoIncrement;
    private String orderBySql;
    private final Map<String, ColumnDataType> customColumnDataTypes;
    private final Map<String, EntityFieldValueMapping> customDatabaseColumns;
    private final Map<List<String>, CompoundEntityFieldValueMapping> customCompoundDatabaseColumns;
    private final Map<String, String> customFieldToColumnMappings;
    private final Map<String, FlattenedCollectionBlueprint> flattenedCollectionBlueprints;
    private final Map<String, Converter> customDatabaseColumnSerializers;

    protected String idFieldName;
    protected String parentTableName;
    protected String foreignKeyToParent;

    public String getTableName()
    {
        return StringUtils.isNotBlank(tableName) ? tableName : determineDefaultTableName();
    }

    public String getForeignKeyToParent()
    {
        return foreignKeyToParent;
    }

    public Map<String, String> getCustomFieldToColumnMappings()
    {
        return Collections.unmodifiableMap(customFieldToColumnMappings);
    }

    public Map<List<String>, CompoundEntityFieldValueMapping> getCustomCompoundDatabaseColumns()
    {
        return Collections.unmodifiableMap(customCompoundDatabaseColumns);
    }

    public Map<String, FlattenedCollectionBlueprint> getFlattenedCollectionBlueprints()
    {
        return Collections.unmodifiableMap(flattenedCollectionBlueprints);
    }

    public TableBlueprintBuilder(EntityBlueprintBuilder entityBlueprintBuilder, PhotonOptions photonOptions)
    {
        this(null, null, null, entityBlueprintBuilder, photonOptions);
    }

    public TableBlueprintBuilder(
        Class entityClass,
        String tableName,
        JoinType joinType,
        EntityBlueprintBuilder entityBlueprintBuilder,
        PhotonOptions photonOptions)
    {
        this.entityClass = entityClass;
        this.tableName = tableName;
        this.joinType = joinType != null ? joinType : JoinType.InnerJoin;
        this.entityBlueprintBuilder = entityBlueprintBuilder;
        this.photonOptions = photonOptions;
        this.isPrimaryKeyAutoIncrement = false;
        this.customColumnDataTypes = new HashMap<>();
        this.customDatabaseColumns = new HashMap<>();
        this.customCompoundDatabaseColumns = new HashMap<>();
        this.customFieldToColumnMappings = new HashMap<>();
        this.flattenedCollectionBlueprints = new HashMap<>();
        this.customDatabaseColumnSerializers = new HashMap<>();

        if(entityClass != null)
        {
            entityBlueprintBuilder.withMappedClass(entityClass);

            Arrays
                .stream(entityClass.getDeclaredFields())
                .forEach(f -> customFieldToColumnMappings.put(f.getName(), f.getName()));
        }
    }

    /**
     * The database table name for the entity. This only needs to be set if the entity name is different than the
     * table name.
     * @param tableName - table name
     * @return - builder for chaining
     */
    public TableBlueprintBuilder withTableName(String tableName)
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
    public TableBlueprintBuilder withId(String idFieldName)
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
    public TableBlueprintBuilder withId(String idFieldName, boolean isPrimaryKeyAutoIncrement)
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
    public TableBlueprintBuilder withId(String idFieldName, ColumnDataType columnDataType)
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
    public TableBlueprintBuilder withId(String idFieldName, ColumnDataType columnDataType, boolean isPrimaryKeyAutoIncrement)
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
    public TableBlueprintBuilder withPrimaryKeyAutoIncrement()
    {
        this.isPrimaryKeyAutoIncrement = true;
        return this;
    }

    public TableBlueprintBuilder withParentTable(String parentTableName)
    {
        this.parentTableName = parentTableName;
        return this;
    }

    public TableBlueprintBuilder withParentTable(String parentTableName, String foreignKeyToParent)
    {
        this.parentTableName = parentTableName;
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
     * @return - builder for chaining
     */
    public TableBlueprintBuilder withForeignKeyToParent(String foreignKeyToParent)
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
    public TableBlueprintBuilder withForeignKeyToParent(String foreignKeyToParent, ColumnDataType columnDataType)
    {
        this.foreignKeyToParent = foreignKeyToParent;
        this.customColumnDataTypes.put(foreignKeyToParent, columnDataType);
        return this;
    }

    /**
     * Sets up a collection that flattens a child table into a list of values from a column in the table.
     *
     * @param fieldName - the field that will contain the flattened collection
     * @param fieldClass - the class type for the items in the field collection
     * @param tableName - the table name of the collection to flatten
     * @param foreignKeyToParent - the foreign table column that joins back to the aggregate
     * @param columnName - the foreign table column that contains the values in the flattened collection
     * @param columnDataType - the column data type for the foreign table key column
     * @return - builder for chaining
     */
    public TableBlueprintBuilder withFlattenedCollection(
        String fieldName,
        Class fieldClass,
        String tableName,
        String foreignKeyToParent,
        String columnName,
        ColumnDataType columnDataType)
    {
        flattenedCollectionBlueprints.put(fieldName, new FlattenedCollectionBlueprint(
            fieldClass,
            tableName,
            foreignKeyToParent,
            columnName,
            columnDataType
        ));
        return this;
    }

    public TableBlueprintBuilder withDatabaseColumn(String columnName)
    {
        customFieldToColumnMappings.put(columnName, columnName);
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
    public TableBlueprintBuilder withDatabaseColumn(String columnName, ColumnDataType columnDataType)
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
    public TableBlueprintBuilder withDatabaseColumn(String columnName, String fieldName)
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
    public TableBlueprintBuilder withDatabaseColumn(String columnName, String fieldName, ColumnDataType columnDataType)
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
    public TableBlueprintBuilder withDatabaseColumn(String columnName, ColumnDataType columnDataType,
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
    public TableBlueprintBuilder withDatabaseColumns(
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
     * Sets a custom converter for serializing an entity field value into a database value.
     *
     * @param columnName - the database column name
     * @param databaseColumnSerializer - the converter for serializing values into the database
     * @return - builder for chaining
     */
    public TableBlueprintBuilder withDatabaseColumnSerializer(String columnName, Converter databaseColumnSerializer)
    {
        customDatabaseColumnSerializers.put(columnName, databaseColumnSerializer);
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
    public TableBlueprintBuilder withOrderBySql(String orderBySql)
    {
        this.orderBySql = orderBySql;
        return this;
    }

    public EntityBlueprintBuilder addAsJoinedTable()
    {
        return entityBlueprintBuilder.addAsJoinedTable(this);
    }

    public TableBlueprint build(
        boolean isSimpleEntity,
        List<FieldBlueprint> fields,
        List<String> parentEntityTables,
        TableBlueprint mainTableBlueprint,
        List<JoinedTableBlueprintBuilder> joinedTableBuilders)
    {
        boolean isPrimaryTable = mainTableBlueprint == null;

        if(StringUtils.isBlank(tableName))
        {
            tableName = determineDefaultTableName();
        }

        if(isSimpleEntity)
        {
            if(!isPrimaryTable)
            {
                throw new PhotonException("Table '%s' is for a simple entity but is not the primary table.", tableName);
            }

            if(!joinedTableBuilders.isEmpty())
            {
                throw new PhotonException("Table '%s' is for a simple entity but has joined tables.", tableName);
            }
        }

        if(StringUtils.isBlank(idFieldName))
        {
            idFieldName = determineDefaultIdFieldName(fields);
            if(!isSimpleEntity && idFieldName == null)
            {
                throw new PhotonException("Id not specified for '%s' and unable to determine a default id field.", tableName);
            }
        }

        if(StringUtils.isBlank(parentTableName))
        {
            if(mainTableBlueprint != null)
            {
                // If this is a joined table, use the main table as the parent.
                parentTableName = mainTableBlueprint.getTableName();
            }
            else if(parentEntityTables != null && !parentEntityTables.isEmpty())
            {
                parentTableName = parentEntityTables.get(0);
            }
        }

        List<FieldBlueprint> fieldsWithColumnMappings = fields
            .stream()
            .filter(f -> f.getFieldType() != FieldType.FlattenedCollection)
            .collect(Collectors.toList());

        List<ColumnBlueprint> columns = new ArrayList<>(fieldsWithColumnMappings.size() + 2); // 2 extra for primary key and foreign key to parent.

        for(FieldBlueprint fieldBlueprint : fieldsWithColumnMappings)
        {
            ColumnBlueprint columnBlueprint = createColumnForField(
                fieldBlueprint,
                null,
                isPrimaryTable,
                joinedTableBuilders,
                columns.size()
            );
            if(columnBlueprint != null)
            {
                columns.add(columnBlueprint);
            }
        }

        for(Map.Entry<String, EntityFieldValueMapping> customDatabaseColumnEntry : customDatabaseColumns.entrySet())
        {
            FieldBlueprint fieldBlueprint = new FieldBlueprint(
                null,
                null,
                null,
                null,
                null,
                null,
                customDatabaseColumnEntry.getValue(),
                null
            );
            fields.add(fieldBlueprint);
            ColumnBlueprint columnBlueprint = createColumnForField(
                fieldBlueprint,
                customDatabaseColumnEntry.getKey(),
                isPrimaryTable,
                joinedTableBuilders,
                columns.size()
            );
            if(columnBlueprint != null)
            {
                columns.add(columnBlueprint);
            }
        }

        for(Map.Entry<List<String>, CompoundEntityFieldValueMapping> customCompoundDatabaseColumnEntry : customCompoundDatabaseColumns.entrySet())
        {
            FieldBlueprint fieldBlueprint = new FieldBlueprint(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                customCompoundDatabaseColumnEntry.getValue()
            );
            fields.add(fieldBlueprint);
            for(String columnName : customCompoundDatabaseColumnEntry.getKey())
            {
                ColumnBlueprint columnBlueprint = createColumnForField(
                    fieldBlueprint,
                    columnName,
                    isPrimaryTable,
                    joinedTableBuilders,
                    columns.size()
                );
                if (columnBlueprint != null)
                {
                    columns.add(columnBlueprint);
                }
            }
        }

        columns = columns
            .stream()
            .sorted(Comparator.comparingInt(ColumnBlueprint::getColumnIndex))
            .sorted((c1, c2) -> c1.isPrimaryKeyColumn() == c2.isPrimaryKeyColumn() ? 0 : c1.isPrimaryKeyColumn() ? 1 : -1)
            .collect(Collectors.toList());

        int i = 0;
        for(ColumnBlueprint columnBlueprint : columns)
        {
            columnBlueprint.moveColumnToIndex(i);
            i++;
        }

        ColumnBlueprint primaryKeyColumn = null;
        ColumnBlueprint foreignKeyToParentColumn = null;

        for(ColumnBlueprint columnBlueprint : columns)
        {
            if(columnBlueprint.isPrimaryKeyColumn())
            {
                primaryKeyColumn = columnBlueprint;
            }
            if(columnBlueprint.isForeignKeyToParentColumn())
            {
                foreignKeyToParentColumn = columnBlueprint;
            }
        }

        if(primaryKeyColumn == null && customColumnDataTypes.containsKey(idFieldName))
        {
            primaryKeyColumn = new ColumnBlueprint(
                tableName,
                idFieldName,
                customColumnDataTypes.get(idFieldName),
                true,
                isPrimaryKeyAutoIncrement,
                idFieldName.equals(foreignKeyToParent),
                customDatabaseColumnSerializers.get(idFieldName),
                null,
                columns.size()
            );
            columns.add(primaryKeyColumn);
        }

        if(!isSimpleEntity && primaryKeyColumn == null)
        {
            throw new PhotonException("The column data type for '%s' must be specified since it is the id and is not in the entity.", idFieldName);
        }

        if(StringUtils.isNotBlank(foreignKeyToParent) && foreignKeyToParentColumn == null)
        {
            if(!customColumnDataTypes.containsKey(foreignKeyToParent))
            {
                throw new PhotonException(String.format(
                    "The column data type for '%s' must be specified since it is a foreign key and is not in the entity '%s'.",
                    foreignKeyToParent,
                    tableName));
            }
            foreignKeyToParentColumn = new ColumnBlueprint(
                tableName,
                foreignKeyToParent,
                customColumnDataTypes.get(foreignKeyToParent),
                false,
                false,
                true,
                customDatabaseColumnSerializers.get(foreignKeyToParent),
                null,
                columns.size()
            );
            columns.add(foreignKeyToParentColumn);
        }

        columns = normalizeColumnOrder(columns);

        return new TableBlueprint(
            mainTableBlueprint,
            parentTableName,
            joinType,
            entityClass,
            columns,
            primaryKeyColumn,
            foreignKeyToParentColumn,
            primaryKeyColumn != null && primaryKeyColumn.getMappedFieldBlueprint() != null,
            tableName,
            orderBySql
        );
    }

    private ColumnBlueprint createColumnForField(
        FieldBlueprint fieldBlueprint,
        String columnName,
        boolean isPrimaryTable,
        List<JoinedTableBlueprintBuilder> joinedTableBuilders,
        int currentColumnCount)
    {
        String fieldName = fieldBlueprint.getFieldName();
        if(StringUtils.isBlank(columnName))
        {
            columnName = customFieldToColumnMappings.containsKey(fieldName) ?
                customFieldToColumnMappings.get(fieldName) : fieldName;
        }
        if(!columnIsMappedToThisTable(columnName, isPrimaryTable, joinedTableBuilders))
        {
            return null;
        }
        DefaultColumnDataTypeResult columnDataTypeResult = customColumnDataTypes.containsKey(columnName) ?
            new DefaultColumnDataTypeResult(customColumnDataTypes.get(columnName)) :
            defaultColumnDataTypeForField(fieldBlueprint.getFieldClass(), photonOptions);
        if (!columnDataTypeResult.foundDataType)
        {
            return null;
        }
        boolean isPrimaryKey = idFieldName != null && StringUtils.equals(fieldName, idFieldName);
        return new ColumnBlueprint(
            tableName,
            columnName,
            columnDataTypeResult.dataType,
            isPrimaryKey,
            isPrimaryKey && isPrimaryKeyAutoIncrement,
            foreignKeyToParent != null && StringUtils.equals(fieldName, foreignKeyToParent),
            customDatabaseColumnSerializers.get(columnName),
            fieldBlueprint,
            currentColumnCount
        );
    }

    private boolean columnIsMappedToThisTable(String columnName, boolean isPrimaryTable, List<JoinedTableBlueprintBuilder> joinedTableBuilders)
    {
        if(StringUtils.equals(columnName, idFieldName) || StringUtils.equals(columnName, foreignKeyToParent))
        {
            return true;
        }

        if(!isPrimaryTable)
        {
            return customFieldToColumnMappings.containsKey(columnName) ||
                customCompoundDatabaseColumns.keySet().stream().anyMatch(c -> c.contains(columnName));
        }

        List<TableBlueprintBuilder> otherTableBuilders = joinedTableBuilders
            .stream()
            .filter(j -> !this.equals(j))
            .collect(Collectors.toList());

        List<String> otherTableColumns = otherTableBuilders
            .stream()
            .flatMap(j -> j.getCustomFieldToColumnMappings().keySet().stream())
            .collect(Collectors.toList());

        otherTableColumns.addAll(otherTableBuilders
            .stream()
            .flatMap(j -> j.getCustomCompoundDatabaseColumns().keySet().stream())
            .flatMap(Collection::stream)
            .collect(Collectors.toList())
        );

        return !otherTableColumns.contains(columnName);
    }

    public static DefaultColumnDataTypeResult defaultColumnDataTypeForField(Class fieldType, PhotonOptions photonOptions)
    {
        if(fieldType == null)
        {
            return DefaultColumnDataTypeResult.notFound();
        }

        if(fieldType.equals(int.class) || fieldType.equals(Integer.class))
        {
            return new DefaultColumnDataTypeResult(ColumnDataType.INTEGER);
        }

        if(fieldType.equals(long.class) || fieldType.equals(Long.class))
        {
            return new DefaultColumnDataTypeResult(ColumnDataType.BIGINT);
        }

        if(fieldType.equals(float.class) || fieldType.equals(Float.class))
        {
            return new DefaultColumnDataTypeResult(ColumnDataType.FLOAT);
        }

        if(fieldType.equals(double.class) || fieldType.equals(Double.class))
        {
            return new DefaultColumnDataTypeResult(ColumnDataType.DOUBLE);
        }

        if(fieldType.equals(boolean.class) || fieldType.equals(Boolean.class))
        {
            return new DefaultColumnDataTypeResult(ColumnDataType.BOOLEAN);
        }

        if(fieldType.equals(UUID.class))
        {
            return new DefaultColumnDataTypeResult(photonOptions != null ? photonOptions.getDefaultUuidDataType() : PhotonOptions.DEFAULT_UUID_DATA_TYPE);
        }

        if(fieldType.equals(String.class))
        {
            return new DefaultColumnDataTypeResult(ColumnDataType.VARCHAR);
        }

        if(fieldType.isEnum())
        {
            return new DefaultColumnDataTypeResult(ColumnDataType.INTEGER);
        }

        if(fieldType.equals(Date.class) ||
            fieldType.equals(Instant.class) ||
            fieldType.equals(ZonedDateTime.class) ||
            fieldType.equals(LocalDate.class) ||
            fieldType.equals(LocalDateTime.class))
        {
            return new DefaultColumnDataTypeResult(ColumnDataType.TIMESTAMP);
        }

        return DefaultColumnDataTypeResult.notFound();
    }

    private String determineDefaultTableName()
    {
        Class entityClass = this.entityClass;

        if(entityClass == null)
        {
            entityClass = entityBlueprintBuilder.getEntityClass();
        }

        switch(photonOptions.getDefaultTableName())
        {
            default:
            case ClassName:
                return entityClass.getSimpleName();
            case ClassNameLowerCase:
                return entityClass.getSimpleName().toLowerCase();
        }
    }

    protected String determineDefaultIdFieldName(List<FieldBlueprint> fields)
    {
        Optional<FieldBlueprint> idField = fields.stream().filter(f -> f.getFieldName().equalsIgnoreCase("id")).findFirst();
        if(idField.isPresent())
        {
            return idField.get().getFieldName();
        }

        if(entityClass != null)
        {
            String fullIdName = entityClass.getSimpleName().toLowerCase() + "Id";
            Optional<FieldBlueprint> fullIdField = fields.stream().filter(f -> f.getFieldName().equalsIgnoreCase(fullIdName)).findFirst();
            if(fullIdField.isPresent())
            {
                return fullIdField.get().getFieldName();
            }
        }
        else
        {
            String fullIdName = entityBlueprintBuilder.getEntityClass().getSimpleName().toLowerCase() + "Id";
            Optional<FieldBlueprint> fullIdField = fields.stream().filter(f -> f.getFieldName().equalsIgnoreCase(fullIdName)).findFirst();
            if(fullIdField.isPresent())
            {
                return fullIdField.get().getFieldName();
            }
        }

        return null;
    }

    private List<ColumnBlueprint> normalizeColumnOrder(List<ColumnBlueprint> columns)
    {
        // Sort columns by putting primary key columns at the end, then sort by current column index.
        columns = columns
            .stream()
            .sorted(Comparator.comparingInt(ColumnBlueprint::getColumnIndex))
            .sorted((c1, c2) -> c1.isPrimaryKeyColumn() == c2.isPrimaryKeyColumn() ? 0 : c1.isPrimaryKeyColumn() ? 1 : -1)
            .collect(Collectors.toList());

        int i = 0;
        for(ColumnBlueprint columnBlueprint : columns)
        {
            columnBlueprint.moveColumnToIndex(i);
            i++;
        }

        return columns;
    }
}
