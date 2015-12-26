package org.spacebison.multimic.ui.player;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.spacebison.multimic.MultimicApplication;
import org.spacebison.multimic.R;
import org.spacebison.multimic.audio.WavHeaderException;
import org.spacebison.multimic.audio.WavUtils;
import org.spacebison.multimic.io.AudioTrackOutputStream;
import org.spacebison.multimic.io.OffsetInputStream;
import org.spacebison.multimic.model.MediaReceiverServer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class PlayerActivity extends AppCompatActivity {
    public static final String EXTRA_SESSION_PREFIX = "session_prefix";
    private static final String TAG = "cmb.PlayerActivity";
    static final TreeSet<Track> sTracks = new TreeSet<>();
    private String mSessionPrefix;
    private TrackListAdapter mListAdapter;
    ListView mListView;
    private Tracker mTracker;
    private AudioPlayThread mThread;
    private Track mSelectedTrack;
    private ActionMode mActionMode;
    private ActionModeCallback mActionModeCallback = new ActionModeCallback();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mSessionPrefix = getIntent().getStringExtra(EXTRA_SESSION_PREFIX);
        String dirPath = MediaReceiverServer.getRecordingDirPath();
        File dir = new File(dirPath);

        setTitle(mSessionPrefix);

        if (savedInstanceState == null) {
            sTracks.clear();
        }

        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(mSessionPrefix) && filename.endsWith(".wav");
            }
        });

        for (File file : files) {
            if (file.isFile()) {
                Log.d(TAG, "Adding " + file);
                sTracks.add(new Track(file));
            }
        }

        mListView = (ListView) findViewById(R.id.trackList);
        mListAdapter = new TrackListAdapter(this);
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode != null) {
                    return false;
                }

                mSelectedTrack = (Track) mListAdapter.getItem(position);
                mActionMode = startActionMode(mActionModeCallback);
                view.setSelected(true);
                return true;
            }
        });

        mTracker = MultimicApplication.getDefaultTracker();
    }

    protected void onResume() {
        super.onResume();
        mTracker.setScreenName("Player");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.export:
                final EditText fileNameEdit = new EditText(this);
                fileNameEdit.setText(mSessionPrefix + "_mix.wav");
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Export");
                builder.setView(fileNameEdit);
                builder.setCancelable(true);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new AsyncTask<Void, Long, Void>() {
                            private ProgressDialog mProgressDialog;
                            private String mFilename;

                            @Override
                            protected void onPreExecute() {
                                mFilename = fileNameEdit.getText().toString();
                                mProgressDialog = new ProgressDialog(PlayerActivity.this);
                                mProgressDialog.setTitle("Exporting");
                                mProgressDialog.setCancelable(false);
                                mProgressDialog.setMax(1);
                                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                mProgressDialog.setIndeterminate(true);
                                mProgressDialog.setProgressNumberFormat(null);
                                mProgressDialog.setProgressPercentFormat(null);
                                mProgressDialog.show();
                            }

                            @Override
                            protected Void doInBackground(Void... params) {
                                File tmpFile = null;
                                FileOutputStream fos = null;
                                FileInputStream fis = null;
                                try {
                                    File exportDir = new File(MediaReceiverServer.getExportDirPath());

                                    if (!exportDir.exists()) {
                                        exportDir.mkdirs();
                                    }

                                    tmpFile = new File(exportDir, mFilename + ".tmp");
                                    tmpFile.createNewFile();
                                    fos = new FileOutputStream(tmpFile);
                                    writeMixedTracks(getAudioFileInputs(sTracks), fos, 1024);
                                    fos.close();

                                    long fileBytes = tmpFile.length();
                                    int numSamples = (int) (fileBytes / 2 / 16 * 8);

                                    File exportFile = new File(exportDir, mFilename);
                                    exportFile.createNewFile();
                                    fos = new FileOutputStream(exportFile);
                                    WavUtils.writeRiffHeader(fos, (short)2, 44100, (short)16, numSamples);

                                    fis = new FileInputStream(tmpFile);
                                    byte[] buffer = new byte[1024];
                                    int bytesRead;
                                    long totalBytesRead = 0;

                                    mProgressDialog.setIndeterminate(false);
                                    mProgressDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());

                                    while((bytesRead = fis.read(buffer)) > 0 ) {
                                        fos.write(buffer, 0, bytesRead);
                                        totalBytesRead += bytesRead;
                                        publishProgress(fileBytes, totalBytesRead);
                                    }
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                    cancel(true);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } finally {
                                    if (fis != null) {
                                        try {
                                            fis.close();
                                        } catch (IOException ignored) {
                                        }
                                    }

                                    if (fos != null) {
                                        try {
                                            fos.close();
                                        } catch (IOException ignored) {
                                        }
                                    }

                                    if (tmpFile != null) {
                                        tmpFile.delete();
                                    }
                                }

                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                mProgressDialog.dismiss();
                            }

                            @Override
                            protected void onProgressUpdate(Long... values) {
                                mProgressDialog.setIndeterminate(false);
                                mProgressDialog.setMax((int) (values[0] / 1024));
                                mProgressDialog.setProgress((int) (values[1] / 1024));
                            }
                        }.execute();
                    }
                });
                builder.show();
                return true;

            case R.id.reset:
                for (Track t : sTracks) {
                    t.offset = 0;
                    t.pan = 0;
                    t.volume = 1;
                }
                mListAdapter.notifyDataSetChanged();
                return true;

            default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit player?");
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PlayerActivity.super.onBackPressed();
            }
        });
        builder.setCancelable(true);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
    }

    public void clickPlayButton(View view) {
        Button b = (Button) view;
        if (mThread == null) {
            b.setText("Stop");
            mThread = new AudioPlayThread();
            mThread.start();
            mListAdapter.setOffsetControlVisible(false);
        } else {
            b.setText("Play");
            mThread.interrupt();
            mThread = null;
            mListAdapter.setOffsetControlVisible(true);
        }
    }

    class Track implements Comparable {
        public String fileName;
        public File file;
        public float volume = 1;
        public float pan = 0;
        public long offset = 0;

        public Track(File file) {
            this.file = file;
            fileName = file.getName();
        }

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

            HashMap<Track, DataInputStream> inputStreams = getAudioFileInputs(sTracks);

            Log.d(TAG, "Start play");

            mAudioTrack.play();
            OutputStream os = new AudioTrackOutputStream(mAudioTrack);

            writeMixedTracks(inputStreams, os, mBufferSize);

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

            mListAdapter.setOffsetControlVisible(true);
        }
    }

    private static void writeMixedTracks(HashMap<Track, DataInputStream> inputStreams, OutputStream os, int bufferSize) {
        final int samplesInBufferSize = bufferSize / 2;
        final byte[] buffer = new byte[2 * bufferSize];
        final byte[] trackBuffer = new byte[bufferSize];
        final float[] leftBuffer = new float[samplesInBufferSize];
        final float[] rightBuffer = new float[samplesInBufferSize];
        float pan;
        short left = 0;
        short right = 0;
        float fSample;
        int bytes;
        Iterator<Map.Entry<Track, DataInputStream>> it;

        while (!Thread.interrupted() && !inputStreams.isEmpty()) {
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

            for(int i = 0; i < samplesInBufferSize; i++) {
                left = (short) Math.round(leftBuffer[i]);
                right = (short) Math.round(rightBuffer[i]);

                buffer[4*i+1] += (byte)(left >>> 8);
                buffer[4*i+0] += (byte)(left);


                buffer[4*i+3] += (byte)(right >>> 8);
                buffer[4*i+2] += (byte)(right);
            }

            try {
                os.write(buffer, 0, buffer.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @NonNull
    public static HashMap<Track, DataInputStream> getAudioFileInputs(Set<Track> tracks) {
        HashMap<Track, DataInputStream> inputStreams = new HashMap<>();

        for (Track t : tracks) {
            try {
                FileInputStream fis = new FileInputStream(t.file);
                WavUtils.readRiffHeader(fis);
                DataInputStream dis = new DataInputStream(new OffsetInputStream(fis, t.offset));
                inputStreams.put(t, dis);
            } catch (FileNotFoundException e) {
                Log.w(TAG, "Could not open file to play: " + e);
            } catch (WavHeaderException e) {
                Log.d(TAG, "Invalid WAV header: " + e);
            } catch (IOException e) {
                Log.d(TAG, "Error reading WAV header: " + e);
            }
        }
        return inputStreams;
    }

    private void alignToTrack(final Track track) {
        if (sTracks.size() <= 1) {
            return;
        }

        new AsyncTask<Void, Integer, Void>() {
            private ProgressDialog mProgressDialog;

            @Override
            protected void onPreExecute() {
                mProgressDialog = new ProgressDialog(PlayerActivity.this);
                mProgressDialog.setTitle("Aligning");
                mProgressDialog.setCancelable(false);
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setProgressNumberFormat(null);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                final int streamBufferSize = 1024;
                final int maxCollectedSamples = 88200;
                long shortestLength = track.file.length();

                for (Track t : sTracks) {
                    long length = (t.file.length() - t.offset / 2);

                    if (length < shortestLength) {
                        shortestLength = length;
                    }
                }

                int collectedSamples;
                if (shortestLength < maxCollectedSamples) {
                    collectedSamples = (int) (shortestLength / 4);
                } else {
                    collectedSamples = maxCollectedSamples;
                }
                final int offsetRange = collectedSamples / 2;
                final int analyzedSamples = collectedSamples - offsetRange;
                mProgressDialog.setMax((sTracks.size() - 1) * offsetRange);

                Log.d(TAG, "Collect model samples");
                System.gc();
                byte[] sampleBuffer = new byte[2];
                short[] modelSamples = new short[analyzedSamples];
                try {
                    InputStream is = new OffsetInputStream(new BufferedInputStream(new FileInputStream(track.file), streamBufferSize), track.offset);
                    is.skip((long) analyzedSamples); /* firstByte = analyzedSamples / 2 * 2 */
                    int sample;
                    for (int i = 0; i < analyzedSamples; ++i) {
                        is.read(sampleBuffer);
                        sample = sampleBuffer[1] << 8;
                        sample |= sampleBuffer[0] & 0xff;
                        modelSamples[i] = (short) sample;
                    }
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "Aligning start");
                System.gc();
                short[] alignedSamples = new short[collectedSamples];
                for (Track t : sTracks) {
                    if (t == track) {
                        continue;
                    }

                    Log.d(TAG, "Reading track: " + t);

                    try {
                        InputStream is = new OffsetInputStream(new BufferedInputStream(new FileInputStream(t.file), streamBufferSize), t.offset - 2 * offsetRange);
                        is.skip((long) collectedSamples); /* firstByte = analyzedSamples / 2 * 2 */
                        int sample;
                        for (int i = 0; i < collectedSamples; ++i) {
                            is.read(sampleBuffer);
                            sample = sampleBuffer[1] << 8;
                            sample |= sampleBuffer[0] & 0xff;
                            alignedSamples[i] = (short) sample;
                        }
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Log.d(TAG, "Aliging track: " + t);
                    float bestScore = Float.MAX_VALUE;
                    int bestOffset = getBestOffset(0, offsetRange, analyzedSamples, 16, modelSamples, alignedSamples, bestScore);
                    Log.d(TAG, "First run offset:  " + bestOffset);
                    bestOffset = getBestOffset(bestOffset - 8, bestOffset + 8, analyzedSamples, 1, modelSamples, alignedSamples, bestScore);
                    Log.d(TAG, "Second run offset: " + bestOffset);

                    int offsetDiff = bestOffset * 2 - offsetRange;
                    Log.d(TAG, "Offset diff: " + offsetDiff);
                    t.offset += offsetDiff;
                }

                return null;
            }

            private int getBestOffset(int minOffset, int maxOffset, int analyzedSamples, int step, short[] modelSamples, short[] alignedSamples, float bestScore) {
                int bestOffset = 0;
                for (int offset = minOffset; offset < maxOffset; ++offset) {
                    float score = 0;
                    for (int sample = 0; sample < analyzedSamples; sample += step) {
                        short modelSample = modelSamples[sample];
                        short alignedSample = alignedSamples[offset + sample];
                        int diff = modelSample - alignedSample;
                        score += diff * diff;
                    }

                    if (score < bestScore) {
                        bestScore = score;
                        bestOffset = offset;
                        Log.d(TAG, "Offset " + offset + " score " + score);
                    }

                    publishProgress(offset);
                }
                return bestOffset;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                mListAdapter.notifyDataSetInvalidated();
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                mProgressDialog.setProgress(values[0]);
            }
        }.execute();
    }

    private class ActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater menuInflater = mode.getMenuInflater();
            menuInflater.inflate(R.menu.menu_track, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.align:
                    alignToTrack(mSelectedTrack);
                    mode.finish();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            mSelectedTrack = null;
        }
    }
}
