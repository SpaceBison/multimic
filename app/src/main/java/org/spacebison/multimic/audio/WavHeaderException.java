package org.spacebison.multimic.audio;

/**
 * Created by cmb on 11.12.15.
 */
public class WavHeaderException extends Exception {
    public WavHeaderException() {
    }

    public WavHeaderException(String detailMessage) {
        super(detailMessage);
    }

    public WavHeaderException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public WavHeaderException(Throwable throwable) {
        super(throwable);
    }
}
