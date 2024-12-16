package com.infernalsuite.asp.serialization;

import com.infernalsuite.asp.api.world.SlimeWorld;

public interface SlimeWorldReader<T> {

    SlimeWorld readFromData(T data);
}
