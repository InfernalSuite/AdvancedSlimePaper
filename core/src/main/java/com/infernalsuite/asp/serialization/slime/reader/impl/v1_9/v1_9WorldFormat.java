package com.infernalsuite.asp.serialization.slime.reader.impl.v1_9;

public interface v1_9WorldFormat {

    com.infernalsuite.asp.serialization.slime.reader.impl.SimpleWorldFormat<v1_9SlimeWorld> FORMAT = new com.infernalsuite.asp.serialization.slime.reader.impl.SimpleWorldFormat<>(new v1_v9SlimeConverter(), new v1_9SlimeWorldDeserializer());

}
