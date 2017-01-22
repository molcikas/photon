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
        Map<EntityBlueprint, String> entitySelectSql = aggregateBlueprint.getEntitySelectSql();
        Map<Class, List<PopulatedEntity>> entityOrphans = new HashMap<>();

        for(Map.Entry<EntityBlueprint, String> entityAndSelectSql : entitySelectSql.entrySet())
        {
            EntityBlueprint entityBlueprint = entityAndSelectSql.getKey();
            entityOrphans.put(
                entityBlueprint.getEntityClass(),
                queryAndCreateEntityOrphans(entityBlueprint, entityAndSelectSql.getValue(), id));
        }

        for(Class entityClass : entityOrphans.keySet())
        {
            connectEntityOrphans(entityClass, entityOrphans);
        }

        List<PopulatedEntity> populatedAggregateRoots = entityOrphans.get(aggregateBlueprint.getAggregateRootClass());

        if(populatedAggregateRoots.size() == 0)
        {
            return null;
        }

        return (T) populatedAggregateRoots.get(0).getEntityInstance();
    }

    private List<PopulatedEntity> queryAndCreateEntityOrphans(EntityBlueprint entityBlueprint, String sql, Object id)
    {
        ColumnBlueprint primaryKeyColumnBlueprint = entityBlueprint.getPrimaryKeyColumn();

        try (PreparedStatement statement = prepareStatementAndSetParameters(sql, primaryKeyColumnBlueprint, id, entityBlueprint.getEntityClassName()))
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
        for(PopulatedEntity entityOrphan : entityOrphans.get(rootEntityClass))
        {
            Object primaryKey = entityOrphan.getPrimaryKeyValue();
            Map<EntityFieldBlueprint, Integer> childIndexes = new HashMap<>();

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

    private PreparedStatement prepareStatementAndSetParameters(String sql, ColumnBlueprint primaryKeyColumnBlueprint, Object id, String entityClassName)
    {
        try
        {
            PreparedStatement statement = connection.prepareStatement(sql);

            if (primaryKeyColumnBlueprint.getColumnDataType() != null)
            {
                if (primaryKeyColumnBlueprint.getColumnDataType() == Types.BINARY && id.getClass().equals(UUID.class))
                {
                    statement.setObject(1, IOUtils.uuidToBytes((UUID) id), primaryKeyColumnBlueprint.getColumnDataType());
                }
                else
                {
                    statement.setObject(1, id, primaryKeyColumnBlueprint.getColumnDataType());
                }
            }
            else
            {
                statement.setObject(1, id);
            }

            return statement;
        }
        catch (Exception ex)
        {
            throw new PhotonException(String.format("Error preparing SELECT for entityBlueprint '%s'.", entityClassName), ex);
        }
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
