package photon.query;

import photon.exceptions.PhotonException;
import photon.blueprints.AggregateBlueprint;
import photon.blueprints.ColumnBlueprint;
import photon.blueprints.EntityBlueprint;
import photon.blueprints.FieldBlueprint;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

public class PhotonAggregateQuery<T>
{
    private final AggregateBlueprint aggregateBlueprint;
    private final Connection connection;

    public PhotonAggregateQuery(
        AggregateBlueprint aggregateBlueprint,
        Connection connection)
    {
        this.aggregateBlueprint = aggregateBlueprint;
        this.connection = connection;
    }

    public T fetchById(Object id)
    {
        List<PopulatedEntity> populatedAggregateRoots = getPopulatedAggregateRoots(Collections.singletonList(id));

        if(populatedAggregateRoots.size() == 0)
        {
            return null;
        }

        return (T) populatedAggregateRoots.get(0).getEntityInstance();
    }

    public List<T> fetchByIds(List ids)
    {
        List<PopulatedEntity> populatedAggregateRoots = getPopulatedAggregateRoots(ids);

        return (List<T>) populatedAggregateRoots
            .stream()
            .map(pe -> pe.getEntityInstance())
            .collect(Collectors.toList());
    }

    public void save(T aggregate)
    {
        saveEntityRecursive(Collections.singletonList(aggregate), aggregateBlueprint.getAggregateRootEntityBlueprint(), null, null);
    }

