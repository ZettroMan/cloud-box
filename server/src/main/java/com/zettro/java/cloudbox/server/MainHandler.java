package com.zettro.java.cloudbox.server;

import com.zettro.java.cloudbox.common.ChunkedFileMessage;
import com.zettro.java.cloudbox.common.ConfirmationMessage;
import com.zettro.java.cloudbox.common.ErrorMessage;
import com.zettro.java.cloudbox.common.FileRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Semaphore;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private final String username;
    private FileOutputStream fos = null;
    private int chunkCounter = 0;
    Semaphore semaphore = new Semaphore(1);

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
        // System.out.println("Message received");
        if (msg instanceof FileRequest) {
           // System.out.println("File request received");
            FileRequest fr = (FileRequest) msg;
            String filename = Server.serverStoragePath + username + "/" + fr.getFilename();
            if (Files.exists(Paths.get(filename)) && (!Files.isDirectory(Paths.get(filename)))) {
                semaphore = new Semaphore(1);
                new Thread(() -> {
                     try {
                         ChunkedFileMessage cfm = new ChunkedFileMessage(Paths.get(filename), 1024 * 1024);
                         chunkCounter = 0;
                         semaphore.acquire();
                         ctx.writeAndFlush(cfm); // сначала создаем файл (bytesRead == 0)
                         semaphore.acquire();
                         // пока не будет прочитан конец файла последовательно читаем файл в буффер передаем через сериализацию                    int chunkCounter = 0;
                         while (cfm.readNextChunk() != -1) {
                            ctx.writeAndFlush(cfm);
                            chunkCounter++;
                           // System.out.println(cfm.getBytesRead() + " bytes sent in chunk #" + chunkCounter);
                            semaphore.acquire();
                        }
                        ctx.writeAndFlush(cfm); // передаем конец файла});
                         // System.out.println("Writing eof");
                         semaphore.acquire();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                System.out.println("File not found or it is a directory");
                ctx.writeAndFlush(new ErrorMessage("File not found or it is a directory..."));
            }
        }

        if (msg instanceof ConfirmationMessage) {
            semaphore.release();
            //System.out.println("Confirmation received. Semaphore value is " + semaphore.availablePermits());
        }
        if (msg instanceof ChunkedFileMessage) {
            //System.out.println("ChunkedFileMessage received");
            // здесь мы принимаем файл от клиента
            ChunkedFileMessage cfm = (ChunkedFileMessage) msg;
            if (cfm.getBytesRead() != -1) {
                if (cfm.getBytesRead() == 0) {
                   // System.out.println("Creating file " + username + "/" + cfm.getFilename());
                    fos = new FileOutputStream(Server.serverStoragePath + username + "/" + cfm.getFilename());
                    chunkCounter = 0;
                    //Files.write(Paths.get(CloudBoxClient.clientStoragePath + userName + "/" + cfm.getFilename()), new byte[0], StandardOpenOption.CREATE);
                } else {
                    chunkCounter++;
                 //   System.out.println("Writing " + chunkCounter + " chunk to " + username + "/" + cfm.getFilename() + "; Bytes read = " + cfm.getBytesRead());
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
