package com.infernalsuite.asp.serialization.slime.reader.impl.v12;

import com.infernalsuite.asp.serialization.slime.reader.impl.SimpleWorldFormat;

public interface v12WorldFormat {

    SimpleWorldFormat<com.infernalsuite.asp.api.world.SlimeWorld> FORMAT = new SimpleWorldFormat<>(data -> data, new v12SlimeWorldDeSerializer());

}
