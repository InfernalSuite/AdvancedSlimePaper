package com.infernalsuite.asp.util;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InputStream;

/**
 * An {@link InputStream} that reads at most {@code limit} bytes from an underlying stream,
 * then signals end-of-stream — leaving the underlying stream open and positioned immediately
 * after the last byte of the limit.
 *
 * <h2>Close behaviour</h2>
 * <p>{@link #close()} intentionally does <em>not</em> close the underlying stream. Instead, it
 * calls {@link #drainRemaining()}, which reads and discards any bytes that the caller did not
 * consume. This ensures two invariants:
 * <ol>
 *   <li><b>The underlying stream stays open.</b> Closing it would destroy the underlying stream, making further reads impossible.</li>
 *   <li><b>The underlying stream is left at a predictable position.</b> If {@code close()} were
 *       called without draining, the unconsumed bytes would be seen by the next reader as the
 *       start of the following data block, silently corrupting the data.</li>
 * </ol>
 *
 * <p><b>Note:</b> The caller is responsible for closing the underlying stream once all messages
 * have been processed.
 */
public class LimitedInputStream extends InputStream {
    private final InputStream in;
    private int remaining;

    public LimitedInputStream(InputStream in, int limit) {
        Preconditions.checkNotNull(in, "Input stream cannot be null");
        Preconditions.checkArgument(limit >= 0, "Limit must be non-negative");
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

    @Override
    public void close() throws IOException {
        drainRemaining();
    }
}
