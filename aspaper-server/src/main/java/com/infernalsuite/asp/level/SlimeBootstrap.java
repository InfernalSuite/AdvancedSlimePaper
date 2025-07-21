package com.infernalsuite.asp.level;

import com.infernalsuite.asp.api.world.SlimeWorld;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public record SlimeBootstrap(
        SlimeWorld initial
) {

    public UUID generateId() {
        return UUID.nameUUIDFromBytes(("SlimeWorld" + initial.getName()).getBytes(StandardCharsets.UTF_8));
    }

}
