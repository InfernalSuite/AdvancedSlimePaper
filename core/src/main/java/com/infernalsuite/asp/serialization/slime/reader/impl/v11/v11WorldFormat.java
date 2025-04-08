package com.infernalsuite.asp.serialization.slime.reader.impl.v11;

public interface v11WorldFormat {

    com.infernalsuite.asp.serialization.slime.reader.impl.SimpleWorldFormat<com.infernalsuite.asp.api.world.SlimeWorld> FORMAT = new com.infernalsuite.asp.serialization.slime.reader.impl.SimpleWorldFormat<>(data -> data, new v11SlimeWorldDeSerializer());

}
