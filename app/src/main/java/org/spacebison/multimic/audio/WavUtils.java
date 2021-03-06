package org.spacebison.multimic.audio;

import org.spacebison.common.CrashlyticsLog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by cmb on 2015-11-11.
 */
public class WavUtils {
    public static void makeWavFile(File rawPcmFile, short numChannels, int sampleRate, short bitsPerSample, File outputFile) throws IOException {
        BufferedInputStream input = null;
        BufferedOutputStream output = null;
        try {
            input = new BufferedInputStream(new FileInputStream(rawPcmFile));
            output = new BufferedOutputStream(new FileOutputStream(outputFile));

            int numSamples = (int) (rawPcmFile.length() / numChannels / bitsPerSample * 8);
            writeRiffHeader(output, numChannels, sampleRate, bitsPerSample, numSamples);

            byte[] buf = new byte[1024];
            int bytesRead = 0;

            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException(e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ignored) {}
            }

            if (output != null) {
                try {
                    output.close();
                } catch (Exception ignored) {
                }
            }
        }
    }


    public static void writeRiffHeader(OutputStream outputStream, WavHeader header) throws IOException {
        writeRiffHeader(outputStream, header.numChannels, header.sampleRate, header.bitsPerSample, header.numSamples);
    }

    public static void writeRiffHeader(OutputStream outputStream, short numChannels, int sampleRate, short bitsPerSample, int numSamples) throws IOException {
        int dataLength = numChannels*bitsPerSample/8 * numSamples;
        // RIFF
        outputStream.write(toEndianSwappedByteArray(0x46464952)); // ChunkID
        outputStream.write(toEndianSwappedByteArray(36 + dataLength)); // ChunkSize
        outputStream.write(toEndianSwappedByteArray(0x45564157)); // Format

        // fmt
        outputStream.write(toEndianSwappedByteArray(0x20746d66)); // SubChunk1ID
        outputStream.write(toEndianSwappedByteArray(16)); // SubChunk1Size
        outputStream.write(toEndianSwappedByteArray((short)1)); // AudioFormat
        outputStream.write(toEndianSwappedByteArray(numChannels)); // NumChannels
        outputStream.write(toEndianSwappedByteArray(sampleRate)); // SampleRate
        outputStream.write(toEndianSwappedByteArray(sampleRate * numChannels * bitsPerSample/8)); // ByteRate
        outputStream.write(toEndianSwappedByteArray((short) (numChannels * bitsPerSample/8))); // BlockAlign
        outputStream.write(toEndianSwappedByteArray(bitsPerSample)); // BitsPerSample

        // data
        outputStream.write(toEndianSwappedByteArray(0x61746164)); // SubChunk2ID
        outputStream.write(toEndianSwappedByteArray(dataLength)); // SubChunk2Size
    }

    public static WavHeader readRiffHeader(InputStream inputStream) throws IOException, WavHeaderException {
        WavHeader header = new WavHeader();
        byte[] shortArray = new byte[2];
        byte[] intArray = new byte[4];
        inputStream.read(intArray);
        int chunkId = toEndianSwappedInt(intArray);

        if (chunkId != 0x46464952) {
            throw new WavHeaderException("Invalid chunkId: " + Integer.toHexString(chunkId) + " (should be " + Integer.toHexString(0x46464952) + ')');
        }

        inputStream.read(intArray);
        // TODO: don't ignore this stuff
        CrashlyticsLog.d("RIFF", "Skipping next " + 36 + " bytes");
        inputStream.skip(36);

        return header;
    }

    public static byte[] toEndianSwappedByteArray(int integer) {
        byte[] array = new byte[4];

        for(int i=0; i < 4; ++i) {
            array[i] = (byte)(integer & 0xFF);
            integer >>= 8;
        }

        return array;
    }

    public static int toEndianSwappedInt(byte[] array) {
        int integer = 0;

        for (int i = 0; i < 4; ++i) {
            integer <<= 8;
            integer |= array[3 - i];
        }

        return integer;
    }

    public static byte[] toEndianSwappedByteArray(short integer) {
        byte[] array = new byte[2];

        for(int i=0; i < 2; ++i) {
            array[i] = (byte)(integer & 0xFF);
            integer >>= 8;
        }

        return array;
    }

    public static short toEndianSwappedShort(byte[] array) {
        short integer = 0;

        for (int i = 0; i < 2; ++i) {
            integer <<= 8;
            integer |= array[1 - i];
        }

        return integer;
    }
}
