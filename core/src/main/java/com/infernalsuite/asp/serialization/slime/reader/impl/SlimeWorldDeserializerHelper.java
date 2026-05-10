package com.infernalsuite.asp.serialization.slime.reader.impl;

import com.github.luben.zstd.ZstdInputStream;
import com.infernalsuite.asp.util.LimitedInputStream;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

public class SlimeWorldDeserializerHelper {

    public static DataInputStream openCompressedStream(DataInputStream stream) throws IOException {
        int compressedLength = stream.readInt();
        stream.readInt(); //Decompressed length, legacy

        LimitedInputStream limitedInputStream = new LimitedInputStream(stream, compressedLength);
        ZstdInputStream inputStream = new ZstdInputStream(limitedInputStream);
        return new DataInputStream(new BufferedInputStream(inputStream));
    }

    public static @NotNull CompoundBinaryTag readLimitedCompound(DataInputStream stream) throws IOException {
        int length = stream.readInt();
        if(length == 0) return CompoundBinaryTag.empty();

        LimitedInputStream limitedInputStream = new LimitedInputStream(stream, length);

        //Avoid a buffered input stream by casting to DataInput. Buffered Input Streams make the memory
        //usage explode (e.g. with buffered streams here 1,3gb; with a data input directly: 300mb)
        CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read((DataInput) new DataInputStream(limitedInputStream));

        //binary tag reading does not guarantee that the buffer is fully read. If we don't do this,
        //we might error out later
        limitedInputStream.drainRemaining();
        return tag;
    }

    public static @NotNull CompoundBinaryTag readCompressedCompound(DataInputStream stream) throws IOException {
        int compressedLength = stream.readInt();
        int decompressedLength = stream.readInt();

        if(decompressedLength == 0) return CompoundBinaryTag.empty();

        LimitedInputStream limitedInputStream = new LimitedInputStream(stream, compressedLength);
        try(ZstdInputStream zstd = new ZstdInputStream(limitedInputStream)) {

            //Avoid a buffered input stream by casting to DataInput. Buffered Input Streams make the memory
            //usage explode (e.g. with buffered streams here 1,3gb; with a data input directly: 300mb)
            CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read((DataInput) new DataInputStream(zstd));

            //binary tag reading does not guarantee that the buffer is fully read. If we don't do this,
            //we might error out later
            byte[] buffer = new byte[512];
            while (zstd.read(buffer) != -1) {}

            return tag;
        }
    }

}
