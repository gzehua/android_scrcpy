package com.genymobile.scrcpy;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class DesktopConnection implements Closeable {


    private static LocalSocket socket = null;
    private OutputStream outputStream;
    private InputStream inputStream;

    private DesktopConnection(LocalSocket socket) throws IOException {
        this.socket = socket;

        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }

    private static LocalSocket connect(String abstractName) throws IOException {
        LocalSocket localSocket = new LocalSocket();
        localSocket.connect(new LocalSocketAddress(abstractName));
        return localSocket;
    }


    private static LocalSocket listenAndAccept() throws IOException {
        System.out.println("listenAndAccept");
        LocalSocket sock = null;
        try {
            sock = connect("scrcpy");
        } catch (Exception e){

        }
        return sock;
    }

    public static DesktopConnection open(String ip) throws IOException {
        socket = listenAndAccept();
        DesktopConnection connection = new DesktopConnection(socket);
        Log.d("DroidConnection", "open");
        return connection;
    }

    public void close() throws IOException {
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }


    public int[] NewreceiveControlEvent() throws IOException {

        byte[] buf = new byte[16];
        int n = inputStream.read(buf, 0, 16);
        if (n == -1) {
            throw new EOFException("Event controller socket closed");
        }


        final int[] array = new int[buf.length / 4];
        for (int i = 0; i < array.length; i++)
            array[i] = (((int) (buf[i * 4]) << 24) & 0xFF000000) |
                    (((int) (buf[i * 4 + 1]) << 16) & 0xFF0000) |
                    (((int) (buf[i * 4 + 2]) << 8) & 0xFF00) |
                    ((int) (buf[i * 4 + 3]) & 0xFF);
        return array;


    }

}

