package com.grinderwolf.swm.api.utils;

import lombok.Getter;

/**
 * Credits to Minikloon for this class.
 *
 * Source: https://github.com/Minikloon/CraftyWorld/blob/master/crafty-common/src/main/kotlin/world/crafty/common/utils/NibbleArray.kt
 */
public class NibbleArray {

    @Getter
    private final byte[] backing;

    public NibbleArray(int size) {
        this(new byte[size / 2]);
    }

    public NibbleArray(byte[] backing) {
        this.backing = backing;
    }

    public int get(int index) {
        int value = this.backing[index / 2];

        return index % 2 == 0 ? value & 0xF : (value & 0xF0) >> 4;
    }

    public void set(int index, int value) {
        int nibble = value & 0xF;
        int halfIndex = index / 2;
        int previous = this.backing[halfIndex];

        if (index % 2 == 0) {
            this.backing[halfIndex] = (byte) (previous & 0xF0 | nibble);
        } else {
            this.backing[halfIndex] = (byte) (previous & 0xF | nibble << 4);
        }
    }
}
