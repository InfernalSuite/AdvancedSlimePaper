package com.infernalsuite.asp.util;

import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends InputStream {
    private final InputStream in;
    private int remaining;

    public LimitedInputStream(InputStream in, int limit) {
        this.in = in;
        this.remaining = limit;
    }

    @Override
    public int read() throws IOException {
        if (remaining <= 0) return -1;
        int b = in.read();
        if (b != -1) remaining--;
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (remaining <= 0) return -1;
        len = Math.min(len, remaining);
        int read = in.read(b, off, len);
        if (read > 0) remaining -= read;
        return read;
    }

    public void drainRemaining() {
        byte[] buffer = new byte[512];
        try {
            while (remaining > 0) {
                int read = read(buffer, 0, Math.min(buffer.length, remaining));
                if (read == -1) break;
            }
        } catch (IOException ignored) {
        }
    }
}
