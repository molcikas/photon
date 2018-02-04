package com.github.molcikas.photon.query;

import com.github.molcikas.photon.blueprints.entity.EntityBlueprint;
import com.github.molcikas.photon.blueprints.entity.FieldBlueprint;
import com.github.molcikas.photon.blueprints.entity.FieldBlueprintAndKey;
import com.github.molcikas.photon.blueprints.table.TableBlueprint;
import com.github.molcikas.photon.blueprints.table.TableKey;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PopulatedEntitySnapshot<T>
{
    private final Class entityClass;
    private final EntityBlueprint entityBlueprint;
    private final TableKey primaryKey;
    private final ListValuedMap<TableBlueprint, PhotonPreparedStatement.ParameterValue> tableValues;
    private final Map<EntityBlueprint, PopulatedEntitySnapshot<?>> childEntities;

    public PopulatedEntitySnapshot(PopulatedEntity<T> populatedEntity, PopulatedEntity<?> parentPopulatedEntity)
    {
        this.entityClass = populatedEntity.getEntityInstance().getClass();
        this.entityBlueprint = populatedEntity.getEntityBlueprint();
        this.primaryKey = populatedEntity.getPrimaryKey();
        this.tableValues = new ArrayListValuedHashMap<>();
        this.childEntities = new HashMap<>();

        for(TableBlueprint tableBlueprint : populatedEntity.getEntityBlueprint().getTableBlueprintsForInsertOrUpdate())
        {
            List<PhotonPreparedStatement.ParameterValue> parameterValues =
                populatedEntity.getParameterValues(tableBlueprint, parentPopulatedEntity);
            tableValues.remove(tableBlueprint);
            tableValues.putAll(tableBlueprint, parameterValues);
        }

        for(FieldBlueprint fieldBlueprint : populatedEntity.getEntityBlueprint().getFields())
        {
            if(!fieldBlueprint.getFieldType().isHasChildEntities())
            {
                continue;
            }

            List<PopulatedEntity<?>> fieldChildEntities =
                populatedEntity.getChildPopulatedEntitiesForField(fieldBlueprint);

            for(PopulatedEntity<?> fieldChildEntity : fieldChildEntities)
            {
                childEntities.put(
                    new FieldBlueprintAndKey(fieldBlueprint, fieldChildEntity.getPrimaryKey()),
                    new PopulatedEntitySnapshot<>(fieldChildEntity, populatedEntity));
            }
        }
    }

    public List<PhotonPreparedStatement.ParameterValue> getTrackedValues(
        EntityBlueprint entityBlueprint,
        TableBlueprint tableBlueprint,
        TableKey primaryKey)
    {
        if(entityBlueprint.equals(this.entityBlueprint))
        {
            return tableValues.get(tableBlueprint);
        }


    }
}
