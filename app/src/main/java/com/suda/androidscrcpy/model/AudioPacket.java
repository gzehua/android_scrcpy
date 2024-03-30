package com.suda.androidscrcpy.model;

public class AudioPacket extends MediaPacket {

    private static final long PACKET_FLAG_CONFIG_PORT = 1L << 63;
    private static final long PACKET_FLAG_KEY_FRAME = 1L << 62;

    public Flag flag;
    public long presentationTimeStamp;
    public byte[] data;

    // create packet from byte array
    public static AudioPacket fromArray(byte[] values, long ptsAndFlags) {
        AudioPacket audio = new AudioPacket();
        audio.type = Type.AUDIO;
        if (ptsAndFlags == PACKET_FLAG_CONFIG_PORT) {
            audio.flag = Flag.CONFIG;
        } else if ((ptsAndFlags & PACKET_FLAG_KEY_FRAME) == PACKET_FLAG_KEY_FRAME) {
            audio.flag = Flag.KEY_FRAME;
            audio.presentationTimeStamp = ptsAndFlags & ~PACKET_FLAG_KEY_FRAME;
        } else {
            audio.flag = Flag.FRAME;
            audio.presentationTimeStamp = ptsAndFlags;
        }
        audio.data = values;
        return audio;
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


}
