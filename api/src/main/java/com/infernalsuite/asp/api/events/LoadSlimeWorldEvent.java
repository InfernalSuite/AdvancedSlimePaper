package com.infernalsuite.asp.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class LoadSlimeWorldEvent extends Event {

  private static final HandlerList handlers = new HandlerList();
  private final com.infernalsuite.asp.api.world.SlimeWorld slimeWorld;

  public LoadSlimeWorldEvent(com.infernalsuite.asp.api.world.SlimeWorld slimeWorld) {
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

  public com.infernalsuite.asp.api.world.SlimeWorld getSlimeWorld() {
    return slimeWorld;
  }
}
