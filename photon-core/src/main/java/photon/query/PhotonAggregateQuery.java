package photon.query;

import photon.IOUtils;
import photon.exceptions.PhotonException;
import photon.blueprints.AggregateBlueprint;
import photon.blueprints.ColumnBlueprint;
import photon.blueprints.EntityBlueprint;
import photon.blueprints.EntityFieldBlueprint;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

public class PhotonAggregateQuery<T>
{
    private final AggregateBlueprint aggregateBlueprint;
    private final Connection connection;

    public PhotonAggregateQuery(AggregateBlueprint aggregateBlueprint, Connection connection)
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

    private List<PopulatedEntity> getPopulatedAggregateRoots(List ids)
    {
        Map<EntityBlueprint, String> entitySelectSqlTemplates = aggregateBlueprint.getEntitySelectSqlTemplates();
        Map<Class, List<PopulatedEntity>> entityOrphans = new HashMap<>();

        for(Map.Entry<EntityBlueprint, String> entityAndSelectSql : entitySelectSqlTemplates.entrySet())
        {
            EntityBlueprint entityBlueprint = entityAndSelectSql.getKey();
            entityOrphans.put(
                entityBlueprint.getEntityClass(),
                queryAndCreateEntityOrphans(entityBlueprint, entityAndSelectSql.getValue(), ids));
        }

        for(Class entityClass : entityOrphans.keySet())
        {
            connectEntityOrphans(entityClass, entityOrphans);
        }

        return entityOrphans.get(aggregateBlueprint.getAggregateRootClass());
    }

    private List<PopulatedEntity> queryAndCreateEntityOrphans(EntityBlueprint entityBlueprint, String sqlTemplate, List ids)
    {
        ColumnBlueprint primaryKeyColumnBlueprint = entityBlueprint.getPrimaryKeyColumn();

        try (PreparedStatement statement = prepareStatementAndSetParameters(sqlTemplate, primaryKeyColumnBlueprint, ids, entityBlueprint.getEntityClassName()))
        {
            try (ResultSet resultSet = executeJdbcQuery(statement, entityBlueprint.getEntityClassName()))
            {
                return createEntityOrphans(resultSet, entityBlueprint);
            }
        }
        catch(Exception ex)
        {
            throw new PhotonException("Error executing query.", ex);
        }
    }

    private List<PopulatedEntity> createEntityOrphans(
        ResultSet resultSet,
        EntityBlueprint entityBlueprint)
    {
        // 50 is the typical max length for an aggregate sub entity list.
        List<PopulatedEntity> populatedEntities = new ArrayList<>(50);

        try
        {
            while (resultSet.next())
            {
                Map<String, Object> databaseValues = new HashMap<>();
                for (ColumnBlueprint columnBlueprint : entityBlueprint.getColumns().values())
                {
                    Object databaseValue = resultSet.getObject(columnBlueprint.getColumnName());
                    if(databaseValue != null)
                    {
                        databaseValues.put(columnBlueprint.getColumnName(), databaseValue);
                    }
                }
                populatedEntities.add(new PopulatedEntity(entityBlueprint, databaseValues));
            }
        }
        catch (Exception ex)
        {
            throw new PhotonException(String.format("Error parsing SELECT results for entity %s.", entityBlueprint.getEntityClassName()), ex);
        }

        return populatedEntities;
    }

    private void connectEntityOrphans(Class rootEntityClass, Map<Class, List<PopulatedEntity>> entityOrphans)
    {
        Map<EntityFieldBlueprint, Integer> childIndexes = new HashMap<>();

        for(PopulatedEntity entityOrphan : entityOrphans.get(rootEntityClass))
        {
            Object primaryKey = entityOrphan.getPrimaryKeyValue();

            for(EntityFieldBlueprint entityFieldBlueprint : entityOrphan.getEntityBlueprint().getFields().values())
            {
                if(entityFieldBlueprint.entityBlueprint != null)
                {
                    if(Collection.class.isAssignableFrom(entityFieldBlueprint.fieldClass))
                    {
                        Collection collection = createCompatibleCollection(entityFieldBlueprint.fieldClass);

                        List<PopulatedEntity> allChildOrphans = entityOrphans.get(entityFieldBlueprint.entityBlueprint.getEntityClass());
                        Integer childIndex = childIndexes.get(entityFieldBlueprint);
                        if (childIndex == null)
                        {
                            childIndex = 0;
                        }
                        while (childIndex < allChildOrphans.size() && keysAreEqual(primaryKey, allChildOrphans.get(childIndex).getForeignKeyToParentValue()))
                        {
                            collection.add(allChildOrphans.get(childIndex).getEntityInstance());
                            childIndex++;
                        }
                        childIndexes.put(entityFieldBlueprint, childIndex);

                        try
                        {
                            Field field = entityOrphan.getEntityBlueprint().getEntityClass().getDeclaredField(entityFieldBlueprint.fieldName);
                            field.setAccessible(true);
                            field.set(entityOrphan.getEntityInstance(), collection);
                        }
                        catch(Exception ex)
                        {
                            throw new PhotonException(String.format("Error setting field '%s' on entity '%s'.", entityFieldBlueprint.fieldName, entityOrphan.getEntityBlueprint().getEntityClassName()), ex);
                        }
                    }

                    // TODO: Handle entity as field, i.e. one-to-one relationship.
                }
            }
        }
    }

    private Collection createCompatibleCollection(Class<? extends Collection> collectionClass)
    {
        if(List.class.isAssignableFrom(collectionClass))
        {
            return new ArrayList();
        }
        else if(Set.class.isAssignableFrom(collectionClass))
        {
            return new HashSet();
        }

        throw new PhotonException(String.format("Unable to create instance of collection type '%s'.", collectionClass.getName()));
    }


    private boolean keysAreEqual(Object primaryKey, Object foreignKey)
    {
        if(primaryKey.equals(foreignKey))
        {
            return true;
        }

        if (primaryKey instanceof byte[] && foreignKey instanceof byte[])
        {
            return Arrays.equals((byte[]) primaryKey, (byte[]) foreignKey);
        }

        return false;
    }

    private PreparedStatement prepareStatementAndSetParameters(String sqlTemplate, ColumnBlueprint primaryKeyColumnBlueprint, List ids, String entityClassName)
    {
        try
        {
            String sqlWithQuestionMarks = String.format(sqlTemplate, getQuestionMarks(ids.size()));
            PreparedStatement statement = connection.prepareStatement(sqlWithQuestionMarks);

            int i = 1;
            for(Object id : ids)
            {
                if (primaryKeyColumnBlueprint.getColumnDataType() != null)
                {
                    if (primaryKeyColumnBlueprint.getColumnDataType() == Types.BINARY && id.getClass().equals(UUID.class))
                    {
                        statement.setObject(i, IOUtils.uuidToBytes((UUID) id), primaryKeyColumnBlueprint.getColumnDataType());
                    }
                    else
                    {
                        statement.setObject(i, id, primaryKeyColumnBlueprint.getColumnDataType());
                    }
                }
                else
                {
                    statement.setObject(i, id);
                }

                i++;
            }

            return statement;
        }
        catch (Exception ex)
        {
            throw new PhotonException(String.format("Error preparing SELECT for entityBlueprint '%s'.", entityClassName), ex);
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

    private ResultSet executeJdbcQuery(PreparedStatement statement, String entityClassName)
    {
        try
        {
            return statement.executeQuery();
        }
        catch (Exception ex)
        {
            throw new PhotonException(String.format("Error executing SELECT for entityBlueprint '%s'.", entityClassName), ex);
        }
    }
}
