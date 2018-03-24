package com.github.molcikas.photon.query;

import com.github.molcikas.photon.PhotonEntityState;
import com.github.molcikas.photon.blueprints.entity.EntityBlueprint;
import com.github.molcikas.photon.blueprints.entity.FieldBlueprint;
import com.github.molcikas.photon.blueprints.table.TableBlueprint;
import com.github.molcikas.photon.blueprints.table.TableValue;
import com.github.molcikas.photon.options.PhotonOptions;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

public class Orphans
{
    public static void findAndDelete(
        EntityBlueprint entityBlueprint,
        List<PopulatedEntity> populatedEntities,
        PopulatedEntity parentPopulatedEntity,
        FieldBlueprint parentFieldBlueprint,
        PhotonEntityState photonEntityState,
        Connection connection,
        PhotonOptions photonOptions)
    {
        if(parentFieldBlueprint == null)
        {
            return;
        }

        TableBlueprint tableBlueprint = entityBlueprint.getTableBlueprint();

        List<TableValue> childIds = populatedEntities
            .stream()
            .map(PopulatedEntity::getPrimaryKeyValue)
            .filter(Objects::nonNull) // Auto increment entities that have not been inserted yet will have null primary key values.
            .map(TableValue::new)
            .collect(Collectors.toList());

        Set<TableValue> trackedKeys =
            photonEntityState.getTrackedChildrenKeys(parentFieldBlueprint, parentPopulatedEntity.getPrimaryKey());
        if(trackedKeys != null && new HashSet<>(childIds).equals(trackedKeys))
        {
            return;
        }

        if(tableBlueprint.isPrimaryKeyMappedToField())
        {
            String primaryKeyColumnName = tableBlueprint.getPrimaryKeyColumnName();
            String selectOrphansSql = tableBlueprint.getSelectOrphansSql();
            if(selectOrphansSql == null)
            {
                // If the primary key and foreign key to parent are equal, there won't be any select orphans sql because
                // there can't be any orphans, so just return.
                return;
            }

            List<Object> orphanIds;

            try(PhotonPreparedStatement statement = new PhotonPreparedStatement(selectOrphansSql, false, connection, photonOptions))
            {
                statement.setNextParameter(
                    parentPopulatedEntity.getPrimaryKeyValue(),
                    tableBlueprint.getParentTableBlueprint().getPrimaryKeyColumn().getColumnDataType(),
                    tableBlueprint.getParentTableBlueprint().getPrimaryKeyColumnSerializer());
                statement.setNextArrayParameter(
                    childIds.stream().map(TableValue::getValue).collect(Collectors.toList()),
                    tableBlueprint.getPrimaryKeyColumn().getColumnDataType(),
                    tableBlueprint.getPrimaryKeyColumnSerializer());
                List<PhotonQueryResultRow> rows =
                    statement.executeQuery(Collections.singletonList(primaryKeyColumnName), Collections.singletonList(primaryKeyColumnName.toLowerCase()));
                orphanIds =
                    rows.stream().map(r -> r.getValue(primaryKeyColumnName)).collect(Collectors.toList());
            }

            if(orphanIds.size() == 0)
            {
                return;
            }

            deleteOrphansAndTheirChildrenRecursive(orphanIds, entityBlueprint, Collections.emptyList(), connection, photonOptions);

            photonEntityState.untrackChildrenRecursive(
                parentFieldBlueprint,
                parentPopulatedEntity.getPrimaryKey(),
                entityBlueprint,
                tableBlueprint,
                orphanIds.stream().map(TableValue::new).collect(Collectors.toList()));
        }
        else
        {
            // If a child does not have a primary key, then it has to be deleted and re-inserted on every save.
            try(PhotonPreparedStatement statement = new PhotonPreparedStatement(
                tableBlueprint.getDeleteChildrenExceptSql(),
                false,
                connection,
                photonOptions))
            {
                statement.setNextParameter(
                    parentPopulatedEntity.getPrimaryKeyValue(),
                    tableBlueprint.getParentTableBlueprint().getPrimaryKeyColumn().getColumnDataType(),
                    tableBlueprint.getParentTableBlueprint().getPrimaryKeyColumnSerializer());
                statement.setNextParameter(Collections.emptyList(), null, null);
                statement.executeUpdate();
            }
        }
    }

