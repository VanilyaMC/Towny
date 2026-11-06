package com.palmergames.bukkit.towny.listeners.version;

import io.canvasmc.canvas.event.PlayerRespawnAsyncEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class TownyCanvasEventsTests {

	@Test
	void respawnEventNameMatchesCanvas() {
		assertEquals(PlayerRespawnAsyncEvent.class.getName(), TownyCanvasEvents.ASYNC_RESPAWN_EVENT);
	}

	@Test
	void noRespawnHandleOffCanvas() {
		assertNull(TownyCanvasEvents.setRespawnLocationHandle("io.canvasmc.canvas.event.DoesNotExist"));
	}

	@Test
	void setterWritesRespawnLocation() {
		MethodHandle handle = TownyCanvasEvents.setRespawnLocationHandle(TownyCanvasEvents.ASYNC_RESPAWN_EVENT);
		assertNotNull(handle);

		PlayerRespawnAsyncEvent event = new PlayerRespawnAsyncEvent(mock(Player.class), new Location(null, 0, 64, 0));
		Location townSpawn = new Location(null, 100, 70, -20);
		List<Throwable> failures = new ArrayList<>();

		TownyCanvasEvents.respawnLocationSetter(event, handle, failures::add).accept(townSpawn);

		assertEquals(townSpawn, event.getRespawnLocation());
		assertTrue(failures.isEmpty());
	}

	@Test
	void setterReportsFailureInsteadOfThrowing() throws ReflectiveOperationException {
		MethodHandle failing = MethodHandles.lookup().findStatic(TownyCanvasEventsTests.class, "fail",
			MethodType.methodType(void.class, PlayerRespawnAsyncEvent.class, Location.class));
		PlayerRespawnAsyncEvent event = new PlayerRespawnAsyncEvent(mock(Player.class), new Location(null, 0, 64, 0));
		List<Throwable> failures = new ArrayList<>();

		TownyCanvasEvents.respawnLocationSetter(event, failing, failures::add).accept(new Location(null, 1, 2, 3));

		assertEquals(1, failures.size());
		assertEquals(new Location(null, 0, 64, 0), event.getRespawnLocation());
	}

	@SuppressWarnings("unused")
	private static void fail(PlayerRespawnAsyncEvent event, Location location) {
		throw new IllegalStateException("boom");
	}
}
