package com.infernalsuite.aswm.serialization.reader.impl.v10;

import com.infernalsuite.aswm.world.SlimeWorld;
import com.infernalsuite.aswm.serialization.reader.impl.SimpleWorldFormat;

public interface v10WorldFormat {

    // Latest, returns same
    SimpleWorldFormat<SlimeWorld> FORMAT = new SimpleWorldFormat<>(data -> data, new v10SlimeWorldDeSerializer());

}
