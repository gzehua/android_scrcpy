package com.suda.androidscrcpy.model;

import java.nio.ByteBuffer;

/**
 * Created by Alexandr Golovach on 27.06.16.
 * https://www.github.com/alexmprog/VideoCodec
 */

public class VideoPacket extends MediaPacket {


    private static final long PACKET_FLAG_CONFIG_PORT = 1L << 63;
    private static final long PACKET_FLAG_KEY_FRAME = 1L << 62;
    private static final long PACKET_FLAG_KEY_CONFIG_LAND = 1L << 61;

    public Flag flag;
    public long presentationTimeStamp;
    public byte[] data;
    public boolean isPort;

    public VideoPacket() {
    }

    public VideoPacket(Type type, Flag flag, long presentationTimeStamp, byte[] data) {
        this.type = type;
        this.flag = flag;
        this.presentationTimeStamp = presentationTimeStamp;
        this.data = data;
    }

    // create packet from byte array
    public static VideoPacket fromArray(byte[] values, long ptsAndFlags) {
        VideoPacket videoPacket = new VideoPacket();
        videoPacket.type = Type.VIDEO;
        if (ptsAndFlags == PACKET_FLAG_CONFIG_PORT || ptsAndFlags == PACKET_FLAG_KEY_CONFIG_LAND) {
            videoPacket.flag = Flag.CONFIG;
            videoPacket.isPort = ptsAndFlags == PACKET_FLAG_CONFIG_PORT;
        } else if ((ptsAndFlags & PACKET_FLAG_KEY_FRAME) == PACKET_FLAG_KEY_FRAME) {
            videoPacket.flag = Flag.KEY_FRAME;
            videoPacket.presentationTimeStamp = ptsAndFlags & ~PACKET_FLAG_KEY_FRAME;
        } else {
            videoPacket.flag = Flag.FRAME;
            videoPacket.presentationTimeStamp = ptsAndFlags;
        }
        videoPacket.data = values;
        return videoPacket;
    }

    public static StreamSettings getStreamSettings(byte[] buffer) {
        byte[] sps, pps;

        ByteBuffer spsPpsBuffer = ByteBuffer.wrap(buffer);
        if (spsPpsBuffer.getInt() == 0x00000001) {
            System.out.println("parsing sps/pps");
        } else {
            System.out.println("something is amiss?");
        }
        int ppsIndex = 0;
        while (!(spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x01)) {

        }
        ppsIndex = spsPpsBuffer.position();
        sps = new byte[ppsIndex - 4];
        System.arraycopy(buffer, 0, sps, 0, sps.length);
        ppsIndex -= 4;
        pps = new byte[buffer.length - ppsIndex];
        System.arraycopy(buffer, ppsIndex, pps, 0, pps.length);

        // sps buffer
        ByteBuffer spsBuffer = ByteBuffer.wrap(sps, 0, sps.length);

        // pps buffer
        ByteBuffer ppsBuffer = ByteBuffer.wrap(pps, 0, pps.length);

        StreamSettings streamSettings = new StreamSettings();
        streamSettings.sps = spsBuffer;
        streamSettings.pps = ppsBuffer;

        return streamSettings;
    }


    public enum Flag {

        FRAME((byte) 0), KEY_FRAME((byte) 1), CONFIG((byte) 2), END((byte) 4);

        private byte type;

        Flag(byte type) {
            this.type = type;
        }

        public static Flag getFlag(byte value) {
            for (Flag type : Flag.values()) {
                if (type.getFlag() == value) {
                    return type;
                }
            }

            return null;
        }

        public byte getFlag() {
            return type;
        }
    }

    public static class StreamSettings {
        public ByteBuffer pps;
        public ByteBuffer sps;
    }
}
