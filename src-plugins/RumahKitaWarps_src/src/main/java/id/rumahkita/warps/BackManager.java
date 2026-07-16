package id.rumahkita.warps;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackManager implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, Location> backLocations = new HashMap<>();

    public BackManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        backLocations.put(player.getUniqueId(), player.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Track teleportations to allow /back to also return to pre-teleport locations
        if (event.getCause() == TeleportCause.COMMAND || event.getCause() == TeleportCause.PLUGIN) {
            backLocations.put(event.getPlayer().getUniqueId(), event.getFrom());
        }
    }

    public Location getBackLocation(UUID uuid) {
        return backLocations.get(uuid);
    }
}
