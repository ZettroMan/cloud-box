package com.zettro.java.cloudbox.server;

import com.zettro.java.cloudbox.common.*;
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
    private boolean transferMode = false;
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
        if (msg instanceof FileRequest) {
            if (transferMode) {
                System.out.println("File transfer in progress. Request ignored!");
                return;
            }
            FileRequest fr = (FileRequest) msg;
            String filename = Server.serverStoragePath + username + "/" + fr.getFilename();
            if (Files.exists(Paths.get(filename)) && (!Files.isDirectory(Paths.get(filename)))) {
                semaphore = new Semaphore(1);
                transferMode = true;
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
                            semaphore.acquire();
                        }
                        ctx.writeAndFlush(cfm); // передаем конец файла});
                        semaphore.acquire();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    transferMode = false;
                }).start();
            } else {
                System.out.println("File not found or it is a directory");
                ctx.writeAndFlush(new ErrorMessage("File not found or it is a directory..."));
            }
        }

        if (msg instanceof ConfirmationMessage) {
            semaphore.release();
        }
        if (msg instanceof ChunkedFileMessage) {
            //System.out.println("ChunkedFileMessage received");
            // здесь мы принимаем файл от клиента
            ChunkedFileMessage cfm = (ChunkedFileMessage) msg;
            if (cfm.getBytesRead() != -1) {
                if (cfm.getBytesRead() == 0) {
                    fos = new FileOutputStream(Server.serverStoragePath + username + "/" + cfm.getFilename());
                    chunkCounter = 0;
                } else {
                    chunkCounter++;
                    fos.write(cfm.getData(), 0, cfm.getBytesRead());
                }
            } else {
                fos.flush();
                fos.close();
                fos = null;
            }
        }
        if (msg instanceof FilesListRequest) {
            if (transferMode) {
                System.out.println("File transfer in progress. Request ignored!");
                return;
            }
            FilesListMessage flm = new FilesListMessage();
            Files.list(Paths.get(Server.serverStoragePath + username + "/"))
                    .filter(p -> !Files.isDirectory(p))
                    .map(p -> new FileInfo(p.getFileName().toString()))
                    .forEach(flm::addFileInfo);
            ctx.writeAndFlush(flm);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
