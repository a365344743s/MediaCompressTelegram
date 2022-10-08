package org.thoughtcrime.securesms.util;

public final class FileUtils {

    static {
        System.loadLibrary("native-utils");
    }

    public static native int createMemoryFileDescriptor(String name);
}
