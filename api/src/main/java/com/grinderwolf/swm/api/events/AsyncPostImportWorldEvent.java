package com.grinderwolf.swm.api.events;

import com.grinderwolf.swm.api.loaders.SlimeLoader;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;

public class AsyncPostImportWorldEvent extends Event {

  private static final HandlerList handlers = new HandlerList();
  private final File worldDir;
  private final String worldName;
  private final SlimeLoader slimeLoader;

  public AsyncPostImportWorldEvent(File worldDir, String worldName, SlimeLoader slimeLoader) {
    super(true);
    this.worldDir = Objects.requireNonNull(worldDir, "worldDir cannot be null");
    this.worldName = Objects.requireNonNull(worldName, "worldName cannot be null");
    this.slimeLoader = Objects.requireNonNull(slimeLoader, "slimeLoader cannot be null");
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlers;
  }

  public File getWorldDir() {
    return this.worldDir;
  }

  public String getWorldName() {
    return this.worldName;
  }

  public SlimeLoader getSlimeLoader() {
    return this.slimeLoader;
  }
}
