package com.infernalsuite.asp.serialization.slime.reader.impl.v12;

public interface v12WorldFormat {

    com.infernalsuite.asp.serialization.slime.reader.impl.SimpleWorldFormat<com.infernalsuite.asp.api.world.SlimeWorld> FORMAT = new com.infernalsuite.asp.serialization.slime.reader.impl.SimpleWorldFormat<>(data -> data, new v12SlimeWorldDeSerializer());

}
