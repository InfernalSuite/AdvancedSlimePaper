package com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.upgrade;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.StringTag;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.Upgrade;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.v1_9SlimeChunk;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.v1_9SlimeWorld;

public class v1_9WorldUpgrade implements Upgrade {

    private static final JsonParser PARSER = new JsonParser();

    @Override
    public void upgrade(v1_9SlimeWorld world) {
        // In 1.9, all signs must be formatted using JSON
        for (v1_9SlimeChunk chunk : world.chunks.values()) {
            for (CompoundTag entityTag : chunk.tileEntities) {
                String type = entityTag.getAsStringTag("id").get().getValue();

                if (type.equals("Sign")) {
                    CompoundMap map = entityTag.getValue();

                    for (int i = 1; i < 5; i++) {
                        String id = "Text" + i;

                        map.put(id, new StringTag(id, fixJson(entityTag.getAsStringTag(id).map(StringTag::getValue).orElse(null))));
                    }
                }
            }
        }
    }

    private static String fixJson(String value) {
        if (value == null || value.equalsIgnoreCase("null") || value.isEmpty()) {
            return "{\"text\":\"\"}";
        }

        try {
            PARSER.parse(value);
        } catch (JsonSyntaxException ex) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("text", value);

            return jsonObject.toString();
        }

        return value;
    }
}
