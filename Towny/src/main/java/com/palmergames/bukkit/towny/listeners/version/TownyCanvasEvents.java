package com.palmergames.bukkit.towny.listeners.version;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.listeners.TownyPlayerListener;
import com.palmergames.util.JavaUtil;
import io.papermc.paper.event.player.AbstractRespawnEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.lang.invoke.MethodHandle;
import java.util.function.Consumer;
import java.util.logging.Level;

public class TownyCanvasEvents implements VersionDependentListener {

    private static final String ASYNC_TELEPORT_EVENT = "io.canvasmc.canvas.event.EntityTeleportAsyncEvent";

    private static final MethodHandle ASYNC_TELEPORT_EVENT_FROM = JavaUtil.getMethodHandle(ASYNC_TELEPORT_EVENT, "getFrom");
    private static final MethodHandle ASYNC_TELEPORT_EVENT_TO = JavaUtil.getMethodHandle(ASYNC_TELEPORT_EVENT, "getTo");
    private static final MethodHandle ASYNC_TELEPORT_EVENT_CAUSE = JavaUtil.getMethodHandle(ASYNC_TELEPORT_EVENT, "getCause");

    // Canvas respawns players without firing PlayerRespawnEvent (only END_PORTAL exits still fire it),
    // so town and jail respawns have to be applied on its async respawn event as well.
    static final String ASYNC_RESPAWN_EVENT = "io.canvasmc.canvas.event.PlayerRespawnAsyncEvent";

    private static final MethodHandle ASYNC_RESPAWN_EVENT_SET_LOCATION = setRespawnLocationHandle(ASYNC_RESPAWN_EVENT);

    private final Towny plugin;
    private final TownyPlayerListener townyPlayerListener;

    public TownyCanvasEvents(Towny instance, TownyPlayerListener townyPlayerListener) {
        this.plugin = instance;
        this.townyPlayerListener = townyPlayerListener;
    }

    @Override
    public void register() {
        registerEvent(ASYNC_TELEPORT_EVENT, this::asyncTeleportEventListener, EventPriority.NORMAL, true);

        if (ASYNC_RESPAWN_EVENT_SET_LOCATION != null) {
            // Same priorities as the PlayerRespawnEvent handlers, so a jail spawn still wins over a town spawn.
            registerEvent(ASYNC_RESPAWN_EVENT, () -> asyncRespawnEventListener(townyPlayerListener::handleTownRespawn), EventPriority.NORMAL, false);
            registerEvent(ASYNC_RESPAWN_EVENT, () -> asyncRespawnEventListener(townyPlayerListener::handleJailRespawn), EventPriority.HIGHEST, false);
        }
    }

    @FunctionalInterface
    interface RespawnHandler {
        void handle(AbstractRespawnEvent event, Consumer<Location> setRespawnLocation);
    }

    private Consumer<AbstractRespawnEvent> asyncRespawnEventListener(RespawnHandler handler) {
        return event -> handler.handle(event, respawnLocationSetter(event, ASYNC_RESPAWN_EVENT_SET_LOCATION, failure ->
            plugin.getLogger().log(Level.WARNING, "An exception occurred while invoking " + ASYNC_RESPAWN_EVENT + "#setRespawnLocation reflectively", failure)));
    }

    /**
     * @return a setter that writes the respawn location through the given handle and reports a failure instead of throwing.
     */
    static Consumer<Location> respawnLocationSetter(AbstractRespawnEvent event, MethodHandle setRespawnLocation, Consumer<Throwable> onFailure) {
        return location -> {
            try {
                setRespawnLocation.invoke(event, location);
            } catch (final Throwable e) {
                onFailure.accept(e);
            }
        };
    }

    static MethodHandle setRespawnLocationHandle(String className) {
        try {
            return JavaUtil.getMethodHandle(Class.forName(className), "setRespawnLocation", Location.class);
        } catch (ClassNotFoundException notCanvas) {
            return null;
        }
    }

    private Consumer<EntityEvent> asyncTeleportEventListener() {
        return event -> {
            if (ASYNC_TELEPORT_EVENT_FROM == null || ASYNC_TELEPORT_EVENT_TO == null || ASYNC_TELEPORT_EVENT_CAUSE == null || !(event.getEntity() instanceof Player player))
                return;

            final Location from;
            final Location to;
            final PlayerTeleportEvent.TeleportCause cause;
            try {
                from = (Location) ASYNC_TELEPORT_EVENT_FROM.invoke(event);
                to = (Location) ASYNC_TELEPORT_EVENT_TO.invoke(event);
                cause = (PlayerTeleportEvent.TeleportCause) ASYNC_TELEPORT_EVENT_CAUSE.invoke(event);
            } catch (final Throwable e) {
                plugin.getLogger().log(Level.WARNING, "An exception occurred while invoking " + ASYNC_TELEPORT_EVENT + "#from/#to/#getCause reflectively", e);
                return;
            }

            townyPlayerListener.handleTeleportCellChange(player, cause, from, to, (Cancellable) event);
        };
    }
}
