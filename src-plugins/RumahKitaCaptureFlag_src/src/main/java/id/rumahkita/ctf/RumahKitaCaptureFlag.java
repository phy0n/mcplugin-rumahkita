/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.EntityPickupItemEvent
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.scoreboard.DisplaySlot
 *  org.bukkit.scoreboard.Objective
 *  org.bukkit.scoreboard.Scoreboard
 *  org.bukkit.scoreboard.ScoreboardManager
 */
package id.rumahkita.ctf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public final class RumahKitaCaptureFlag
extends JavaPlugin
implements Listener,
CommandExecutor,
TabCompleter {
    private GameState state = GameState.IDLE;
    private final Map<UUID, Participant> participants = new LinkedHashMap<UUID, Participant>();
    private final Map<UUID, Location> deathRespawnTargets = new HashMap<UUID, Location>();
    private BukkitTask countdownTask;
    private BukkitTask gameTask;
    private BukkitTask particleTask;
    private BukkitTask scoreboardTask;
    private BukkitTask captureRotationTask;
    private CtfPlaceholderExpansion placeholderExpansion;
    private int countdownLeft;
    private int timeLeft;
    private boolean enabled;
    private int minPlayers;
    private int countdownSeconds;
    private int durationSeconds;
    private int pointsPerSecond;
    private boolean freezeBeforeStart;
    private boolean teleportToExitAfterEvent;
    private boolean restoreScoreboardAfterEvent;
    private boolean announceEveryMinute;
    private boolean allowJoinWhileCountdown;
    private boolean allowJoinWhileRunning;
    private boolean onlyAliveCanScore;
    private boolean winnersOnlyAlive;
    private boolean soundsEnabled;
    private boolean particlesEnabled;
    private boolean showCaptureRing;
    private boolean rewardsEnabled;
    private boolean arenaBoundaryEnabled;
    private boolean forceScoreboardEnabled;
    private int forceScoreboardTicks;
    private boolean showCaptureStatusInScoreboard;
    private boolean zoneActionBarEnabled;
    private boolean zoneChatMessageEnabled;
    private String captureShape;
    private final List<CapturePoint> capturePoints = new ArrayList<CapturePoint>();
    private int activeCaptureIndex = 0;
    private int captureRotateSeconds;
    private int captureNextRotateSeconds;
    private boolean captureRotationEnabled;
    private boolean captureRotationRandom;
    private boolean captureAnnounceOnRotate;
    private boolean inventorySystemEnabled;
    private boolean restoreInventoryAfterEvent;
    private boolean clearInventoryOnJoin;
    private boolean restoreBackupOnJoin;
    private boolean preventItemDrop;
    private boolean preventItemPickup;
    private boolean preventInventoryClick;
    private boolean clearDeathDrops;
    private boolean preventBlockBreak;
    private boolean preventBlockPlace;
    private boolean resetHealthFoodOnJoin;
    private boolean kitEnabled;
    private boolean kitKnockbackStick;
    private int kitStickSlot;
    private int kitKnockbackLevel;
    private String kitStickName;
    private List<String> kitStickLore;
    private String prefix;

    public void onEnable() {
        this.saveDefaultConfig();
        this.loadSettings();
        this.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this);
        if (this.getCommand("rkctf") != null) {
            this.getCommand("rkctf").setExecutor((CommandExecutor)this);
            this.getCommand("rkctf").setTabCompleter((TabCompleter)this);
        }
        this.startParticleTask();
        this.startScoreboardTask();
        this.registerPlaceholderExpansion();
        this.getLogger().info("RumahKitaCaptureFlag v1.5.0 enabled.");
    }

    public void onDisable() {
        this.stopAllTasks();
        if (this.placeholderExpansion != null) {
            this.placeholderExpansion.unregister();
            this.placeholderExpansion = null;
        }
        this.restoreAllPlayers(true);
        this.participants.clear();
        this.getLogger().info("RumahKitaCaptureFlag disabled.");
    }

    private void loadSettings() {
        this.reloadConfig();
        this.enabled = this.getConfig().getBoolean("enabled", true);
        this.minPlayers = Math.max(1, this.getConfig().getInt("settings.min-players", 2));
        this.countdownSeconds = Math.max(3, this.getConfig().getInt("settings.countdown-seconds", 15));
        this.durationSeconds = Math.max(10, this.getConfig().getInt("settings.duration-seconds", 300));
        this.pointsPerSecond = Math.max(1, this.getConfig().getInt("settings.points-per-second", 1));
        this.freezeBeforeStart = this.getConfig().getBoolean("settings.freeze-before-start", true);
        this.teleportToExitAfterEvent = this.getConfig().getBoolean("settings.teleport-to-exit-after-event", true);
        this.restoreScoreboardAfterEvent = this.getConfig().getBoolean("settings.restore-scoreboard-after-event", true);
        this.announceEveryMinute = this.getConfig().getBoolean("settings.announce-every-minute", true);
        this.allowJoinWhileCountdown = this.getConfig().getBoolean("settings.allow-join-while-countdown", true);
        this.allowJoinWhileRunning = this.getConfig().getBoolean("settings.allow-join-while-running", false);
        this.onlyAliveCanScore = this.getConfig().getBoolean("settings.only-alive-can-score", true);
        this.winnersOnlyAlive = this.getConfig().getBoolean("settings.winners-only-alive", true);
        this.arenaBoundaryEnabled = this.getConfig().getBoolean("arena.enabled", false);
        this.soundsEnabled = this.getConfig().getBoolean("sounds.enabled", true);
        this.particlesEnabled = this.getConfig().getBoolean("particles.enabled", true);
        this.showCaptureRing = this.getConfig().getBoolean("particles.show-capture-ring", true);
        this.rewardsEnabled = this.getConfig().getBoolean("rewards.enabled", true);
        this.forceScoreboardEnabled = this.getConfig().getBoolean("scoreboard.force-update.enabled", true);
        this.forceScoreboardTicks = Math.max(1, this.getConfig().getInt("scoreboard.force-update.interval-ticks", 1));
        this.showCaptureStatusInScoreboard = this.getConfig().getBoolean("scoreboard.show-capture-status", true);
        this.zoneActionBarEnabled = this.getConfig().getBoolean("capture.actionbar.enabled", true);
        this.zoneChatMessageEnabled = this.getConfig().getBoolean("capture.chat-message-every-second", false);
        this.captureShape = this.getConfig().getString("capture.shape", "CIRCLE").toUpperCase(Locale.ROOT);
        this.captureRotationEnabled = this.getConfig().getBoolean("capture.rotation.enabled", true);
        this.captureRotationRandom = this.getConfig().getBoolean("capture.rotation.random", false);
        this.captureAnnounceOnRotate = this.getConfig().getBoolean("capture.rotation.announce", true);
        this.captureNextRotateSeconds = this.captureRotateSeconds = Math.max(5, this.getConfig().getInt("capture.rotation.interval-seconds", 25));
        this.loadCapturePoints();
        this.inventorySystemEnabled = this.getConfig().getBoolean("inventory.enabled", true);
        this.restoreInventoryAfterEvent = this.getConfig().getBoolean("inventory.restore-after-event", true);
        this.clearInventoryOnJoin = this.getConfig().getBoolean("inventory.clear-on-join", true);
        this.restoreBackupOnJoin = this.getConfig().getBoolean("inventory.restore-backup-on-join", true);
        this.preventItemDrop = this.getConfig().getBoolean("inventory.prevent-item-drop", true);
        this.preventItemPickup = this.getConfig().getBoolean("inventory.prevent-item-pickup", true);
        this.preventInventoryClick = this.getConfig().getBoolean("inventory.prevent-inventory-click", false);
        this.clearDeathDrops = this.getConfig().getBoolean("inventory.clear-death-drops", true);
        this.preventBlockBreak = this.getConfig().getBoolean("inventory.prevent-block-break", true);
        this.preventBlockPlace = this.getConfig().getBoolean("inventory.prevent-block-place", true);
        this.resetHealthFoodOnJoin = this.getConfig().getBoolean("inventory.reset-health-food-on-join", true);
        this.kitEnabled = this.getConfig().getBoolean("kit.enabled", true);
        this.kitKnockbackStick = this.getConfig().getBoolean("kit.knockback-stick.enabled", true);
        this.kitStickSlot = Math.max(0, Math.min(8, this.getConfig().getInt("kit.knockback-stick.slot", 0)));
        this.kitKnockbackLevel = Math.max(1, this.getConfig().getInt("kit.knockback-stick.knockback-level", 2));
        this.kitStickName = this.getConfig().getString("kit.knockback-stick.name", "&c&lCTF Stick &7(Knockback II)");
        this.kitStickLore = this.getConfig().getStringList("kit.knockback-stick.lore");
        this.prefix = this.color(this.getConfig().getString("messages.prefix", "&8[&dRumahKita CTF&8] "));
    }

    private void registerPlaceholderExpansion() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            this.getLogger().warning("PlaceholderAPI tidak ditemukan. Placeholder %rumahkitactf_*% tidak aktif.");
            return;
        }
        this.placeholderExpansion = new CtfPlaceholderExpansion(this);
        this.placeholderExpansion.register();
        this.getLogger().info("PlaceholderAPI hooked. Placeholders %rumahkitactf_*% aktif.");
    }

    private void stopAllTasks() {
        if (this.countdownTask != null) {
            this.countdownTask.cancel();
            this.countdownTask = null;
        }
        if (this.gameTask != null) {
            this.gameTask.cancel();
            this.gameTask = null;
        }
        if (this.captureRotationTask != null) {
            this.captureRotationTask.cancel();
            this.captureRotationTask = null;
        }
        if (this.particleTask != null) {
            this.particleTask.cancel();
            this.particleTask = null;
        }
        if (this.scoreboardTask != null) {
            this.scoreboardTask.cancel();
            this.scoreboardTask = null;
        }
    }

    private void startScoreboardTask() {
        if (this.scoreboardTask != null) {
            this.scoreboardTask.cancel();
        }
        this.scoreboardTask = Bukkit.getScheduler().runTaskTimer((Plugin)this, () -> {
            if (!this.enabled || !this.forceScoreboardEnabled) {
                return;
            }
            if (this.state == GameState.IDLE || this.participants.isEmpty()) {
                return;
            }
            this.updateAllScoreboards();
        }, 5L, (long)this.forceScoreboardTicks);
    }

    private void startParticleTask() {
        if (this.particleTask != null) {
            this.particleTask.cancel();
        }
        this.particleTask = Bukkit.getScheduler().runTaskTimer((Plugin)this, () -> {
            if (!(this.enabled && this.particlesEnabled && this.showCaptureRing)) {
                return;
            }
            if (this.state == GameState.IDLE || this.participants.isEmpty()) {
                return;
            }
            this.showActiveCaptureMarker();
        }, 20L, 10L);
    }

    private void loadCapturePoints() {
        this.capturePoints.clear();
        if (this.getConfig().isConfigurationSection("capture.points")) {
            for (String id : this.getConfig().getConfigurationSection("capture.points").getKeys(false)) {
                if (!this.getConfig().getBoolean("capture.points." + id + ".enabled", true)) continue;
                String path = "capture.points." + id;
                String world = this.getConfig().getString(path + ".world", this.getConfig().getString("capture.world", "world"));
                String name = this.getConfig().getString(path + ".name", id);
                String shape = this.getConfig().getString(path + ".shape", this.getConfig().getString("capture.shape", "CIRCLE"));
                double radius = this.getConfig().getDouble(path + ".radius", this.getConfig().getDouble("capture.radius", 13.0));
                double radiusX = this.getConfig().getDouble(path + ".radius-x", radius);
                double radiusZ = this.getConfig().getDouble(path + ".radius-z", radius);
                double x = this.getConfig().getDouble(path + ".x", this.getConfig().getDouble("capture.center-x", 4286.0));
                double z = this.getConfig().getDouble(path + ".z", this.getConfig().getDouble("capture.center-z", 2394.0));
                double minY = this.getConfig().getDouble(path + ".min-y", this.getConfig().getDouble("capture.min-y", 120.0));
                double maxY = this.getConfig().getDouble(path + ".max-y", this.getConfig().getDouble("capture.max-y", 150.0));
                this.capturePoints.add(new CapturePoint(id, name, world, shape, x, z, radius, radiusX, radiusZ, minY, maxY));
            }
        }
        if (this.capturePoints.isEmpty()) {
            this.capturePoints.add(new CapturePoint("legacy_center", this.getConfig().getString("capture.name", "Tengah"), this.getConfig().getString("capture.world", "world"), this.getConfig().getString("capture.shape", "CIRCLE"), this.getConfig().getDouble("capture.center-x", 4286.0), this.getConfig().getDouble("capture.center-z", 2394.0), this.getConfig().getDouble("capture.radius", 13.0), this.getConfig().getDouble("capture.radius-x", this.getConfig().getDouble("capture.radius", 13.0)), this.getConfig().getDouble("capture.radius-z", this.getConfig().getDouble("capture.radius", 13.0)), this.getConfig().getDouble("capture.min-y", 120.0), this.getConfig().getDouble("capture.max-y", 150.0)));
        }
        if (this.activeCaptureIndex < 0 || this.activeCaptureIndex >= this.capturePoints.size()) {
            this.activeCaptureIndex = 0;
        }
    }

    private CapturePoint getActiveCapture() {
        if (this.capturePoints.isEmpty()) {
            this.loadCapturePoints();
        }
        if (this.capturePoints.isEmpty()) {
            return null;
        }
        if (this.activeCaptureIndex < 0 || this.activeCaptureIndex >= this.capturePoints.size()) {
            this.activeCaptureIndex = 0;
        }
        return this.capturePoints.get(this.activeCaptureIndex);
    }

    private void startCaptureRotationTask() {
        if (this.captureRotationTask != null) {
            this.captureRotationTask.cancel();
            this.captureRotationTask = null;
        }
        this.captureNextRotateSeconds = this.captureRotateSeconds;
        if (!this.captureRotationEnabled || this.capturePoints.size() <= 1) {
            return;
        }
        this.captureRotationTask = Bukkit.getScheduler().runTaskTimer((Plugin)this, () -> {
            if (this.state != GameState.RUNNING) {
                return;
            }
            --this.captureNextRotateSeconds;
            if (this.captureNextRotateSeconds <= 0) {
                this.rotateCapturePoint(true);
                this.captureNextRotateSeconds = this.captureRotateSeconds;
            }
        }, 20L, 20L);
    }

    private void rotateCapturePoint(boolean announce) {
        if (this.capturePoints.size() <= 1) {
            this.activeCaptureIndex = 0;
            return;
        }
        int old = this.activeCaptureIndex;
        if (this.captureRotationRandom) {
            int next = old;
            int guard = 0;
            while (next == old && guard++ < 20) {
                next = ThreadLocalRandom.current().nextInt(this.capturePoints.size());
            }
            this.activeCaptureIndex = next;
        } else {
            this.activeCaptureIndex = (this.activeCaptureIndex + 1) % this.capturePoints.size();
        }
        CapturePoint point = this.getActiveCapture();
        if (point == null) {
            return;
        }
        for (Participant p : this.participants.values()) {
            p.lastInsideCapture = false;
        }
        if (announce && this.captureAnnounceOnRotate) {
            this.broadcast(this.getConfig().getString("messages.capture-moved", "&eCapture point pindah ke &d%point%&e! Ikuti marker partikel yang menyala.").replace("%point%", point.name));
            for (Player player : this.getOnlineParticipants()) {
                player.sendTitle(this.color("&d&lCAPTURE PINDAH"), this.color("&fLokasi aktif: &e" + point.name), 5, 35, 8);
                this.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
            }
        }
    }

    private void showActiveCaptureMarker() {
        CapturePoint point = this.getActiveCapture();
        if (point == null) {
            return;
        }
        World world = Bukkit.getWorld((String)point.world);
        if (world == null) {
            return;
        }
        double y = (point.minY + point.maxY) / 2.0;
        double radius = Math.max(1.0, point.radius);
        int circlePoints = Math.max(40, (int)(radius * 10.0));
        for (int i = 0; i < circlePoints; ++i) {
            double angle = Math.PI * 2 * (double)i / (double)circlePoints;
            double x = point.x + Math.cos(angle) * radius;
            double z = point.z + Math.sin(angle) * radius;
            world.spawnParticle(Particle.END_ROD, x, y, z, 1, 0.0, 0.0, 0.0, 0.01);
            if (i % 4 != 0) continue;
            world.spawnParticle(Particle.FLAME, x, y + 0.35, z, 1, 0.0, 0.0, 0.0, 0.01);
        }
        for (double yy = point.minY; yy <= point.maxY + 8.0; yy += 0.75) {
            world.spawnParticle(Particle.END_ROD, point.x, yy, point.z, 2, 0.08, 0.02, 0.08, 0.01);
        }
        world.spawnParticle(Particle.FLAME, point.x, y + 1.0, point.z, 12, 0.6, 0.6, 0.6, 0.02);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            this.sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("join")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(this.prefix + "Command ini hanya untuk player.");
                return true;
            }
            Player player = (Player)sender;
            this.joinEvent(player);
            return true;
        }
        if (sub.equals("leave")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(this.prefix + "Command ini hanya untuk player.");
                return true;
            }
            Player player = (Player)sender;
            this.leaveEvent(player, true);
            return true;
        }
        if (sub.equals("status")) {
            this.sendStatus(sender);
            return true;
        }
        if (!sender.hasPermission("rumahkita.ctf.admin")) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Kamu tidak punya permission admin CTF.");
            return true;
        }
        switch (sub) {
            case "start": {
                this.startCountdown(sender, false);
                break;
            }
            case "forcestart": {
                this.startCountdown(sender, true);
                break;
            }
            case "stop": {
                this.stopEvent(sender);
                break;
            }
            case "reload": {
                this.loadSettings();
                this.startParticleTask();
                this.startScoreboardTask();
                sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Config berhasil direload.");
                break;
            }
            case "list": {
                this.sendParticipantList(sender);
                break;
            }
            case "setside1": {
                this.setLocation(sender, "spawns.side1");
                break;
            }
            case "setside2": {
                this.setLocation(sender, "spawns.side2");
                break;
            }
            case "setexit": {
                this.setLocation(sender, "spawns.exit");
                break;
            }
            case "setarena": {
                this.setArena(sender, args);
                break;
            }
            case "setcapture": {
                this.setCapture(sender, args);
                break;
            }
            case "setcapturebox": {
                this.setCaptureBox(sender, args);
                break;
            }
            case "nextcapture": {
                this.rotateCapturePoint(true);
                sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Capture point dipindahkan manual ke: " + this.getActiveCaptureName());
                break;
            }
            case "listcaptures": {
                this.listCapturePoints(sender);
                break;
            }
            case "check": {
                this.checkPosition(sender);
                break;
            }
            case "setduration": {
                this.setDuration(sender, args);
                break;
            }
            case "reset": {
                this.resetEvent(sender);
                break;
            }
            case "restoreitems": {
                this.restoreItemsCommand(sender, args);
                break;
            }
            case "backupstatus": {
                this.backupStatusCommand(sender, args);
                break;
            }
            case "clearbackup": {
                this.clearBackupCommand(sender, args);
                break;
            }
            default: {
                this.sendHelp(sender);
            }
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(this.color("&8&m------------------------------"));
        sender.sendMessage(this.color("&d&lRumahKita Capture Flag"));
        sender.sendMessage(this.color("&e/rkctf join &7- join event"));
        sender.sendMessage(this.color("&e/rkctf leave &7- keluar event"));
        sender.sendMessage(this.color("&e/rkctf status &7- lihat status event"));
        if (sender.hasPermission("rumahkita.ctf.admin")) {
            sender.sendMessage(this.color("&cAdmin:"));
            sender.sendMessage(this.color("&e/rkctf start &7- mulai countdown"));
            sender.sendMessage(this.color("&e/rkctf forcestart &7- paksa mulai"));
            sender.sendMessage(this.color("&e/rkctf stop &7- stop event"));
            sender.sendMessage(this.color("&e/rkctf setside1 &7- set spawn sisi 1"));
            sender.sendMessage(this.color("&e/rkctf setside2 &7- set spawn sisi 2"));
            sender.sendMessage(this.color("&e/rkctf setexit &7- set exit setelah event"));
            sender.sendMessage(this.color("&e/rkctf setarena <radius> &7- set arena center"));
            sender.sendMessage(this.color("&e/rkctf setcapture <radius> <minY> <maxY> &7- set lingkaran capture"));
            sender.sendMessage(this.color("&e/rkctf setcapturebox <radiusX> <radiusZ> <minY> <maxY> &7- set kotak capture"));
            sender.sendMessage(this.color("&e/rkctf nextcapture &7- pindah capture point aktif manual"));
            sender.sendMessage(this.color("&e/rkctf listcaptures &7- lihat semua capture point"));
            sender.sendMessage(this.color("&e/rkctf check &7- cek apakah kamu masuk capture zone"));
            sender.sendMessage(this.color("&e/rkctf setduration <detik> &7- set durasi event"));
            sender.sendMessage(this.color("&e/rkctf restoreitems <player> &7- restore backup item manual"));
            sender.sendMessage(this.color("&e/rkctf backupstatus <player> &7- cek backup item"));
            sender.sendMessage(this.color("&e/rkctf clearbackup <player> &7- hapus backup item"));
            sender.sendMessage(this.color("&e/rkctf reload &7- reload config"));
        }
        sender.sendMessage(this.color("&8&m------------------------------"));
    }

    private void joinEvent(Player player) {
        if (!this.enabled) {
            player.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Event sedang disabled.");
            return;
        }
        if (!player.hasPermission("rumahkita.ctf.use")) {
            player.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Kamu tidak punya permission untuk join event.");
            return;
        }
        if (this.participants.containsKey(player.getUniqueId())) {
            player.sendMessage(this.prefix + this.msg("messages.already-joined"));
            return;
        }
        if (this.state == GameState.RUNNING && !this.allowJoinWhileRunning) {
            player.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Event sudah berjalan, tidak bisa join sekarang.");
            return;
        }
        if (this.state == GameState.COUNTDOWN && !this.allowJoinWhileCountdown) {
            player.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Event sedang countdown, tidak bisa join sekarang.");
            return;
        }
        if (this.state == GameState.ENDING) {
            player.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Event sedang selesai, tunggu reset.");
            return;
        }
        if (this.restoreBackupOnJoin && this.hasBackup(player.getUniqueId()) && !this.participants.containsKey(player.getUniqueId())) {
            this.restoreInventoryBackup(player, true, false);
            player.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Backup item event lama ditemukan dan sudah direstore dulu sebelum join.");
        }
        String team = this.chooseTeam();
        Participant participant = new Participant(player, team);
        this.participants.put(player.getUniqueId(), participant);
        this.preparePlayerForEvent(player, participant);
        Location spawn = this.getTeamSpawn(team);
        if (spawn != null) {
            player.teleport(spawn);
        }
        player.setGameMode(GameMode.SURVIVAL);
        this.updateScoreboard(player);
        this.broadcast(this.applyPlaceholders(this.msg("messages.join"), player, participant, 0));
    }

    private String chooseTeam() {
        int side1 = 0;
        int side2 = 0;
        for (Participant p : this.participants.values()) {
            if (p.team.equalsIgnoreCase("Side 1")) {
                ++side1;
                continue;
            }
            ++side2;
        }
        return side1 <= side2 ? "Side 1" : "Side 2";
    }

    private void preparePlayerForEvent(Player player, Participant participant) {
        if (participant.prepared) {
            return;
        }
        participant.originalLocation = player.getLocation().clone();
        participant.previousScoreboard = player.getScoreboard();
        if (this.inventorySystemEnabled) {
            this.saveInventoryBackup(player, participant.originalLocation);
            if (this.clearInventoryOnJoin) {
                PlayerInventory inv = player.getInventory();
                inv.setStorageContents(new ItemStack[inv.getStorageContents().length]);
                inv.setArmorContents(new ItemStack[4]);
                inv.setItemInOffHand(null);
                player.updateInventory();
            }
            if (this.resetHealthFoodOnJoin) {
                try {
                    player.setHealth(Math.max(1.0, player.getMaxHealth()));
                }
                catch (Exception ignored) {
                    try {
                        player.setHealth(20.0);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                player.setFoodLevel(20);
                player.setSaturation(8.0f);
                player.setFireTicks(0);
                player.setFallDistance(0.0f);
            }
            this.giveEventKit(player);
        }
        participant.prepared = true;
    }

    private void giveEventKit(Player player) {
        if (!this.kitEnabled) {
            return;
        }
        if (this.kitKnockbackStick) {
            player.getInventory().setItem(this.kitStickSlot, this.createKnockbackStick());
        }
        List commands = this.getConfig().getStringList("kit.commands");
        for (String raw : commands) {
            String cmd = raw.replace("%player%", player.getName());
            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)cmd);
        }
        player.updateInventory();
    }

    private ItemStack createKnockbackStick() {
        ItemStack item = new ItemStack(Material.STICK, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Enchantment knockback;
            meta.setDisplayName(this.color(this.kitStickName));
            if (!this.kitStickLore.isEmpty()) {
                ArrayList<String> lore = new ArrayList<String>();
                for (String line : this.kitStickLore) {
                    lore.add(this.color(line));
                }
                meta.setLore(lore);
            }
            if ((knockback = Enchantment.getByKey((NamespacedKey)NamespacedKey.minecraft((String)"knockback"))) != null) {
                meta.addEnchant(knockback, this.kitKnockbackLevel, true);
            }
            meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
            item.setItemMeta(meta);
        }
        return item;
    }

    private void leaveEvent(Player player, boolean announce) {
        Participant participant = this.participants.remove(player.getUniqueId());
        if (participant == null) {
            player.sendMessage(this.prefix + this.msg("messages.not-joined"));
            return;
        }
        this.restorePlayer(player, participant, true, true);
        if (announce) {
            this.broadcast(this.applyPlaceholders(this.msg("messages.leave"), player, participant, 0));
        }
        if (this.participants.isEmpty() && this.state != GameState.IDLE) {
            this.forceStopNoPlayers();
        }
    }

    private void startCountdown(CommandSender sender, boolean force) {
        if (this.state != GameState.IDLE) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Event sedang berjalan/countdown.");
            return;
        }
        if (!force && this.participants.size() < this.minPlayers) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Minimal player: " + this.minPlayers + ". Sekarang: " + this.participants.size());
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Pakai /rkctf forcestart kalau mau paksa mulai.");
            return;
        }
        if (this.participants.isEmpty()) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Belum ada player yang join.");
            return;
        }
        this.state = GameState.COUNTDOWN;
        this.countdownLeft = this.countdownSeconds;
        this.teleportAllToTeams();
        this.updateAllScoreboards();
        this.broadcast("&eCapture Flag akan dimulai. Player dikunci di sisi masing-masing dulu.");
        this.countdownTask = Bukkit.getScheduler().runTaskTimer((Plugin)this, () -> {
            if (this.countdownLeft <= 0) {
                if (this.countdownTask != null) {
                    this.countdownTask.cancel();
                    this.countdownTask = null;
                }
                this.startGame();
                return;
            }
            this.broadcast(this.applyTime(this.msg("messages.countdown"), this.countdownLeft));
            for (Player player : this.getOnlineParticipants()) {
                player.sendTitle(this.color("&d&lCapture Flag"), this.color("&eMulai dalam &c" + this.countdownLeft + " &edetik"), 5, 20, 5);
                this.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.5f);
            }
            --this.countdownLeft;
        }, 0L, 20L);
    }

    private void startGame() {
        this.state = GameState.RUNNING;
        this.timeLeft = this.durationSeconds;
        this.activeCaptureIndex = 0;
        this.captureNextRotateSeconds = this.captureRotateSeconds;
        for (Participant p : this.participants.values()) {
            p.alive = true;
            p.points = 0;
        }
        this.teleportAllToTeams();
        this.updateAllScoreboards();
        this.startCaptureRotationTask();
        this.broadcast(this.msg("messages.started"));
        CapturePoint active = this.getActiveCapture();
        if (active != null) {
            this.broadcast(this.getConfig().getString("messages.capture-active", "&eCapture point aktif sekarang: &d%point%&e. Ikuti marker partikel yang menyala.").replace("%point%", active.name));
        }
        for (Player player : this.getOnlineParticipants()) {
            player.sendTitle(this.color("&a&lMULAI!"), this.color("&fRebut area tengah dan kumpulkan point!"), 10, 40, 10);
            this.playSound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.0f);
        }
        this.gameTask = Bukkit.getScheduler().runTaskTimer((Plugin)this, () -> {
            if (this.state != GameState.RUNNING) {
                return;
            }
            this.tickGame();
        }, 20L, 20L);
    }

    private void tickGame() {
        this.processPlayers();
        this.updateAllScoreboards();
        if (this.announceEveryMinute && this.timeLeft > 0 && this.timeLeft % 60 == 0) {
            this.broadcast(this.applyTime(this.msg("messages.time-left"), this.timeLeft));
        }
        if (this.timeLeft == 30 || this.timeLeft == 10 || this.timeLeft == 5) {
            this.broadcast(this.applyTime(this.msg("messages.time-left"), this.timeLeft));
            for (Player p : this.getOnlineParticipants()) {
                this.playSound(p, Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.0f);
            }
        }
        if (this.timeLeft <= 0 || this.getAliveCount() <= 0) {
            this.endEvent(false);
            return;
        }
        --this.timeLeft;
    }

    private void processPlayers() {
        for (Participant participant : this.participants.values()) {
            boolean insideCapture;
            Player player = Bukkit.getPlayer((UUID)participant.uuid);
            if (player == null || !player.isOnline() || this.onlyAliveCanScore && !participant.alive || player.isDead()) continue;
            boolean wasInsideCapture = participant.lastInsideCapture;
            participant.lastInsideCapture = insideCapture = this.isInsideCapture(player.getLocation());
            if (insideCapture) {
                participant.points += this.pointsPerSecond;
                if (this.zoneChatMessageEnabled) {
                    player.sendMessage(this.prefix + this.applyPlaceholders(this.msg("messages.zone-score"), player, participant, this.pointsPerSecond));
                }
                if (this.zoneActionBarEnabled) {
                    this.sendActionBar(player, this.applyPlaceholders(this.msg("messages.zone-actionbar"), player, participant, this.pointsPerSecond));
                }
                this.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.25f, 1.8f);
                continue;
            }
            if (this.zoneActionBarEnabled) {
                this.sendActionBar(player, this.color("&7CTF: cari marker &d" + this.getActiveCaptureName() + " &8| &fPoint kamu: &a" + participant.points));
            }
            if (wasInsideCapture && this.zoneActionBarEnabled) {
                this.sendActionBar(player, this.color("&7Keluar dari area capture. Point kamu: &a" + participant.points));
            }
            if (!this.arenaBoundaryEnabled || this.isInsideArena(player.getLocation())) continue;
            Location spawn = this.getTeamSpawn(participant.team);
            if (spawn != null) {
                player.teleport(spawn);
            }
            player.sendMessage(this.prefix + this.msg("messages.out-of-arena"));
        }
    }

    private void stopEvent(CommandSender sender) {
        if (this.state == GameState.IDLE) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Event belum berjalan.");
            return;
        }
        this.endEvent(true);
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Event dihentikan paksa.");
    }

    private void forceStopNoPlayers() {
        this.stopAllGameTasksOnly();
        this.state = GameState.IDLE;
    }

    private void endEvent(boolean forced) {
        if (this.state == GameState.ENDING) {
            return;
        }
        this.state = GameState.ENDING;
        this.stopAllGameTasksOnly();
        List<Participant> winners = this.getWinners();
        this.broadcast(this.msg("messages.ended"));
        this.broadcast("&8&m----------------------------------");
        this.broadcast("&d&lHASIL CAPTURE FLAG RUMAHKITA S2");
        if (winners.isEmpty()) {
            this.broadcast(this.msg("messages.no-winners"));
        } else {
            int rank = 1;
            for (Participant winner : winners) {
                String line = this.msg("messages.winner-line").replace("%rank%", String.valueOf(rank)).replace("%player%", winner.name).replace("%points%", String.valueOf(winner.points));
                this.broadcast(line);
                ++rank;
            }
        }
        this.broadcast("&8&m----------------------------------");
        for (Player player : this.getOnlineParticipants()) {
            this.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
            player.sendTitle(this.color("&d&lCTF SELESAI"), this.color("&fCek pemenang di chat!"), 10, 60, 10);
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
            this.restoreAllPlayers(this.teleportToExitAfterEvent);
            this.giveRewardsAfterRestore(winners);
            this.participants.clear();
            this.state = GameState.IDLE;
        }, 60L);
    }

    private void stopAllGameTasksOnly() {
        if (this.countdownTask != null) {
            this.countdownTask.cancel();
            this.countdownTask = null;
        }
        if (this.gameTask != null) {
            this.gameTask.cancel();
            this.gameTask = null;
        }
        if (this.captureRotationTask != null) {
            this.captureRotationTask.cancel();
            this.captureRotationTask = null;
        }
    }

    private List<Participant> getWinners() {
        ArrayList<Participant> list = new ArrayList<Participant>();
        for (Participant p2 : this.participants.values()) {
            if (this.winnersOnlyAlive && !p2.alive) continue;
            list.add(p2);
        }
        list.sort(Comparator.comparingInt(p -> p.points).reversed().thenComparing(p -> p.name));
        if (list.size() > 3) {
            return new ArrayList<Participant>(list.subList(0, 3));
        }
        return list;
    }

    private void giveRewardsAfterRestore(List<Participant> winners) {
        if (!this.rewardsEnabled) {
            return;
        }
        int rank = 1;
        for (Participant winner : winners) {
            this.giveReward(winner, rank);
            ++rank;
        }
    }

    private void giveReward(Participant participant, int rank) {
        Player player = Bukkit.getPlayer((UUID)participant.uuid);
        if (player == null || !player.isOnline()) {
            return;
        }
        List commands = this.getConfig().getStringList("rewards.rank" + rank);
        for (String raw : commands) {
            String cmd = raw.replace("%player%", player.getName()).replace("%rank%", String.valueOf(rank)).replace("%points%", String.valueOf(participant.points));
            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)cmd);
        }
    }

    private void handleEventDeath(Player player) {
        Participant participant = this.participants.get(player.getUniqueId());
        if (participant == null) {
            return;
        }
        participant.alive = true;
        participant.lastInsideCapture = false;
        this.broadcast(this.applyPlaceholders(this.msg("messages.death-respawn"), player, participant, 0));
        this.playSound(player, Sound.ENTITY_PLAYER_HURT, 0.7f, 1.2f);
        Location teamSpawn = this.getTeamSpawn(participant.team);
        if (teamSpawn != null) {
            this.deathRespawnTargets.put(player.getUniqueId(), teamSpawn);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (this.state != GameState.RUNNING) {
            return;
        }
        if (!this.participants.containsKey(event.getEntity().getUniqueId())) {
            return;
        }
        if (this.clearDeathDrops) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
        this.handleEventDeath(event.getEntity());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Location target = this.deathRespawnTargets.remove(event.getPlayer().getUniqueId());
        if (target != null) {
            event.setRespawnLocation(target);
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
            Player player = event.getPlayer();
            Participant participant = this.participants.get(player.getUniqueId());
            if (participant != null && this.state == GameState.RUNNING) {
                Location teamSpawn;
                participant.alive = true;
                participant.lastInsideCapture = false;
                player.setGameMode(GameMode.SURVIVAL);
                if (this.inventorySystemEnabled && this.clearInventoryOnJoin) {
                    player.getInventory().clear();
                    player.getInventory().setArmorContents(new ItemStack[4]);
                    player.getInventory().setItemInOffHand(null);
                    this.giveEventKit(player);
                }
                if ((teamSpawn = this.getTeamSpawn(participant.team)) != null) {
                    player.teleport(teamSpawn);
                }
                try {
                    player.setHealth(Math.max(1.0, player.getMaxHealth()));
                }
                catch (Exception ignored) {
                    try {
                        player.setHealth(20.0);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                player.setFoodLevel(20);
                player.setSaturation(8.0f);
                player.setFireTicks(0);
                player.setFallDistance(0.0f);
                player.sendTitle(this.color("&a&lRESPAWN"), this.color("&7Kamu kembali ke spawn team dan lanjut main."), 5, 35, 10);
                this.updateScoreboard(player);
            }
        }, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Participant p = this.participants.get(event.getPlayer().getUniqueId());
        if (p == null) {
            return;
        }
        if (this.state == GameState.RUNNING) {
            p.alive = false;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Participant participant = this.participants.get(player.getUniqueId());
        if (participant != null) {
            this.updateScoreboard(player);
            if (!participant.alive && this.state == GameState.RUNNING) {
                participant.alive = true;
                Location teamSpawn = this.getTeamSpawn(participant.team);
                if (teamSpawn != null) {
                    player.teleport(teamSpawn);
                }
                player.setGameMode(GameMode.SURVIVAL);
                this.giveEventKit(player);
            }
            return;
        }
        if (this.restoreBackupOnJoin && this.hasBackup(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
                if (!this.participants.containsKey(player.getUniqueId()) && this.hasBackup(player.getUniqueId())) {
                    this.restoreInventoryBackup(player, true, false);
                    player.sendMessage(this.prefix + this.color("&aBackup item dari event CTF sebelumnya berhasil direstore otomatis."));
                }
            }, 20L);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!this.freezeBeforeStart) {
            return;
        }
        if (this.state == GameState.RUNNING || this.state == GameState.ENDING) {
            return;
        }
        Participant participant = this.participants.get(event.getPlayer().getUniqueId());
        if (participant == null) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            event.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(), to.getPitch()));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (this.preventItemDrop && this.participants.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(this.prefix + this.color("&cItem event tidak bisa dibuang."));
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        Player player;
        LivingEntity entity = event.getEntity();
        if (this.preventItemPickup && entity instanceof Player && this.participants.containsKey((player = (Player)entity).getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player;
        if (!this.preventInventoryClick) {
            return;
        }
        HumanEntity humanEntity = event.getWhoClicked();
        if (humanEntity instanceof Player && this.participants.containsKey((player = (Player)humanEntity).getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (this.preventBlockBreak && this.participants.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (this.preventBlockPlace && this.participants.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void teleportAllToTeams() {
        for (Participant p : this.participants.values()) {
            Player player = Bukkit.getPlayer((UUID)p.uuid);
            if (player == null || !player.isOnline()) continue;
            Location spawn = this.getTeamSpawn(p.team);
            if (spawn != null) {
                player.teleport(spawn);
            }
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    private int getAliveCount() {
        int count = 0;
        for (Participant p : this.participants.values()) {
            Player player;
            if (!p.alive || (player = Bukkit.getPlayer((UUID)p.uuid)) == null || !player.isOnline()) continue;
            ++count;
        }
        return count;
    }

    private List<Player> getOnlineParticipants() {
        ArrayList<Player> players = new ArrayList<Player>();
        for (Participant p : this.participants.values()) {
            Player player = Bukkit.getPlayer((UUID)p.uuid);
            if (player == null || !player.isOnline()) continue;
            players.add(player);
        }
        return players;
    }

    private boolean isInsideArena(Location loc) {
        double dz;
        World world = Bukkit.getWorld((String)this.getConfig().getString("arena.world", "world"));
        if (world == null || loc.getWorld() == null || !loc.getWorld().equals((Object)world)) {
            return false;
        }
        double cx = this.getConfig().getDouble("arena.center-x", 4283.0);
        double cz = this.getConfig().getDouble("arena.center-z", 2334.0);
        double radius = this.getConfig().getDouble("arena.radius", 120.0);
        double dx = loc.getX() - cx;
        return dx * dx + (dz = loc.getZ() - cz) * dz <= radius * radius;
    }

    private boolean isInsideCapture(Location loc) {
        return this.isInsideCapturePoint(loc, this.getActiveCapture());
    }

    private boolean isInsideCapturePoint(Location loc, CapturePoint point) {
        if (point == null || loc == null || loc.getWorld() == null) {
            return false;
        }
        World world = Bukkit.getWorld((String)point.world);
        if (world == null || !loc.getWorld().equals((Object)world)) {
            return false;
        }
        double y = loc.getY();
        boolean ignoreY = this.getConfig().getBoolean("capture.ignore-y", false);
        if (!ignoreY && (y < point.minY || y > point.maxY)) {
            return false;
        }
        double dx = loc.getX() - point.x;
        double dz = loc.getZ() - point.z;
        String shape = point.shape.toUpperCase(Locale.ROOT);
        if (shape.equals("BOX") || shape.equals("RECTANGLE") || shape.equals("SQUARE")) {
            return Math.abs(dx) <= point.radiusX && Math.abs(dz) <= point.radiusZ;
        }
        return dx * dx + dz * dz <= point.radius * point.radius;
    }

    private String getActiveCaptureName() {
        CapturePoint point = this.getActiveCapture();
        return point == null ? "-" : point.name;
    }

    private String getActiveCaptureCoordText() {
        CapturePoint point = this.getActiveCapture();
        if (point == null) {
            return "-";
        }
        double y = (point.minY + point.maxY) / 2.0;
        return "X " + this.round(point.x) + " Y " + this.round(y) + " Z " + this.round(point.z);
    }

    private void listCapturePoints(CommandSender sender) {
        sender.sendMessage(this.color("&8&m------------------------------"));
        sender.sendMessage(this.color("&d&lCTF Capture Points"));
        int i = 0;
        for (CapturePoint point : this.capturePoints) {
            String active = i == this.activeCaptureIndex ? " &a(AKTIF)" : "";
            sender.sendMessage(this.color("&7- &e" + point.name + active + " &8| &f" + point.world + " X " + this.round(point.x) + " Z " + this.round(point.z) + " R " + this.round(point.radius) + " Y " + this.round(point.minY) + "-" + this.round(point.maxY)));
            ++i;
        }
        sender.sendMessage(this.color("&8&m------------------------------"));
    }

    private String captureDebug(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return this.color("&cLokasi tidak valid.");
        }
        CapturePoint point = this.getActiveCapture();
        if (point == null) {
            return this.color("&cCapture point tidak ditemukan.");
        }
        double dx = loc.getX() - point.x;
        double dz = loc.getZ() - point.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        boolean inside = this.isInsideCapture(loc);
        return this.color("&7World: &e" + loc.getWorld().getName() + " &8(target " + point.world + ")\n&7Active point: &d" + point.name + "\n&7Shape: &e" + point.shape + "\n&7Posisi kamu: &eX " + this.round(loc.getX()) + " Y " + this.round(loc.getY()) + " Z " + this.round(loc.getZ()) + "\n&7Center: &eX " + this.round(point.x) + " Z " + this.round(point.z) + "\n&7Jarak horizontal: &e" + this.round(distance) + " &8(radius " + this.round(point.radius) + ")\n&7Y valid: &e" + this.round(point.minY) + " - " + this.round(point.maxY) + "\n&7Next rotate: &e" + Math.max(0, this.captureNextRotateSeconds) + "s\n&7Inside capture: " + (inside ? "&aYA" : "&cTIDAK"));
    }

    private Location getTeamSpawn(String team) {
        if (team.equalsIgnoreCase("Side 1")) {
            return this.getConfiguredLocation("spawns.side1");
        }
        return this.getConfiguredLocation("spawns.side2");
    }

    private Location getConfiguredLocation(String path) {
        String worldName = this.getConfig().getString(path + ".world", "world");
        World world = Bukkit.getWorld((String)worldName);
        if (world == null) {
            return null;
        }
        double x = this.getConfig().getDouble(path + ".x");
        double y = this.getConfig().getDouble(path + ".y");
        double z = this.getConfig().getDouble(path + ".z");
        float yaw = (float)this.getConfig().getDouble(path + ".yaw", 0.0);
        float pitch = (float)this.getConfig().getDouble(path + ".pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void setLocation(CommandSender sender, String path) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.prefix + "Command ini harus dari player.");
            return;
        }
        Player player = (Player)sender;
        Location loc = player.getLocation();
        this.getConfig().set(path + ".world", (Object)loc.getWorld().getName());
        this.getConfig().set(path + ".x", (Object)this.round(loc.getX()));
        this.getConfig().set(path + ".y", (Object)this.round(loc.getY()));
        this.getConfig().set(path + ".z", (Object)this.round(loc.getZ()));
        this.getConfig().set(path + ".yaw", (Object)this.round(loc.getYaw()));
        this.getConfig().set(path + ".pitch", (Object)this.round(loc.getPitch()));
        this.saveConfig();
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Lokasi " + path + " berhasil diset.");
    }

    private void setArena(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.prefix + "Command ini harus dari player.");
            return;
        }
        Player player = (Player)sender;
        if (args.length < 2) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Gunakan: /rkctf setarena <radius>");
            return;
        }
        double radius = this.parseDouble(args[1], 120.0);
        Location loc = player.getLocation();
        this.getConfig().set("arena.world", (Object)loc.getWorld().getName());
        this.getConfig().set("arena.center-x", (Object)this.round(loc.getX()));
        this.getConfig().set("arena.center-y", (Object)this.round(loc.getY()));
        this.getConfig().set("arena.center-z", (Object)this.round(loc.getZ()));
        this.getConfig().set("arena.radius", (Object)radius);
        this.getConfig().set("arena.enabled", (Object)true);
        this.saveConfig();
        this.loadSettings();
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Arena center diset dengan radius " + radius + ". Boundary arena sekarang aktif.");
    }

    private void setCapture(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.prefix + "Command ini harus dari player.");
            return;
        }
        Player player = (Player)sender;
        if (args.length < 4) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Gunakan: /rkctf setcapture <radius> <minY> <maxY>");
            return;
        }
        double radius = this.parseDouble(args[1], 13.0);
        double minY = this.parseDouble(args[2], player.getLocation().getY() - 5.0);
        double maxY = this.parseDouble(args[3], player.getLocation().getY() + 5.0);
        Location loc = player.getLocation();
        this.getConfig().set("capture.world", (Object)loc.getWorld().getName());
        this.getConfig().set("capture.shape", (Object)"CIRCLE");
        this.getConfig().set("capture.center-x", (Object)this.round(loc.getX()));
        this.getConfig().set("capture.center-z", (Object)this.round(loc.getZ()));
        this.getConfig().set("capture.radius", (Object)radius);
        this.getConfig().set("capture.min-y", (Object)Math.min(minY, maxY));
        this.getConfig().set("capture.max-y", (Object)Math.max(minY, maxY));
        this.saveConfig();
        this.loadSettings();
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Capture zone CIRCLE diset. Radius " + radius + ", Y " + minY + " sampai " + maxY + ".");
    }

    private void setCaptureBox(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.prefix + "Command ini harus dari player.");
            return;
        }
        Player player = (Player)sender;
        if (args.length < 5) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Gunakan: /rkctf setcapturebox <radiusX> <radiusZ> <minY> <maxY>");
            return;
        }
        double radiusX = this.parseDouble(args[1], 13.0);
        double radiusZ = this.parseDouble(args[2], 13.0);
        double minY = this.parseDouble(args[3], player.getLocation().getY() - 5.0);
        double maxY = this.parseDouble(args[4], player.getLocation().getY() + 5.0);
        Location loc = player.getLocation();
        this.getConfig().set("capture.world", (Object)loc.getWorld().getName());
        this.getConfig().set("capture.shape", (Object)"BOX");
        this.getConfig().set("capture.center-x", (Object)this.round(loc.getX()));
        this.getConfig().set("capture.center-z", (Object)this.round(loc.getZ()));
        this.getConfig().set("capture.radius-x", (Object)Math.max(1.0, radiusX));
        this.getConfig().set("capture.radius-z", (Object)Math.max(1.0, radiusZ));
        this.getConfig().set("capture.radius", (Object)Math.max(radiusX, radiusZ));
        this.getConfig().set("capture.min-y", (Object)Math.min(minY, maxY));
        this.getConfig().set("capture.max-y", (Object)Math.max(minY, maxY));
        this.saveConfig();
        this.loadSettings();
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Capture BOX diset. RadiusX " + radiusX + ", RadiusZ " + radiusZ + ", Y " + minY + " sampai " + maxY + ".");
    }

    private void checkPosition(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.prefix + "Command ini harus dari player.");
            return;
        }
        Player player = (Player)sender;
        sender.sendMessage(this.color("&8&m------------------------------"));
        sender.sendMessage(this.color("&d&lCTF Capture Check"));
        for (String line : this.captureDebug(player.getLocation()).split("\\n")) {
            sender.sendMessage(line);
        }
        Participant participant = this.participants.get(player.getUniqueId());
        if (participant != null) {
            sender.sendMessage(this.color("&7Point kamu: &a" + participant.points));
            sender.sendMessage(this.color("&7State: &e" + String.valueOf((Object)this.state)));
            if (this.state != GameState.RUNNING) {
                sender.sendMessage(this.color("&cPoint hanya bertambah saat state RUNNING. Pakai /rkctf forcestart untuk test."));
            }
        } else {
            sender.sendMessage(this.color("&7Kamu belum join event CTF."));
        }
        sender.sendMessage(this.color("&8&m------------------------------"));
    }

    private void setDuration(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Gunakan: /rkctf setduration <detik>");
            return;
        }
        int seconds = (int)this.parseDouble(args[1], 300.0);
        seconds = Math.max(10, seconds);
        this.getConfig().set("settings.duration-seconds", (Object)seconds);
        this.saveConfig();
        this.loadSettings();
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Durasi event diset ke " + seconds + " detik.");
    }

    private void resetEvent(CommandSender sender) {
        if (this.state != GameState.IDLE) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Stop event dulu sebelum reset.");
            return;
        }
        this.participants.clear();
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Participant event direset.");
    }

    private void restoreItemsCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Gunakan: /rkctf restoreitems <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target == null) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Player harus online untuk restore manual.");
            return;
        }
        if (!this.hasBackup(target.getUniqueId())) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Backup item player itu tidak ditemukan.");
            return;
        }
        this.restoreInventoryBackup(target, true, false);
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Backup item " + target.getName() + " berhasil direstore.");
    }

    private void backupStatusCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Gunakan: /rkctf backupstatus <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target != null) {
            sender.sendMessage(this.prefix + (this.hasBackup(target.getUniqueId()) ? String.valueOf(ChatColor.GREEN) + "Backup ada untuk " + target.getName() : String.valueOf(ChatColor.YELLOW) + "Backup tidak ada untuk " + target.getName()));
        } else {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Player offline. Cek file backup manual di folder backups jika perlu.");
        }
    }

    private void clearBackupCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Gunakan: /rkctf clearbackup <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target == null) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Player harus online untuk clearbackup aman.");
            return;
        }
        File file = this.getBackupFile(target.getUniqueId());
        if (file.exists() && file.delete()) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Backup " + target.getName() + " sudah dihapus.");
        } else {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Backup tidak ada / gagal dihapus.");
        }
    }

    private double parseDouble(String input, double fallback) {
        try {
            return Double.parseDouble(input);
        }
        catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double round(double value) {
        return (double)Math.round(value * 100.0) / 100.0;
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(this.color("&8&m------------------------------"));
        sender.sendMessage(this.color("&d&lRumahKita CTF Status"));
        sender.sendMessage(this.color("&7State: &e" + String.valueOf((Object)this.state)));
        if (this.state != GameState.RUNNING) {
            sender.sendMessage(this.color("&cPoint hanya bertambah saat state RUNNING. Pakai /rkctf forcestart untuk test."));
        }
        sender.sendMessage(this.color("&7Players: &e" + this.participants.size()));
        sender.sendMessage(this.color("&7Alive: &a" + this.getAliveCount()));
        sender.sendMessage(this.color("&7Time left: &e" + this.formatTime(this.timeLeft)));
        sender.sendMessage(this.color("&7Inventory backup: &e" + this.getBackupFolder().getAbsolutePath()));
        sender.sendMessage(this.color("&7Capture aktif: &d" + this.getActiveCaptureName() + " &8| &f" + this.getActiveCaptureCoordText()));
        sender.sendMessage(this.color("&7Rotasi: &e" + (String)(this.captureRotationEnabled ? this.captureRotateSeconds + "s" : "OFF") + " &8| &7Next: &e" + Math.max(0, this.captureNextRotateSeconds) + "s"));
        sender.sendMessage(this.color("&8&m------------------------------"));
    }

    private void sendParticipantList(CommandSender sender) {
        if (this.participants.isEmpty()) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Belum ada participant.");
            return;
        }
        sender.sendMessage(this.color("&dParticipant CTF:"));
        for (Participant p : this.participants.values()) {
            sender.sendMessage(this.color("&7- &e" + p.name + " &8| &f" + p.team + " &8| &a" + p.points + " point &8| " + (p.alive ? "&aAlive" : "&cGugur")));
        }
    }

    private void updateAllScoreboards() {
        for (Player player : this.getOnlineParticipants()) {
            this.updateScoreboard(player);
        }
    }

    private void updateScoreboard(Player player) {
        if (!this.forceScoreboardEnabled) {
            return;
        }
        Participant participant = this.participants.get(player.getUniqueId());
        if (participant == null) {
            return;
        }
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("rkctf", "dummy", this.color(this.getConfig().getString("scoreboard.title", "&d&lRumahKita CTF")));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        ArrayList<String> lines = new ArrayList<String>();
        lines.add(this.color(this.getConfig().getString("scoreboard.line", "&8&m----------")));
        lines.add(this.color("&fStatus: &e" + String.valueOf((Object)this.state)));
        lines.add(this.color("&fWaktu: &c" + this.formatTime(this.timeLeft)));
        lines.add(this.color("&fTeam: &b" + participant.team));
        lines.add(this.color("&fPoint kamu: &a" + participant.points));
        if (this.showCaptureStatusInScoreboard) {
            lines.add(this.color("&fCapture: &d" + this.getActiveCaptureName()));
            lines.add(this.color("&fZone: " + (this.isInsideCapture(player.getLocation()) ? "&aDI ZONE" : "&7di luar")));
        }
        lines.add(this.color("&fHidup: &a" + this.getAliveCount() + "&7/&f" + this.participants.size()));
        lines.add(this.color("&7"));
        lines.add(this.color("&dTop 3 Point"));
        int rank = 1;
        for (Participant top : this.getTopParticipants()) {
            lines.add(this.color("&6#" + rank + " &e" + top.name + " &7- &a" + top.points));
            if (++rank <= 3) continue;
            break;
        }
        while (rank <= 3) {
            lines.add(this.color("&6#" + rank + " &7-"));
            ++rank;
        }
        lines.add(this.color("&8&m----------"));
        int score = lines.size();
        int duplicate = 0;
        for (String line : lines) {
            Object entry = line;
            while (board.getEntries().contains(entry)) {
                entry = line + String.valueOf(ChatColor.values()[duplicate % ChatColor.values().length]);
                ++duplicate;
            }
            objective.getScore((String)entry).setScore(score--);
        }
        player.setScoreboard(board);
    }

    private List<Participant> getTopParticipants() {
        ArrayList<Participant> list = new ArrayList<Participant>(this.participants.values());
        list.sort(Comparator.comparingInt(p -> p.points).reversed().thenComparing(p -> p.name));
        return list;
    }

    private void restoreAllPlayers(boolean teleportExit) {
        for (Participant participant : this.participants.values()) {
            Player player = Bukkit.getPlayer((UUID)participant.uuid);
            if (player == null || !player.isOnline()) continue;
            this.restorePlayer(player, participant, teleportExit, this.restoreInventoryAfterEvent);
        }
    }

    private void restorePlayer(Player player, Participant participant, boolean teleportExit, boolean restoreInventory) {
        Location exit;
        if (this.restoreScoreboardAfterEvent && participant.previousScoreboard != null) {
            player.setScoreboard(participant.previousScoreboard);
        }
        if (restoreInventory && this.inventorySystemEnabled) {
            this.restoreInventoryBackup(player, true, false);
        }
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        if (teleportExit && (exit = this.getConfiguredLocation("spawns.exit")) != null) {
            player.teleport(exit);
        }
    }

    private File getBackupFolder() {
        File folder = new File(this.getDataFolder(), "backups");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    private File getBackupFile(UUID uuid) {
        return new File(this.getBackupFolder(), uuid.toString() + ".yml");
    }

    private boolean hasBackup(UUID uuid) {
        return this.getBackupFile(uuid).exists();
    }

    private void saveInventoryBackup(Player player, Location originalLocation) {
        File file = this.getBackupFile(player.getUniqueId());
        YamlConfiguration yaml = new YamlConfiguration();
        PlayerInventory inv = player.getInventory();
        yaml.set("player-name", (Object)player.getName());
        yaml.set("created", (Object)System.currentTimeMillis());
        yaml.set("gamemode", (Object)player.getGameMode().name());
        yaml.set("health", (Object)player.getHealth());
        yaml.set("food", (Object)player.getFoodLevel());
        yaml.set("saturation", (Object)Float.valueOf(player.getSaturation()));
        yaml.set("exp", (Object)Float.valueOf(player.getExp()));
        yaml.set("level", (Object)player.getLevel());
        yaml.set("total-exp", (Object)player.getTotalExperience());
        yaml.set("fire-ticks", (Object)player.getFireTicks());
        if (originalLocation != null && originalLocation.getWorld() != null) {
            yaml.set("location.world", (Object)originalLocation.getWorld().getName());
            yaml.set("location.x", (Object)originalLocation.getX());
            yaml.set("location.y", (Object)originalLocation.getY());
            yaml.set("location.z", (Object)originalLocation.getZ());
            yaml.set("location.yaw", (Object)Float.valueOf(originalLocation.getYaw()));
            yaml.set("location.pitch", (Object)Float.valueOf(originalLocation.getPitch()));
        }
        ItemStack[] storage = inv.getStorageContents();
        yaml.set("inventory.storage-size", (Object)storage.length);
        for (int i = 0; i < storage.length; ++i) {
            yaml.set("inventory.storage." + i, (Object)storage[i]);
        }
        ItemStack[] armor = inv.getArmorContents();
        yaml.set("inventory.armor-size", (Object)armor.length);
        for (int i = 0; i < armor.length; ++i) {
            yaml.set("inventory.armor." + i, (Object)armor[i]);
        }
        yaml.set("inventory.offhand", (Object)inv.getItemInOffHand());
        try {
            yaml.save(file);
        }
        catch (IOException e) {
            this.getLogger().warning("Gagal menyimpan inventory backup untuk " + player.getName() + ": " + e.getMessage());
            player.sendMessage(this.prefix + this.color("&cGagal backup inventory. Demi keamanan, event tidak disarankan dilanjutkan untuk kamu."));
        }
    }

    private boolean restoreInventoryBackup(Player player, boolean deleteAfterRestore, boolean teleportOriginal) {
        Location loc;
        File file = this.getBackupFile(player.getUniqueId());
        if (!file.exists()) {
            return false;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration((File)file);
        PlayerInventory inv = player.getInventory();
        int storageSize = yaml.getInt("inventory.storage-size", inv.getStorageContents().length);
        ItemStack[] storage = new ItemStack[inv.getStorageContents().length];
        for (int i = 0; i < Math.min(storage.length, storageSize); ++i) {
            storage[i] = yaml.getItemStack("inventory.storage." + i);
        }
        inv.setStorageContents(storage);
        int armorSize = yaml.getInt("inventory.armor-size", 4);
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < Math.min(armor.length, armorSize); ++i) {
            armor[i] = yaml.getItemStack("inventory.armor." + i);
        }
        inv.setArmorContents(armor);
        inv.setItemInOffHand(yaml.getItemStack("inventory.offhand"));
        try {
            GameMode gm = GameMode.valueOf((String)yaml.getString("gamemode", "SURVIVAL"));
            player.setGameMode(gm);
        }
        catch (Exception ignored) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        try {
            double health = yaml.getDouble("health", 20.0);
            double max = Math.max(1.0, player.getMaxHealth());
            player.setHealth(Math.max(1.0, Math.min(max, health)));
        }
        catch (Exception health) {
            // empty catch block
        }
        player.setFoodLevel(Math.max(0, Math.min(20, yaml.getInt("food", 20))));
        player.setSaturation((float)yaml.getDouble("saturation", 5.0));
        player.setExp((float)yaml.getDouble("exp", 0.0));
        player.setLevel(yaml.getInt("level", 0));
        player.setTotalExperience(yaml.getInt("total-exp", 0));
        player.setFireTicks(yaml.getInt("fire-ticks", 0));
        player.updateInventory();
        if (teleportOriginal && (loc = this.readLocation(yaml, "location")) != null) {
            player.teleport(loc);
        }
        if (deleteAfterRestore && file.exists() && !file.delete()) {
            this.getLogger().warning("Backup file gagal dihapus: " + file.getName());
        }
        return true;
    }

    private Location readLocation(YamlConfiguration yaml, String path) {
        String worldName = yaml.getString(path + ".world", null);
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld((String)worldName);
        if (world == null) {
            return null;
        }
        double x = yaml.getDouble(path + ".x");
        double y = yaml.getDouble(path + ".y");
        double z = yaml.getDouble(path + ".z");
        float yaw = (float)yaml.getDouble(path + ".yaw", 0.0);
        float pitch = (float)yaml.getDouble(path + ".pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void broadcast(String message) {
        String finalMessage = this.prefix + this.color(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(finalMessage);
        }
        Bukkit.getConsoleSender().sendMessage(finalMessage);
    }

    private String msg(String path) {
        return this.color(this.getConfig().getString(path, ""));
    }

    private String applyPlaceholders(String message, Player player, Participant participant, int rank) {
        return this.color(message.replace("%player%", player.getName()).replace("%team%", participant.team).replace("%points%", String.valueOf(this.pointsPerSecond)).replace("%rank%", String.valueOf(rank)).replace("%score%", String.valueOf(participant.points)));
    }

    private String applyTime(String message, int seconds) {
        return this.color(message.replace("%time%", this.formatTime(seconds)).replace("%seconds%", String.valueOf(seconds)));
    }

    private String formatTime(int seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format(Locale.US, "%02d:%02d", min, sec);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)(text == null ? "" : text));
    }

    private void sendActionBar(Player player, String message) {
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText((String)this.color(message)));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void playSound(Player player, Sound sound, float volume, float pitch) {
        if (!this.soundsEnabled) {
            return;
        }
        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            ArrayList<String> base = new ArrayList<String>();
            Collections.addAll(base, "join", "leave", "status", "help");
            if (sender.hasPermission("rumahkita.ctf.admin")) {
                Collections.addAll(base, "start", "forcestart", "stop", "reload", "list", "setside1", "setside2", "setexit", "setarena", "setcapture", "setcapturebox", "nextcapture", "listcaptures", "check", "setduration", "reset", "restoreitems", "backupstatus", "clearbackup");
            }
            return this.filter(base, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setarena")) {
            return this.filter(List.of("120", "100", "80"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setduration")) {
            return this.filter(List.of("300", "180", "600"), args[1]);
        }
        if (args[0].equalsIgnoreCase("setcapture")) {
            if (args.length == 2) {
                return this.filter(List.of("13", "15", "20"), args[1]);
            }
            if (args.length == 3) {
                return this.filter(List.of("120", "125", "130"), args[2]);
            }
            if (args.length == 4) {
                return this.filter(List.of("150", "145", "140"), args[3]);
            }
        }
        if (args[0].equalsIgnoreCase("setcapturebox")) {
            if (args.length == 2) {
                return this.filter(List.of("13", "20", "25"), args[1]);
            }
            if (args.length == 3) {
                return this.filter(List.of("13", "20", "25"), args[2]);
            }
            if (args.length == 4) {
                return this.filter(List.of("120", "125", "130"), args[3]);
            }
            if (args.length == 5) {
                return this.filter(List.of("150", "145", "140"), args[4]);
            }
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("restoreitems") || args[0].equalsIgnoreCase("backupstatus") || args[0].equalsIgnoreCase("clearbackup"))) {
            ArrayList<String> names = new ArrayList<String>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return this.filter(names, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        ArrayList<String> result = new ArrayList<String>();
        for (String option : options) {
            if (!option.toLowerCase(Locale.ROOT).startsWith(lower)) continue;
            result.add(option);
        }
        return result;
    }

    private String placeholderValue(Player player, String identifier) {
        String id = identifier == null ? "" : identifier.toLowerCase(Locale.ROOT);
        Participant participant = player == null ? null : this.participants.get(player.getUniqueId());
        return switch (id) {
            case "active", "in_event" -> {
                if (participant != null && this.state != GameState.IDLE) {
                    yield "true";
                }
                yield "false";
            }
            case "global_active", "event_active" -> {
                if (this.state != GameState.IDLE) {
                    yield "true";
                }
                yield "false";
            }
            case "running" -> {
                if (this.state == GameState.RUNNING) {
                    yield "true";
                }
                yield "false";
            }
            case "state" -> this.state.name();
            case "state_display" -> this.getStateDisplay();
            case "time", "time_left" -> this.formatTime(Math.max(0, this.timeLeft));
            case "time_seconds" -> String.valueOf(Math.max(0, this.timeLeft));
            case "countdown" -> String.valueOf(Math.max(0, this.countdownLeft));
            case "players", "participants" -> String.valueOf(this.participants.size());
            case "alive" -> String.valueOf(this.getAliveCount());
            case "score", "points" -> {
                if (participant == null) {
                    yield "0";
                }
                yield String.valueOf(participant.points);
            }
            case "rank" -> {
                if (participant == null) {
                    yield "-";
                }
                yield String.valueOf(this.getRank(participant));
            }
            case "team" -> {
                if (participant == null) {
                    yield "-";
                }
                yield participant.team;
            }
            case "is_alive" -> {
                if (participant != null && participant.alive) {
                    yield "true";
                }
                yield "false";
            }
            case "capture_inside" -> {
                if (player != null && participant != null && this.isInsideCapture(player.getLocation())) {
                    yield "true";
                }
                yield "false";
            }
            case "capture_status" -> {
                if (player != null && participant != null && this.isInsideCapture(player.getLocation())) {
                    yield this.color("&aDI ZONE");
                }
                yield this.color("&7di luar");
            }
            case "active_capture", "capture_name" -> this.getActiveCaptureName();
            case "active_capture_coord", "capture_coord" -> this.getActiveCaptureCoordText();
            case "next_capture_seconds", "next_rotation" -> String.valueOf(Math.max(0, this.captureNextRotateSeconds));
            case "top_1" -> this.getTopLine(1);
            case "top_2" -> this.getTopLine(2);
            case "top_3" -> this.getTopLine(3);
            case "top_1_name" -> this.getTopName(1);
            case "top_2_name" -> this.getTopName(2);
            case "top_3_name" -> this.getTopName(3);
            case "top_1_points" -> this.getTopPoints(1);
            case "top_2_points" -> this.getTopPoints(2);
            case "top_3_points" -> this.getTopPoints(3);
            default -> "";
        };
    }

    private String getStateDisplay() {
        return switch (this.state.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> this.getConfig().getString("placeholder.state.idle", "Menunggu");
            case 1 -> this.getConfig().getString("placeholder.state.countdown", "Countdown");
            case 2 -> this.getConfig().getString("placeholder.state.running", "Berjalan");
            case 3 -> this.getConfig().getString("placeholder.state.ending", "Selesai");
        };
    }

    private int getRank(Participant participant) {
        List<Participant> top = this.getTopParticipants();
        for (int i = 0; i < top.size(); ++i) {
            if (!top.get((int)i).uuid.equals(participant.uuid)) continue;
            return i + 1;
        }
        return 0;
    }

    private String getTopLine(int rank) {
        List<Participant> top = this.getTopParticipants();
        if (rank < 1 || rank > top.size()) {
            return this.getConfig().getString("placeholder.no-top", "-");
        }
        Participant p = top.get(rank - 1);
        return this.getConfig().getString("placeholder.top-format", "%player% - %points%").replace("%rank%", String.valueOf(rank)).replace("%player%", p.name).replace("%points%", String.valueOf(p.points));
    }

    private String getTopName(int rank) {
        List<Participant> top = this.getTopParticipants();
        if (rank < 1 || rank > top.size()) {
            return "-";
        }
        return top.get((int)(rank - 1)).name;
    }

    private String getTopPoints(int rank) {
        List<Participant> top = this.getTopParticipants();
        if (rank < 1 || rank > top.size()) {
            return "0";
        }
        return String.valueOf(top.get((int)(rank - 1)).points);
    }

    private static enum GameState {
        IDLE,
        COUNTDOWN,
        RUNNING,
        ENDING;

    }

    private static final class CtfPlaceholderExpansion
    extends PlaceholderExpansion {
        private final RumahKitaCaptureFlag plugin;

        private CtfPlaceholderExpansion(RumahKitaCaptureFlag plugin) {
            this.plugin = plugin;
        }

        public String getIdentifier() {
            return "rumahkitactf";
        }

        public String getAuthor() {
            return "HansM x ChatGPT";
        }

        public String getVersion() {
            return this.plugin.getDescription().getVersion();
        }

        public boolean persist() {
            return true;
        }

        public String onPlaceholderRequest(Player player, String identifier) {
            return this.plugin.placeholderValue(player, identifier);
        }
    }

    private static final class CapturePoint {
        private final String id;
        private final String name;
        private final String world;
        private final String shape;
        private final double x;
        private final double z;
        private final double radius;
        private final double radiusX;
        private final double radiusZ;
        private final double minY;
        private final double maxY;

        private CapturePoint(String id, String name, String world, String shape, double x, double z, double radius, double radiusX, double radiusZ, double minY, double maxY) {
            this.id = id;
            this.name = name;
            this.world = world;
            this.shape = shape == null ? "CIRCLE" : shape.toUpperCase(Locale.ROOT);
            this.x = x;
            this.z = z;
            this.radius = radius;
            this.radiusX = radiusX;
            this.radiusZ = radiusZ;
            this.minY = Math.min(minY, maxY);
            this.maxY = Math.max(minY, maxY);
        }
    }

    private static final class Participant {
        private final UUID uuid;
        private final String name;
        private String team;
        private int points;
        private boolean alive;
        private Location originalLocation;
        private Scoreboard previousScoreboard;
        private boolean prepared;
        private boolean lastInsideCapture;

        private Participant(Player player, String team) {
            this.uuid = player.getUniqueId();
            this.name = player.getName();
            this.team = team;
            this.points = 0;
            this.alive = true;
            this.originalLocation = player.getLocation().clone();
            this.previousScoreboard = player.getScoreboard();
            this.prepared = false;
            this.lastInsideCapture = false;
        }
    }
}

