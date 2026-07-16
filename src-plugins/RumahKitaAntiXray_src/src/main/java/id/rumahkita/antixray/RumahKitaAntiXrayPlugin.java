/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Statistic
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package id.rumahkita.antixray;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class RumahKitaAntiXrayPlugin
extends JavaPlugin
implements Listener,
TabExecutor {
    private final Map<UUID, RiskData> risks = new HashMap<UUID, RiskData>();
    private final Map<UUID, Long> notifyCooldown = new HashMap<UUID, Long>();
    private final Map<UUID, Long> discordCooldown = new HashMap<UUID, Long>();
    private final Map<UUID, Long> freezeUntil = new HashMap<UUID, Long>();
    private final Map<UUID, Long> limitedUntil = new HashMap<UUID, Long>();
    private File dataFile;
    private YamlConfiguration data;
    private final HttpClient http = HttpClient.newHttpClient();

    public void onEnable() {
        this.saveDefaultConfig();
        this.saveResource("data.yml", false);
        this.dataFile = new File(this.getDataFolder(), "data.yml");
        this.data = YamlConfiguration.loadConfiguration((File)this.dataFile);
        this.loadData();
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        this.getCommand("rkxray").setExecutor((CommandExecutor)this);
        this.getCommand("rkxray").setTabCompleter((TabCompleter)this);
        long decayTicks = Math.max(1L, this.getConfig().getLong("risk.decay-every-minutes", 10L)) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer((Plugin)this, this::decayScores, decayTicks, decayTicks);
        long saveTicks = Math.max(1L, this.getConfig().getLong("logs.save-data-every-minutes", 5L)) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer((Plugin)this, this::saveData, saveTicks, saveTicks);
        this.getLogger().info("RumahKitaAntiXray v1.0.6 enabled.");
    }

    public void onDisable() {
        this.saveData();
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onBlockBreak(BlockBreakEvent event) {
        String reason;
        if (!this.enabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (!this.shouldCheck(player)) {
            return;
        }
        Block block = event.getBlock();
        Material material = block.getType();
        if (!this.isWorldAllowed(block.getWorld())) {
            return;
        }
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();
        RiskData risk = this.risks.computeIfAbsent(uuid, id -> new RiskData(player.getName()));
        risk.name = player.getName();
        risk.lastWorld = block.getWorld().getName();
        risk.lastX = block.getX();
        risk.lastY = block.getY();
        risk.lastZ = block.getZ();
        risk.lastSeen = now;
        risk.bedrock = this.isBedrock(player);
        boolean rareOre = this.isRareOre(material);
        boolean normalBlock = this.isNormalMiningBlock(material);
        if (!rareOre && !normalBlock) {
            return;
        }
        risk.events.addLast(new MineEvent(now, material.name(), block.getX(), block.getY(), block.getZ(), block.getWorld().getName(), rareOre));
        this.trimEvents(risk);
        if (normalBlock) {
            ++risk.normalBlocks;
            return;
        }
        if (this.isLimited(uuid)) {
            event.setCancelled(true);
            this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.limited", "&cMining ore dibatasi sementara."));
            return;
        }
        ++risk.rareOres;
        int add = this.calculateOreRisk(player, material, block.getLocation(), risk);
        risk.score += add;
        ++risk.flags;
        risk.lastReason = reason = this.buildReason(player, material, block.getLocation(), risk, add);
        this.writeLog(player, material, block.getLocation(), add, risk.score, reason);
        this.maybeAlert(player, block.getLocation(), risk.score, reason);
        this.applyAction(player, risk, reason);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Long until = this.freezeUntil.get(player.getUniqueId());
        if (until == null) {
            return;
        }
        if (until <= System.currentTimeMillis()) {
            this.freezeUntil.remove(player.getUniqueId());
            return;
        }
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockY() != event.getTo().getBlockY() || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setTo(event.getFrom());
            this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.frozen", "&cKamu sedang dibekukan sementara."));
        }
    }

    private int calculateOreRisk(Player player, Material material, Location loc, RiskData risk) {
        double ratio;
        int minBlocks;
        int recentRare;
        int recentNormal;
        int base = this.getConfig().getInt("ore-points." + material.name(), 5);
        int extra = 0;
        int diamondCount = this.countRecent(risk, this.getConfig().getInt("thresholds.diamond-window-minutes", 10), "DIAMOND");
        int ancientCount = this.countRecent(risk, this.getConfig().getInt("thresholds.ancient-window-minutes", 20), "ANCIENT_DEBRIS");
        int emeraldCount = this.countRecent(risk, this.getConfig().getInt("thresholds.emerald-window-minutes", 10), "EMERALD");
        if (material.name().contains("DIAMOND") && diamondCount > this.getConfig().getInt("thresholds.diamond-max-in-window", 14)) {
            extra += 30;
        }
        if (material == Material.ANCIENT_DEBRIS && ancientCount > this.getConfig().getInt("thresholds.ancient-max-in-window", 6)) {
            extra += 45;
        }
        if (material.name().contains("EMERALD") && emeraldCount > this.getConfig().getInt("thresholds.emerald-max-in-window", 10)) {
            extra += 25;
        }
        if ((recentNormal = this.countRecentNormal(risk, this.getConfig().getInt("detection.recent-window-minutes", 15))) + (recentRare = this.countRecentRare(risk, this.getConfig().getInt("detection.recent-window-minutes", 15))) >= (minBlocks = this.getConfig().getInt("thresholds.min-blocks-for-ratio-check", 80)) && (ratio = (double)recentRare * 100.0 / (double)Math.max(1, recentNormal + recentRare)) >= this.getConfig().getDouble("thresholds.rare-ore-ratio-percent", 9.0)) {
            extra += 25;
        }
        if (this.isNewbie(player) && recentRare > this.getConfig().getInt("thresholds.newbie-rare-ore-limit", 6)) {
            extra += 30;
        }
        if (material.name().contains("DIAMOND") && loc.getBlockY() >= this.getConfig().getInt("thresholds.diamond-suspicious-y-min", -64) && loc.getBlockY() <= this.getConfig().getInt("thresholds.diamond-suspicious-y-max", -45)) {
            extra += 4;
        }
        if (material == Material.ANCIENT_DEBRIS && loc.getBlockY() >= this.getConfig().getInt("thresholds.ancient-suspicious-y-min", 8) && loc.getBlockY() <= this.getConfig().getInt("thresholds.ancient-suspicious-y-max", 18)) {
            extra += 6;
        }
        double mult = 1.0;
        if (this.isNewbie(player)) {
            mult *= this.getConfig().getDouble("detection.newbie-risk-multiplier", 1.35);
        }
        return Math.max(1, (int)Math.round((double)(base + extra) * (mult *= this.isBedrock(player) ? this.getConfig().getDouble("detection.bedrock-risk-multiplier", 1.0) : this.getConfig().getDouble("detection.java-risk-multiplier", 1.0))));
    }

    private String buildReason(Player player, Material material, Location loc, RiskData risk, int add) {
        int mins = this.getConfig().getInt("detection.recent-window-minutes", 15);
        return material.name() + " +" + add + ", recentRare=" + this.countRecentRare(risk, mins) + ", recentNormal=" + this.countRecentNormal(risk, mins) + ", platform=" + (this.isBedrock(player) ? "Bedrock" : "Java") + ", y=" + loc.getBlockY();
    }

    private void maybeAlert(Player player, Location loc, int score, String reason) {
        if (score < this.getConfig().getInt("risk.alert-score", 60)) {
            return;
        }
        long now = System.currentTimeMillis();
        long cooldown = Math.max(1L, this.getConfig().getLong("alerts.cooldown-seconds", 20L)) * 1000L;
        if (now - this.notifyCooldown.getOrDefault(player.getUniqueId(), 0L) >= cooldown) {
            this.notifyCooldown.put(player.getUniqueId(), now);
            if (this.getConfig().getBoolean("alerts.in-game", true)) {
                String message = this.getConfig().getString("alerts.message", "&cAntiXray alert.").replace("%player%", player.getName()).replace("%score%", String.valueOf(score)).replace("%reason%", reason).replace("%x%", String.valueOf(loc.getBlockX())).replace("%y%", String.valueOf(loc.getBlockY())).replace("%z%", String.valueOf(loc.getBlockZ()));
                for (Player staff : Bukkit.getOnlinePlayers()) {
                    if (!staff.hasPermission("rumahkita.antixray.notify")) continue;
                    this.msg((CommandSender)staff, message);
                }
                this.getLogger().info(ChatColor.stripColor((String)this.color(message)));
            }
        }
        this.maybeDiscordAlert(player, loc, score, reason);
    }

    private void maybeDiscordAlert(Player player, Location loc, int score, String reason) {
        if (!this.getConfig().getBoolean("discord.enabled", false)) {
            return;
        }
        String webhook = this.getConfig().getString("discord.webhook-url", "");
        if (webhook == null || webhook.isBlank() || webhook.contains("PASTE_WEBHOOK")) {
            return;
        }
        long now = System.currentTimeMillis();
        long cooldown = Math.max(1L, this.getConfig().getLong("discord.cooldown-seconds", 20L)) * 1000L;
        if (now - this.discordCooldown.getOrDefault(player.getUniqueId(), 0L) < cooldown) {
            return;
        }
        this.discordCooldown.put(player.getUniqueId(), now);
        String desc = "**Player:** " + this.esc(player.getName()) + "\\n**Score:** " + score + "\\n**World:** " + this.esc(loc.getWorld().getName()) + "\\n**Location:** " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + "\\n**Reason:** " + this.esc(reason);
        String json = "{\"embeds\":[{\"title\":\"" + this.esc(this.getConfig().getString("discord.title", "RumahKita AntiXray Alert")) + "\",\"description\":\"" + desc + "\",\"color\":" + this.getConfig().getInt("discord.color", 16753920) + "}]}";
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this, () -> {
            try {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(webhook)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
                this.http.send(req, HttpResponse.BodyHandlers.discarding());
            }
            catch (Exception e) {
                this.getLogger().warning("Discord AntiXray alert gagal: " + e.getMessage());
            }
        });
    }

    private void applyAction(Player player, RiskData risk, String reason) {
        String mode = this.getConfig().getString("risk.action-mode", "alert_only").toLowerCase(Locale.ROOT);
        int high = this.getConfig().getInt("risk.high-risk-score", 110);
        int critical = this.getConfig().getInt("risk.critical-score", 170);
        if (mode.equals("limited") && risk.score >= high) {
            this.limitedUntil.put(player.getUniqueId(), System.currentTimeMillis() + Math.max(30L, this.getConfig().getLong("risk.limited-seconds", 300L)) * 1000L);
        }
        if (mode.equals("freeze") && risk.score >= critical) {
            this.freezeUntil.put(player.getUniqueId(), System.currentTimeMillis() + Math.max(30L, this.getConfig().getLong("risk.freeze-seconds", 300L)) * 1000L);
        }
        if (mode.equals("command") && risk.score >= critical) {
            for (String cmd : this.getConfig().getStringList("risk.command-on-critical")) {
                Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)cmd.replace("%player%", player.getName()).replace("%score%", String.valueOf(risk.score)).replace("%reason%", reason));
            }
        }
    }

    private void trimEvents(RiskData risk) {
        long cutoff = System.currentTimeMillis() - Math.max(1L, this.getConfig().getLong("detection.recent-window-minutes", 15L)) * 60000L;
        while (!risk.events.isEmpty() && risk.events.peekFirst().time < cutoff) {
            risk.events.removeFirst();
        }
    }

    private int countRecent(RiskData risk, int minutes, String contains) {
        long c = System.currentTimeMillis() - (long)Math.max(1, minutes) * 60000L;
        int n = 0;
        for (MineEvent e : risk.events) {
            if (e.time < c || !e.rare || !e.material.contains(contains)) continue;
            ++n;
        }
        return n;
    }

    private int countRecentRare(RiskData risk, int minutes) {
        long c = System.currentTimeMillis() - (long)Math.max(1, minutes) * 60000L;
        int n = 0;
        for (MineEvent e : risk.events) {
            if (e.time < c || !e.rare) continue;
            ++n;
        }
        return n;
    }

    private int countRecentNormal(RiskData risk, int minutes) {
        long c = System.currentTimeMillis() - (long)Math.max(1, minutes) * 60000L;
        int n = 0;
        for (MineEvent e : risk.events) {
            if (e.time < c || e.rare) continue;
            ++n;
        }
        return n;
    }

    private void decayScores() {
        int amount = Math.max(0, this.getConfig().getInt("risk.decay-amount", 8));
        for (RiskData r : this.risks.values()) {
            r.score = Math.max(0, r.score - amount);
        }
    }

    private boolean enabled() {
        return this.getConfig().getBoolean("enabled", true);
    }

    private boolean shouldCheck(Player p) {
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        return !this.getConfig().getBoolean("detection.bypass-permission", true) || !p.hasPermission("rumahkita.antixray.bypass");
    }

    private boolean isWorldAllowed(World w) {
        List worlds = this.getConfig().getStringList("worlds");
        return worlds.isEmpty() || worlds.contains(w.getName());
    }

    private boolean isRareOre(Material m) {
        return this.listContains("rare-ores", m.name());
    }

    private boolean isNormalMiningBlock(Material m) {
        return this.listContains("normal-mining-blocks", m.name());
    }

    private boolean listContains(String path, String v) {
        for (String s : this.getConfig().getStringList(path)) {
            if (!s.equalsIgnoreCase(v)) continue;
            return true;
        }
        return false;
    }

    private boolean isNewbie(Player p) {
        try {
            return p.getStatistic(Statistic.PLAY_ONE_MINUTE) < this.getConfig().getInt("detection.newbie-playtime-minutes", 120) * 60 * 20;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isBedrock(Player p) {
        if (!this.getConfig().getBoolean("detection.detect-bedrock", true)) {
            return false;
        }
        try {
            Class<?> c = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = c.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
            Method m = c.getMethod("isFloodgatePlayer", UUID.class);
            Object r = m.invoke(api, p.getUniqueId());
            return r instanceof Boolean && (Boolean)r != false;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isLimited(UUID u) {
        Long x = this.limitedUntil.get(u);
        if (x == null) {
            return false;
        }
        if (x <= System.currentTimeMillis()) {
            this.limitedUntil.remove(u);
            return false;
        }
        return true;
    }

    private void writeLog(Player p, Material m, Location loc, int add, int score, String reason) {
        if (!this.getConfig().getBoolean("logs.enabled", true)) {
            return;
        }
        File f = new File(this.getDataFolder(), this.getConfig().getString("logs.file", "logs/antixray.log"));
        File parent = f.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        String line = this.now() + " | player=" + p.getName() + " | uuid=" + String.valueOf(p.getUniqueId()) + " | platform=" + (this.isBedrock(p) ? "Bedrock" : "Java") + " | material=" + m.name() + " | world=" + loc.getWorld().getName() + " | x=" + loc.getBlockX() + " | y=" + loc.getBlockY() + " | z=" + loc.getBlockZ() + " | add=" + add + " | score=" + score + " | reason=" + reason + System.lineSeparator();
        try (FileWriter fw = new FileWriter(f, true);){
            fw.write(line);
        }
        catch (Exception e) {
            this.getLogger().warning("Gagal tulis log antixray: " + e.getMessage());
        }
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private void loadData() {
        this.risks.clear();
        ConfigurationSection sec = this.data.getConfigurationSection("players");
        if (sec == null) {
            return;
        }
        for (String key : sec.getKeys(false)) {
            try {
                UUID u = UUID.fromString(key);
                RiskData r = new RiskData(this.data.getString("players." + key + ".name", "-"));
                r.score = this.data.getInt("players." + key + ".score", 0);
                r.flags = this.data.getInt("players." + key + ".flags", 0);
                r.rareOres = this.data.getInt("players." + key + ".rare-ores", 0);
                r.normalBlocks = this.data.getInt("players." + key + ".normal-blocks", 0);
                r.lastReason = this.data.getString("players." + key + ".last-reason", "-");
                r.lastWorld = this.data.getString("players." + key + ".last-world", "-");
                r.lastX = this.data.getInt("players." + key + ".last-x", 0);
                r.lastY = this.data.getInt("players." + key + ".last-y", 0);
                r.lastZ = this.data.getInt("players." + key + ".last-z", 0);
                this.risks.put(u, r);
            }
            catch (Exception exception) {}
        }
    }

    private void saveData() {
        this.data.set("players", null);
        for (Map.Entry<UUID, RiskData> e : this.risks.entrySet()) {
            String p = "players." + String.valueOf(e.getKey());
            RiskData r = e.getValue();
            this.data.set(p + ".name", (Object)r.name);
            this.data.set(p + ".score", (Object)r.score);
            this.data.set(p + ".flags", (Object)r.flags);
            this.data.set(p + ".rare-ores", (Object)r.rareOres);
            this.data.set(p + ".normal-blocks", (Object)r.normalBlocks);
            this.data.set(p + ".last-reason", (Object)r.lastReason);
            this.data.set(p + ".last-world", (Object)r.lastWorld);
            this.data.set(p + ".last-x", (Object)r.lastX);
            this.data.set(p + ".last-y", (Object)r.lastY);
            this.data.set(p + ".last-z", (Object)r.lastZ);
            this.data.set(p + ".last-seen", (Object)r.lastSeen);
        }
        try {
            this.data.save(this.dataFile);
        }
        catch (Exception ex) {
            this.getLogger().warning("Gagal save data AntiXray: " + ex.getMessage());
        }
    }

    private Player findPlayer(String name) {
        Player exact = Bukkit.getPlayerExact((String)name);
        return exact != null ? exact : Bukkit.getPlayer((String)name);
    }

    private RiskData findRisk(String name) {
        Player p = this.findPlayer(name);
        if (p != null) {
            return this.risks.get(p.getUniqueId());
        }
        for (RiskData r : this.risks.values()) {
            if (!r.name.equalsIgnoreCase(name)) continue;
            return r;
        }
        return null;
    }

    private UUID findUuid(String name) {
        Player p = this.findPlayer(name);
        if (p != null) {
            return p.getUniqueId();
        }
        for (Map.Entry<UUID, RiskData> e : this.risks.entrySet()) {
            if (!e.getValue().name.equalsIgnoreCase(name)) continue;
            return e.getKey();
        }
        return null;
    }

    private void resetRisk(String name) {
        UUID u = this.findUuid(name);
        if (u != null) {
            this.risks.remove(u);
            this.freezeUntil.remove(u);
            this.limitedUntil.remove(u);
        }
        this.saveData();
    }

    private String color(String in) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)(in == null ? "" : in));
    }

    private void msg(CommandSender s, String in) {
        s.sendMessage(this.color(in));
    }

    private String pref() {
        return this.getConfig().getString("messages.prefix", "&8[&bAntiXray&8] ");
    }

    private String esc(String in) {
        return in == null ? "" : in.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rumahkita.antixray.admin")) {
            this.msg(sender, this.pref() + this.getConfig().getString("messages.no-permission", "&cKamu tidak punya permission."));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            this.msg(sender, this.pref() + "&7Enabled: &f" + this.getConfig().getBoolean("enabled", true));
            this.msg(sender, this.pref() + "&7Players tracked: &f" + this.risks.size());
            this.msg(sender, this.pref() + "&7Action mode: &f" + this.getConfig().getString("risk.action-mode", "alert_only"));
            this.msg(sender, this.pref() + "&7Alert score: &f" + this.getConfig().getInt("risk.alert-score", 60));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload": {
                this.reloadConfig();
                this.data = YamlConfiguration.loadConfiguration((File)this.dataFile);
                this.loadData();
                this.msg(sender, this.pref() + this.getConfig().getString("messages.reloaded", "&aAntiXray berhasil direload."));
                break;
            }
            case "on": {
                this.getConfig().set("enabled", (Object)true);
                this.saveConfig();
                this.msg(sender, this.pref() + "&aAntiXray ON.");
                break;
            }
            case "off": {
                this.getConfig().set("enabled", (Object)false);
                this.saveConfig();
                this.msg(sender, this.pref() + "&cAntiXray OFF.");
                break;
            }
            case "check": {
                if (args.length < 2) {
                    this.msg(sender, "&e/rkxray check <player>");
                    return true;
                }
                RiskData r2 = this.findRisk(args[1]);
                if (r2 == null) {
                    this.msg(sender, this.pref() + "&ePlayer belum punya data AntiXray.");
                    return true;
                }
                this.msg(sender, "&8&m-----------------------------");
                this.msg(sender, "&bAntiXray Check: &f" + r2.name);
                this.msg(sender, "&7Score: &f" + r2.score);
                this.msg(sender, "&7Flags: &f" + r2.flags);
                this.msg(sender, "&7Rare ores: &f" + r2.rareOres);
                this.msg(sender, "&7Normal blocks: &f" + r2.normalBlocks);
                this.msg(sender, "&7Last reason: &f" + r2.lastReason);
                this.msg(sender, "&7Last loc: &f" + r2.lastWorld + " " + r2.lastX + " " + r2.lastY + " " + r2.lastZ);
                this.msg(sender, "&8&m-----------------------------");
                break;
            }
            case "top": {
                this.msg(sender, "&8&m-----------------------------");
                this.msg(sender, "&bTop Suspicious AntiXray");
                ArrayList<RiskData> list = new ArrayList<RiskData>(this.risks.values());
                list.sort(Comparator.comparingInt(r -> r.score).reversed());
                for (int i = 0; i < Math.min(10, list.size()); ++i) {
                    RiskData r3 = (RiskData)list.get(i);
                    this.msg(sender, "&e#" + (i + 1) + " &f" + r3.name + " &7score &c" + r3.score + " &7flags &f" + r3.flags);
                }
                this.msg(sender, "&8&m-----------------------------");
                break;
            }
            case "reset": {
                if (args.length < 2) {
                    this.msg(sender, "&e/rkxray reset <player>");
                    return true;
                }
                this.resetRisk(args[1]);
                this.msg(sender, this.pref() + this.getConfig().getString("messages.reset", "&aRisk player berhasil direset."));
                break;
            }
            case "freeze": {
                if (args.length < 2) {
                    this.msg(sender, "&e/rkxray freeze <player>");
                    return true;
                }
                Player p = this.findPlayer(args[1]);
                if (p == null) {
                    this.msg(sender, this.pref() + "&cPlayer tidak online.");
                    return true;
                }
                this.freezeUntil.put(p.getUniqueId(), System.currentTimeMillis() + Math.max(30L, this.getConfig().getLong("risk.freeze-seconds", 300L)) * 1000L);
                this.msg(sender, this.pref() + "&cPlayer dibekukan sementara.");
                break;
            }
            case "unfreeze": {
                if (args.length < 2) {
                    this.msg(sender, "&e/rkxray unfreeze <player>");
                    return true;
                }
                UUID u = this.findUuid(args[1]);
                if (u != null) {
                    this.freezeUntil.remove(u);
                }
                this.msg(sender, this.pref() + "&aFreeze player dibuka.");
                break;
            }
            case "logs": {
                this.msg(sender, this.pref() + "&7Log file: &fplugins/RumahKitaAntiXray/" + this.getConfig().getString("logs.file", "logs/antixray.log"));
                break;
            }
            default: {
                this.msg(sender, "&8&m-----------------------------");
                this.msg(sender, "&bRumahKitaAntiXray");
                this.msg(sender, "&e/rkxray status");
                this.msg(sender, "&e/rkxray reload");
                this.msg(sender, "&e/rkxray check <player>");
                this.msg(sender, "&e/rkxray top");
                this.msg(sender, "&e/rkxray reset <player>");
                this.msg(sender, "&e/rkxray freeze <player>");
                this.msg(sender, "&e/rkxray unfreeze <player>");
                this.msg(sender, "&e/rkxray logs");
                this.msg(sender, "&e/rkxray on/off");
                this.msg(sender, "&8&m-----------------------------");
            }
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("status", "reload", "check", "top", "reset", "freeze", "unfreeze", "logs", "on", "off");
        }
        if (args.length == 2 && List.of("check", "reset", "freeze", "unfreeze").contains(args[0].toLowerCase(Locale.ROOT))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }

    private static final class RiskData {
        String name;
        int score = 0;
        int flags = 0;
        int rareOres = 0;
        int normalBlocks = 0;
        String lastReason = "-";
        String lastWorld = "-";
        int lastX = 0;
        int lastY = 0;
        int lastZ = 0;
        long lastSeen = 0L;
        boolean bedrock = false;
        ArrayDeque<MineEvent> events = new ArrayDeque();

        RiskData(String name) {
            this.name = name;
        }
    }

    private static final class MineEvent {
        long time;
        String material;
        int x;
        int y;
        int z;
        String world;
        boolean rare;

        MineEvent(long time, String material, int x, int y, int z, String world, boolean rare) {
            this.time = time;
            this.material = material;
            this.x = x;
            this.y = y;
            this.z = z;
            this.world = world;
            this.rare = rare;
        }
    }
}

