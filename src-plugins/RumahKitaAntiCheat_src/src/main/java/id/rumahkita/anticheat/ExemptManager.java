/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package id.rumahkita.anticheat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class ExemptManager {
    private final Map<UUID, TimedExempt> timed = new HashMap<UUID, TimedExempt>();

    public void exempt(UUID uuid, long seconds, String reason) {
        this.timed.put(uuid, new TimedExempt(System.currentTimeMillis() + seconds * 1000L, reason == null ? "manual" : reason));
    }

    public void exempt(Player player, long seconds, String reason) {
        this.exempt(player.getUniqueId(), seconds, reason);
    }

    public boolean remove(UUID uuid) {
        return this.timed.remove(uuid) != null;
    }

    public boolean isTimedExempt(UUID uuid) {
        TimedExempt entry = this.timed.get(uuid);
        if (entry == null) {
            return false;
        }
        if (entry.until < System.currentTimeMillis()) {
            this.timed.remove(uuid);
            return false;
        }
        return true;
    }

    public long getRemainingSeconds(UUID uuid) {
        TimedExempt entry = this.timed.get(uuid);
        if (entry == null) {
            return 0L;
        }
        return Math.max(0L, (entry.until - System.currentTimeMillis()) / 1000L);
    }

    public int size() {
        this.cleanup();
        return this.timed.size();
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, TimedExempt>> iterator = this.timed.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().until >= now) continue;
            iterator.remove();
        }
    }

    private record TimedExempt(long until, String reason) {
    }
}

