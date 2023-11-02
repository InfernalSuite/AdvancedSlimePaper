package com.infernalsuite.aswm.serialization.slime.reader.impl.v11;

import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.serialization.slime.reader.impl.SimpleWorldFormat;

public interface v11WorldFormat {

    SimpleWorldFormat<SlimeWorld> FORMAT = new SimpleWorldFormat<>(data -> data, new v11SlimeWorldDeSerializer());

}
