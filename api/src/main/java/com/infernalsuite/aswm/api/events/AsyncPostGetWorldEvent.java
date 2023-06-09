package com.infernalsuite.aswm.api.events;

import com.infernalsuite.aswm.api.world.SlimeWorld;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AsyncPostGetWorldEvent extends Event {

  private static final HandlerList handlers = new HandlerList();
  private final SlimeWorld slimeWorld;

  public AsyncPostGetWorldEvent(SlimeWorld slimeWorld) {
    super(true);
    this.slimeWorld = Objects.requireNonNull(slimeWorld, "slimeWorld cannot be null");
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlers;
  }

  public SlimeWorld getSlimeWorld() {
    return slimeWorld;
  }
}
