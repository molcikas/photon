package photon.blueprints;

import org.apache.commons.lang3.StringUtils;
import photon.converters.Converter;
import photon.exceptions.PhotonException;

import java.util.*;
import java.util.stream.Collectors;

public class AggregateEntityBlueprint extends EntityBlueprint
{
    private ColumnBlueprint foreignKeyToParentColumn;
    private boolean isPrimaryKeyMappedToField;

    private String selectOrphansSql;
    private Map<Integer, String> deleteOrphansSql;

    public ColumnBlueprint getForeignKeyToParentColumn()
    {
        return foreignKeyToParentColumn;
    }

    public boolean isPrimaryKeyMappedToField()
    {
        return isPrimaryKeyMappedToField;
    }

    public String getSelectOrphansSql()
    {
        return selectOrphansSql;
    }

    public String getDeleteOrphanSql(int parentLevelsUpForOrphanIds)
    {
        return deleteOrphansSql.get(parentLevelsUpForOrphanIds);
    }

    AggregateEntityBlueprint(
        Class entityClass,
        String idFieldName,
        boolean isPrimaryKeyAutoIncrement,
        String foreignKeyToParentColumnName,
        String orderByColumnName,
        SortDirection orderByDirection,
        Map<String, Integer> customColumnDataTypes,
        List<String> ignoredFields,
        Map<String, EntityFieldValueMapping> customDatabaseColumns,
        Map<String, String> customFieldToColumnMappings,
        Map<String, AggregateEntityBlueprint> childEntities,
        Map<String, ForeignKeyListBlueprint> foreignKeyListBlueprints,
        Map<String, Converter> customToFieldValueConverters,
        Map<String, Converter> customToDatabaseValueConverters,
        EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        if(entityClass == null)
        {
            throw new PhotonException("EntityBlueprint class cannot be null.");
        }
        if(StringUtils.isBlank(idFieldName))
        {
            throw new PhotonException("EntityBlueprint id cannot be blank.");
        }
        if(orderByDirection == null)
        {
            orderByDirection = SortDirection.Ascending;
        }

        this.deleteOrphansSql = new HashMap<>();
        this.entityClass = entityClass;
        this.orderByDirection = orderByDirection;
        this.fields = entityBlueprintConstructorService.getFieldsForEntity(entityClass, ignoredFields, customDatabaseColumns, customFieldToColumnMappings, childEntities, foreignKeyListBlueprints, customToFieldValueConverters);
        this.columns = entityBlueprintConstructorService.getColumnsForEntityFields(fields, idFieldName, isPrimaryKeyAutoIncrement, foreignKeyToParentColumnName, customColumnDataTypes, customToDatabaseValueConverters);

        try
        {
            this.entityConstructor = entityClass.getDeclaredConstructor();
            entityConstructor.setAccessible(true);
        }
        catch (Exception ex)
        {
            throw new PhotonException(
                String.format("Error getting constructor for entity '%s'. Make sure the entity has a parameterless constructor (private is ok).", getEntityClassName()),
                ex
            );
        }

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

        if(primaryKeyColumn == null)
        {
            if(!customColumnDataTypes.containsKey(idFieldName))
            {
                throw new PhotonException(String.format("The column data type for '%s' must be specified since it is the id and is not in the entity.", idFieldName));
            }
            primaryKeyColumn = new ColumnBlueprint(
                idFieldName,
                customColumnDataTypes.get(idFieldName),
                true,
                isPrimaryKeyAutoIncrement,
                idFieldName.equals(foreignKeyToParentColumnName),
                customToDatabaseValueConverters.get(idFieldName),
                null,
                columns.size()
            );
            columns.add(primaryKeyColumn);
        }
        this.isPrimaryKeyMappedToField = getPrimaryKeyColumn().getMappedFieldBlueprint() != null;

        if(StringUtils.isNotBlank(foreignKeyToParentColumnName) && foreignKeyToParentColumn == null)
        {
            if(!customColumnDataTypes.containsKey(foreignKeyToParentColumnName))
            {
                throw new PhotonException(String.format("The column data type for '%s' must be specified since it is a foreign key and is not in the entity '%s'.", foreignKeyToParentColumnName, entityClass.getName()));
            }
            foreignKeyToParentColumn = new ColumnBlueprint(
                foreignKeyToParentColumnName,
                customColumnDataTypes.get(foreignKeyToParentColumnName),
                false,
                false,
                true,
                customToDatabaseValueConverters.get(foreignKeyToParentColumnName),
                null,
                columns.size()
            );
            columns.add(foreignKeyToParentColumn);
        }

        if(StringUtils.isBlank(orderByColumnName))
        {
            orderByColumnName = primaryKeyColumn.getColumnName();
        }
        this.orderByColumnName = orderByColumnName;

        if(!getColumn(orderByColumnName).isPresent())
        {
            throw new PhotonException(String.format("The order by column '%s' is not a column for the entity '%s'.", orderByColumnName, entityClass.getName()));
        }

        normalizeColumnOrder();
    }

    public String getForeignKeyToParentColumnName()
    {
        return foreignKeyToParentColumn != null ? foreignKeyToParentColumn.getColumnName() : null;
    }

    public List<FieldBlueprint> getFieldsWithChildEntities()
    {
        return fields
            .stream()
            .filter(f -> f.getFieldType() == FieldType.Entity || f.getFieldType() == FieldType.EntityList)
            .collect(Collectors.toList());
    }

    public List<FieldBlueprint> getForeignKeyListFields()
    {
        return fields
            .stream()
            .filter(f -> f.getFieldType() == FieldType.ForeignKeyList)
            .collect(Collectors.toList());
    }

    public void setSelectOrphansSql(String selectOrphansSql)
    {
        this.selectOrphansSql = selectOrphansSql;
    }

    public void setDeleteOrphansSql(String deleteOrphanSql, int parentLevelsUpForOrphanIds)
    {
        deleteOrphansSql.put(parentLevelsUpForOrphanIds, deleteOrphanSql);
    }
}
