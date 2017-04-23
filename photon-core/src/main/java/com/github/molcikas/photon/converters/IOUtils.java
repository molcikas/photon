package com.github.molcikas.photon.converters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class IOUtils
{
    private static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }

    public static String toString(Reader input) throws IOException {
        StringBuilder output = new StringBuilder();
        char[] buffer = new char[DEFAULT_BUFFER_SIZE];
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.append(buffer, 0, n);
        }
        return output.toString();
    }
}
