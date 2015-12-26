package org.spacebison.multimic.io;

import android.support.annotation.NonNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Created by cmb on 19.12.15.
 */
public class OffsetInputStream extends FilterInputStream {
    private long mOffset;

    public OffsetInputStream(InputStream in, long offset) throws IOException {
        super(in);
        mOffset = offset;
        if (offset >= 0) {
            in.skip(offset);
            mOffset = 0;
        }
    }

    @Override
    public int read() throws IOException {
        if (mOffset < 0) {
            mOffset++;
            return 0;
        }
        return super.read();
    }

    @Override
    public int read(@NonNull byte[] buffer, int byteOffset, int byteCount) throws IOException {
        if (mOffset < 0) {
            long padByteCount = -mOffset;
            if (padByteCount < byteCount) {
                int newOffset = (int) (byteOffset + padByteCount);
                int newByteCount = (int) (byteCount - padByteCount);
                Arrays.fill(buffer, byteOffset, newOffset, (byte)0);
                mOffset = 0;
                return (int) (padByteCount + super.read(buffer, newOffset, newByteCount));
            } else {
                Arrays.fill(buffer, byteOffset, byteOffset + byteCount, (byte)0);
                mOffset += byteCount;
                return byteCount;
            }
        } else {
            return super.read(buffer, byteOffset, byteCount);
        }
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public long skip(long byteCount) throws IOException {
        if (mOffset < 0) {
            long padByteCount = -mOffset;
            if (padByteCount < byteCount) {
                int newByteCount = (int) (byteCount - padByteCount);
                mOffset = 0;
                return (int) (padByteCount + super.skip(newByteCount));
            } else {
                mOffset += byteCount;
                return byteCount;
            }
        } else {
            return super.skip(byteCount);
        }
    }
}
