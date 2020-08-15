package com.zettro.java.cloudbox.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public class ChunkedFileMessage extends AbstractMessage {

    private static final long serialVersionUID = 2352661013718553948L;

    private final String filename;
    private final byte[] data;
    private int bytesRead = 0;
    transient private final FileInputStream fis;

    public String getFileName() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }

    public ChunkedFileMessage(Path path, int bufferSize) throws IOException {
        filename = path.getFileName().toString();
        fis = new FileInputStream(path.toFile());
        data = new byte[bufferSize];
    }

    public int readNextChunk() throws IOException {
        bytesRead = fis.read(data);
        return bytesRead;
    }

    public int getBytesRead() {
        return bytesRead;
    }

    public void close() throws IOException {
        fis.close();
    }
}