    private void saveEntityRecursive(Collection entityInstances, EntityBlueprint entityBlueprint, Object parentEntityInstance, EntityBlueprint parentEntityBlueprint)
    {
        // TODO: Refactor this method!! Maybe use PopulatedEntity instead of constantly using reflection in this method?

        String updateSqlTemplate = aggregateBlueprint.getEntityUpdateSqlTemplate(entityBlueprint);
        String insertSqlTemplate = aggregateBlueprint.getEntityInsertSqlTemplate(entityBlueprint);

        try(PhotonPreparedStatement updateStatement = new PhotonPreparedStatement(connection, updateSqlTemplate);
            PhotonPreparedStatement insertStatement = new PhotonPreparedStatement(connection, insertSqlTemplate))
        {
            for(Object entityInstance : entityInstances)
            {
                updateStatement.resetParameterCounter();
                insertStatement.resetParameterCounter();
                boolean canPerformUpdate = true;

                ColumnBlueprint primaryKeyColumn = entityBlueprint.getPrimaryKeyColumn();
                FieldBlueprint primaryKeyField = primaryKeyColumn.getMappedFieldBlueprint();
                Object primaryKeyValue = null;

                if(primaryKeyField != null)
                {
                    Field field = entityBlueprint.getEntityClass().getDeclaredField(primaryKeyField.getFieldName());
                    field.setAccessible(true);
                    primaryKeyValue = field.get(entityInstance);
                }
                else
                {
                    canPerformUpdate = false;
                }

                if(canPerformUpdate)
                {
                    for (ColumnBlueprint columnBlueprint : entityBlueprint.getColumns())
                    {
                        Object fieldValue;
                        FieldBlueprint fieldBlueprint = columnBlueprint.getMappedFieldBlueprint();

                        if (fieldBlueprint != null)
                        {
                            Field field = entityBlueprint.getEntityClass().getDeclaredField(fieldBlueprint.getFieldName());
                            field.setAccessible(true);
                            fieldValue = field.get(entityInstance);
                        }
                        else if (columnBlueprint.isForeignKeyToParentColumn())
                        {
                            Field field = parentEntityBlueprint.getEntityClass().getDeclaredField(parentEntityBlueprint.getPrimaryKeyColumn().getMappedFieldBlueprint().getFieldName());
                            field.setAccessible(true);
                            fieldValue = field.get(parentEntityInstance);
                        }
                        else
                        {
                            canPerformUpdate = false;
                            break;
                        }

                        updateStatement.setNextParameter(fieldValue, columnBlueprint.getColumnDataType());
                    }
                }

                int rowsUpdated = canPerformUpdate ? updateStatement.executeUpdate() : 0;

                if(rowsUpdated == 0)
                {
                    for (ColumnBlueprint columnBlueprint : entityBlueprint.getColumns())
                    {
                        Object fieldValue;
                        FieldBlueprint fieldBlueprint = columnBlueprint.getMappedFieldBlueprint();

                        if(fieldBlueprint != null)
                        {
                            Field field = entityBlueprint.getEntityClass().getDeclaredField(fieldBlueprint.getFieldName());
                            field.setAccessible(true);
                            fieldValue = field.get(entityInstance);
                        }
                        else if(columnBlueprint.isForeignKeyToParentColumn())
                        {
                            Field field = parentEntityBlueprint.getEntityClass().getDeclaredField(parentEntityBlueprint.getPrimaryKeyColumn().getMappedFieldBlueprint().getFieldName());
                            field.setAccessible(true);
                            fieldValue = field.get(parentEntityInstance);
                        }
                        else if(columnBlueprint.isPrimaryKeyColumn())
                        {
                            // TODO: Need to have options to set what type of primary key this is (UUID, identity, something else?).
                            fieldValue = UUID.randomUUID();
                        }
                        else
                        {
                            throw new PhotonException(String.format("Cannot save entity '%s' because a value for column '%s' could not be determined.",
                                entityBlueprint.getEntityClassName(),
                                columnBlueprint.getColumnName()
                            ));
                        }

                        insertStatement.setNextParameter(fieldValue, columnBlueprint.getColumnDataType());
                    }

                    insertStatement.executeUpdate();
                }

                for(FieldBlueprint fieldBlueprint : entityBlueprint.getFieldsWithChildEntities())
                {
                    Field field = entityBlueprint.getEntityClass().getDeclaredField(fieldBlueprint.getFieldName());
                    field.setAccessible(true);
                    Object fieldValue = field.get(entityInstance);
                    Collection childEntityInstances;

                    if(fieldValue == null)
                    {
                        childEntityInstances = Collections.emptyList();
                    }
                    else if(Collection.class.isAssignableFrom(fieldValue.getClass()))
                    {
                        childEntityInstances = (Collection) fieldValue;
                    }
                    else
                    {
                        childEntityInstances = Collections.singletonList(fieldValue);
                    }

                    EntityBlueprint childEntityBlueprint = fieldBlueprint.getChildEntityBlueprint();
                    ColumnBlueprint childPrimaryKeyColumn = childEntityBlueprint.getPrimaryKeyColumn();
                    FieldBlueprint childPrimaryKeyField = childPrimaryKeyColumn.getMappedFieldBlueprint();

                    if(childPrimaryKeyField == null || childEntityInstances.size() == 0)
                    {
                        PhotonPreparedStatement deleteAllOrphans = new PhotonPreparedStatement(connection,
                            String.format("DELETE FROM `%s` WHERE `%s`.`%s` = ?",
                                childEntityBlueprint.getTableName(),
                                childEntityBlueprint.getTableName(),
                                childEntityBlueprint.getForeignKeyToParentColumnName()
                            )
                        );
                        deleteAllOrphans.setNextParameter(primaryKeyValue, primaryKeyColumn.getColumnDataType());
                        deleteAllOrphans.executeUpdate();
                    }
                    else
                    {
                        List<Object> childPrimaryKeyValues = new ArrayList<>();

                        for (Object childEntityInstance : childEntityInstances)
                        {
                            Field childPrimaryKeyReflectionField = childEntityBlueprint.getEntityClass().getDeclaredField(childPrimaryKeyField.getFieldName());
                            childPrimaryKeyReflectionField.setAccessible(true);
                            childPrimaryKeyValues.add(childPrimaryKeyReflectionField.get(childEntityInstance));
                        }

                        PhotonPreparedStatement deleteAllOrphans = new PhotonPreparedStatement(connection,
                            String.format("DELETE FROM `%s` WHERE `%s`.`%s` = ? AND `%s`.`%s` NOT IN (%s)",
                                childEntityBlueprint.getTableName(),
                                childEntityBlueprint.getTableName(),
                                childEntityBlueprint.getForeignKeyToParentColumnName(),
                                childEntityBlueprint.getTableName(),
                                childEntityBlueprint.getPrimaryKeyColumnName(),
                                getQuestionMarks(childPrimaryKeyValues.size())
                            )
                        );
                        deleteAllOrphans.setNextParameter(primaryKeyValue, primaryKeyColumn.getColumnDataType());
                        for(Object childPrimaryKeyValue : childPrimaryKeyValues)
                        {
                            deleteAllOrphans.setNextParameter(childPrimaryKeyValue, childPrimaryKeyColumn.getColumnDataType());
                        }
                        deleteAllOrphans.executeUpdate();
                    }

                    if(childEntityInstances.size() > 0)
                    {
                        saveEntityRecursive(childEntityInstances, childEntityBlueprint, entityInstance, entityBlueprint);
                    }
                }
            }
        }
        catch(Exception ex)
        {
            throw new PhotonException("Error executing SQL UPDATE query.", ex);
        }
    }

