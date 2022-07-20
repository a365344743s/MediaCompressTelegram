package org.thoughtcrime.securesms.util;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class FileUtils {
    private static final int sBufferSize = 8192;

    static {
        System.loadLibrary("native-utils");
    }

    public static native int createMemoryFileDescriptor(String name);

    public static boolean writeFileFromIS(String dstPath, InputStream is) {
        try (FileOutputStream os = new FileOutputStream(dstPath); BufferedOutputStream bos = new BufferedOutputStream(os)) {
            byte[] data = new byte[sBufferSize];
            for (int len; (len = is.read(data)) != -1; ) {
                os.write(data, 0, len);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean copyFile(String srcPath, String dstPath) {
        try (FileInputStream is = new FileInputStream(srcPath);) {
            return writeFileFromIS(dstPath, is);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
