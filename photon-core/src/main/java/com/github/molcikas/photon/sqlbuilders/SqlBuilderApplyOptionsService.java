package com.github.molcikas.photon.sqlbuilders;

import com.github.molcikas.photon.options.PhotonOptions;

public class SqlBuilderApplyOptionsService
{
    public static String applyPhotonOptionsToSql(String sql, PhotonOptions photonOptions)
    {
        return sql
            .replaceAll("\\[", photonOptions.getDelimitIdentifierStart())
            .replaceAll("\\]", photonOptions.getGetDelimitIdentifierEnd());
    }
}
