package com.github.molcikas.photon;

import com.github.molcikas.photon.blueprints.entity.EntityBlueprint;
import com.github.molcikas.photon.blueprints.entity.EntityBlueprintAndKey;
import com.github.molcikas.photon.blueprints.entity.FieldBlueprint;
import com.github.molcikas.photon.blueprints.entity.FieldBlueprintAndKey;
import com.github.molcikas.photon.blueprints.table.TableBlueprint;
import com.github.molcikas.photon.blueprints.table.TableBlueprintAndKey;
import com.github.molcikas.photon.blueprints.table.TableValue;
import com.github.molcikas.photon.exceptions.PhotonException;
import com.github.molcikas.photon.query.ParameterValue;
import com.github.molcikas.photon.query.PopulatedEntity;
import com.github.molcikas.photon.query.PopulatedEntityMap;
import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.util.*;
import java.util.stream.Collectors;

public class PhotonEntityState
{
    private final Map<TableBlueprintAndKey, Map<String, ParameterValue>> trackedValues;
    private final SetValuedMap<FieldBlueprintAndKey, EntityBlueprintAndKey> trackedChildren;
    private final SetValuedMap<FieldBlueprintAndKey, Object> trackedFlattenedCollectionValues;

    public PhotonEntityState()
    {
        this.trackedValues = new HashMap<>();
        this.trackedChildren = new HashSetValuedHashMap<>();
        this.trackedFlattenedCollectionValues = new HashSetValuedHashMap<>();
    }

    public void track(PopulatedEntity<?> populatedEntity)
    {
        for(TableBlueprint tableBlueprint : populatedEntity.getEntityBlueprint().getTableBlueprintsForInsertOrUpdate())
        {
            Map<String, ParameterValue> values = populatedEntity
                .getParameterValuesForUpdate(tableBlueprint, populatedEntity.getParentPopulatedEntity(), null)
                .getValues();

            updateTrackedValues(tableBlueprint, populatedEntity.getPrimaryKey(), values);
        }

        for(FieldBlueprint fieldBlueprint : populatedEntity.getEntityBlueprint().getFieldsWithChildEntities())
        {
            FieldBlueprintAndKey parentBlueprintAndKey =
                new FieldBlueprintAndKey(fieldBlueprint, populatedEntity.getPrimaryKey());
            trackedChildren.remove(parentBlueprintAndKey);

            List<TableValue> childKeys = populatedEntity
                .getChildPopulatedEntitiesForField(fieldBlueprint)
                .stream()
                .map(PopulatedEntity::getPrimaryKey)
                .collect(Collectors.toList());

            for(TableValue childKey : childKeys)
            {
                trackedChildren.put(
                    parentBlueprintAndKey,
                    new EntityBlueprintAndKey(fieldBlueprint.getChildEntityBlueprint(), childKey));
            }
        }

        for(FieldBlueprint fieldBlueprint : populatedEntity.getEntityBlueprint().getFlattenedCollectionFields())
        {
            FieldBlueprintAndKey parentBlueprintAndKey =
                new FieldBlueprintAndKey(fieldBlueprint, populatedEntity.getPrimaryKey());
            trackedFlattenedCollectionValues.remove(parentBlueprintAndKey);

            Collection flattenedCollectionValues = (Collection) populatedEntity.getInstanceValue(fieldBlueprint, null);
            if(flattenedCollectionValues == null)
            {
                flattenedCollectionValues = Collections.emptyList();
            }
            trackedFlattenedCollectionValues.putAll(parentBlueprintAndKey, flattenedCollectionValues);
        }
    }

    public void track(PopulatedEntityMap populatedEntityMap)
    {
        populatedEntityMap.getAllPopulatedEntities().forEach(this::track);
    }

    public Map<String, ParameterValue> getTrackedValues(TableBlueprint tableBlueprint, TableValue primaryKey)
    {
        Map<String, ParameterValue> values = trackedValues.get(new TableBlueprintAndKey(tableBlueprint, primaryKey));
        return values != null ? values : Collections.emptyMap();
    }

    public List<TableValue> getTrackedKeys(TableBlueprint tableBlueprint, List<TableValue> primaryKeys)
    {
        return primaryKeys
            .stream()
            .filter(p -> (trackedValues.containsKey(new TableBlueprintAndKey(tableBlueprint, p))))
            .collect(Collectors.toList());
    }

    public void updateTrackedValues(TableBlueprint tableBlueprint, TableValue tableKey, Map<String, ParameterValue> values)
    {
        updateTrackedValues(new TableBlueprintAndKey(tableBlueprint, tableKey), values);
    }

    public void updateTrackedValues(TableBlueprintAndKey key, Map<String, ParameterValue> values)
    {
        if(!key.getTableBlueprint().isPrimaryKeyMappedToField())
        {
            // We can't track the entity if the primary key is not mapped to a field.
            return;
        }
        if(key.getPrimaryKey().getValue() == null)
        {
            throw new PhotonException("Null table primary keys are not allowed in tracking.");
        }
        trackedValues.remove(key);
        trackedValues.put(key, values);
    }

