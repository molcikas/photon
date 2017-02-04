package photon.blueprints;

import org.apache.commons.lang3.StringUtils;
import photon.Photon;
import photon.exceptions.PhotonException;

import java.util.HashMap;
import java.util.Map;

public class EntityBlueprintBuilder
{
    private final Photon photon;
    private final EntityBlueprintConstructorService entityBlueprintConstructorService;
    private final EntityBlueprintBuilder parentBuilder;
    private final Class entityClass;
    private String idFieldName;
    private boolean isPrimaryKeyAutoIncrement;
    private String foreignKeyToParent;
    private String orderByColumnName;
    private SortDirection orderByDirection;
    private final Map<String, Integer> customColumnDataTypes;
    private final Map<String, AggregateEntityBlueprint> childEntities;
    private final Map<String, String> customFieldToColumnMappings;

    public EntityBlueprintBuilder(Class entityClass, EntityBlueprintBuilder parentBuilder, EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        this(entityClass, parentBuilder, null, entityBlueprintConstructorService);
    }

    public EntityBlueprintBuilder(Class entityClass, Photon photon, EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        this(entityClass, null, photon, entityBlueprintConstructorService);
    }

    public EntityBlueprintBuilder(Class entityClass, EntityBlueprintBuilder parentBuilder, Photon photon,
                                  EntityBlueprintConstructorService entityBlueprintConstructorService)
    {
        this.entityClass = entityClass;
        this.photon = photon;
        this.entityBlueprintConstructorService = entityBlueprintConstructorService;
        this.parentBuilder = parentBuilder;
        this.isPrimaryKeyAutoIncrement = false;
        this.customColumnDataTypes = new HashMap<>();
        this.childEntities = new HashMap<>();
        this.customFieldToColumnMappings = new HashMap<>();
    }

    public EntityBlueprintBuilder withId(String idFieldName)
    {
        this.idFieldName = idFieldName;
        return this;
    }

    public EntityBlueprintBuilder withPrimaryKeyAutoIncrement()
    {
        this.isPrimaryKeyAutoIncrement = true;
        return this;
    }

    public EntityBlueprintBuilder withForeignKeyToParent(String foreignKeyToParent)
    {
        this.foreignKeyToParent = foreignKeyToParent;
        return this;
    }

    public EntityBlueprintBuilder withColumnDataType(String columnName, Integer columnDataType)
    {
        customColumnDataTypes.put(columnName, columnDataType);
        return this;
    }

    public EntityBlueprintBuilder withFieldToColmnnMapping(String fieldName, String columnName)
    {
        customFieldToColumnMappings.put(fieldName, columnName);
        return this;
    }

    public EntityBlueprintBuilder withOrderBy(String orderByColumnName)
    {
        return withOrderBy(orderByColumnName, SortDirection.Ascending);
    }

    public EntityBlueprintBuilder withOrderBy(String orderByColumnName, SortDirection orderByDirection)
    {
        this.orderByColumnName = orderByColumnName;
        this.orderByDirection = orderByDirection;
        return this;
    }

    public EntityBlueprintBuilder withChild(Class childClass)
    {
        return new EntityBlueprintBuilder(childClass, this, entityBlueprintConstructorService);
    }

    public EntityBlueprintBuilder addAsChild(String fieldName)
    {
        if(parentBuilder == null)
        {
            throw new PhotonException(String.format("Cannot add entity to field '%s' as a child because it does not have a parent entity.", fieldName));
        }
        if(StringUtils.isBlank(foreignKeyToParent))
        {
            throw new PhotonException(String.format("Cannot add entity to parent field '%s' because the entity does not have a foreign key to parent set.", fieldName));
        }
        parentBuilder.addChild(fieldName, buildEntity());
        return parentBuilder;
    }

    public void register()
    {
        if(photon == null)
        {
            throw new PhotonException("Cannot register entityBlueprint because it is not the aggregate root.");
        }
        photon.registerAggregate(buildEntity());
    }

    private AggregateEntityBlueprint buildEntity()
    {
        return new AggregateEntityBlueprint(
            entityClass,
            idFieldName,
            isPrimaryKeyAutoIncrement,
            foreignKeyToParent,
            orderByColumnName,
            orderByDirection,
            customColumnDataTypes,
            customFieldToColumnMappings,
            childEntities,
            entityBlueprintConstructorService
        );
    }

    private void addChild(String fieldName, AggregateEntityBlueprint childEntityBlueprint)
    {
        if(childEntities.containsKey(fieldName))
        {
            throw new PhotonException(String.format("EntityBlueprint already contains a child for field %s", fieldName));
        }
        childEntities.put(fieldName, childEntityBlueprint);
    }
}
