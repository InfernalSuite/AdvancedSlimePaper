package com.grinderwolf.swm.api.events;

import com.grinderwolf.swm.api.loaders.SlimeLoader;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AsyncPreMigrateWorldEvent extends Event implements Cancellable {

  private static final HandlerList handlers = new HandlerList();
  private boolean isCancelled;
  private String worldName;
  private SlimeLoader currentLoader;
  private SlimeLoader newLoader;

  public AsyncPreMigrateWorldEvent(String worldName, SlimeLoader currentLoader, SlimeLoader newLoader) {
    super(true);
    this.worldName = Objects.requireNonNull(worldName, "worldName cannot be null");
    this.currentLoader = Objects.requireNonNull(currentLoader, "currentLoader cannot be null");
    this.newLoader = Objects.requireNonNull(newLoader, "newLoader cannot be null");
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

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

  public String getWorldName() {
    return this.worldName;
  }

  public void setWorldName(String worldName) {
    this.worldName = Objects.requireNonNull(worldName, "worldName cannot be null");
  }

  public SlimeLoader getCurrentLoader() {
    return this.currentLoader;
  }

  public void setCurrentLoader(SlimeLoader currentLoader) {
    this.currentLoader = Objects.requireNonNull(currentLoader, "currentLoader cannot be null");
  }

  public SlimeLoader getNewLoader() {
    return this.newLoader;
  }

  public void setNewLoader(SlimeLoader newLoader) {
    this.newLoader = Objects.requireNonNull(newLoader, "newLoader cannot be null");
  }
}