    private List<PopulatedEntity> getPopulatedAggregateRoots(List ids)
    {
        Map<EntityBlueprint, String> entitySelectSqlTemplates = aggregateBlueprint.getEntitySelectSqlTemplates();
        PopulatedEntityMap populatedEntityMap = new PopulatedEntityMap();

        for(Map.Entry<EntityBlueprint, String> entityAndSelectSql : entitySelectSqlTemplates.entrySet())
        {
            EntityBlueprint entityBlueprint = entityAndSelectSql.getKey();
            executeQueryAndCreateEntityOrphans(populatedEntityMap, entityBlueprint, entityAndSelectSql.getValue(), ids);
        }

        populatedEntityMap.mapAllEntityInstanceChildren();

        return populatedEntityMap.getPopulatedEntitiesForClass(aggregateBlueprint.getAggregateRootClass());
    }

    private void executeQueryAndCreateEntityOrphans(PopulatedEntityMap populatedEntityMap, EntityBlueprint entityBlueprint, String sqlTemplate, List ids)
    {
        ColumnBlueprint primaryKeyColumnBlueprint = entityBlueprint.getPrimaryKeyColumn();

        try (PhotonPreparedStatement statement = prepareStatementAndSetParameters(sqlTemplate, primaryKeyColumnBlueprint, ids, entityBlueprint.getEntityClassName()))
        {
            try (ResultSet resultSet = statement.executeQuery())
            {
                createEntityOrphans(populatedEntityMap, resultSet, entityBlueprint);
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
        }
    }

    private void createEntityOrphans(
        PopulatedEntityMap populatedEntityMap,
        ResultSet resultSet,
        EntityBlueprint entityBlueprint)
    {
        try
        {
            while (resultSet.next())
            {
                Map<String, Object> databaseValues = new HashMap<>();
                for (ColumnBlueprint columnBlueprint : entityBlueprint.getColumns())
                {
                    Object databaseValue = resultSet.getObject(columnBlueprint.getColumnName());
                    if(databaseValue != null)
                    {
                        databaseValues.put(columnBlueprint.getColumnName(), databaseValue);
                    }
                }
                populatedEntityMap.createPopulatedEntity(entityBlueprint, databaseValues);
            }
        }
        catch (Exception ex)
        {
            throw new PhotonException(String.format("Error parsing SELECT results for entity %s.", entityBlueprint.getEntityClassName()), ex);
        }
    }

    private PhotonPreparedStatement prepareStatementAndSetParameters(String sqlTemplate, ColumnBlueprint primaryKeyColumnBlueprint, List ids, String entityClassName)
    {
        try
        {
            String sqlWithQuestionMarks = String.format(sqlTemplate, getQuestionMarks(ids.size()));
            PhotonPreparedStatement statement = new PhotonPreparedStatement(connection, sqlWithQuestionMarks);

            for(Object id : ids)
            {
                statement.setNextParameter(id, primaryKeyColumnBlueprint.getColumnDataType());
            }

            return statement;
        }
        catch (Exception ex)
        {
            throw new PhotonException(String.format("Error preparing SELECT for entity '%s'.", entityClassName), ex);
        }
    }

    private String getQuestionMarks(int count)
    {
        StringBuilder questionMarks = new StringBuilder(count * 2 - 1);
        for(int i = 0; i < count; i++)
        {
            if(i < count - 1)
            {
                questionMarks.append("?,");
            }
            else
            {
                questionMarks.append("?");
            }
        }
        return questionMarks.toString();
    }
}
