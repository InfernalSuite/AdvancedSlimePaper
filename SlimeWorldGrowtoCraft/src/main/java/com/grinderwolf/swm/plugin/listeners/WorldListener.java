package com.grinderwolf.swm.plugin.listeners;

import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.Utils.WorldManager;
import com.infernalsuite.aswm.api.SlimeNMSBridge;
import com.infernalsuite.aswm.api.world.SlimeWorldInstance;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.List;

public class WorldListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        SlimeWorldInstance world = SlimeNMSBridge.instance().getInstance(event.getWorld());

        if (world != null) {
            Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> WorldManager.unlockWorld(world.getSlimeWorldMirror()));
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        Player target = e.getPlayer();
        World world = target.getWorld();
        String[] worldName = new String[]{world.getName()};

        List<Player> players = world.getPlayers();
        players.remove(target);
        if (players.isEmpty()) WorldManager.unloadWorld(worldName);
    }
}