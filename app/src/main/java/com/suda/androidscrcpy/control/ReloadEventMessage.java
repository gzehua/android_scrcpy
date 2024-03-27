package com.suda.androidscrcpy.control;

import java.nio.ByteBuffer;

public class ReloadEventMessage implements ControlEventMessage {

    @Override
    public byte[] makeEvent() {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put((byte) TYPE_RELOAD);
        return buffer.array();
    }
}
