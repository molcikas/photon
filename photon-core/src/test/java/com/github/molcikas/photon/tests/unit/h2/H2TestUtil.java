package com.github.molcikas.photon.tests.unit.h2;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class H2TestUtil
{
    public static final String h2Url = "jdbc:h2:mem:test;MODE=MySql;DB_CLOSE_DELAY=-1";
    public static final String h2User = "sa";
    public static final String h2Password = "";

    public static File getResourceFile(String name)
    {
        String filePath = H2TestUtil.class.getResource("/" + name).getFile();
        return new File(filePath);
    }

    public static String readResourceFile(String name)
    {
        File resourceFile = getResourceFile(name);
        try
        {
            return FileUtils.readFileToString(resourceFile, "UTF-8");
        }
        catch(IOException ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