    public static void findAndDeleteJoined(
        EntityBlueprint entityBlueprint,
        List<PopulatedEntity> populatedEntities,
        Connection connection,
        PhotonOptions photonOptions)
    {
        for(PopulatedEntity populatedEntity : populatedEntities)
        {
            for (TableBlueprint tableBlueprint : entityBlueprint.getJoinedTableBlueprints())
            {
                if(tableBlueprint.isApplicableForEntityClass(populatedEntity.getEntityInstance().getClass()))
                {
                    continue;
                }

                String primaryKeyColumnName = tableBlueprint.getPrimaryKeyColumnName();
                List<?> orphanIds;

                try(PhotonPreparedStatement statement = new PhotonPreparedStatement(
                    tableBlueprint.getSelectByIdSql(),
                    false,
                    connection,
                    photonOptions))
                {
                    statement.setNextParameter(
                        populatedEntity.getPrimaryKeyValue(),
                        tableBlueprint.getPrimaryKeyColumn().getColumnDataType(),
                        tableBlueprint.getPrimaryKeyColumnSerializer());
                    List<PhotonQueryResultRow> rows =
                        statement.executeQuery(Collections.singletonList(primaryKeyColumnName), Collections.singletonList(primaryKeyColumnName.toLowerCase()));
                    orphanIds =
                        rows.stream().map(r -> r.getValue(primaryKeyColumnName)).collect(Collectors.toList());
                }
                if(orphanIds.size() > 0)
                {
                    deleteTableOrphansAndItsChildrenRecursive(orphanIds, entityBlueprint, tableBlueprint, connection, photonOptions);
                }
            }
        }
    }

    private static void deleteOrphansAndTheirChildrenRecursive(
        List<?> orphanIds,
        EntityBlueprint entityBlueprint,
        List<EntityBlueprint> parentEntityBlueprints,
        Connection connection,
        PhotonOptions photonOptions)
    {
        TableBlueprint rootEntityTableBlueprint = parentEntityBlueprints.size() > 0 ?
            parentEntityBlueprints.get(parentEntityBlueprints.size() - 1).getTableBlueprint() :
            entityBlueprint.getTableBlueprint();

        for(FieldBlueprint fieldBlueprint : entityBlueprint.getFieldsWithChildEntities())
        {
            List<EntityBlueprint> childParentEntityBlueprints = new ArrayList<>(parentEntityBlueprints.size() + 1);
            childParentEntityBlueprints.add(entityBlueprint);
            childParentEntityBlueprints.addAll(parentEntityBlueprints);
            deleteOrphansAndTheirChildrenRecursive(
                orphanIds,
                fieldBlueprint.getChildEntityBlueprint(),
                childParentEntityBlueprints,
                connection,
                photonOptions);
        }

        for(TableBlueprint tableBlueprint : entityBlueprint.getTableBlueprintsForDelete())
        {
            try(PhotonPreparedStatement statement = new PhotonPreparedStatement(
                tableBlueprint.getDeleteOrphansSql(parentEntityBlueprints.size()),
                false,
                connection,
                photonOptions))
            {
                statement.setNextArrayParameter(
                    orphanIds,
                    rootEntityTableBlueprint.getPrimaryKeyColumn().getColumnDataType(),
                    rootEntityTableBlueprint.getPrimaryKeyColumnSerializer());
                statement.executeUpdate();
            }
        }
    }

    private static void deleteTableOrphansAndItsChildrenRecursive(
        List<?> orphanIds,
        EntityBlueprint entityBlueprint,
        TableBlueprint tableBlueprint,
        Connection connection,
        PhotonOptions photonOptions)
    {
        List<FieldBlueprint> fieldsWithChildEntities = entityBlueprint
            .getFieldsWithChildEntities()
            .stream()
            .filter(t -> tableBlueprint.equals(t.getChildEntityBlueprint().getTableBlueprint().getParentTableBlueprint()))
            .collect(Collectors.toList());

        for(FieldBlueprint fieldBlueprint : fieldsWithChildEntities)
        {
            List<EntityBlueprint> childParentEntityBlueprints = new ArrayList<>(1);
            childParentEntityBlueprints.add(entityBlueprint);
            deleteOrphansAndTheirChildrenRecursive(
                orphanIds,
                fieldBlueprint.getChildEntityBlueprint(),
                childParentEntityBlueprints,
                connection,
                photonOptions);
        }

        try(PhotonPreparedStatement statement = new PhotonPreparedStatement(
            tableBlueprint.getDeleteOrphansSql(0),
            false,
            connection,
            photonOptions))
        {
            statement.setNextArrayParameter(
                orphanIds,
                entityBlueprint.getTableBlueprint().getPrimaryKeyColumn().getColumnDataType(),
                entityBlueprint.getTableBlueprint().getPrimaryKeyColumnSerializer());
            statement.executeUpdate();
        }
    }
}
