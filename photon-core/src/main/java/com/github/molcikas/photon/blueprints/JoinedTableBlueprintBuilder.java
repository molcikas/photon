package com.github.molcikas.photon.blueprints;

import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.options.PhotonOptions;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class JoinedTableBlueprintBuilder extends TableBlueprintBuilder
{
    public JoinedTableBlueprintBuilder(
        String tableName,
        EntityBlueprintBuilder entityBlueprintBuilder,
        PhotonOptions photonOptions)
    {
        super(tableName, entityBlueprintBuilder, photonOptions);
    }

    @Override
    public TableBlueprintBuilder withParentTable(String parentTableName)
    {
        throw new PhotonException("Cannot call withParentTable() on a joined table builder.");
    }

    @Override
    public TableBlueprintBuilder withForeignKeyToParent(String foreignKeyToParent)
    {
        throw new PhotonException("Cannot call withForeignKeyToParent() on a joined table builder.");
    }

    @Override
    public TableBlueprintBuilder withForeignKeyToParent(String foreignKeyToParent, ColumnDataType columnDataType)
    {
        throw new PhotonException("Cannot call withForeignKeyToParent() on a joined table builder.");
    }

    @Override
    TableBlueprint build(
        Class entityClass,
        List<FieldBlueprint> fields,
        List<String> parentEntityTables,
        boolean isPrimaryTable,
        List<JoinedTableBlueprintBuilder> joinedTableBuilders)
    {
        if(StringUtils.isBlank(idFieldName))
        {
            idFieldName = determineDefaultIdFieldName(entityClass, fields);
        }

        this.parentTableName = parentEntityTables.get(0);
        this.foreignKeyToParent = idFieldName;

        return super.build(entityClass, fields, parentEntityTables, isPrimaryTable, joinedTableBuilders);
    }
}
