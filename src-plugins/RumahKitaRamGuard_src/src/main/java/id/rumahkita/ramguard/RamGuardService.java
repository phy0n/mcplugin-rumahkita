/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Chunk
 *  org.bukkit.World
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Ambient
 *  org.bukkit.entity.Animals
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.ExperienceOrb
 *  org.bukkit.entity.FallingBlock
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.ItemFrame
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Monster
 *  org.bukkit.entity.Painting
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.entity.Tameable
 *  org.bukkit.entity.Villager
 *  org.bukkit.entity.WaterMob
 */
package id.rumahkita.ramguard;

import id.rumahkita.ramguard.CleanupReport;
import id.rumahkita.ramguard.MemoryInfo;
import id.rumahkita.ramguard.RumahKitaRamGuardPlugin;
import id.rumahkita.ramguard.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Ambient;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WaterMob;

public final class RamGuardService {
    private final RumahKitaRamGuardPlugin plugin;
    private long lastCleanupMillis;
    private long lastEmergencyMillis;
    private boolean reliefMode;
    private boolean warned;

    public RamGuardService(RumahKitaRamGuardPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkMemory() {
        MemoryInfo info = MemoryInfo.current();
        double percent = info.getPercent();
        int warn = this.plugin.getConfig().getInt("settings.warn-threshold-percent", 85);
        int cleanup = this.plugin.getConfig().getInt("settings.cleanup-threshold-percent", 90);
        int emergency = this.plugin.getConfig().getInt("settings.emergency-threshold-percent", 95);
        int restore = this.plugin.getConfig().getInt("dynamic-view-distance.restore-when-below-percent", 75);
        if (percent >= (double)emergency) {
            if (this.canRunEmergency()) {
                this.lastEmergencyMillis = System.currentTimeMillis();
                this.lastCleanupMillis = System.currentTimeMillis();
                this.plugin.sendAlert(this.plugin.message("emergency", info));
                CleanupReport report = this.runCleanup(CleanupLevel.EMERGENCY, info);
                this.plugin.log("Emergency cleanup selesai: " + report.summary());
            }
            return;
        }
        if (percent >= (double)cleanup) {
            if (this.canRunCleanup()) {
                this.lastCleanupMillis = System.currentTimeMillis();
                this.plugin.sendAlert(this.plugin.message("cleanup", info));
                CleanupReport report = this.runCleanup(CleanupLevel.CLEANUP, info);
                this.plugin.log("Cleanup selesai: " + report.summary());
            }
            return;
        }
        if (percent >= (double)warn) {
            if (!this.warned) {
                this.warned = true;
                this.plugin.sendAlert(this.plugin.message("warning", info));
                this.plugin.log("Warning RAM: " + info.getPercentFormatted() + "% used=" + info.getUsedFormatted() + "/" + info.getMaxFormatted());
            }
            return;
        }
        this.warned = false;
        if (this.reliefMode && this.plugin.getConfig().getBoolean("dynamic-view-distance.enabled", true) && percent <= (double)restore) {
            this.restoreViewDistance();
            this.reliefMode = false;
            this.plugin.sendAlert(this.plugin.message("restored", info));
            this.plugin.log("View distance dikembalikan normal. RAM sekarang " + info.getPercentFormatted() + "%");
        }
    }

    public CleanupReport runCleanup(CleanupLevel level, MemoryInfo info) {
        CleanupReport report = new CleanupReport();
        if (this.plugin.getConfig().getBoolean("dynamic-view-distance.enabled", true)) {
            boolean changed = this.applyReliefViewDistance(level);
            report.setViewDistanceChanged(changed);
            if (changed) {
                this.reliefMode = true;
            }
        }
        report.addEntitiesRemoved(this.clearLightEntities());
        report.addMobsRemoved(this.limitMobs());
        report.addChunksUnloaded(this.unloadUnusedChunks());
        this.runConfiguredCommands(level, info);
        if (this.plugin.getConfig().getBoolean("settings.run-garbage-collector-after-cleanup", true)) {
            System.gc();
            report.setGarbageCollectorRequested(true);
        }
        return report;
    }

    public void restoreViewDistance() {
        int viewDistance = this.plugin.getConfig().getInt("dynamic-view-distance.normal-view-distance", 5);
        int simulationDistance = this.plugin.getConfig().getInt("dynamic-view-distance.normal-simulation-distance", 3);
        for (World world : Bukkit.getWorlds()) {
            this.setWorldDistances(world, viewDistance, simulationDistance);
        }
    }

    public boolean isReliefMode() {
        return this.reliefMode;
    }

    private boolean canRunCleanup() {
        long cooldown = this.plugin.getConfig().getLong("settings.cleanup-cooldown-seconds", 120L) * 1000L;
        return System.currentTimeMillis() - this.lastCleanupMillis >= cooldown;
    }

    private boolean canRunEmergency() {
        long cooldown = this.plugin.getConfig().getLong("settings.emergency-cooldown-seconds", 300L) * 1000L;
        return System.currentTimeMillis() - this.lastEmergencyMillis >= cooldown;
    }

    private boolean applyReliefViewDistance(CleanupLevel level) {
        int simulationPath;
        int viewPath;
        if (level == CleanupLevel.EMERGENCY) {
            viewPath = this.plugin.getConfig().getInt("dynamic-view-distance.emergency-view-distance", 3);
            simulationPath = this.plugin.getConfig().getInt("dynamic-view-distance.emergency-simulation-distance", 2);
        } else {
            viewPath = this.plugin.getConfig().getInt("dynamic-view-distance.cleanup-view-distance", 4);
            simulationPath = this.plugin.getConfig().getInt("dynamic-view-distance.cleanup-simulation-distance", 2);
        }
        boolean changed = false;
        for (World world : Bukkit.getWorlds()) {
            if (world.getViewDistance() == viewPath && world.getSimulationDistance() == simulationPath) continue;
            this.setWorldDistances(world, viewPath, simulationPath);
            changed = true;
        }
        return changed;
    }

    private void setWorldDistances(World world, int viewDistance, int simulationDistance) {
        int safeView = Math.max(2, Math.min(32, viewDistance));
        int safeSimulation = Math.max(2, Math.min(32, simulationDistance));
        world.setViewDistance(safeView);
        world.setSimulationDistance(safeSimulation);
    }

    private int clearLightEntities() {
        boolean groundItems = this.plugin.getConfig().getBoolean("clear.ground-items", true);
        boolean xpOrbs = this.plugin.getConfig().getBoolean("clear.xp-orbs", true);
        boolean projectiles = this.plugin.getConfig().getBoolean("clear.projectiles", true);
        boolean fallingBlocks = this.plugin.getConfig().getBoolean("clear.falling-blocks", true);
        boolean frames = this.plugin.getConfig().getBoolean("clear.paintings-and-item-frames", false);
        boolean excludeNamed = this.plugin.getConfig().getBoolean("clear.exclude-named-entities", true);
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (excludeNamed && entity.getCustomName() != null && !entity.getCustomName().isBlank()) continue;
                boolean shouldRemove = false;
                if (groundItems && entity instanceof Item) {
                    shouldRemove = true;
                } else if (xpOrbs && entity instanceof ExperienceOrb) {
                    shouldRemove = true;
                } else if (projectiles && entity instanceof Projectile) {
                    shouldRemove = true;
                } else if (fallingBlocks && entity instanceof FallingBlock) {
                    shouldRemove = true;
                } else if (frames && (entity instanceof Painting || entity instanceof ItemFrame)) {
                    shouldRemove = true;
                }
                if (!shouldRemove) continue;
                entity.remove();
                ++removed;
            }
        }
        return removed;
    }

    private int limitMobs() {
        if (!this.plugin.getConfig().getBoolean("mob-limiter.enabled", true)) {
            return 0;
        }
        int hostileMax = this.plugin.getConfig().getInt("mob-limiter.max-hostile-per-world", 350);
        int passiveMax = this.plugin.getConfig().getInt("mob-limiter.max-passive-per-world", 250);
        int maxRemoval = this.plugin.getConfig().getInt("mob-limiter.max-removal-per-world-per-cleanup", 150);
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            ArrayList<LivingEntity> hostile = new ArrayList<LivingEntity>();
            ArrayList<LivingEntity> passive = new ArrayList<LivingEntity>();
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof Player || this.shouldProtectMob(entity)) continue;
                if (entity instanceof Monster) {
                    hostile.add(entity);
                    continue;
                }
                if (!(entity instanceof Animals) && !(entity instanceof WaterMob) && !(entity instanceof Ambient)) continue;
                passive.add(entity);
            }
            int worldRemoved = 0;
            if ((worldRemoved += this.removeExcessMobs(world, hostile, hostileMax, maxRemoval - worldRemoved)) < maxRemoval) {
                worldRemoved += this.removeExcessMobs(world, passive, passiveMax, maxRemoval - worldRemoved);
            }
            removed += worldRemoved;
        }
        return removed;
    }

    private boolean shouldProtectMob(LivingEntity entity) {
        Tameable tameable;
        boolean excludeNamed = this.plugin.getConfig().getBoolean("clear.exclude-named-entities", true);
        if (excludeNamed && entity.getCustomName() != null && !entity.getCustomName().isBlank()) {
            return true;
        }
        if (entity instanceof Tameable && (tameable = (Tameable)entity).isTamed()) {
            return true;
        }
        return entity instanceof Villager;
    }

    private int removeExcessMobs(World world, List<LivingEntity> mobs, int maxAllowed, int maxRemoval) {
        if (maxRemoval <= 0 || mobs.size() <= maxAllowed) {
            return 0;
        }
        int keepNearBlocks = this.plugin.getConfig().getInt("mob-limiter.keep-mobs-within-blocks-of-player", 48);
        double keepNearSquared = keepNearBlocks * keepNearBlocks;
        int needRemove = Math.min(mobs.size() - maxAllowed, maxRemoval);
        mobs.sort(Comparator.comparingDouble(this::nearestPlayerDistanceSquared).reversed());
        int removed = 0;
        for (LivingEntity mob : mobs) {
            if (removed >= needRemove) break;
            if (!mob.isValid() || mob.isDead() || this.nearestPlayerDistanceSquared((Entity)mob) <= keepNearSquared || !mob.getWorld().equals((Object)world)) continue;
            mob.remove();
            ++removed;
        }
        return removed;
    }

    private double nearestPlayerDistanceSquared(Entity entity) {
        double nearest = Double.MAX_VALUE;
        for (Player player : entity.getWorld().getPlayers()) {
            double distance = player.getLocation().distanceSquared(entity.getLocation());
            if (!(distance < nearest)) continue;
            nearest = distance;
        }
        return nearest;
    }

    private int unloadUnusedChunks() {
        if (!this.plugin.getConfig().getBoolean("chunk-cleanup.enabled", true)) {
            return 0;
        }
        boolean save = this.plugin.getConfig().getBoolean("chunk-cleanup.save-before-unload", true);
        int keepBlocks = this.plugin.getConfig().getInt("chunk-cleanup.keep-chunks-within-blocks-of-player", 192);
        int keepChunks = Math.max(1, keepBlocks / 16);
        int unloaded = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                if (this.isChunkNearPlayer(world, chunk, keepChunks) || !chunk.unload(save)) continue;
                ++unloaded;
            }
        }
        return unloaded;
    }

    private boolean isChunkNearPlayer(World world, Chunk chunk, int keepChunks) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        for (Player player : world.getPlayers()) {
            int playerChunkX = player.getLocation().getBlockX() >> 4;
            int playerChunkZ = player.getLocation().getBlockZ() >> 4;
            if (Math.abs(playerChunkX - chunkX) > keepChunks || Math.abs(playerChunkZ - chunkZ) > keepChunks) continue;
            return true;
        }
        return false;
    }

    private void runConfiguredCommands(CleanupLevel level, MemoryInfo info) {
        String path = level == CleanupLevel.EMERGENCY ? "commands.emergency" : "commands.cleanup";
        List commands = this.plugin.getConfig().getStringList(path);
        if (commands.isEmpty()) {
            return;
        }
        for (String rawCommand : commands) {
            if (rawCommand == null || rawCommand.isBlank()) continue;
            String command = rawCommand.replace("%percent%", info.getPercentFormatted()).replace("%used%", info.getUsedFormatted()).replace("%max%", info.getMaxFormatted());
            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)command);
        }
    }

    public void sendStatus(CommandSender sender) {
        MemoryInfo info = MemoryInfo.current();
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        sender.sendMessage(Text.color("&8&m--------------------------------"));
        sender.sendMessage(Text.color("&aRumahKita RamGuard Status"));
        sender.sendMessage(Text.color("&7RAM Used: &f" + info.getUsedFormatted() + " &7/ &f" + info.getMaxFormatted() + " &8(&e" + info.getPercentFormatted() + "%&8)"));
        sender.sendMessage(Text.color("&7Allocated: &f" + MemoryInfo.formatBytes(total) + " &7| Free in allocated: &f" + MemoryInfo.formatBytes(free)));
        sender.sendMessage(Text.color("&7Relief Mode: " + (this.reliefMode ? "&cON" : "&aOFF")));
        for (World world : Bukkit.getWorlds()) {
            sender.sendMessage(Text.color("&7World &f" + world.getName() + "&7: view=&e" + world.getViewDistance() + " &7simulation=&e" + world.getSimulationDistance() + " &7loadedChunks=&e" + world.getLoadedChunks().length + " &7entities=&e" + world.getEntities().size()));
        }
        sender.sendMessage(Text.color("&8&m--------------------------------"));
    }

    public static enum CleanupLevel {
        CLEANUP,
        EMERGENCY;

    }
}

