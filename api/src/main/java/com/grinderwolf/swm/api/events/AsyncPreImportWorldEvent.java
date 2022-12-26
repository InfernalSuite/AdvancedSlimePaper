package com.grinderwolf.swm.api.events;

import com.grinderwolf.swm.api.loaders.SlimeLoader;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;

public class AsyncPreImportWorldEvent extends Event implements Cancellable {

  private static final HandlerList handlers = new HandlerList();
  private boolean isCancelled;
  private File worldDir;
  private String worldName;
  private SlimeLoader slimeLoader;

  public AsyncPreImportWorldEvent(File worldDir, String worldName, SlimeLoader slimeLoader) {
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

  @Override
  public boolean isCancelled() {
    return this.isCancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.isCancelled = cancelled;
  }

  public File getWorldDir() {
    return this.worldDir;
  }

  public void setWorldDir(File worldDir) {
    this.worldDir = Objects.requireNonNull(worldDir, "worldDir cannot be null");
  }

  public String getWorldName() {
    return this.worldName;
  }

  public void setWorldName(String worldName) {
    this.worldName = Objects.requireNonNull(worldName, "worldName cannot be null");
  }

  public SlimeLoader getSlimeLoader() {
    return this.slimeLoader;
  }

  public void setSlimeLoader(SlimeLoader slimeLoader) {
    this.slimeLoader = Objects.requireNonNull(slimeLoader, "slimeLoader cannot be null");
  }
}
