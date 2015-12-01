package org.spacebison.multimic;

import android.media.AudioRecord;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by cmb on 03.11.15.
 */
public class AudioRecordInputStream extends InputStream {
    private AudioRecord mAudioRecord;

    public AudioRecordInputStream(AudioRecord audioRecord) {
        mAudioRecord = audioRecord;
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        mAudioRecord.read(b, 0, 1);
        return b[0];
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        return mAudioRecord.read(buffer, byteOffset, byteCount);
    }

    private AudioRecordInputStream() {
        super();
    }
}
