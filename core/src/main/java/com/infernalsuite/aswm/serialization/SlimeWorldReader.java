package com.infernalsuite.aswm.serialization;

import com.infernalsuite.aswm.api.world.SlimeWorld;

public interface SlimeWorldReader<T> {

    SlimeWorld readFromData(T data);
}
