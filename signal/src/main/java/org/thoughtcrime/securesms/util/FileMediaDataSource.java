package org.thoughtcrime.securesms.util;

import android.media.MediaDataSource;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

@RequiresApi(23)
public class FileMediaDataSource extends MediaDataSource {
    private RandomAccessFile mFile;
    private long mFileSize;

    public FileMediaDataSource(String path) throws IOException {
        this(new File(path));
    }

    public FileMediaDataSource(File file) throws IOException {
        mFile = new RandomAccessFile(file, "r");
        mFileSize = mFile.length();
    }

    @Override
    public int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
        if (mFile.getFilePointer() != position) {
            mFile.seek(position);
        }
        if (size == 0) {
            return 0;
        }
        return mFile.read(buffer, offset, size);
    }

    @Override
    public long getSize() {
        return mFileSize;
    }

    @Override
    public void close() throws IOException {
        mFileSize = 0;
        mFile.close();
        mFile = null;
    }
}