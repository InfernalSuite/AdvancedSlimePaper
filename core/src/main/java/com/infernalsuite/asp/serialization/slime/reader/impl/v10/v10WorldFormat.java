package com.infernalsuite.asp.serialization.slime.reader.impl.v10;

public interface v10WorldFormat {

    // Latest, returns same
    com.infernalsuite.asp.serialization.slime.reader.impl.SimpleWorldFormat<com.infernalsuite.asp.api.world.SlimeWorld> FORMAT = new com.infernalsuite.asp.serialization.slime.reader.impl.SimpleWorldFormat<>(data -> data, new v10SlimeWorldDeSerializer());

}
