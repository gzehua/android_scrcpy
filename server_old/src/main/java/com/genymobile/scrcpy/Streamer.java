package com.genymobile.scrcpy;

import android.media.MediaCodec;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class Streamer {

    private static final long PACKET_FLAG_CONFIG_PORT = 1L << 63;
    private static final long PACKET_FLAG_KEY_FRAME = 1L << 62;
    private static final long PACKET_FLAG_KEY_CONFIG_LAND = 1L << 61;


    private final FileDescriptor fd;
    private final Codec codec;
    private final boolean sendCodecMeta;
    private final boolean sendFrameMeta;

    private final ByteBuffer headerBuffer = ByteBuffer.allocate(12);

    public Streamer(FileDescriptor fd, Codec codec, boolean sendCodecMeta, boolean sendFrameMeta) {
        this.fd = fd;
        this.codec = codec;
        this.sendCodecMeta = sendCodecMeta;
        this.sendFrameMeta = sendFrameMeta;
    }

    public Codec getCodec() {
        return codec;
    }

    public void writeVideoHeader(Size videoSize) throws IOException {
        if (sendCodecMeta) {
            ByteBuffer buffer = ByteBuffer.allocate(12);
            buffer.putInt(codec.getId());
            buffer.putInt(videoSize.getWidth());
            buffer.putInt(videoSize.getHeight());
            buffer.flip();
            IO.writeFully(fd, buffer);
        }
    }

    public void writePacket(ByteBuffer buffer, long pts, boolean config, boolean keyFrame, boolean isPort) throws IOException {
        if (config) {
        }

        if (sendFrameMeta) {
            writeFrameMeta(fd, buffer.remaining(), pts, config, keyFrame,isPort);
        }

        IO.writeFully(fd, buffer);
    }

    public void writePacket(ByteBuffer codecBuffer, MediaCodec.BufferInfo bufferInfo) throws IOException {
        writePacket(codecBuffer, bufferInfo, true);
    }

    public void writePacket(ByteBuffer codecBuffer, MediaCodec.BufferInfo bufferInfo,boolean isPort) throws IOException {
        long pts = bufferInfo.presentationTimeUs;
        boolean config = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0;
        boolean keyFrame = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
        writePacket(codecBuffer, pts, config, keyFrame,isPort);
    }

    private void writeFrameMeta(FileDescriptor fd, int packetSize, long pts, boolean config, boolean keyFrame, boolean isPort) throws IOException {
        headerBuffer.clear();

        long ptsAndFlags;
        if (config) {
            ptsAndFlags = isPort ? PACKET_FLAG_CONFIG_PORT : PACKET_FLAG_KEY_CONFIG_LAND; // non-media data packet
        } else {
            ptsAndFlags = pts;
            if (keyFrame) {
                ptsAndFlags |= PACKET_FLAG_KEY_FRAME;
            }
        }

        headerBuffer.putLong(ptsAndFlags);
        headerBuffer.putInt(packetSize);
        headerBuffer.flip();
        IO.writeFully(fd, headerBuffer);
    }
}
