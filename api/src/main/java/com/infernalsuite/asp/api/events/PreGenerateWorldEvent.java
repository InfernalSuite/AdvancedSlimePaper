package com.infernalsuite.asp.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PreGenerateWorldEvent extends Event implements Cancellable {

  private static final HandlerList handlers = new HandlerList();
  private boolean isCancelled;
  private com.infernalsuite.asp.api.world.SlimeWorld slimeWorld;

  public PreGenerateWorldEvent(com.infernalsuite.asp.api.world.SlimeWorld slimeWorld) {
    super(false);
    this.slimeWorld = Objects.requireNonNull(slimeWorld, "slimeWorld cannot be null");
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

  public com.infernalsuite.asp.api.world.SlimeWorld getSlimeWorld() {
    return this.slimeWorld;
  }

  public void setSlimeWorld(com.infernalsuite.asp.api.world.SlimeWorld slimeWorld) {
    this.slimeWorld = Objects.requireNonNull(slimeWorld, "slimeWorld cannot be null");
  }
}
