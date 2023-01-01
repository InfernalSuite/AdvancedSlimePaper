package com.infernalsuite.aswm.serialization;

import com.infernalsuite.aswm.world.SlimeWorld;

public interface SlimeWorldReader<T> {

    SlimeWorld readFromData(T data);
}
