package org.spacebison.multimic.ui.player;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import org.spacebison.multimic.R;
import org.spacebison.multimic.Util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by cmb on 19.12.15.
 */
class TrackListAdapter extends BaseAdapter {
    private static final String TAG = "cmb.TrackListAdapter";
    private PlayerActivity mPlayerActivity;
    private boolean mOffsetVisible = true;
    private ScheduledExecutorService mAutoClickExecutor = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture mAutoClickFuture;

    public TrackListAdapter(PlayerActivity playerActivity) {
        mPlayerActivity = playerActivity;
    }

    @Override
    public int getCount() {
        return PlayerActivity.sTracks.size();
    }

    @Override
    public Object getItem(int position) {
        return Util.getObjectAt(PlayerActivity.sTracks, position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void setOffsetControlVisible(boolean visible) {
        mOffsetVisible = visible;
        mPlayerActivity.mListView.post(new Runnable() {
            @Override
            public void run() {
                mPlayerActivity.mListView.invalidateViews();
            }
        });
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final PlayerActivity.Track track = (PlayerActivity.Track) getItem(position);

        final TextView numberText;
        final TextView volumeText;
        final SeekBar volumeSeek;
        final TextView panText;
        final SeekBar panSeek;
        final View offsetControl;
        final TextView offsetText;
        final Button minusButton;
        final Button plusButton;

        if (convertView == null) {
            convertView = mPlayerActivity.getLayoutInflater().inflate(R.layout.list_item_track_controls, parent, false);
            volumeText = (TextView) convertView.findViewById(R.id.volumeText);
            panText = (TextView) convertView.findViewById(R.id.panText);

            volumeSeek = (SeekBar) convertView.findViewById(R.id.volumeSeek);
            volumeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    track.volume = progress / 256f;
                    setVolumeText(volumeText, track);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            panSeek = (SeekBar) convertView.findViewById(R.id.panSeek);
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

            offsetText = (TextView) convertView.findViewById(R.id.offsetText);

            AutoClickOnTouchListener autoClickOnTouchListener = new AutoClickOnTouchListener();

            minusButton = (Button) convertView.findViewById(R.id.minusButton);
            minusButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    track.offset -= 44;
                    setOffsetText(offsetText, track);
                }
            });
            minusButton.setOnLongClickListener(new AutoClickLongClickListener(track, offsetText, -440, 300, 50));
            minusButton.setOnTouchListener(autoClickOnTouchListener);

            plusButton = (Button) convertView.findViewById(R.id.plusButton);
            plusButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    track.offset += 44;
                    setOffsetText(offsetText, track);
                }
            });

            plusButton.setOnLongClickListener(new AutoClickLongClickListener(track, offsetText, +440, 300, 50));
            plusButton.setOnTouchListener(autoClickOnTouchListener);
        } else {
            volumeText = (TextView) convertView.findViewById(R.id.volumeText);
            volumeSeek = (SeekBar) convertView.findViewById(R.id.volumeSeek);
            panSeek = (SeekBar) convertView.findViewById(R.id.panSeek);
            panText = (TextView) convertView.findViewById(R.id.panText);
            offsetText = (TextView) convertView.findViewById(R.id.offsetText);
        }

        numberText = (TextView) convertView.findViewById(R.id.trackNumberText);
        offsetControl = convertView.findViewById(R.id.offsetControl);

        offsetControl.setVisibility(mOffsetVisible ? View.VISIBLE : View.GONE);

        numberText.setText(Integer.toString(position));

        setVolumeText(volumeText, track);
        volumeSeek.setProgress((int) (track.volume * 256));

        setPanText(track, panText);
        panSeek.setProgress((int) ((track.pan + 1) * 128));

        setOffsetText(offsetText, track);

        return convertView;
    }

    public void setOffsetText(final TextView offsetText, final PlayerActivity.Track track) {
        offsetText.post(new Runnable() {
            @Override
            public void run() {
                String text = String.format("%.2f ms", track.offset / 44.1f);
                Log.d(TAG, "Offset: " + track.offset + " text: " + text);
                offsetText.setText(text);
            }
        });
        offsetText.postInvalidate();
    }

    public void setPanText(PlayerActivity.Track track, TextView panText) {
        if (track.pan == 0) {
            panText.setText("Pan: C");
        } else if (track.pan < 0) {
            panText.setText("Pan: " + (int) (track.pan * -100) + "L");
        } else {
            panText.setText("Pan: " + (int) (track.pan * 100) + "R");
        }
    }

    public void setVolumeText(TextView volumeText, PlayerActivity.Track track) {
        volumeText.setText("Volume: " + (int) (track.volume * 100) + '%');
    }

    private class AutoClickLongClickListener implements View.OnLongClickListener  {
        private static final float EXPONENT_BASE = 1.1f;
        private PlayerActivity.Track mTrack;
        private TextView mOffsetText;
        private int mValueStep;
        private int mCurrentSteps = 0;
        private long mInitialDelay;
        private long mMinDelay;
        private Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "auto click");
                mTrack.offset += mValueStep;
                setOffsetText(mOffsetText, mTrack);
                long nextDelay = (long) ((mInitialDelay - mMinDelay) / Math.pow(EXPONENT_BASE, mCurrentSteps++)) + mMinDelay;
                mAutoClickFuture.cancel(true);
                mAutoClickFuture = mAutoClickExecutor.schedule(this, nextDelay, TimeUnit.MILLISECONDS);
            }
        };

        public AutoClickLongClickListener(PlayerActivity.Track track, TextView offsetText, int valueStep, long initialDelay, long minDelay) {
            mTrack = track;
            mOffsetText = offsetText;
            mValueStep = valueStep;
            mInitialDelay = initialDelay;
            mMinDelay = minDelay;
        }

        @Override
        public boolean onLongClick(View v) {
            Log.d(TAG, "Long click start");
            mCurrentSteps = 0;
            if (mAutoClickFuture != null) {
                mAutoClickFuture.cancel(true);
            }
            mAutoClickFuture = mAutoClickExecutor.schedule(mRunnable, mInitialDelay, TimeUnit.MILLISECONDS);
            return true;
        }
    }

    private class AutoClickOnTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP && mAutoClickFuture != null) {
                Log.d(TAG, "Long click end");
                mAutoClickFuture.cancel(true);
                mAutoClickFuture = null;
            }
            return false;
        }
    }
}
