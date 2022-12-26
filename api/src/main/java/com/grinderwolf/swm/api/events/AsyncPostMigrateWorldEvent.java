package com.grinderwolf.swm.api.events;

import com.grinderwolf.swm.api.loaders.SlimeLoader;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AsyncPostMigrateWorldEvent extends Event {

  private static final HandlerList handlers = new HandlerList();
  private final String worldName;
  private final SlimeLoader currentLoader;
  private final SlimeLoader newLoader;

  public AsyncPostMigrateWorldEvent(String worldName, SlimeLoader currentLoader, SlimeLoader newLoader) {
    super(true);
    this.worldName = Objects.requireNonNull(worldName, "worldName cannot be null");
    this.currentLoader = Objects.requireNonNull(currentLoader, "currentLoader cannot be null");
    this.newLoader = Objects.requireNonNull(newLoader, "newLoader cannot be null");
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlers;
  }

  public String getWorldName() {
    return this.worldName;
  }

  public SlimeLoader getCurrentLoader() {
    return this.currentLoader;
  }

  public SlimeLoader getNewLoader() {
    return this.newLoader;
  }
}
