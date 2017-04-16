package org.photon.blueprints;

public enum SortDirection
{
    Ascending("ASC"),
    Descending("DESC");

    public final String sqlSortDirection;

    SortDirection(String sqlSortDirection)
    {
        this.sqlSortDirection = sqlSortDirection;
    }
}
