package com.infernalsuite.aswm.serialization.slime.reader.impl.v19;

import com.infernalsuite.aswm.serialization.slime.reader.impl.SimpleWorldFormat;

public interface v1_9WorldFormat {

    SimpleWorldFormat<v1_9SlimeWorld> FORMAT = new SimpleWorldFormat<>(new v1_9SlimeConverter(), new v1_9SlimeWorldDeserializer());

}
