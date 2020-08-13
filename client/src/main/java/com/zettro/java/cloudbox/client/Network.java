package com.zettro.java.cloudbox.client;

import com.zettro.java.cloudbox.common.AbstractMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;

public class Network {
    private static Socket socket;
    private static ObjectEncoderOutputStream out;
    private static ObjectDecoderInputStream in;
    private static boolean started = false;

    public static void start(String host, int port) throws IOException {
        if (started) stop();
        socket = new Socket(host, port);
        out = new ObjectEncoderOutputStream(socket.getOutputStream());
        in = new ObjectDecoderInputStream(socket.getInputStream(), 2048 * 1024);
        started = true;
    }

    public static void stop() {
        if (!started) return;
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        started = false;
    }

    public static boolean sendMsg(AbstractMessage msg) {
        if (!started) {
          throw new RuntimeException("Network isn't started yet!");
        }
        try {
            out.writeObject(msg);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static AbstractMessage readObject() throws ClassNotFoundException, IOException {
        if (!started) {
            throw new RuntimeException("Network isn't started yet!");
        }
        Object obj = in.readObject();
        return (AbstractMessage) obj;
    }

    public static boolean isStarted() {
        return started;
    }
}