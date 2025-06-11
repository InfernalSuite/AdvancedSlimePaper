package com.infernalsuite.asp;

import com.infernalsuite.asp.api.SlimeDataConverter;
import com.infernalsuite.asp.api.SlimeNMSBridge;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.level.SlimeBootstrap;
import com.infernalsuite.asp.level.SlimeInMemoryWorld;
import com.infernalsuite.asp.level.SlimeLevelInstance;
import com.mojang.serialization.Lifecycle;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.CommandStorage;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataTypeRegistry;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class SlimeNMSBridgeImpl implements SlimeNMSBridge {

    private static final CraftPersistentDataTypeRegistry REGISTRY = new CraftPersistentDataTypeRegistry();
    private static final SimpleDataFixerConverter DATA_FIXER_CONVERTER = new SimpleDataFixerConverter();

    private SlimeWorld defaultWorld;
    private SlimeWorld defaultNetherWorld;
    private SlimeWorld defaultEndWorld;

    public static SlimeNMSBridgeImpl instance() {
        return (SlimeNMSBridgeImpl) SlimeNMSBridge.instance();
    }

    @Override
    public void extractCraftPDC(PersistentDataContainer source, CompoundBinaryTag.Builder builder) {
        if (source instanceof CraftPersistentDataContainer craftPDC) {
            craftPDC.getRaw().forEach((key, nmsTag) -> builder.put(key, Converter.convertTag(nmsTag)));
        } else {
            throw new IllegalArgumentException("PersistentDataContainer is not a CraftPersistentDataContainer");
        }
    }

    @Override
    public SlimeDataConverter getSlimeDataConverter() {
        return DATA_FIXER_CONVERTER;
    }

    @Override
    public boolean loadOverworldOverride() {
        if (defaultWorld == null) {
            return false;
        }

        // See MinecraftServer loading logic
        // Some stuff is needed when loading overworld world
        SlimeLevelInstance instance = ((SlimeInMemoryWorld) this.loadInstance(defaultWorld, Level.OVERWORLD)).getInstance();
        DimensionDataStorage worldpersistentdata = instance.getDataStorage();
        instance.getCraftServer().scoreboardManager = new org.bukkit.craftbukkit.scoreboard.CraftScoreboardManager(instance.getServer(), instance.getScoreboard());
        instance.getServer().commandStorage = new CommandStorage(worldpersistentdata);

        return true;
    }

    @Override
    public boolean loadNetherOverride() {
        if (defaultNetherWorld == null) {
            return false;
        }

        this.loadInstance(defaultNetherWorld, Level.NETHER);

        return true;
    }

    @Override
    public boolean loadEndOverride() {
        if (defaultEndWorld == null) {
            return false;
        }

        this.loadInstance(defaultEndWorld, Level.END);

        return true;
    }

    /**
     * Sets the default worlds for the server.<br>
     * <b>NOTE: These worlds should be unloaded!</b>
     * @param normalWorld The default overworld
     * @param netherWorld The default nether
     * @param endWorld The default end
     */
    @Override
    public void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld) {
        if (normalWorld != null) {
            normalWorld.getPropertyMap().setValue(SlimeProperties.ENVIRONMENT, World.Environment.NORMAL.toString().toLowerCase());
            defaultWorld = normalWorld;
        }

        if (netherWorld != null) {
            netherWorld.getPropertyMap().setValue(SlimeProperties.ENVIRONMENT, World.Environment.NETHER.toString().toLowerCase());
            defaultNetherWorld = netherWorld;
        }

        if (endWorld != null) {
            endWorld.getPropertyMap().setValue(SlimeProperties.ENVIRONMENT, World.Environment.THE_END.toString().toLowerCase());
            defaultEndWorld = endWorld;
        }

    }

    @Override
    public SlimeWorldInstance loadInstance(SlimeWorld slimeWorld) {
        return this.loadInstance(slimeWorld, null);
    }

    public SlimeWorldInstance loadInstance(SlimeWorld slimeWorld, @Nullable ResourceKey<Level> dimensionOverride) {
        String worldName = slimeWorld.getName();

        if (Bukkit.getWorld(worldName) != null) {
            throw new IllegalArgumentException("World " + worldName + " already exists! Maybe it's an outdated SlimeWorld object?");
        }

        SlimeLevelInstance server = createCustomWorld(slimeWorld, dimensionOverride);
        registerWorld(server);
        return server.getSlimeInstance();
    }

    @Override
    public SlimeWorldInstance getInstance(World world) {
        CraftWorld craftWorld = (CraftWorld) world;

        if (!(craftWorld.getHandle() instanceof SlimeLevelInstance worldServer)) {
            return null;
        }

        return worldServer.getSlimeInstance();
    }


    @Override
    public int getCurrentVersion() {
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }

    public void registerWorld(SlimeLevelInstance server) {
        MinecraftServer mcServer = MinecraftServer.getServer();
        mcServer.initWorld(server, server.serverLevelData, mcServer.getWorldData(), server.serverLevelData.worldGenOptions());

        mcServer.addLevel(server);
    }

    private SlimeLevelInstance createCustomWorld(SlimeWorld world, @Nullable ResourceKey<Level> dimensionOverride) {
        SlimeBootstrap bootstrap = new SlimeBootstrap(world);
        String worldName = world.getName();

        PrimaryLevelData worldDataServer = createWorldData(world);
        World.Environment environment = getEnvironment(world);
        ResourceKey<LevelStem> dimension = switch (environment) {
            case NORMAL -> LevelStem.OVERWORLD;
            case NETHER -> LevelStem.NETHER;
            case THE_END -> LevelStem.END;
            default -> throw new IllegalArgumentException("Unknown dimension supplied");
        };

        ResourceKey<Level> worldKey = dimensionOverride == null ? ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(worldName.toLowerCase(Locale.ENGLISH))) : dimensionOverride;
        LevelStem stem = MinecraftServer.getServer().registries().compositeAccess().lookupOrThrow(Registries.LEVEL_STEM).get(dimension).orElseThrow().value();

        SlimeLevelInstance level;

        try {
            level = new SlimeLevelInstance(bootstrap, worldDataServer, worldKey, dimension, stem, environment);
        } catch (IOException ex) {
            throw new RuntimeException(ex); // TODO do something better with this?
        }

        // level.setReady(true);
        level.setSpawnSettings(world.getPropertyMap().getValue(SlimeProperties.ALLOW_MONSTERS));

        CompoundTag nmsExtraData = (CompoundTag) Converter.convertTag(CompoundBinaryTag.from(world.getExtraData()));

        //Attempt to read PDC
        if (nmsExtraData.get("BukkitValues") != null) level.getWorld().readBukkitValues(nmsExtraData.get("BukkitValues"));

        return level;
    }

    private World.Environment getEnvironment(SlimeWorld world) {
        return World.Environment.valueOf(world.getPropertyMap().getValue(SlimeProperties.ENVIRONMENT).toUpperCase());
    }

    private PrimaryLevelData createWorldData(SlimeWorld world) {
        MinecraftServer mcServer = MinecraftServer.getServer();
        DedicatedServerProperties serverProps = ((DedicatedServer) mcServer).getProperties();
        String worldName = world.getName();
        WorldLoader.DataLoadContext context = mcServer.worldLoader;

        LevelSettings worldsettings = new LevelSettings(worldName, serverProps.gamemode, false, serverProps.difficulty,
                true, new GameRules(context.dataConfiguration().enabledFeatures()), mcServer.worldLoader.dataConfiguration());

        WorldOptions worldoptions = new WorldOptions(0, false, false);

        PrimaryLevelData data = new PrimaryLevelData(worldsettings, worldoptions, PrimaryLevelData.SpecialWorldProperty.FLAT, Lifecycle.stable());
        data.checkName(worldName);
        data.setModdedInfo(mcServer.getServerModName(), mcServer.getModdedStatus().shouldReportAsModified());
        data.setInitialized(true);

        return data;
    }

}
