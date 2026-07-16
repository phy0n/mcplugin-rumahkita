/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.attribute.AttributeInstance
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Mob
 *  org.bukkit.entity.Monster
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.util.Vector
 */
package id.rumahkita.farmai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class RumahKitaFarmAI
extends JavaPlugin {
    private BukkitTask scanTask;
    private boolean enabled;
    private int intervalTicks;
    private int maxMobsPerScan;
    private boolean usePlayerNearbyScan;
    private Set<String> enabledWorlds = new HashSet<String>();
    private Set<String> allowedMobs = new HashSet<String>();
    private final List<FarmZone> zones = new ArrayList<FarmZone>();
    private boolean zonesEnabled;
    private boolean allowSurvival;
    private boolean allowAdventure;
    private boolean ignoreCreative;
    private boolean ignoreSpectator;
    private double horizontalRadius;
    private double verticalRadius;
    private boolean forceTargetThroughBlocks;
    private boolean retargetEveryScan;
    private boolean setAiEnabled;
    private boolean setAwareEnabled;
    private boolean removeNoAi;
    private boolean applyFollowRange;
    private double followRange;
    private boolean applyMinimumMovementSpeed;
    private double minimumMovementSpeed;
    private boolean ignoreNamedMobs;
    private boolean makePersistent;
    private boolean stuckHelperEnabled;
    private int stuckAfterScans;
    private double minimumMoveDistance;
    private double nudgeStrength;
    private double maxYDifferenceForNudge;
    private boolean nudgeOnlyWhenHasTarget;
    private String prefix;
    private final Map<UUID, StuckData> stuckDataMap = new HashMap<UUID, StuckData>();
    private int lastPlayers;
    private int lastScanned;
    private int lastFixed;
    private int lastNudged;

    public void onEnable() {
        this.saveDefaultConfig();
        this.loadSettings();
        this.startScanner();
        this.getLogger().info("RumahKitaFarmAI enabled. Mob farm AI helper is running.");
    }

    public void onDisable() {
        this.stopScanner();
        this.stuckDataMap.clear();
        this.getLogger().info("RumahKitaFarmAI disabled.");
    }

    private void loadSettings() {
        this.reloadConfig();
        this.enabled = this.getConfig().getBoolean("enabled", true);
        this.intervalTicks = Math.max(1, this.getConfig().getInt("scan.interval-ticks", 10));
        this.maxMobsPerScan = Math.max(1, this.getConfig().getInt("scan.max-mobs-per-scan", 250));
        this.usePlayerNearbyScan = this.getConfig().getBoolean("scan.use-player-nearby-scan", true);
        this.enabledWorlds = new HashSet<String>();
        for (String worldName : this.getConfig().getStringList("worlds")) {
            if (worldName == null || worldName.trim().isEmpty()) continue;
            this.enabledWorlds.add(worldName.trim());
        }
        this.allowedMobs = new HashSet<String>();
        for (String mobName : this.getConfig().getStringList("allowed-mobs")) {
            if (mobName == null || mobName.trim().isEmpty()) continue;
            this.allowedMobs.add(mobName.trim().toUpperCase(Locale.ROOT));
        }
        this.loadZones();
        this.allowSurvival = this.getConfig().getBoolean("player.allow-survival", true);
        this.allowAdventure = this.getConfig().getBoolean("player.allow-adventure", true);
        this.ignoreCreative = this.getConfig().getBoolean("player.ignore-creative", true);
        this.ignoreSpectator = this.getConfig().getBoolean("player.ignore-spectator", true);
        this.horizontalRadius = Math.max(1.0, this.getConfig().getDouble("targeting.horizontal-radius", 64.0));
        this.verticalRadius = Math.max(1.0, this.getConfig().getDouble("targeting.vertical-radius", 48.0));
        this.forceTargetThroughBlocks = this.getConfig().getBoolean("targeting.force-target-through-blocks", true);
        this.retargetEveryScan = this.getConfig().getBoolean("targeting.retarget-every-scan", true);
        this.setAiEnabled = this.getConfig().getBoolean("mob.set-ai-enabled", true);
        this.setAwareEnabled = this.getConfig().getBoolean("mob.set-aware-enabled", true);
        this.removeNoAi = this.getConfig().getBoolean("mob.remove-no-ai", true);
        this.applyFollowRange = this.getConfig().getBoolean("mob.apply-follow-range", true);
        this.followRange = Math.max(1.0, this.getConfig().getDouble("mob.follow-range", 80.0));
        this.applyMinimumMovementSpeed = this.getConfig().getBoolean("mob.apply-minimum-movement-speed", false);
        this.minimumMovementSpeed = Math.max(0.01, this.getConfig().getDouble("mob.minimum-movement-speed", 0.28));
        this.ignoreNamedMobs = this.getConfig().getBoolean("mob.ignore-named-mobs", false);
        this.makePersistent = this.getConfig().getBoolean("mob.make-persistent", false);
        this.stuckHelperEnabled = this.getConfig().getBoolean("stuck-helper.enabled", true);
        this.stuckAfterScans = Math.max(1, this.getConfig().getInt("stuck-helper.after-scans", 3));
        this.minimumMoveDistance = Math.max(0.01, this.getConfig().getDouble("stuck-helper.minimum-move-distance", 0.08));
        this.nudgeStrength = Math.max(0.0, this.getConfig().getDouble("stuck-helper.nudge-strength", 0.1));
        this.maxYDifferenceForNudge = Math.max(0.0, this.getConfig().getDouble("stuck-helper.max-y-difference-for-nudge", 6.0));
        this.nudgeOnlyWhenHasTarget = this.getConfig().getBoolean("stuck-helper.only-when-has-target", true);
        this.prefix = this.color(this.getConfig().getString("messages.prefix", "&8[&aRumahKitaFarmAI&8] "));
    }

    private void loadZones() {
        this.zones.clear();
        this.zonesEnabled = this.getConfig().getBoolean("zones.enabled", false);
        ConfigurationSection list = this.getConfig().getConfigurationSection("zones.list");
        if (list == null) {
            return;
        }
        for (String key : list.getKeys(false)) {
            ConfigurationSection section = list.getConfigurationSection(key);
            if (section == null) continue;
            String worldName = section.getString("world", "world");
            double centerX = section.getDouble("center-x", 0.0);
            double centerY = section.getDouble("center-y", 64.0);
            double centerZ = section.getDouble("center-z", 0.0);
            double radiusX = Math.max(1.0, section.getDouble("radius-x", 80.0));
            double radiusY = Math.max(1.0, section.getDouble("radius-y", 80.0));
            double radiusZ = Math.max(1.0, section.getDouble("radius-z", 80.0));
            this.zones.add(new FarmZone(key, worldName, centerX, centerY, centerZ, radiusX, radiusY, radiusZ));
        }
    }

    private void startScanner() {
        this.stopScanner();
        this.scanTask = Bukkit.getScheduler().runTaskTimer((Plugin)this, this::scanAllWorlds, (long)this.intervalTicks, (long)this.intervalTicks);
    }

    private void stopScanner() {
        if (this.scanTask != null) {
            this.scanTask.cancel();
            this.scanTask = null;
        }
    }

    private void scanAllWorlds() {
        if (!this.enabled) {
            return;
        }
        this.lastPlayers = 0;
        this.lastScanned = 0;
        this.lastFixed = 0;
        this.lastNudged = 0;
        for (World world : Bukkit.getWorlds()) {
            if (!this.isWorldEnabled(world)) continue;
            this.scanWorld(world);
        }
    }

    private void scanWorld(World world) {
        List<Player> validPlayers = this.getValidPlayers(world);
        this.lastPlayers += validPlayers.size();
        if (validPlayers.isEmpty()) {
            return;
        }
        int processed = 0;
        if (this.usePlayerNearbyScan) {
            HashSet<UUID> processedEntities = new HashSet<UUID>();
            block0: for (Player player : validPlayers) {
                if (processed < this.maxMobsPerScan) {
                    for (Entity entity : player.getNearbyEntities(this.horizontalRadius, this.verticalRadius, this.horizontalRadius)) {
                        if (processed >= this.maxMobsPerScan) continue block0;
                        if (!processedEntities.add(entity.getUniqueId()) || !this.tryProcessEntity(entity, validPlayers)) continue;
                        ++processed;
                    }
                    continue;
                }
                break;
            }
        } else {
            for (Entity entity : world.getEntities()) {
                if (processed < this.maxMobsPerScan) {
                    if (!this.tryProcessEntity(entity, validPlayers)) continue;
                    ++processed;
                    continue;
                }
                break;
            }
        }
    }

    private boolean tryProcessEntity(Entity entity, List<Player> validPlayers) {
        if (!(entity instanceof Monster)) {
            return false;
        }
        Monster monster = (Monster)entity;
        if (!this.isValidMob(monster)) {
            return false;
        }
        if (!this.isInAllowedArea(monster.getLocation())) {
            return false;
        }
        ++this.lastScanned;
        Player nearestPlayer = this.findNearestValidPlayer((Mob)monster, validPlayers);
        if (nearestPlayer == null) {
            return true;
        }
        this.applyMobFix((Mob)monster, nearestPlayer);
        ++this.lastFixed;
        return true;
    }

    private List<Player> getValidPlayers(World world) {
        ArrayList<Player> players = new ArrayList<Player>();
        for (Player player : world.getPlayers()) {
            if (!this.isValidTargetPlayer(player) || !this.isInAllowedArea(player.getLocation())) continue;
            players.add(player);
        }
        return players;
    }

    private boolean isWorldEnabled(World world) {
        if (world == null) {
            return false;
        }
        if (!this.enabledWorlds.isEmpty() && !this.enabledWorlds.contains(world.getName())) {
            return false;
        }
        if (!this.zonesEnabled) {
            return true;
        }
        for (FarmZone zone : this.zones) {
            if (!zone.worldName.equalsIgnoreCase(world.getName())) continue;
            return true;
        }
        return false;
    }

    private boolean isInAllowedArea(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!this.zonesEnabled) {
            return true;
        }
        for (FarmZone zone : this.zones) {
            if (!zone.contains(location)) continue;
            return true;
        }
        return false;
    }

    private boolean isValidTargetPlayer(Player player) {
        if (player == null || !player.isOnline() || player.isDead()) {
            return false;
        }
        GameMode gameMode = player.getGameMode();
        if (this.ignoreCreative && gameMode == GameMode.CREATIVE) {
            return false;
        }
        if (this.ignoreSpectator && gameMode == GameMode.SPECTATOR) {
            return false;
        }
        if (gameMode == GameMode.SURVIVAL) {
            return this.allowSurvival;
        }
        if (gameMode == GameMode.ADVENTURE) {
            return this.allowAdventure;
        }
        return false;
    }

    private boolean isValidMob(Monster monster) {
        if (monster == null || !monster.isValid() || monster.isDead()) {
            return false;
        }
        if (this.ignoreNamedMobs && monster.customName() != null) {
            return false;
        }
        String mobType = monster.getType().name().toUpperCase(Locale.ROOT);
        return this.allowedMobs.isEmpty() || this.allowedMobs.contains(mobType);
    }

    private Player findNearestValidPlayer(Mob mob, List<Player> players) {
        Player nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;
        Location mobLocation = mob.getLocation();
        for (Player player : players) {
            double distanceSquared;
            if (player.getWorld() != mob.getWorld()) continue;
            Location playerLocation = player.getLocation();
            double dx = Math.abs(playerLocation.getX() - mobLocation.getX());
            double dy = Math.abs(playerLocation.getY() - mobLocation.getY());
            double dz = Math.abs(playerLocation.getZ() - mobLocation.getZ());
            if (dx > this.horizontalRadius || dy > this.verticalRadius || dz > this.horizontalRadius || !this.forceTargetThroughBlocks && !mob.hasLineOfSight((Entity)player) || !((distanceSquared = playerLocation.distanceSquared(mobLocation)) < nearestDistanceSquared)) continue;
            nearestDistanceSquared = distanceSquared;
            nearest = player;
        }
        return nearest;
    }

    private void applyMobFix(Mob mob, Player target) {
        if (this.setAiEnabled || this.removeNoAi) {
            mob.setAI(true);
        }
        if (this.setAwareEnabled) {
            mob.setAware(true);
        }
        if (this.makePersistent) {
            mob.setPersistent(true);
            mob.setRemoveWhenFarAway(false);
        }
        if (this.applyFollowRange) {
            this.setAttributeMinimum((LivingEntity)mob, "FOLLOW_RANGE", this.followRange);
            this.setAttributeMinimum((LivingEntity)mob, "GENERIC_FOLLOW_RANGE", this.followRange);
        }
        if (this.applyMinimumMovementSpeed) {
            this.setAttributeMinimum((LivingEntity)mob, "MOVEMENT_SPEED", this.minimumMovementSpeed);
            this.setAttributeMinimum((LivingEntity)mob, "GENERIC_MOVEMENT_SPEED", this.minimumMovementSpeed);
        }
        if (this.retargetEveryScan || mob.getTarget() == null || !mob.getTarget().isValid()) {
            mob.setTarget((LivingEntity)target);
        }
        if (this.stuckHelperEnabled) {
            this.applyStuckHelper(mob, target);
        }
    }

    private void applyStuckHelper(Mob mob, Player target) {
        if (this.nudgeOnlyWhenHasTarget && mob.getTarget() == null) {
            return;
        }
        UUID uuid = mob.getUniqueId();
        Location now = mob.getLocation();
        StuckData data = this.stuckDataMap.get(uuid);
        if (data == null || data.lastLocation == null || data.lastLocation.getWorld() != now.getWorld()) {
            this.stuckDataMap.put(uuid, new StuckData(now.clone(), 0));
            return;
        }
        double moved = Math.sqrt(Math.max(0.0, data.lastLocation.distanceSquared(now)));
        if (moved <= this.minimumMoveDistance) {
            ++data.stuckScans;
        } else {
            data.stuckScans = 0;
            data.lastLocation = now.clone();
            return;
        }
        data.lastLocation = now.clone();
        if (data.stuckScans < this.stuckAfterScans) {
            return;
        }
        Location targetLocation = target.getLocation();
        double yDiff = Math.abs(targetLocation.getY() - now.getY());
        if (yDiff > this.maxYDifferenceForNudge) {
            return;
        }
        Vector direction = targetLocation.toVector().subtract(now.toVector());
        direction.setY(0.0);
        if (direction.lengthSquared() < 1.0E-4) {
            return;
        }
        direction.normalize().multiply(this.nudgeStrength);
        if (!this.isSafeNudgeDirection(mob, direction)) {
            return;
        }
        Vector velocity = mob.getVelocity();
        Vector newVelocity = velocity.add(direction);
        double maxHorizontal = Math.max(0.15, this.nudgeStrength * 2.5);
        newVelocity.setX(this.clamp(newVelocity.getX(), -maxHorizontal, maxHorizontal));
        newVelocity.setZ(this.clamp(newVelocity.getZ(), -maxHorizontal, maxHorizontal));
        mob.setVelocity(newVelocity);
        ++this.lastNudged;
    }

    private boolean isSafeNudgeDirection(Mob mob, Vector direction) {
        Location ahead = mob.getLocation().clone().add(direction.clone().normalize().multiply(0.65));
        Location head = ahead.clone().add(0.0, 1.0, 0.0);
        return ahead.getBlock().isPassable() && head.getBlock().isPassable();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void setAttributeMinimum(LivingEntity entity, String attributeName, double minimum) {
        Attribute attribute = this.getAttribute(attributeName);
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (instance.getBaseValue() < minimum) {
            instance.setBaseValue(minimum);
        }
    }

    private Attribute getAttribute(String name) {
        try {
            return Attribute.valueOf((String)name);
        }
        catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub;
        if (!command.getName().equalsIgnoreCase("rkfarmai")) {
            return false;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            this.sendHelp(sender);
            return true;
        }
        switch (sub = args[0].toLowerCase(Locale.ROOT)) {
            case "reload": {
                this.loadSettings();
                this.startScanner();
                sender.sendMessage(this.prefix + this.color("&aConfig berhasil direload."));
                return true;
            }
            case "status": {
                this.sendStatus(sender);
                return true;
            }
            case "scan": {
                this.scanAllWorlds();
                sender.sendMessage(this.prefix + this.color("&aScan manual selesai. &7Mob scanned: &f" + this.lastScanned + " &7fixed: &f" + this.lastFixed + " &7nudged: &f" + this.lastNudged));
                return true;
            }
            case "on": {
                this.enabled = true;
                this.getConfig().set("enabled", (Object)true);
                this.saveConfig();
                sender.sendMessage(this.prefix + this.color("&aPlugin diaktifkan."));
                return true;
            }
            case "off": {
                this.enabled = false;
                this.getConfig().set("enabled", (Object)false);
                this.saveConfig();
                sender.sendMessage(this.prefix + this.color("&cPlugin dimatikan."));
                return true;
            }
            case "zonehere": {
                this.handleZoneHere(sender, args);
                return true;
            }
        }
        this.sendHelp(sender);
        return true;
    }

    private void handleZoneHere(CommandSender sender, String[] args) {
        double radius;
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.prefix + this.color("&cCommand ini harus dari player."));
            return;
        }
        Player player = (Player)sender;
        if (args.length < 3) {
            sender.sendMessage(this.prefix + this.color("&eGunakan: /rkfarmai zonehere <nama> <radius>"));
            sender.sendMessage(this.prefix + this.color("&7Contoh: /rkfarmai zonehere mobfarm1 80"));
            return;
        }
        String rawName = args[1].replaceAll("[^A-Za-z0-9_-]", "");
        if (rawName.isEmpty()) {
            sender.sendMessage(this.prefix + this.color("&cNama zone tidak valid."));
            return;
        }
        try {
            radius = Math.max(8.0, Double.parseDouble(args[2]));
        }
        catch (NumberFormatException exception) {
            sender.sendMessage(this.prefix + this.color("&cRadius harus angka."));
            return;
        }
        Location location = player.getLocation();
        String path = "zones.list." + rawName;
        this.getConfig().set("zones.enabled", (Object)true);
        this.getConfig().set(path + ".world", (Object)location.getWorld().getName());
        this.getConfig().set(path + ".center-x", (Object)Math.floor(location.getX()));
        this.getConfig().set(path + ".center-y", (Object)Math.floor(location.getY()));
        this.getConfig().set(path + ".center-z", (Object)Math.floor(location.getZ()));
        this.getConfig().set(path + ".radius-x", (Object)radius);
        this.getConfig().set(path + ".radius-y", (Object)radius);
        this.getConfig().set(path + ".radius-z", (Object)radius);
        this.saveConfig();
        this.loadSettings();
        sender.sendMessage(this.prefix + this.color("&aZone &f" + rawName + " &adibuat di lokasi kamu dengan radius &f" + radius + "&a."));
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(this.prefix + this.color("&fStatus: " + (this.enabled ? "&aON" : "&cOFF")));
        sender.sendMessage(this.color("&7Interval: &f" + this.intervalTicks + " tick &8| &7Max mobs/scan: &f" + this.maxMobsPerScan));
        sender.sendMessage(this.color("&7Radius: &f" + this.horizontalRadius + "x" + this.verticalRadius + " &8| &7Follow range: &f" + this.followRange));
        sender.sendMessage(this.color("&7Zones: &f" + (String)(this.zonesEnabled ? "ON (" + this.zones.size() + ")" : "OFF")));
        sender.sendMessage(this.color("&7Last scan players: &f" + this.lastPlayers + " &8| &7scanned: &f" + this.lastScanned + " &8| &7fixed: &f" + this.lastFixed + " &8| &7nudged: &f" + this.lastNudged));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(this.prefix + this.color("&eCommands:"));
        sender.sendMessage(this.color("&a/rkfarmai status &7- cek status plugin"));
        sender.sendMessage(this.color("&a/rkfarmai scan &7- scan manual sekarang"));
        sender.sendMessage(this.color("&a/rkfarmai reload &7- reload config"));
        sender.sendMessage(this.color("&a/rkfarmai on/off &7- aktifkan/matikan plugin"));
        sender.sendMessage(this.color("&a/rkfarmai zonehere <nama> <radius> &7- buat zone mob farm di lokasi kamu"));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)(message == null ? "" : message));
    }

    private static final class FarmZone {
        private final String name;
        private final String worldName;
        private final double centerX;
        private final double centerY;
        private final double centerZ;
        private final double radiusX;
        private final double radiusY;
        private final double radiusZ;

        private FarmZone(String name, String worldName, double centerX, double centerY, double centerZ, double radiusX, double radiusY, double radiusZ) {
            this.name = name;
            this.worldName = worldName;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.radiusX = radiusX;
            this.radiusY = radiusY;
            this.radiusZ = radiusZ;
        }

        private boolean contains(Location location) {
            if (location == null || location.getWorld() == null) {
                return false;
            }
            if (!location.getWorld().getName().equalsIgnoreCase(this.worldName)) {
                return false;
            }
            return Math.abs(location.getX() - this.centerX) <= this.radiusX && Math.abs(location.getY() - this.centerY) <= this.radiusY && Math.abs(location.getZ() - this.centerZ) <= this.radiusZ;
        }
    }

    private static final class StuckData {
        private Location lastLocation;
        private int stuckScans;

        private StuckData(Location lastLocation, int stuckScans) {
            this.lastLocation = lastLocation;
            this.stuckScans = stuckScans;
        }
    }
}

