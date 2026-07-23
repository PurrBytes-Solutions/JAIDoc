package com.purrbyte.ai.util;

import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZIPHelper {

    /**
     * Finds a ZIP entry by filename, searching recursively (entries may be under a version-prefixed directory).
     * Handles both Unix ({@code /}) and Windows ({@code \}) path separators.
     */
    public static ZipEntry findZipEntry(ZipFile zipFile, String name) throws IOException {
        String normalized = name.replace('\\', '/');
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String entryName = entry.getName().replace('\\', '/');
            if (entryName.equals(normalized) || entryName.endsWith("/" + normalized)) {
                return entry;
            }
        }
        return null;
    }
}
