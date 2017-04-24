package com.github.molcikas.photon.tests.integration.sqlserver.entities;

import java.time.ZonedDateTime;
import java.util.UUID;

public class MySqlServerTable
{
    private int id;
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

    private MySqlServerTable()
    {
    }

    public MySqlServerTable(int id, UUID uuidColumn, ZonedDateTime dateColumn, String varcharColumn)
    {
        this.id = id;
        this.uuidColumn = uuidColumn;
        this.dateColumn = dateColumn;
        this.varcharColumn = varcharColumn;
    }
}
