package com.infernalsuite.aswm.loaders.redis.util;

import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StringByteCodec implements RedisCodec<String, byte[]> {

    public static final StringByteCodec INSTANCE = new StringByteCodec();
    private static final byte[] EMPTY = new byte[0];
    private final Charset charset = StandardCharsets.UTF_8;

    @Override
    public String decodeKey(final ByteBuffer bytes) {
        return charset.decode(bytes).toString();
    }

    @Override
    public byte[] decodeValue(final ByteBuffer bytes) {
        return getBytes(bytes);
    }

    @Override
    public ByteBuffer encodeKey(final String key) {
        return charset.encode(key);
    }

    @Override
    public ByteBuffer encodeValue(final byte[] value) {
        if (value == null) {
            return ByteBuffer.wrap(EMPTY);
        }

        return ByteBuffer.wrap(value);
    }

    private static byte[] getBytes(final ByteBuffer buffer) {
        final byte[] b = new byte[buffer.remaining()];
        buffer.get(b);
        return b;
    }

}