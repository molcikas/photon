package com.github.molcikas.photon;

import com.github.molcikas.photon.blueprints.AggregateBlueprint;
import com.github.molcikas.photon.blueprints.AggregateBlueprintAndKey;
import com.github.molcikas.photon.blueprints.entity.FieldBlueprint;
import com.github.molcikas.photon.blueprints.entity.FieldBlueprintAndKey;
import com.github.molcikas.photon.blueprints.table.TableBlueprint;
import com.github.molcikas.photon.blueprints.table.TableBlueprintAndKey;
import com.github.molcikas.photon.blueprints.table.TableKey;
import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.query.PhotonPreparedStatement;
import com.github.molcikas.photon.query.PopulatedEntity;
import com.github.molcikas.photon.query.PopulatedEntityMap;
import com.github.molcikas.photon.query.PopulatedEntitySnapshot;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.util.*;
import java.util.stream.Collectors;

public class PhotonEntityState
{
    private final Map<AggregateBlueprintAndKey, PopulatedEntitySnapshot<?>> trackedAggregates;

    public PhotonEntityState()
    {
        this.trackedAggregates = new HashMap<>();
    }

    public void track(PopulatedEntityMap populatedEntityMap, AggregateBlueprint<?> aggregateBlueprint)
    {
        List<PopulatedEntity<?>> rootEntities =
            populatedEntityMap.getPopulatedEntitiesForBlueprint(aggregateBlueprint.getAggregateRootEntityBlueprint());

        for(PopulatedEntity<?> rootEntity : rootEntities)
        {
            PopulatedEntitySnapshot snapshot = new PopulatedEntitySnapshot<>(rootEntity);
            trackedAggregates.put(new AggregateBlueprintAndKey(aggregateBlueprint, rootEntity.getPrimaryKey()), snapshot);
        }
    }

    public List<PhotonPreparedStatement.ParameterValue> getTrackedValues(
        AggregateBlueprint<?> aggregateBlueprint,
        TableKey aggregateKey,
        TableBlueprint tableBlueprint,
        TableKey primaryKey)
    {
        PopulatedEntitySnapshot<?> snapshot = trackedAggregates.get(new AggregateBlueprintAndKey(aggregateBlueprint, aggregateKey));
        if(snapshot == null)
        {
            return null;
        }


        snapshot

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