    public Set<TableValue> getTrackedChildrenKeys(FieldBlueprint fieldBlueprint, TableValue parentKey)
    {
        FieldBlueprintAndKey parentBlueprintAndKey = new FieldBlueprintAndKey(fieldBlueprint, parentKey);
        if (!trackedChildren.containsKey(parentBlueprintAndKey))
        {
            return null;
        }
        return trackedChildren
            .get(parentBlueprintAndKey)
            .stream()
            .map(EntityBlueprintAndKey::getPrimaryKey)
            .collect(Collectors.toSet());
    }

    public Collection getTrackedFlattenedCollectionValues(FieldBlueprint fieldBlueprint, TableValue primaryKey)
    {
        return trackedFlattenedCollectionValues.get(new FieldBlueprintAndKey(fieldBlueprint, primaryKey));
    }

    public void addTrackedChild(
        FieldBlueprint fieldBlueprint,
        TableValue parentKey,
        EntityBlueprint childEntityBlueprint,
        TableValue childKey)
    {
        if(parentKey.getValue() == null)
        {
            throw new PhotonException("Null table primary keys are not allowed in tracking.");
        }
        trackedChildren.put(
            new FieldBlueprintAndKey(fieldBlueprint, parentKey),
            new EntityBlueprintAndKey(childEntityBlueprint, childKey));
    }

    public void untrack(PopulatedEntity<?> populatedEntity)
    {
        for (TableBlueprint tableBlueprint : populatedEntity.getEntityBlueprint().getTableBlueprintsForDelete())
        {
            TableBlueprintAndKey key = new TableBlueprintAndKey(tableBlueprint, populatedEntity.getPrimaryKey());
            trackedValues.remove(key);
        }

        for (FieldBlueprint fieldBlueprint : populatedEntity.getEntityBlueprint().getFlattenedCollectionFields())
        {
            trackedFlattenedCollectionValues.remove(fieldBlueprint);
        }

        for(FieldBlueprint fieldBlueprint : populatedEntity.getEntityBlueprint().getFieldsWithChildEntities())
        {
            for(TableBlueprint tableBlueprint : fieldBlueprint.getChildEntityBlueprint().getTableBlueprintsForDelete())
            {
                List<TableValue> childKeys = populatedEntity
                    .getChildPopulatedEntitiesForField(fieldBlueprint)
                    .stream()
                    .map(PopulatedEntity::getPrimaryKey)
                    .collect(Collectors.toList());

                untrackChildrenRecursive(
                    fieldBlueprint,
                    populatedEntity.getPrimaryKey(),
                    fieldBlueprint.getChildEntityBlueprint(),
                    tableBlueprint,
                    childKeys
                );
            }
        }
    }

    public void untrackChildrenRecursive(
        FieldBlueprint parentFieldBlueprint,
        TableValue parentKey,
        EntityBlueprint childEntityBlueprint,
        TableBlueprint childTableBlueprint,
        List<TableValue> childKeysToRemove)
    {
        for(TableValue childKey : childKeysToRemove)
        {
            TableBlueprintAndKey key = new TableBlueprintAndKey(childTableBlueprint, childKey);
            trackedValues.remove(key);
        }

        for(FieldBlueprint fieldBlueprint : childEntityBlueprint.getFlattenedCollectionFields())
        {
            trackedFlattenedCollectionValues.remove(fieldBlueprint);
        }

        if(parentFieldBlueprint == null)
        {
            return;
        }

        FieldBlueprintAndKey parentBlueprintAndKey = new FieldBlueprintAndKey(parentFieldBlueprint, parentKey);
        Set<EntityBlueprintAndKey> childKeys = trackedChildren.get(parentBlueprintAndKey);
        if(childKeys == null)
        {
            return;
        }

        childKeys.removeAll(
            childKeysToRemove.stream().map(o -> new EntityBlueprintAndKey(childEntityBlueprint, o)).collect(Collectors.toList()));

        // All children of orphans are also orphans and need to be removed as well.
        for(FieldBlueprint childFieldBlueprint : childEntityBlueprint.getFieldsWithChildEntities())
        {
            for(TableValue childKey : childKeysToRemove)
            {
                Set<EntityBlueprintAndKey> grandchildKeys = trackedChildren.get(new FieldBlueprintAndKey(childFieldBlueprint, childKey));
                if(grandchildKeys == null)
                {
                    return;
                }
                EntityBlueprint grandchildEntityBlueprint =childFieldBlueprint.getChildEntityBlueprint();
                for(TableBlueprint grandchildTableBlueprint : grandchildEntityBlueprint.getTableBlueprintsForDelete())
                untrackChildrenRecursive(
                    childFieldBlueprint,
                    childKey,
                    grandchildEntityBlueprint,
                    grandchildTableBlueprint,
                    grandchildKeys.stream().map(EntityBlueprintAndKey::getPrimaryKey).collect(Collectors.toList()));
            }
        }
    }
}
