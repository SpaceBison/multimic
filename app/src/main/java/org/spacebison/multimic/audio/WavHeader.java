package org.spacebison.multimic.audio;

/**
 * Created by cmb on 11.12.15.
 */
public class WavHeader {
    public short numChannels;
    public int sampleRate;
    public short bitsPerSample;
    public int numSamples;

    @Override
    public String toString() {
        return "WavHeader{" +
                "channels=" + numChannels +
                ", sample rate=" + sampleRate +
                ", bits/sample=" + bitsPerSample +
                ", samples=" + numSamples +
                '}';
    }
}
