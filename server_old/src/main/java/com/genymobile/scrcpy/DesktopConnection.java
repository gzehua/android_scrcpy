package com.genymobile.scrcpy;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public final class DesktopConnection implements Closeable {


    private LocalSocket videoSocket;
    private OutputStream videoOutput;

    private final LocalSocket controlSocket;
    private final ControlChannel controlChannel;

    private DesktopConnection(LocalSocket videoSocket, LocalSocket controlSocket) throws IOException {
        this.videoSocket = videoSocket;
        this.controlSocket = controlSocket;
        videoOutput = videoSocket.getOutputStream();
        controlChannel = controlSocket != null ? new ControlChannel(controlSocket) : null;
    }

    private static LocalSocket connect(String abstractName) throws IOException {
        LocalSocket localSocket = new LocalSocket();
        localSocket.connect(new LocalSocketAddress(abstractName));
        return localSocket;
    }

    public static DesktopConnection open(String ip) throws IOException {
        LocalSocket videoSocket = connect("scrcpy");
        LocalSocket controlSocket = connect("scrcpy");
        DesktopConnection connection = new DesktopConnection(videoSocket, controlSocket);
        Log.d("DroidConnection", "open");
        return connection;
    }

    public void close() throws IOException {
        videoSocket.shutdownInput();
        videoSocket.shutdownOutput();
        videoSocket.close();

        controlSocket.shutdownInput();
        controlSocket.shutdownOutput();
        controlSocket.close();
    }

    public OutputStream getVideoOutput() {
        return videoOutput;
    }

    public ControlChannel getControlChannel() {
        return controlChannel;
    }

}

