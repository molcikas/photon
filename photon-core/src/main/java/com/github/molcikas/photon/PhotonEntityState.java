package com.github.molcikas.photon;

import com.github.molcikas.photon.blueprints.entity.FieldBlueprint;
import com.github.molcikas.photon.blueprints.entity.FieldBlueprintAndKey;
import com.github.molcikas.photon.blueprints.table.TableBlueprint;
import com.github.molcikas.photon.blueprints.table.TableBlueprintAndKey;
import com.github.molcikas.photon.blueprints.table.TableKey;
import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.query.PhotonPreparedStatement;
import com.github.molcikas.photon.query.PopulatedEntity;
import com.github.molcikas.photon.query.PopulatedEntityMap;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PhotonEntityState
{
    private final ListValuedMap<TableBlueprintAndKey, PhotonPreparedStatement.ParameterValue> trackedValues;
    private final SetValuedMap<FieldBlueprintAndKey, TableKey> trackedChildren;

    public PhotonEntityState()
    {
        this.trackedValues = new ArrayListValuedHashMap<>();
        this.trackedChildren = new HashSetValuedHashMap<>();
    }

    public void track(PopulatedEntityMap populatedEntityMap)
    {
        for(PopulatedEntity<?> populatedEntity : populatedEntityMap.getAllPopulatedEntities())
        {
            for(TableBlueprint tableBlueprint : populatedEntity.getEntityBlueprint().getTableBlueprintsForInsertOrUpdate())
            {
                List<PhotonPreparedStatement.ParameterValue> values =
                    populatedEntity.getParameterValues(tableBlueprint, populatedEntity.getParentPopulatedEntity());

                updateTrackedValues(tableBlueprint, populatedEntity.getPrimaryKey(), values);
            }

            for(FieldBlueprint fieldBlueprint : populatedEntity.getEntityBlueprint().getFieldsWithChildEntities())
            {
                List<TableKey> childKeys = populatedEntity
                    .getChildPopulatedEntitiesForField(fieldBlueprint)
                    .stream()
                    .map(PopulatedEntity::getPrimaryKey)
                    .collect(Collectors.toList());

                FieldBlueprintAndKey key = new FieldBlueprintAndKey(fieldBlueprint, populatedEntity.getPrimaryKey());
                trackedChildren.remove(key);
                trackedChildren.putAll(key, childKeys);
            }
        }
    }

    public List<PhotonPreparedStatement.ParameterValue> getTrackedValues(TableBlueprint tableBlueprint, TableKey primaryKey)
    {
        List<PhotonPreparedStatement.ParameterValue> values =
            trackedValues.get(new TableBlueprintAndKey(tableBlueprint, primaryKey));
        return values != null ? values : Collections.emptyList();
    }

    public void updateTrackedValues(TableBlueprint tableBlueprint, TableKey tableKey, List<PhotonPreparedStatement.ParameterValue> values)
    {
        updateTrackedValues(new TableBlueprintAndKey(tableBlueprint, tableKey), values);
    }

    public void updateTrackedValues(TableBlueprintAndKey key, List<PhotonPreparedStatement.ParameterValue> values)
    {
        if(key.getPrimaryKey().getKey() == null)
        {
            throw new PhotonException("Null table primary keys are not allowed in tracking.");
        }
        trackedValues.remove(key);
        trackedValues.putAll(key, values);
    }

    public Set<TableKey> getTrackedChildren(FieldBlueprint fieldBlueprint, TableKey tableKey)
    {
        FieldBlueprintAndKey key = new FieldBlueprintAndKey(fieldBlueprint, tableKey);
        if (!trackedChildren.containsKey(key))
        {
            return null;
        }
        return trackedChildren.get(key);
    }

    public void addTrackedChild(FieldBlueprint fieldBlueprint, TableKey tableKey, TableKey childKey)
    {
        if(tableKey.getKey() == null)
        {
            throw new PhotonException("Null table primary keys are not allowed in tracking.");
        }
        trackedChildren.put(new FieldBlueprintAndKey(fieldBlueprint, tableKey), childKey);
    }
}
