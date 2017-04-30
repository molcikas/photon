package com.github.molcikas.photon.tests.integration.sqlserver.entities;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

public class PhotonTestTable
{
    private Integer id;
    private UUID uuidColumn;
    private ZonedDateTime dateColumn;
    private String varcharColumn;

    public int getId()
    {
        return id;
    }

    public UUID getUuidColumn()
    {
        return uuidColumn;
    }

    public ZonedDateTime getDateColumn()
    {
        return dateColumn;
    }

    public String getVarcharColumn()
    {
        return varcharColumn;
    }

    private PhotonTestTable()
    {
    }

    public PhotonTestTable(Integer id, UUID uuidColumn, ZonedDateTime dateColumn, String varcharColumn)
    {
        this.id = id;
        this.uuidColumn = uuidColumn;
        this.dateColumn = dateColumn;
        this.varcharColumn = varcharColumn;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotonTestTable that = (PhotonTestTable) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(uuidColumn, that.uuidColumn) &&
            Objects.equals(dateColumn, that.dateColumn) &&
            Objects.equals(varcharColumn, that.varcharColumn);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, uuidColumn, dateColumn, varcharColumn);
    }
}
