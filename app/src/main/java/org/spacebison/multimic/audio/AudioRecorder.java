package org.spacebison.multimic.audio;

/**
 * Created by cmb on 24.10.15.
 */
public interface AudioRecorder {
    boolean prepare(String path);
    void start();
    void stop();
}
