package com.github.molcikas.photon.blueprints;

public enum JoinType
{
    InnerJoin("JOIN"),
    LeftOuterJoin("LEFT JOIN");

    private final String joinSql;

    public String getJoinSql()
    {
        return joinSql;
    }

    JoinType(String joinSql)
    {
        this.joinSql = joinSql;
    }
}
