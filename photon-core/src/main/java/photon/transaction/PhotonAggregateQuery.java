package photon.transaction;

import photon.exceptions.PhotonException;
import photon.blueprints.AggregateBlueprint;
import photon.blueprints.ColumnBlueprint;
import photon.blueprints.EntityBlueprint;
import photon.blueprints.EntityFieldBlueprint;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
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
            ColumnBlueprint primaryKeyColumnBlueprint = entityBlueprint.getPrimaryKeyColumn();
            PreparedStatement statement = null;
            ResultSet resultSet = null;

            try
            {
                statement = prepareStatementAndSetParameters(
                    entityAndSelectSql.getValue(),
                    primaryKeyColumnBlueprint,
                    id,
                    entityBlueprint.getEntityClassName()
                );

                try
                {
                    resultSet = executeJdbcQuery(statement, entityBlueprint.getEntityClassName());
                    entityOrphans.put(entityBlueprint.getEntityClass(), createEntityOrphans(resultSet, entityBlueprint));
                }
                finally
                {
                    try { resultSet.close(); } catch(Exception ex) {}
                }
            }
            finally
            {
                try { statement.close(); } catch(Exception ex) {}
            }
        }

        for(Class entityClass : entityOrphans.keySet())
        {
            connectOrphans(entityClass, entityOrphans);
        }

        return (T) entityOrphans.get(aggregateBlueprint.getAggregateRootClass()).get(0).getEntityInstance();
    }

    private void connectOrphans(Class rootEntityClass, Map<Class, List<PopulatedEntity>> entityOrphans)
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
                            // TODO: Add better exception handling.
                            throw new RuntimeException(ex);
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

//    private T buildAggregateInstance(Map<EntityBlueprint, List<PopulatedEntityBlueprint>> populatedEntityBlueprints)
//    {
//        Map<Object, Object> builtEntities = populatedEntityBlueprints
//            .values()
//            .stream()
//            .map(peb -> peb.constructInstance())
//
//        EntityBlueprint aggregateRootEntityBlueprint = aggregateBlueprint.getAggregateRootEntityBlueprint();
//        PopulatedEntityBlueprint populatedEntityBlueprint = populatedEntityBlueprints.get(aggregateRootEntityBlueprint).get(0);
//
//        T aggregateRootInstance;
//
//        try
//        {
//            Constructor<T> constructor = aggregateRootEntityBlueprint.getEntityClass().getConstructor();
//            constructor.setAccessible(true);
//            aggregateRootInstance = constructor.newInstance();
//        }
//        catch(Exception ex)
//        {
//            throw new PhotonException(String.format("Error constructing aggregate root entity of type '%s'.", aggregateRootEntityBlueprint.getEntityClassName()), ex);
//        }

//        for(EntityFieldBlueprint entityFieldBlueprint : aggregateRootEntityBlueprint.getFields().values())
//        {
//            Field field;
//
//            try
//            {
//                field = aggregateRootEntityBlueprint.getEntityClass().getDeclaredField(entityFieldBlueprint.fieldName);
//                field.setAccessible(true);
//            }
//            catch(Exception ex)
//            {
//                throw new PhotonException(String.format("Failed to map field '%s' on entity '%s'.", entityFieldBlueprint.fieldName, aggregateRootEntityBlueprint.getEntityClassName()), ex);
//            }
//
//            if(entityFieldBlueprint.entityBlueprint == null)
//            {
//                try
//                {
//                    Object value = populatedEntityBlueprint.getValue(entityFieldBlueprint.fieldName);
//                    Object convertedValue = Convert.getConverter(field.getType()).convert(value);
//                    field.set(aggregateRootInstance, convertedValue);
//                }
//                catch(Exception ex)
//                {
//                    throw new PhotonException(String.format("Failed to set primitive value for field '%s' on entity '%s'.", entityFieldBlueprint.fieldName, aggregateRootEntityBlueprint.getEntityClassName()), ex);
//                }
//            }
//            else if(Collection.class.isAssignableFrom(entityFieldBlueprint.fieldClass))
//            {
//                try
//                {
//                    Object primaryKeyValue = populatedEntityBlueprint.getPrimaryKeyValue();
//                    List<PopulatedEntityBlueprint> populatedSubEntityBlueprints = populatedEntityBlueprints
//                        .get(entityFieldBlueprint.entityBlueprint)
//                        .stream()
//                        .filter(peb -> keysAreEqual(primaryKeyValue, peb.getForeignKeyValueToParent()))
//                        .collect(Collectors.toList());
//                    Object subEntitiesList = buildEntityCollection(entityFieldBlueprint.fieldClass, populatedSubEntityBlueprints);
//                    //field.set(aggregateRootInstance, subEntitiesList);
//                }
//                catch(Exception ex)
//                {
//                    throw new PhotonException(String.format("Failed to set collection value for field '%s' on entity '%s'.", entityFieldBlueprint.fieldName, aggregateRootEntityBlueprint.getEntityClassName()), ex);
//                }
//            }
//            else
//            {
//                // TODO: Build single entity.
//            }
//        }
//
//        return aggregateRootInstance;
//    }

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

            if (primaryKeyColumnBlueprint.columnDataType != null)
            {
                if (primaryKeyColumnBlueprint.columnDataType == Types.BINARY && id.getClass().equals(UUID.class))
                {
                    statement.setObject(1, uuidToBytes((UUID) id), primaryKeyColumnBlueprint.columnDataType);
                }
                else
                {
                    statement.setObject(1, id, primaryKeyColumnBlueprint.columnDataType);
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
                    Object databaseValue = resultSet.getObject(columnBlueprint.columnName);
                    if(databaseValue != null)
                    {
                        databaseValues.put(columnBlueprint.columnName, databaseValue);
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

    private byte[] uuidToBytes(UUID uuid)
    {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
