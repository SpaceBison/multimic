package org.spacebison.multimic.ui.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.spacebison.multimic.MultimicApplication;
import org.spacebison.multimic.R;
import org.spacebison.multimic.Util;
import org.spacebison.multimic.audio.WavHeaderException;
import org.spacebison.multimic.audio.WavUtils;
import org.spacebison.multimic.model.MediaReceiverServer;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

public class PlayerActivity extends AppCompatActivity {
    public static final String EXTRA_SESSION_PREFIX = "session_prefix";
    private static final String TAG = "cmb.PlayerActivity";
    private static final TreeSet<Track> sTracks = new TreeSet<>();
    private Tracker mTracker;
    private AudioPlayThread mThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        String sessionPrefix = getIntent().getStringExtra(EXTRA_SESSION_PREFIX);
        String dirPath = MediaReceiverServer.getRecordingDirPath();
        File dir = new File(dirPath);

        setTitle(sessionPrefix);

        if (savedInstanceState == null) {
            sTracks.clear();
        }

        for (String fileName : dir.list()) {
            if (fileName.startsWith(sessionPrefix) && fileName.endsWith(".wav")) {
                Log.d(TAG, "Adding " + fileName);
                sTracks.add(new Track(dir.getAbsolutePath(), fileName));
            }
        }

        ListView trackList = (ListView) findViewById(R.id.trackList);
        trackList.setAdapter(new TrackListAdapter());

