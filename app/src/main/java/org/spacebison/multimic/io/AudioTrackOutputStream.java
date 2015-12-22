package org.spacebison.multimic.io;

import android.media.AudioTrack;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by cmb on 19.12.15.
 */
public class AudioTrackOutputStream extends OutputStream {
    private AudioTrack mAudioTrack;

    public AudioTrackOutputStream(AudioTrack audioTrack) {
        mAudioTrack = audioTrack;
    }

    @Override
    public void write(int oneByte) throws IOException {
        byte[] buf = new byte[1];
        buf[0] = (byte) oneByte;
        mAudioTrack.write(buf, 0, 1);
    }

    @Override
    public void write(@NonNull byte[] buffer, int offset, int count) throws IOException {
        mAudioTrack.write(buffer, offset, count);
    }

    @Override
    public void close() throws IOException {
        super.close();
        mAudioTrack.stop();
    }
}
