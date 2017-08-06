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

    TableBlueprint build(Class entityClass, List<FieldBlueprint> fields, boolean isPrimaryTable, List<JoinedTableBlueprintBuilder> joinedTableBuilders)
    {
        throw new PhotonException("Cannot call build() on a joined table builder without also passing the parent table builder.");
    }

    TableBlueprint build(TableBlueprint parent, Class entityClass, List<FieldBlueprint> fields, boolean isPrimaryTable, List<JoinedTableBlueprintBuilder> joinedTableBuilders)
    {
        if(StringUtils.isBlank(idFieldName))
        {
            idFieldName = determineDefaultIdFieldName(entityClass, fields);
            if(idFieldName == null)
            {
                throw new PhotonException("Id not specified for '%s' and unable to determine a default id field.", entityClass.getName());
            }
        }

        this.parentTableName = parent.getTableName();
        this.foreignKeyToParent = idFieldName;
        return super.build(entityClass, fields, isPrimaryTable, joinedTableBuilders);
    }
}
