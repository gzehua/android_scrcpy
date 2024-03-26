package com.suda.androidscrcpy.control;

import android.view.MotionEvent;

import java.nio.ByteBuffer;

public class TouchEventMessage implements ControlEventMessage {


    MotionEvent motionEvent;
    public int surfaceWidth;
    public int surfaceHeight;
    public int screenWidth;
    public int screenHeight;
    int pointIndex;

    public TouchEventMessage(MotionEvent motionEvent,
                             int surfaceWidth, int surfaceHeight,
                             int screenWidth, int screenHeight, int pointIndex) {
        this.motionEvent = MotionEvent.obtain(motionEvent);
        this.surfaceWidth = surfaceWidth;
        this.surfaceHeight = surfaceHeight;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.pointIndex = pointIndex;
    }

    static final int INJECT_TOUCH_EVENT_PAYLOAD_LENGTH = 31;


    @Override
    public byte[] makeEvent() {
        ByteBuffer buffer = ByteBuffer.allocate(1 + INJECT_TOUCH_EVENT_PAYLOAD_LENGTH);
//        int action = Binary.toUnsigned(buffer.get());
//        long pointerId = buffer.getLong();
//        Position position = readPosition(buffer);
//        float pressure = Binary.u16FixedPointToFloat(buffer.getShort());
//        int actionButton = buffer.getInt();
//        int buttons = buffer.getInt();
        buffer.put((byte) TYPE_INJECT_TOUCH_EVENT);
        buffer.put((byte) motionEvent.getAction());
        buffer.putLong(motionEvent.getPointerId(pointIndex));
        buffer.putInt((int) (motionEvent.getX(pointIndex) * screenWidth / surfaceWidth));
        buffer.putInt((int) (motionEvent.getY(pointIndex) * screenHeight / surfaceHeight));
        buffer.putShort((short) screenWidth);
        buffer.putShort((short) screenHeight);
        buffer.putShort((short) motionEvent.getPressure(pointIndex));
        buffer.putInt(motionEvent.getActionButton());
        buffer.putInt(0);
        motionEvent.recycle();
        return buffer.array();
    }
}
