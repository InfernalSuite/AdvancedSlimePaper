package com.infernalsuite.aswm.api.world.properties;

import com.infernalsuite.aswm.api.world.properties.type.SlimePropertyBoolean;
import com.infernalsuite.aswm.api.world.properties.type.SlimePropertyFloat;
import com.infernalsuite.aswm.api.world.properties.type.SlimePropertyInt;
import com.infernalsuite.aswm.api.world.properties.type.SlimePropertyString;
import org.jetbrains.annotations.ApiStatus;

/**
 * Class with all existing slime world properties.
 */
public class SlimeProperties {

    /**
     * The X coordinate of the world spawn
     */
    public static final SlimeProperty<Integer> SPAWN_X = new SlimePropertyInt("spawnX", 0);

    /**
     * The Y coordinate of the world spawn
     */
    public static final SlimeProperty<Integer> SPAWN_Y = new SlimePropertyInt("spawnY", 255);

    /**
     * The Z coordinate of the world spawn
     */
    public static final SlimeProperty<Integer> SPAWN_Z = new SlimePropertyInt("spawnZ", 0);

    /**
     * The yaw of the world spawn
     */
    public static final SlimeProperty<Float> SPAWN_YAW = new SlimePropertyFloat("spawnYaw", 0.0f);

    /**
     * The difficulty set for the world
     */
    public static final SlimeProperty<String> DIFFICULTY = new SlimePropertyString("difficulty", "peaceful", (value) ->
        value.equalsIgnoreCase("peaceful") || value.equalsIgnoreCase("easy")
            || value.equalsIgnoreCase("normal") || value.equalsIgnoreCase("hard")
    );

    /**
     * Whether monsters are allowed to spawn at night or in the dark
     */
    public static final SlimeProperty<Boolean> ALLOW_MONSTERS = new SlimePropertyBoolean("allowMonsters", true);

    /**
     * Whether peaceful animals are allowed to spawn
     */
    public static final SlimeProperty<Boolean> ALLOW_ANIMALS = new SlimePropertyBoolean("allowAnimals", true);

    /**
     * Whether the dragon battle should be enabled in end worlds
     */
    public static final SlimeProperty<Boolean> DRAGON_BATTLE = new SlimePropertyBoolean("dragonBattle", false);

    /**
     * Whether PVP combat is allowed
     */
    public static final SlimeProperty<Boolean> PVP = new SlimePropertyBoolean("pvp", true);

    /**
     * The environment of the world
     */
    public static final SlimeProperty<String> ENVIRONMENT = new SlimePropertyString("environment", "normal", (value) ->
        value.equalsIgnoreCase("normal") || value.equalsIgnoreCase("nether") || value.equalsIgnoreCase("the_end")
    );

    /**
     * The type of world
     */
    public static final SlimeProperty<String> WORLD_TYPE = new SlimePropertyString("worldtype", "default", (value) ->
        value.equalsIgnoreCase("default") || value.equalsIgnoreCase("flat") || value.equalsIgnoreCase("large_biomes")
            || value.equalsIgnoreCase("amplified") || value.equalsIgnoreCase("customized")
            || value.equalsIgnoreCase("debug_all_block_states") || value.equalsIgnoreCase("default_1_1")
    );

    /**
     * The default biome generated in empty chunks
     */
    public static final SlimeProperty<String> DEFAULT_BIOME = new SlimePropertyString("defaultBiome", "minecraft:plains");

    @ApiStatus.Experimental
    public static final SlimeProperty<Boolean> SHOULD_LIMIT_SAVE = new SlimePropertyBoolean("hasSaveBounds", false);

    @ApiStatus.Experimental
    public static final SlimeProperty<Integer> SAVE_MIN_X = new SlimePropertyInt("saveMinX", 0);
    @ApiStatus.Experimental
    public static final SlimeProperty<Integer> SAVE_MIN_Z = new SlimePropertyInt("saveMinZ", 0);

    @ApiStatus.Experimental
    public static final SlimeProperty<Integer> SAVE_MAX_X = new SlimePropertyInt("saveMaxX", 0);
    @ApiStatus.Experimental
    public static final SlimeProperty<Integer> SAVE_MAX_Z = new SlimePropertyInt("saveMaxZ", 0);

    @ApiStatus.Experimental
    public static final SlimeProperty<String> CHUNK_PRUNING = new SlimePropertyString("pruning", "aggressive", (value) ->
           value.equalsIgnoreCase("aggressive") || value.equalsIgnoreCase("never")
    );



    @ApiStatus.Experimental
    public static final SlimeProperty<Integer> CHUNK_SECTION_MIN = new SlimePropertyInt("chunkSectionMin", -4);
    @ApiStatus.Experimental
    public static final SlimeProperty<Integer> CHUNK_SECTION_MAX = new SlimePropertyInt("chunkSectionMin", 19);


}
