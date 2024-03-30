package com.suda.androidscrcpy.decoder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class RawAudioDecoder {

    public static final int SAMPLE_RATE = 48000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    public static final int CHANNELS = 2;

    private MediaCodec mCodec;
    private Worker mWorker;
    private AtomicBoolean mIsConfigured = new AtomicBoolean(false);

    AudioTrack audioTrack;

    public void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags) {
        if (mWorker != null) {
            mWorker.decodeSample(data, offset, size, presentationTimeUs, flags);
        }
    }

    public void start() {
        if (mWorker == null) {
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                    CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE, AudioTrack.MODE_STREAM);
            audioTrack.play();
            mWorker = new Worker();
            mWorker.setRunning(true);
            mWorker.start();
        }
    }

    public void stop() {
        if (mWorker != null) {
            mWorker.setRunning(false);
            mWorker = null;
            mIsConfigured.set(false);
            try {
                if (mCodec != null)
                    mCodec.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class Worker extends Thread {

        private AtomicBoolean mIsRunning = new AtomicBoolean(false);

        Worker() {
        }

        private void setRunning(boolean isRunning) {
            mIsRunning.set(isRunning);
        }

        private void configure() {
            if (mIsConfigured.get()) {
                mIsConfigured.set(false);
                try {
                    mCodec.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_RAW,
                    SAMPLE_RATE, CHANNELS);
            format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT);
            try {
                mCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_RAW);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            mCodec.configure(format, null, null, 0);
            mCodec.start();
            mIsConfigured.set(true);
        }


        @SuppressWarnings("deprecation")
        public void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags) {
            if (!mIsConfigured.get()) {
                configure();
                mIsConfigured.set(true);
            }

            if (mIsConfigured.get() && mIsRunning.get()) {
                int index = mCodec.dequeueInputBuffer(-1);
                if (index >= 0) {
                    ByteBuffer buffer;

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        buffer = mCodec.getInputBuffers()[index];
                        buffer.clear();
                    } else {
                        buffer = mCodec.getInputBuffer(index);
                    }
                    if (buffer != null) {
                        buffer.put(data, offset, size);
                        mCodec.queueInputBuffer(index, 0, size, 0, flags);
                    }
                }
            }
        }

        @Override
        public void run() {
            try {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                while (mIsRunning.get()) {
                    if (mIsConfigured.get()) {
                        int index = mCodec.dequeueOutputBuffer(info, 0);
                        if (index >= 0) {
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                ByteBuffer[] outputBuffers = mCodec.getOutputBuffers();
                                ByteBuffer outputBuffer = outputBuffers[index];
                                byte[] chunk = new byte[info.size];
                                outputBuffer.get(chunk);
                                outputBuffer.clear();
                                audioTrack.write(chunk, 0, chunk.length);
                            }

                            // setting true is telling system to render frame onto Surface
                            mCodec.releaseOutputBuffer(index, true);
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                break;
                            }
                        }
                    } else {
                        // just waiting to be configured, then decode and render
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            } catch (IllegalStateException e) {
            }

        }
    }
}