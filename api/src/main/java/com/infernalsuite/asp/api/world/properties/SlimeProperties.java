package com.infernalsuite.asp.api.world.properties;

import com.infernalsuite.asp.api.world.properties.type.SlimePropertyBoolean;
import com.infernalsuite.asp.api.world.properties.type.SlimePropertyFloat;
import com.infernalsuite.asp.api.world.properties.type.SlimePropertyInt;
import com.infernalsuite.asp.api.world.properties.type.SlimePropertyString;
import org.jetbrains.annotations.ApiStatus;

/**
 * Class with all existing slime world properties.
 */
public class SlimeProperties {

    /**
     * The X coordinate of the world spawn
     */
    public static final SlimePropertyInt SPAWN_X = SlimePropertyInt.create("spawnX", 0);

    /**
     * The Y coordinate of the world spawn
     */
    public static final SlimePropertyInt SPAWN_Y = SlimePropertyInt.create("spawnY", 255);

    /**
     * The Z coordinate of the world spawn
     */
    public static final SlimePropertyInt SPAWN_Z = SlimePropertyInt.create("spawnZ", 0);

    /**
     * The yaw of the world spawn
     */
    public static final SlimePropertyFloat SPAWN_YAW = SlimePropertyFloat.create("spawnYaw", 0.0f);

    /**
     * The difficulty set for the world
     */
    public static final SlimePropertyString DIFFICULTY = SlimePropertyString.create("difficulty", "peaceful", (value) ->
        value.equalsIgnoreCase("peaceful") || value.equalsIgnoreCase("easy")
            || value.equalsIgnoreCase("normal") || value.equalsIgnoreCase("hard")
    );

    /**
     * Whether monsters are allowed to spawn at night or in the dark
     */
    public static final SlimePropertyBoolean ALLOW_MONSTERS = SlimePropertyBoolean.create("allowMonsters", true);

    /**
     * Whether peaceful animals are allowed to spawn
     */
    public static final SlimePropertyBoolean ALLOW_ANIMALS = SlimePropertyBoolean.create("allowAnimals", true);

    /**
     * Whether the dragon battle should be enabled in end worlds
     */
    public static final SlimePropertyBoolean DRAGON_BATTLE = SlimePropertyBoolean.create("dragonBattle", false);

    /**
     * Whether PVP combat is allowed
     */
    public static final SlimePropertyBoolean PVP = SlimePropertyBoolean.create("pvp", true);

    /**
     * The environment of the world
     */
    public static final SlimePropertyString ENVIRONMENT = SlimePropertyString.create("environment", "normal", (value) ->
        value.equalsIgnoreCase("normal") || value.equalsIgnoreCase("nether") || value.equalsIgnoreCase("the_end")
    );

    /**
     * The type of world
     */
    public static final SlimePropertyString WORLD_TYPE = SlimePropertyString.create("worldtype", "default", (value) ->
        value.equalsIgnoreCase("default") || value.equalsIgnoreCase("flat") || value.equalsIgnoreCase("large_biomes")
            || value.equalsIgnoreCase("amplified") || value.equalsIgnoreCase("customized")
            || value.equalsIgnoreCase("debug_all_block_states") || value.equalsIgnoreCase("default_1_1")
    );

    /**
     * The default biome generated in empty chunks
     */
    public static final SlimePropertyString DEFAULT_BIOME = SlimePropertyString.create("defaultBiome", "minecraft:plains");

    @ApiStatus.Experimental
    public static final SlimePropertyBoolean SHOULD_LIMIT_SAVE = SlimePropertyBoolean.create("hasSaveBounds", false);

    @ApiStatus.Experimental
    public static final SlimePropertyInt SAVE_MIN_X = SlimePropertyInt.create("saveMinX", 0);
    @ApiStatus.Experimental
    public static final SlimePropertyInt SAVE_MIN_Z = SlimePropertyInt.create("saveMinZ", 0);

    @ApiStatus.Experimental
    public static final SlimePropertyInt SAVE_MAX_X = SlimePropertyInt.create("saveMaxX", 0);
    @ApiStatus.Experimental
    public static final SlimePropertyInt SAVE_MAX_Z = SlimePropertyInt.create("saveMaxZ", 0);

    @ApiStatus.Experimental
    public static final SlimePropertyString CHUNK_PRUNING = SlimePropertyString.create("pruning", "aggressive", (value) ->
           value.equalsIgnoreCase("aggressive") || value.equalsIgnoreCase("never")
    );



    @ApiStatus.Experimental
    public static final SlimePropertyInt CHUNK_SECTION_MIN = SlimePropertyInt.create("chunkSectionMin", -4);
    @ApiStatus.Experimental
    public static final SlimePropertyInt CHUNK_SECTION_MAX = SlimePropertyInt.create("chunkSectionMin", 19);


}
