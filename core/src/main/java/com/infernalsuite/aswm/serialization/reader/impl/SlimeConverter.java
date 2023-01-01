package com.infernalsuite.aswm.serialization.reader.impl;

import com.infernalsuite.aswm.world.SlimeWorld;

public interface SlimeConverter<T> {

    SlimeWorld runConversion(T data);
}
