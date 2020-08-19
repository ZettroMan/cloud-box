package com.zettro.java.cloudbox.server;

import com.zettro.java.cloudbox.common.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Semaphore;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private final String username;
    private FileOutputStream fos = null;
    private int chunkCounter = 0;
    private boolean transferMode = false;
    private Path currentPath;
    private final Path rootPath;
    Semaphore semaphore = new Semaphore(1);

    Logger stdLogger = Server.stdLogger;
    String serverStoragePath = Server.serverStoragePath;

    public MainHandler(String username) {
        this.username = username;
        rootPath = Paths.get(serverStoragePath, username);
        currentPath = rootPath;

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FileRequest) {
            if (transferMode) {
                stdLogger.info("File transfer in progress. Request ignored!");
                return;
            }
            FileRequest fr = (FileRequest) msg;
            Path filePath = Paths.get(currentPath.toString(), fr.getFileName());
            if (Files.exists(filePath) && (!Files.isDirectory(filePath))) {
                semaphore = new Semaphore(1);
                transferMode = true;
                new Thread(() -> {
                    try {
                        ChunkedFileMessage cfm = new ChunkedFileMessage(filePath, 1024 * 1024);
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
                stdLogger.info("File not found or it is a directory");
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
                    fos = new FileOutputStream(currentPath.toString() + "\\" + cfm.getFileName());
                    chunkCounter = 0;
                } else {
                    chunkCounter++;
                    fos.write(cfm.getData(), 0, cfm.getBytesRead());
                }
            } else {
                // fos.flush(); - скорее всего это лишнее
                fos.close();
                fos = null;
            }
        }
        if (msg instanceof FilesListRequest) {
            if (transferMode) {
                stdLogger.info("File transfer in progress. Request ignored!");
                return;
            }
            FilesListMessage flm = new FilesListMessage(rootPath.relativize(currentPath).toString());
            Files.list(currentPath)
                    .map(FileInfo::new).forEach(flm::addFileInfo);
            ctx.writeAndFlush(flm);
        }
        if (msg instanceof TraverseToFolderMessage) {
            if (transferMode) {
                stdLogger.info("File transfer in progress. Request ignored!");
                return;
            }
            TraverseToFolderMessage ttfm = (TraverseToFolderMessage) msg;
            // если находимся в самом верхнем каталоге - то выше уже не поднимаемся
            if(ttfm.getFoldername().equals("..") && currentPath.equals(rootPath)) return;

            currentPath = currentPath.resolve(Paths.get(ttfm.getFoldername())).normalize().toAbsolutePath();
            FilesListMessage flm = new FilesListMessage(rootPath.relativize(currentPath).toString());
            Files.list(currentPath)
                    .map(FileInfo::new).forEach(flm::addFileInfo);
            ctx.writeAndFlush(flm);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
