package com.infernalsuite.asp.serialization.slime.reader.impl.v13;

import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.serialization.slime.reader.impl.SimpleWorldFormat;

public interface v13WorldFormat {

    SimpleWorldFormat<SlimeWorld> FORMAT = new SimpleWorldFormat<>(data -> data, new v13SlimeWorldDeSerializer());

}
