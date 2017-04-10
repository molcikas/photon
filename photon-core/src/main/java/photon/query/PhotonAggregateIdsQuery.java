package photon.query;

import org.apache.commons.lang3.StringUtils;
import photon.blueprints.AggregateBlueprint;
import photon.blueprints.AggregateEntityBlueprint;
import photon.exceptions.PhotonException;

import java.sql.Connection;
import java.util.List;

public class PhotonAggregateIdsQuery<T>
{
    private final AggregateBlueprint<T> aggregateBlueprint;
    private final PhotonAggregateQuery<T> photonAggregateQuery;
    private final PhotonQuery photonQuery;

    public PhotonAggregateIdsQuery(
        AggregateBlueprint<T> aggregateBlueprint,
        String selectIdsSql,
        boolean isWhereClauseOnly,
        Connection connection,
        PhotonAggregateQuery<T> photonAggregateQuery)
    {
        if(StringUtils.isBlank(selectIdsSql))
        {
            throw new PhotonException("Photon aggregate SELECT by ids SQL cannot be blank.");
        }

        this.aggregateBlueprint = aggregateBlueprint;
        this.photonAggregateQuery = photonAggregateQuery;

        AggregateEntityBlueprint aggregateEntityBlueprint = aggregateBlueprint.getAggregateRootEntityBlueprint();

        if(isWhereClauseOnly)
        {
            selectIdsSql = String.format(
                "SELECT `%s` FROM `%s` WHERE %s",
                aggregateEntityBlueprint.getPrimaryKeyColumnName(),
                aggregateEntityBlueprint.getTableName(),
                selectIdsSql
            );
        }

        this.photonQuery = new PhotonQuery(selectIdsSql, false, connection, null);
    }

    public PhotonAggregateIdsQuery<T> addParameter(String parameter, Object value)
    {
        photonQuery.addParameter(parameter, value);
        return this;
    }

    public T fetch()
    {
        Class primaryKeyClass = aggregateBlueprint.getAggregateRootEntityBlueprint().getPrimaryKeyColumn().getMappedFieldBlueprint().getFieldClass();
        Object id = photonQuery.fetchScalar(primaryKeyClass);

        if(id == null)
        {
            return null;
        }

        return photonAggregateQuery.fetchById(id);
    }

    public List<T> fetchList()
    {
        Class primaryKeyClass = aggregateBlueprint.getAggregateRootEntityBlueprint().getPrimaryKeyColumn().getMappedFieldBlueprint().getFieldClass();
        List ids = photonQuery.fetchScalarList(primaryKeyClass);

        return photonAggregateQuery.fetchByIds(ids);
    }
}
