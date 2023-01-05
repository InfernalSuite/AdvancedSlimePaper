package com.grinderwolf.swm.plugin.listeners;

import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.log.Logging;
import com.infernalsuite.aswm.api.SlimeNMSBridge;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.SlimeWorldInstance;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;

import java.io.IOException;

public class WorldUnlocker implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        SlimeWorldInstance world = SlimeNMSBridge.instance().getInstance(event.getWorld());

        if (world != null) {
            Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> unlockWorld(world.getSlimeWorldMirror()));
        }
    }

    private void unlockWorld(SlimeWorld world) {
        try {
            if (world.getLoader() != null) {
                world.getLoader().unlockWorld(world.getName());
            }
        } catch (IOException ex) {
            Logging.error("Failed to unlock world " + world.getName() + ". Retrying in 5 seconds. Stack trace:");
            ex.printStackTrace();

            Bukkit.getScheduler().runTaskLaterAsynchronously(SWMPlugin.getInstance(), () -> unlockWorld(world), 100);
        } catch (UnknownWorldException e) {
        }
    }
}