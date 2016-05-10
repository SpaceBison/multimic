package org.spacebison.multimic.player;

import android.media.AudioManager;
import android.media.AudioTrack;

import org.spacebison.common.CrashlyticsLog;
import org.spacebison.multimic.audio.WavHeaderException;
import org.spacebison.multimic.audio.WavUtils;
import org.spacebison.multimic.model.Config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.fabric.sdk.android.services.common.ExecutorUtils;

/**
 * Created by cmb on 09.05.16.
 */
public class MultiPlayer {
    private static final String TAG = "MultiPlayer";
    private final AudioTrack player = new AudioTrack(AudioManager.STREAM_MUSIC,
            Config.SAMPLE_RATE,
            2,
            Config.AUDIO_FORMAT,
            Config.BUFFER_SIZE,
            AudioTrack.MODE_STREAM);
    private final ArrayList<Track> mTracks = new ArrayList<>();
    private final Executor mExecutor = Executors.newSingleThreadExecutor(ExecutorUtils.getNamedThreadFactory("PlayerThread-"));

    private State mState = State.STOPPED;
    private long mLength;

    public void loadFile(File file) throws IOException, WavHeaderException {
        CrashlyticsLog.d(TAG, "Loading");
        final Track track = new Track(file, file.getName());
        mTracks.add(track); // TODO: 10.05.16 name
        updateLength();
    }

    public void play() {
        mState = State.PLAYING;
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                byte[] trackBuf = new byte[Config.BUFFER_SIZE * 2]; // stereo
                byte[] buf = new byte[Config.BUFFER_SIZE];
                byte[] sampleBuf = new byte[2];
                while (mState == State.PLAYING) {
                    synchronized (mTracks) {
                        for (Track t : mTracks) {
                            // TODO: 10.05.16 zero the buffer if there's no data
                            try {
                                final int bytesRead = t.stream.read(trackBuf);

                                for (int i = 0; i < bytesRead; i+=2) {
                                    sampleBuf[0] = trackBuf[i];
                                    sampleBuf[1] = trackBuf[i + 1];
                                    int sample = WavUtils.toEndianSwappedShort(sampleBuf);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    // TODO: 10.05.16 get PCM data, mix, push to AudioTrack
                }
            }
        });
    }

    public State getState() {
        return mState;
    }

    private void updateLength() {
        long length = 0;
        for (Track track : mTracks) {
            final long trackEnd = track.offset + track.length;
            if (trackEnd > length) {
                length = trackEnd;
            }
        }
        mLength = length;
    }

    public enum State {
        PLAYING,
        PAUSED,
        STOPPED;
    }

    private class Track {
        long startOffset = 0;
        long offset = 0;
        long length;
        float volume = 1f;
        float pan = 0f;
        InputStream stream;
        String name;
        File file;

        public Track(File file, String name) throws IOException, WavHeaderException {
            this.file = file;
            this.name = name;
            stream = new BufferedInputStream(new FileInputStream(file), Config.BUFFER_SIZE);
            WavUtils.readRiffHeader(stream);
        }
    }
}
