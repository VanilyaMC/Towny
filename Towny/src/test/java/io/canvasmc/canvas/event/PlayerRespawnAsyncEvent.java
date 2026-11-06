package io.canvasmc.canvas.event;

import io.papermc.paper.event.player.AbstractRespawnEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Test stand-in with the same shape as Canvas 26.3's PlayerRespawnAsyncEvent, which is not on Maven.
 */
public class PlayerRespawnAsyncEvent extends AbstractRespawnEvent {
	private static final HandlerList HANDLERS = new HandlerList();

	public PlayerRespawnAsyncEvent(Player player, Location respawnLocation) {
		super(player, respawnLocation, false, false, false, PlayerRespawnEvent.RespawnReason.DEATH);
	}

	public void setRespawnLocation(Location respawnLocation) {
		this.respawnLocation = respawnLocation;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}
}
