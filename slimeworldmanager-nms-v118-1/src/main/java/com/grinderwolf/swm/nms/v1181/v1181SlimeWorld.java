package com.grinderwolf.swm.nms.v1181;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.SlimeLogger;
import com.grinderwolf.swm.nms.SlimeNMS;
import com.grinderwolf.swm.nms.world.AbstractSlimeNMSWorld;
import com.grinderwolf.swm.nms.world.ChunkSerialization;
import com.grinderwolf.swm.nms.world.SlimeLoadedWorld;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class v1181SlimeWorld extends AbstractSlimeNMSWorld {

    private static final InternalPlugin INTERNAL_PLUGIN = new InternalPlugin();

    private CustomWorldServer handle;

    public v1181SlimeWorld(SlimeNMS nms, byte version, SlimeLoader loader, String name,
                           Long2ObjectOpenHashMap<SlimeChunk> chunks, CompoundTag extraData,
                           SlimePropertyMap propertyMap, boolean readOnly, boolean lock,
                           Long2ObjectOpenHashMap<List<CompoundTag>> savedEntities) {
        super(version, loader, name, chunks, extraData, propertyMap, readOnly, lock, savedEntities, nms);
    }

    public void setHandle(CustomWorldServer handle) {
        this.handle = handle;
    }

    @Override
    public SlimeLoadedWorld createSlimeWorld(String worldName, SlimeLoader loader, boolean lock) {
        return new v1181SlimeWorld(nms, version, loader == null ? this.loader : loader, worldName, new Long2ObjectOpenHashMap<>(chunks), extraData.clone(),
                propertyMap, loader == null, lock, entities);
    }

    @Override
    public CompletableFuture<ChunkSerialization> serializeChunks(List<SlimeChunk> chunks, byte worldVersion) throws IOException {
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        List<Runnable> runnables = new ArrayList<>(chunks.size() + 1);
        List<CompoundTag> tileEntities = new ArrayList<>();
        List<CompoundTag> entities = new ArrayList<>();

        // Save entities
        runnables.add(() -> {
            if (handle != null) {
                SlimeLogger.debug("Saving entities");
                this.handle.entityManager.saveAll();
                for (List<CompoundTag> value : this.entities.values()) {
                    entities.addAll(value);
                }
            }
        });

        for (SlimeChunk chunk : chunks) {
            Runnable runnable = () -> {
                tileEntities.addAll(chunk.getTileEntities());

                try {
                    // Height Maps
                    byte[] heightMaps = serializeCompoundTag(chunk.getHeightMaps());
                    outStream.writeInt(heightMaps.length);
                    outStream.write(heightMaps);


                    // Chunk sections
                    SlimeChunkSection[] sections = chunk.getSections();

                    outStream.writeInt(chunk.getMinSection());
                    outStream.writeInt(chunk.getMaxSection());
                    outStream.writeInt(Math.toIntExact(Arrays.stream(sections).filter(Objects::nonNull).count()));

                    for (int i = 0; i < sections.length; i++) {
                        SlimeChunkSection section = sections[i];
                        if (section == null) { // Some versions can contain null sections, ignore those.
                            continue;
                        }

                        outStream.writeInt(i);

                        // Block Light
                        boolean hasBlockLight = section.getBlockLight() != null;
                        outStream.writeBoolean(hasBlockLight);

                        if (hasBlockLight) {
                            outStream.write(section.getBlockLight().getBacking());
                        }

                        // Block Data
                        byte[] serializedBlockStates = serializeCompoundTag(section.getBlockStatesTag());
                        outStream.writeInt(serializedBlockStates.length);
                        outStream.write(serializedBlockStates);

                        byte[] serializedBiomes = serializeCompoundTag(section.getBiomeTag());
                        outStream.writeInt(serializedBiomes.length);
                        outStream.write(serializedBiomes);

                        // Sky Light
                        boolean hasSkyLight = section.getSkyLight() != null;
                        outStream.writeBoolean(hasSkyLight);

                        if (hasSkyLight) {
                            outStream.write(section.getSkyLight().getBacking());
                        }
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };

            runnables.add(runnable);
        }


        // Force save the world if the server is currently stopping
        if (Bukkit.isStopping()) {
            if (!Bukkit.isPrimaryThread()) {
                throw new UnsupportedOperationException("Cannot save the world while the server is stopping async!");
            }

            for (Runnable completableFuture : runnables) {
                completableFuture.run();
            }
            return CompletableFuture.completedFuture(new ChunkSerialization(outByteStream.toByteArray(), tileEntities, entities));
        } else {
            CompletableFuture<ChunkSerialization> future = new CompletableFuture<>();

            Iterator<Runnable> futuresIterator = runnables.iterator();

            /*
            Create a task that saves chunks for at the most 200 ms per tick.
             */
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    long timeSaved = 0;
                    long capturedTime = System.currentTimeMillis();

                    // 200 max ms on one tick for saving OR if the server is stopping force it to finish OR if it's on main thread to avoid deadlock
                    while (futuresIterator.hasNext() && (timeSaved < 200 || Bukkit.isStopping() || Bukkit.isPrimaryThread())) {
                        futuresIterator.next().run();
                        timeSaved += System.currentTimeMillis() - capturedTime;
                    }

                    // Once it is empty, complete the future and stop it from executing further.
                    if (!futuresIterator.hasNext()) {
                        future.complete(new ChunkSerialization(outByteStream.toByteArray(), tileEntities, entities));
                        try {
                            cancel();
                        } catch (Exception ignored) { // Errors if the task is not schedule yet, so just ignore it
                        }
                    }
                }
            };

            // If running on main thread, save it all to avoid a possible deadlock
            if (Bukkit.isPrimaryThread()) {
                runnable.run();
            }

            // If there is still more to complete, start the task to begin saving on next ticks
            if (!future.isDone()) {
                runnable.runTaskTimer(INTERNAL_PLUGIN, 0, 1);
            }

            return future;
        }
    }

}
