package org.spacebison.multimic.model;

import android.media.AudioFormat;
import android.media.AudioRecord;

/**
 * Created by cmb on 28.10.15.
 */
public class Config {
    public static final String DISCOVERY_MULTICAST_GROUP = "239.6.6.6";
    public static final int DISCOVERY_MULTICAST_PORT = 23966;
    public static final int SERVER_PORT = 23969;
    public static final int SAMPLE_RATE = 44100;
    public static final int CHANNEL_CONFIG = 1;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
}
