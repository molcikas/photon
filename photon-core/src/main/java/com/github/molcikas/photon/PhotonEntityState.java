package com.github.molcikas.photon;

import com.github.molcikas.photon.blueprints.table.TableBlueprint;
import com.github.molcikas.photon.blueprints.table.TableBlueprintAndKey;
import com.github.molcikas.photon.blueprints.table.TableKey;
import com.github.molcikas.photon.query.PhotonPreparedStatement;
import com.github.molcikas.photon.query.PopulatedEntity;
import com.github.molcikas.photon.query.PopulatedEntityMap;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.Collections;
import java.util.List;

public class PhotonEntityState
{
    private final ListValuedMap<TableBlueprintAndKey, PhotonPreparedStatement.ParameterValue> trackedValues;

    public PhotonEntityState()
    {
        this.trackedValues = new ArrayListValuedHashMap<>();
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
        }
    }

    public List<PhotonPreparedStatement.ParameterValue> getTrackedValues(TableBlueprint tableBlueprint, TableKey primaryKey)
    {
        List<PhotonPreparedStatement.ParameterValue> values =
            trackedValues.get(new TableBlueprintAndKey(tableBlueprint, primaryKey));
        return values != null ? values : Collections.emptyList();
    }

    public void updateTrackedValues(TableBlueprint tableBlueprint, TableKey key, List<PhotonPreparedStatement.ParameterValue> values)
    {
        updateTrackedValues(new TableBlueprintAndKey(tableBlueprint, key), values);
    }

    public void updateTrackedValues(TableBlueprintAndKey key, List<PhotonPreparedStatement.ParameterValue> values)
    {
        trackedValues.remove(key);
        trackedValues.putAll(key, values);
    }
}
