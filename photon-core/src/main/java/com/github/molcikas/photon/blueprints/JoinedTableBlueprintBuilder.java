package com.github.molcikas.photon.blueprints;

import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.options.PhotonOptions;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class JoinedTableBlueprintBuilder extends TableBlueprintBuilder
{
    JoinedTableBlueprintBuilder(
        Class entityClass,
        String tableName,
        JoinType joinType,
        EntityBlueprintBuilder entityBlueprintBuilder,
        PhotonOptions photonOptions)
    {
        super(entityClass, tableName, joinType, entityBlueprintBuilder, photonOptions);
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
        boolean isSimpleEntity,
        List<FieldBlueprint> fields,
        List<String> parentEntityTables,
        TableBlueprint mainTableBlueprint,
        List<JoinedTableBlueprintBuilder> joinedTableBuilders)
    {
        if(StringUtils.isBlank(idFieldName))
        {
            idFieldName = determineDefaultIdFieldName(fields);
        }

        this.parentTableName = parentEntityTables.get(0);
        this.foreignKeyToParent = idFieldName;

        if(isSimpleEntity)
        {
            throw new PhotonException("Simple entity with main table '%s' cannot have joined tables.", parentTableName);
        }

        return super.build(false, fields, parentEntityTables, mainTableBlueprint, joinedTableBuilders);
    }
}
