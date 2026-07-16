/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 */
package id.rumahkita.fishing.manager;

import id.rumahkita.fishing.RumahKitaFishingPlugin;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class AntiAfkManager {
    private static final long FIVE_MINUTES_MS = 300000L;
    private final RumahKitaFishingPlugin plugin;
    private final Map<UUID, Location> lastMovementLocation;
    private final Map<UUID, Long> lastMovementAt;
    private final Map<UUID, Deque<Long>> catchTimes;

    public AntiAfkManager(RumahKitaFishingPlugin plugin) {
        this.plugin = plugin;
        this.lastMovementLocation = new HashMap<UUID, Location>();
        this.lastMovementAt = new HashMap<UUID, Long>();
        this.catchTimes = new HashMap<UUID, Deque<Long>>();
    }

    public void trackMovement(Player player, Location to) {
        if (to == null || to.getWorld() == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        Location previous = this.lastMovementLocation.get(uuid);
        if (previous == null || previous.getWorld() == null || !previous.getWorld().equals((Object)to.getWorld()) || previous.distanceSquared(to) >= 1.0) {
            this.lastMovementLocation.put(uuid, to.clone());
            this.lastMovementAt.put(uuid, System.currentTimeMillis());
        }
    }

    public boolean canReceiveCustomFish(Player player) {
        if (!this.plugin.getConfig().getBoolean("anti-afk.enabled", true)) {
            return true;
        }
        String worldName = player.getWorld().getName();
        for (String blockedWorld : this.plugin.getConfig().getStringList("anti-afk.blocked-worlds")) {
            if (!worldName.equalsIgnoreCase(blockedWorld)) continue;
            return false;
        }
        if (this.plugin.getConfig().getBoolean("anti-afk.require-small-movement", true)) {
            long maxStillMs = Math.max(1L, this.plugin.getConfig().getLong("anti-afk.max-same-location-minutes", 10L)) * 60L * 1000L;
            long lastMove = this.lastMovementAt.getOrDefault(player.getUniqueId(), System.currentTimeMillis());
            if (System.currentTimeMillis() - lastMove > maxStillMs) {
                return false;
            }
        }
        Deque times = this.catchTimes.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque());
        long now = System.currentTimeMillis();
        while (!times.isEmpty() && now - (Long)times.peekFirst() > 300000L) {
            times.removeFirst();
        }
        int maxCatches = Math.max(1, this.plugin.getConfig().getInt("anti-afk.max-catches-per-5-minutes", 25));
        return times.size() < maxCatches;
    }

    public void registerCatch(Player player) {
        Deque times = this.catchTimes.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque());
        times.addLast(System.currentTimeMillis());
    }

    public boolean shouldCancelWhenDetected() {
        return this.plugin.getConfig().getString("anti-afk.action-when-detected", "VANILLA").toUpperCase(Locale.ROOT).equals("CANCEL");
    }

    public void forget(Player player) {
        UUID uuid = player.getUniqueId();
        this.lastMovementLocation.remove(uuid);
        this.lastMovementAt.remove(uuid);
        this.catchTimes.remove(uuid);
    }
}

