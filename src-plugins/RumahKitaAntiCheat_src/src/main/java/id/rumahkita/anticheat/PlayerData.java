/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 */
package id.rumahkita.anticheat;

import org.bukkit.Location;

public final class PlayerData {
    public Location lastSafeLocation;
    public Location lastMoveLocation;
    public long lastMoveMillis = System.currentTimeMillis();
    public long lastViolationReset = System.currentTimeMillis();
    public long lastBlockPlaceMillis = 0L;
    public long lastBlockBreakMillis = 0L;
    public long lastGroundMillis = System.currentTimeMillis();
    public long lastNearSolidMillis = System.currentTimeMillis();
    public int speedBuffer = 0;
    public int airTicks = 0;
    public int stableYTicks = 0;
    public int jesusTicks = 0;

    public PlayerData(Location location) {
        this.lastSafeLocation = location == null ? null : location.clone();
        this.lastMoveLocation = location == null ? null : location.clone();
    }

    public boolean recentlyPlacedBlock(long seconds) {
        return System.currentTimeMillis() - this.lastBlockPlaceMillis <= seconds * 1000L;
    }

    public boolean recentlyBrokeBlock(long seconds) {
        return System.currentTimeMillis() - this.lastBlockBreakMillis <= seconds * 1000L;
    }

    public boolean recentlyGrounded(long millis) {
        return System.currentTimeMillis() - this.lastGroundMillis <= millis;
    }

    public boolean recentlyNearSolid(long millis) {
        return System.currentTimeMillis() - this.lastNearSolidMillis <= millis;
    }
}

