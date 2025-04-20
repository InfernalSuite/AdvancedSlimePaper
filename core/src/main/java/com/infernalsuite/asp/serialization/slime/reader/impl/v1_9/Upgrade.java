package com.infernalsuite.asp.serialization.slime.reader.impl.v1_9;

import com.infernalsuite.asp.api.SlimeDataConverter;

public interface Upgrade {

    void upgrade(v1_9SlimeWorld world, SlimeDataConverter converter);

}