package com.github.molcikas.photon;

import com.github.molcikas.photon.blueprints.entity.EntityBlueprint;
import com.github.molcikas.photon.blueprints.entity.EntityBlueprintAndKey;
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PhotonEntityState
{
    private final ListValuedMap<TableBlueprintAndKey, PhotonPreparedStatement.ParameterValue> trackedValues;
    private final SetValuedMap<FieldBlueprintAndKey, EntityBlueprintAndKey> trackedChildren;
    private final SetValuedMap<FieldBlueprintAndKey, Object> trackedFlattenedCollectionValues;

    public PhotonEntityState()
    {
        this.trackedValues = new ArrayListValuedHashMap<>();
        this.trackedChildren = new HashSetValuedHashMap<>();
        this.trackedFlattenedCollectionValues = new HashSetValuedHashMap<>();
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
                FieldBlueprintAndKey parentBlueprintAndKey =
                    new FieldBlueprintAndKey(fieldBlueprint, populatedEntity.getPrimaryKey());
                trackedChildren.remove(parentBlueprintAndKey);

                List<TableKey> childKeys = populatedEntity
                    .getChildPopulatedEntitiesForField(fieldBlueprint)
                    .stream()
                    .map(PopulatedEntity::getPrimaryKey)
                    .collect(Collectors.toList());

                for(TableKey childKey : childKeys)
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
    }

    public List<PhotonPreparedStatement.ParameterValue> getTrackedValues(TableBlueprint tableBlueprint, TableKey primaryKey)
    {
        List<PhotonPreparedStatement.ParameterValue> values =
            trackedValues.get(new TableBlueprintAndKey(tableBlueprint, primaryKey));
        return values != null ? values : Collections.emptyList();
    }

    public List<TableKey> getTrackedKeys(TableBlueprint tableBlueprint, List<TableKey> primaryKeys)
    {
        return primaryKeys
            .stream()
            .filter(p -> (trackedValues.containsKey(new TableBlueprintAndKey(tableBlueprint, p))))
            .collect(Collectors.toList());
    }

    public void updateTrackedValues(TableBlueprint tableBlueprint, TableKey tableKey, List<PhotonPreparedStatement.ParameterValue> values)
    {
        updateTrackedValues(new TableBlueprintAndKey(tableBlueprint, tableKey), values);
    }

    public void updateTrackedValues(TableBlueprintAndKey key, List<PhotonPreparedStatement.ParameterValue> values)
    {
        if(!key.getTableBlueprint().isPrimaryKeyMappedToField())
        {
            // We can't track the entity if the primary key is not mapped to a field.
            return;
        }
        if(key.getPrimaryKey().getKey() == null)
        {
            throw new PhotonException("Null table primary keys are not allowed in tracking.");
        }
        trackedValues.remove(key);
        trackedValues.putAll(key, values);
    }

    public Set<TableKey> getTrackedChildrenKeys(FieldBlueprint fieldBlueprint, TableKey parentKey)
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

    public Collection getTrackedFlattenedCollectionValues(FieldBlueprint fieldBlueprint, TableKey primaryKey)
    {
        return trackedFlattenedCollectionValues.get(new FieldBlueprintAndKey(fieldBlueprint, primaryKey));
    }

    public void addTrackedChild(
        FieldBlueprint fieldBlueprint,
        TableKey parentKey,
        EntityBlueprint childEntityBlueprint,
        TableKey childKey)
    {
        if(parentKey.getKey() == null)
        {
            throw new PhotonException("Null table primary keys are not allowed in tracking.");
        }
        trackedChildren.put(
            new FieldBlueprintAndKey(fieldBlueprint, parentKey),
            new EntityBlueprintAndKey(childEntityBlueprint, childKey));
    }

    public void removeTrackedChildrenRecursive(
        FieldBlueprint parentFieldBlueprint,
        TableKey parentKey,
        EntityBlueprint childEntityBlueprint,
        TableBlueprint childTableBlueprint,
        List<TableKey> childKeysToRemove)
    {
        for(TableKey childKey : childKeysToRemove)
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
            for(TableKey childKey : childKeysToRemove)
            {
                Set<EntityBlueprintAndKey> grandchildKeys = trackedChildren.get(new FieldBlueprintAndKey(childFieldBlueprint, childKey));
                if(grandchildKeys == null)
                {
                    return;
                }
                EntityBlueprint grandchildEntityBlueprint =childFieldBlueprint.getChildEntityBlueprint();
                for(TableBlueprint grandchildTableBlueprint : grandchildEntityBlueprint.getTableBlueprintsForDelete())
                removeTrackedChildrenRecursive(
                    childFieldBlueprint,
                    childKey,
                    grandchildEntityBlueprint,
                    grandchildTableBlueprint,
                    grandchildKeys.stream().map(EntityBlueprintAndKey::getPrimaryKey).collect(Collectors.toList()));
            }
        }
    }
}
