package com.github.molcikas.photon.tests.unit.blueprints;

import com.github.molcikas.photon.blueprints.table.TableBlueprintAndKey;
import com.github.molcikas.photon.blueprints.table.TableValue;
import com.github.molcikas.photon.converters.Convert;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class TableValueTest
{
    @Test
    public void equals_byteArray_areEqual()
    {
        UUID uuid = UUID.fromString("3e038307-a9b6-11e6-ab83-0a0027000010");
        byte[] bytes = (byte[]) Convert.getConverter(byte[].class).convert(uuid);

        TableValue value1 = new TableValue(bytes);
        TableValue value2 = new TableValue(bytes);
        assertTrue(value1.equals(value2));

        TableBlueprintAndKey tableBlueprintAndKey1 = new TableBlueprintAndKey(null, value1);
        TableBlueprintAndKey tableBlueprintAndKey2 = new TableBlueprintAndKey(null, value2);
        assertTrue(tableBlueprintAndKey1.equals(tableBlueprintAndKey2));
    }
}
