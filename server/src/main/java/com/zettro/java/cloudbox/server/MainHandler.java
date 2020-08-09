package com.zettro.java.cloudbox.server;

import com.zettro.java.cloudbox.common.ChunkedFileMessage;
import com.zettro.java.cloudbox.common.ErrorMessage;
import com.zettro.java.cloudbox.common.FileRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private final String username;
    private FileOutputStream fos = null;
    private int chunkCounter = 0;

    public MainHandler(String username) {
        this.username = username;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected.");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client disconnected.");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("Message received");
        if (msg instanceof FileRequest) {
            System.out.println("File request received");
            FileRequest fr = (FileRequest) msg;
            String filename = Server.serverStoragePath  + username + "/" + fr.getFilename();
            if (Files.exists(Paths.get(filename)) && (!Files.isDirectory(Paths.get(filename)))) {
                ChunkedFileMessage cfm = new ChunkedFileMessage(Paths.get("server_storage/" +
                        username + "/" + fr.getFilename()), 64 * 1024);
                ctx.writeAndFlush(cfm); // сначала создаем файл (bytesRead == 0)
                // пока не будет прочитан конец файла последовательно читаем файл в буффер передаем через сериализацию
                int chunkCounter = 0;
                while(cfm.readNextChunk() != -1) {
                    ctx.writeAndFlush(cfm);
                    chunkCounter++;
                    System.out.println(chunkCounter + " chunk is sent");
                }
                ctx.writeAndFlush(cfm); // передаем конец файла
            } else {
                System.out.println("File not found or it is a directory");
                ctx.writeAndFlush(new ErrorMessage("File not found or it is a directory..."));
            }
        }
        if(msg instanceof ChunkedFileMessage) {
            System.out.println("ChunkedFileMessage received");
            // здесь мы принимаем файл от клиента
            ChunkedFileMessage cfm = (ChunkedFileMessage) msg;
            if (cfm.getBytesRead() != -1) {
                if (cfm.getBytesRead() == 0) {
                    System.out.println("Creating file " + username + "/" + cfm.getFilename());
                    fos = new FileOutputStream(Server.serverStoragePath + username + "/" + cfm.getFilename());
                    chunkCounter = 0;
                    //Files.write(Paths.get(CloudBoxClient.clientStoragePath + userName + "/" + cfm.getFilename()), new byte[0], StandardOpenOption.CREATE);
                } else {
                    chunkCounter++;
                    System.out.println("Writing " + chunkCounter + " chunk to " + username + "/" + cfm.getFilename() + "; Bytes read = " + cfm.getBytesRead());
                    fos.write(cfm.getData(), 0, cfm.getBytesRead());
                    // Files.write(Paths.get(CloudBoxClient.clientStoragePath + userName + "/" + cfm.getFilename()), cfm.getData(), StandardOpenOption.APPEND);
                }
            } else {
                fos.flush();
                fos.close();
                fos = null;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
