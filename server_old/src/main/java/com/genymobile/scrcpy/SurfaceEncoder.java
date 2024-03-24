package com.genymobile.scrcpy;

import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Surface;

import com.genymobile.scrcpy.model.MediaPacket;
import com.genymobile.scrcpy.model.VideoPacket;
import com.genymobile.scrcpy.wrappers.SurfaceControl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class SurfaceEncoder implements Device.RotationListener {

    private static final int DEFAULT_FRAME_RATE = 60; // fps
    private static final int DEFAULT_I_FRAME_INTERVAL = 10; // seconds
    private static final int REPEAT_FRAME_DELAY_US = 100_000; // repeat after 100ms

    private static final int REPEAT_FRAME_DELAY = 6; // repeat after 6 frames

    private static final int MICROSECONDS_IN_ONE_SECOND = 1_000_000;

    private final AtomicBoolean rotationChanged = new AtomicBoolean();

    private int bitRate;
    private int frameRate;
    private int iFrameInterval;

    public SurfaceEncoder(int bitRate, int frameRate, int iFrameInterval) {
        this.bitRate = bitRate;
        this.frameRate = frameRate;
        this.iFrameInterval = iFrameInterval;
    }

    public SurfaceEncoder(int bitRate) {
        this(bitRate, DEFAULT_FRAME_RATE, DEFAULT_I_FRAME_INTERVAL);
    }
	
    public void streamScreen(Device device, OutputStream outputStream) throws IOException {
        MediaFormat format = createFormat(bitRate, frameRate, iFrameInterval);
        device.setRotationListener(this);
        boolean alive;
        try {
            do {
                MediaCodec codec = createCodec();
                //todo 适配
                IBinder display = null;
                try {
                    display = createDisplay();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                Rect deviceRect = device.getScreenInfo().getDeviceSize().toRect();
                Rect videoRect = device.getScreenInfo().getVideoSize().toRect();
                setSize(format, videoRect.width(), videoRect.height());
                configure(codec, format);
                Surface surface = codec.createInputSurface();
                setDisplaySurface(display, surface, deviceRect, videoRect);
                codec.start();
                try {
                    alive = encode(codec, outputStream);
                } finally {
                    codec.stop();
                    destroyDisplay(display);
                    codec.release();
                    surface.release();
                }
            } while (alive);
        } finally {
            device.setRotationListener(null);
        }
    }


    private static MediaCodec createCodec() throws IOException {
        return MediaCodec.createEncoderByType("video/avc");
    }


    private static IBinder createDisplay() throws Exception {
        return SurfaceControl.createDisplay("scrcpy", false);
    }

    private static void configure(MediaCodec codec, MediaFormat format) {
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    private static void setSize(MediaFormat format, int width, int height) {
        format.setInteger(MediaFormat.KEY_WIDTH, width);
        format.setInteger(MediaFormat.KEY_HEIGHT, height);
    }

    private static void setDisplaySurface(IBinder display, Surface surface, Rect deviceRect, Rect displayRect) {
        SurfaceControl.openTransaction();
        try {
            SurfaceControl.setDisplaySurface(display, surface);
            SurfaceControl.setDisplayProjection(display, 0, deviceRect, displayRect);
            SurfaceControl.setDisplayLayerStack(display, 0);
        } finally {
            SurfaceControl.closeTransaction();
        }
    }

    private static void destroyDisplay(IBinder display) {
        SurfaceControl.destroyDisplay(display);
    }

    @Override
    public void onRotationChanged(int rotation) {
        rotationChanged.set(true);
    }

    public boolean consumeRotationChange() {
        return rotationChanged.getAndSet(false);
    }

    private boolean encode(MediaCodec codec, OutputStream outputStream) throws IOException {
        @SuppressWarnings("checkstyle:MagicNumber")
//        byte[] buf = new byte[bitRate / 8]; // may contain up to 1 second of video
        boolean eof = false;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (!consumeRotationChange() && !eof) {
            int outputBufferId = codec.dequeueOutputBuffer(bufferInfo, -1);
            eof = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
            try {
                if (consumeRotationChange()) {
                    // must restart encoding with new size
                    break;
                }
                if (outputBufferId >= 0) {
                    ByteBuffer outputBuffer;
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        ByteBuffer[] outputBuffers = codec.getOutputBuffers();
                        outputBuffer = outputBuffers[outputBufferId];
                    } else {
                        outputBuffer = codec.getOutputBuffer(outputBufferId);
                    }

                    if (bufferInfo.size > 0 && outputBuffer != null) {
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        byte[] b = new byte[outputBuffer.remaining()];
                        outputBuffer.get(b);

                        MediaPacket.Type type = MediaPacket.Type.VIDEO;
                        VideoPacket.Flag flag = VideoPacket.Flag.CONFIG;

                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                            flag = VideoPacket.Flag.END;
                        } else if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                            flag = VideoPacket.Flag.KEY_FRAME;
                        } else if (bufferInfo.flags == 0) {
                            flag = VideoPacket.Flag.FRAME;
                        }
                        VideoPacket packet = new VideoPacket(type, flag, bufferInfo.presentationTimeUs, b);
                        outputStream.write(packet.toByteArray());
                    }

                }
            } finally {
                if (outputBufferId >= 0) {
                    codec.releaseOutputBuffer(outputBufferId, false);
                }
            }
        }

        return !eof;
    }
	
	private static MediaFormat createFormat(int bitRate, int frameRate, int iFrameInterval) throws IOException {
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "video/avc");
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        // must be present to configure the encoder, but does not impact the actual frame rate, which is variable
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 60);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, DEFAULT_I_FRAME_INTERVAL);

        // display the very first frame, and recover from bad quality when no new frames
        format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, REPEAT_FRAME_DELAY_US); // µs
//        if (maxFps > 0) {
//            // The key existed privately before Android 10:
//            // <https://android.googlesource.com/platform/frameworks/base/+/625f0aad9f7a259b6881006ad8710adce57d1384%5E%21/>
//            // <https://github.com/Genymobile/scrcpy/issues/488#issuecomment-567321437>
//            format.setFloat(KEY_MAX_FPS_TO_ENCODER, maxFps);
//        }
        return format;
    }

}