        mTracker = MultimicApplication.getDefaultTracker();
    }

    protected void onResume() {
        super.onResume();
        mTracker.setScreenName("Player");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onBackPressed() {
        if (mThread != null) {
            mThread.interrupt();
        }
        super.onBackPressed();
    }

    public void clickPlayButton(View view) {
        Button b = (Button) view;
        if (mThread == null) {
            b.setText("Stop");
            mThread = new AudioPlayThread();
            mThread.start();
        } else {
            b.setText("Play");
            mThread.interrupt();
            mThread = null;
        }
    }

    private class Track implements Comparable {
        public String fileName;
        public File file;
        public float volume = 1;
        public float pan = 0;
        public long offset;

        public Track(String dir, String fileName) {
            this.fileName = fileName;
            file = new File(dir, fileName);
        }

        @Override
        public int compareTo(@NonNull Object another) {
            return fileName.compareTo(((Track)another).fileName);
        }
    }

    private class AudioPlayThread extends Thread {
        private int mBufferSize = AudioTrack.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        private AudioTrack mAudioTrack =
                new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        44100,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        2 * mBufferSize,
                        AudioTrack.MODE_STREAM);

        public AudioPlayThread() {
            setUncaughtExceptionHandler(
                    new ExceptionReporter(
                            mTracker,
                            getUncaughtExceptionHandler(),
                            getApplicationContext()));
        }

        @Override
        public void run() {
            Log.d(TAG, "Init play");

            HashMap<Track, DataInputStream> inputStreams = new HashMap<>();

            for (Track t : sTracks) {
                try {
                    DataInputStream dis = new DataInputStream((new FileInputStream(t.file)));
                    WavUtils.readRiffHeader(dis);
                    inputStreams.put(t, dis);
                } catch (FileNotFoundException e) {
                    Log.w(TAG, "Could not open file to play: " + e);
                } catch (WavHeaderException e) {
                    Log.d(TAG, "Invalid WAV header: " + e);
                } catch (IOException e) {
                    Log.d(TAG, "Error reading WAV header: " + e);
                }
            }

            final int samplesInBuffer = mBufferSize / 2;
            final byte[] buffer = new byte[2 * mBufferSize];
            final byte[] trackBuffer = new byte[mBufferSize];
            final float[] leftBuffer = new float[samplesInBuffer];
            final float[] rightBuffer = new float[samplesInBuffer];
            float pan;
            short sample = 0;
            short left = 0;
            short right = 0;
            float fSample;
            float fLeft = 0;
            float fRight = 0;
            int bytes;
            Iterator<Map.Entry<Track, DataInputStream>> it;

            Log.d(TAG, "Start play");

            mAudioTrack.play();

            while (!isInterrupted() && !inputStreams.isEmpty()) {
                Arrays.fill(buffer, (byte) 0);
                Arrays.fill(leftBuffer, 0);
                Arrays.fill(rightBuffer, 0);
                it = inputStreams.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Track, DataInputStream> e = it.next();
                    Track t = e.getKey();
                    DataInputStream inputStream = e.getValue();
                    try {
                        pan = (t.pan + 1f) / 2;;

                        bytes = inputStream.read(trackBuffer);
                        if (bytes <= 0) {
                            Log.d(TAG, "End of file: " + t.fileName);
                            try {
                                inputStream.close();
                            } catch (IOException ignored) {
                            }
                            it.remove();
                            continue;
                        }

                        for (int i = 0; i < bytes / 2; i++) {
                            fSample = trackBuffer[2*i+1] << 8;
                            fSample += trackBuffer[2*i] & 0xff;

                            fSample *= t.volume;

                            rightBuffer[i] += fSample * pan / inputStreams.size();
                            leftBuffer[i] += fSample * (1f - pan) / inputStreams.size();
                        }
                    } catch (IOException e1) {
                        Log.w(TAG, "Error submitting data to play: " + e1);
                        try {
                            inputStream.close();
                        } catch (IOException ignored) {
                        }
                        it.remove();
                    }
                }

                for(int i = 0; i < samplesInBuffer; i++) {
                    left = (short) Math.round(leftBuffer[i]);
                    right = (short) Math.round(rightBuffer[i]);

                    buffer[4*i+1] += (byte)(left >>> 8);
                    buffer[4*i+0] += (byte)(left);


                    buffer[4*i+3] += (byte)(right >>> 8);
                    buffer[4*i+2] += (byte)(right);
                }

                mAudioTrack.write(buffer, 0, buffer.length);
            }

            Log.d(TAG, "Stop play, releasing");

            mAudioTrack.stop();
            mAudioTrack.release();

            for (DataInputStream inputStream : inputStreams.values()) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((Button) findViewById(R.id.playButton)).setText("Play");
                }
            });
        }
    }

    private class TrackListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return sTracks.size();
        }

        @Override
        public Object getItem(int position) {
            return Util.getObjectAt(sTracks, position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_item_track_controls, parent, false);
            }

            final Track track = (Track) getItem(position);

            final TextView numberText = (TextView) convertView.findViewById(R.id.trackNumberText);
            final TextView volumeText = (TextView) convertView.findViewById(R.id.volumeText);
            final SeekBar volumeSeek = (SeekBar) convertView.findViewById(R.id.volumeSeek);
            final TextView panText = (TextView) convertView.findViewById(R.id.panText);
            final SeekBar panSeek = (SeekBar) convertView.findViewById(R.id.panSeek);

            numberText.setText(Integer.toString(position));

            setVolumeText(volumeText, track);
            volumeSeek.setProgress((int) (track.volume * 256));
            volumeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    track.volume =  progress / 256f;
                    setVolumeText(volumeText, track);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            setPanText(track, panText);
            panSeek.setProgress((int) ((track.pan + 1) * 128));
            panSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && Math.abs(progress - 128) < 15) {
                        seekBar.setProgress(128);
                        return;
                    }

                    track.pan = progress / 128f - 1f;
                    setPanText(track, panText);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            return convertView;
        }

        public void setPanText(Track track, TextView panText) {
            if (track.pan == 0) {
                panText.setText("Pan: C");
            } else if (track.pan < 0) {
                panText.setText("Pan: " + (int)(track.pan * -100) + "L");
            } else {
                panText.setText("Pan: " + (int)(track.pan * 100) + "R");
            }
        }

        public void setVolumeText(TextView volumeText, Track track) {
            volumeText.setText("Volume: " + (int) (track.volume * 100) + '%');
        }
    }
}
