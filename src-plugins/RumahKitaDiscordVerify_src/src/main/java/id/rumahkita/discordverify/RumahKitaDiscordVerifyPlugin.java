/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package id.rumahkita.discordverify;

import java.io.File;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class RumahKitaDiscordVerifyPlugin
extends JavaPlugin
implements Listener,
TabExecutor {
    private final SecureRandom random = new SecureRandom();
    private final HttpClient http = HttpClient.newHttpClient();
    private final Map<UUID, PendingVerify> pendingByUuid = new ConcurrentHashMap<UUID, PendingVerify>();
    private final Map<String, UUID> pendingByCode = new ConcurrentHashMap<String, UUID>();
    private final Set<String> processedDiscordMessages = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> reminderCooldown = new HashMap<UUID, Long>();
    private File dataFile;
    private YamlConfiguration data;
    private int pollTask = -1;
    private static final Pattern FIVE_DIGITS = Pattern.compile("(?<!\\d)(\\d{5})(?!\\d)");

    public void onEnable() {
        this.saveDefaultConfig();
        this.saveResource("data.yml", false);
        this.dataFile = new File(this.getDataFolder(), "data.yml");
        this.data = YamlConfiguration.loadConfiguration((File)this.dataFile);
        this.loadPending();
        this.loadProcessedMessages();
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        this.getCommand("verify").setExecutor((CommandExecutor)this);
        this.getCommand("verify").setTabCompleter((TabCompleter)this);
        this.getCommand("rkverify").setExecutor((CommandExecutor)this);
        this.getCommand("rkverify").setTabCompleter((TabCompleter)this);
        if (this.getConfig().getBoolean("discord.enabled", true)) {
            if (this.getConfig().getBoolean("discord.ignore-old-messages-on-startup", true)) {
                Bukkit.getScheduler().runTaskLaterAsynchronously((Plugin)this, this::primeDiscordMessages, 40L);
            }
            this.startDiscordPoller();
        }
        Bukkit.getScheduler().runTaskTimer((Plugin)this, this::cleanupExpiredCodes, 100L, 100L);
        this.getLogger().info("RumahKitaDiscordVerify v1.0.1 enabled.");
    }

    public void onDisable() {
        if (this.pollTask != -1) {
            Bukkit.getScheduler().cancelTask(this.pollTask);
        }
        this.savePendingToData();
        this.saveProcessedMessages();
        this.saveData();
    }

    private void startDiscordPoller() {
        if (this.pollTask != -1) {
            Bukkit.getScheduler().cancelTask(this.pollTask);
        }
        long seconds = Math.max(2L, this.getConfig().getLong("discord.poll-interval-seconds", 3L));
        this.pollTask = Bukkit.getScheduler().runTaskTimerAsynchronously((Plugin)this, this::pollDiscordMessages, seconds * 20L, seconds * 20L).getTaskId();
    }

    private boolean enabled() {
        return this.getConfig().getBoolean("enabled", true);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!this.enabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (this.isVerified(player) || this.hasBypass(player)) {
            this.updateVerifiedLoginInfo(player);
            return;
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> this.sendPendingReminder(player, true), 30L);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        this.reminderCooldown.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!this.enabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (this.isVerified(player) || this.hasBypass(player)) {
            return;
        }
        String raw = event.getMessage();
        if (raw == null || raw.isBlank()) {
            return;
        }
        String cmd = raw.split("\\s+")[0].toLowerCase(Locale.ROOT);
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        if (cmd.contains(":")) {
            cmd = cmd.substring(cmd.indexOf(58) + 1);
        }
        if (this.isAllowedBeforeVerify(cmd)) {
            return;
        }
        event.setCancelled(true);
        this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.locked-command", "&cKamu harus verifikasi Discord dulu. Ketik &e/verify&c."));
        this.sendPendingReminder(player, false);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onMove(PlayerMoveEvent event) {
        if (!this.enabled() || !this.getConfig().getBoolean("lock.block-movement", true)) {
            return;
        }
        Player p = event.getPlayer();
        if (this.isVerified(p) || this.hasBypass(p)) {
            return;
        }
        if (this.getConfig().getBoolean("lock.allow-look-around", true)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to == null) {
                return;
            }
            if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
                return;
            }
            to.setX(from.getX());
            to.setY(from.getY());
            to.setZ(from.getZ());
            event.setTo(to);
        } else {
            event.setCancelled(true);
        }
        this.sendPendingReminder(p, false);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!this.enabled() || !this.getConfig().getBoolean("lock.block-chat", true)) {
            return;
        }
        Player p = event.getPlayer();
        if (this.isVerified(p) || this.hasBypass(p)) {
            return;
        }
        event.setCancelled(true);
        this.msg((CommandSender)p, this.pref() + this.getConfig().getString("messages.locked-action", "&cKamu belum verifikasi Discord. Ketik &e/verify&c."));
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event) {
        if (!this.enabled() || !this.getConfig().getBoolean("lock.block-block-break", true)) {
            return;
        }
        if (!this.guardAction(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPlace(BlockPlaceEvent event) {
        if (!this.enabled() || !this.getConfig().getBoolean("lock.block-block-place", true)) {
            return;
        }
        if (!this.guardAction(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onInteract(PlayerInteractEvent event) {
        if (!this.enabled() || !this.getConfig().getBoolean("lock.block-interact", true)) {
            return;
        }
        if (!this.guardAction(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player p;
        if (!this.enabled() || !this.getConfig().getBoolean("lock.block-damage", true)) {
            return;
        }
        Entity entity = event.getDamager();
        if (entity instanceof Player && !this.guardAction(p = (Player)entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
        Player p;
        if (!this.enabled() || !this.getConfig().getBoolean("lock.block-inventory", true)) {
            return;
        }
        HumanEntity humanEntity = event.getWhoClicked();
        if (humanEntity instanceof Player && !this.guardAction(p = (Player)humanEntity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Player p;
        if (!this.enabled() || !this.getConfig().getBoolean("lock.block-inventory", true)) {
            return;
        }
        HumanEntity humanEntity = event.getWhoClicked();
        if (humanEntity instanceof Player && !this.guardAction(p = (Player)humanEntity)) {
            event.setCancelled(true);
        }
    }

    private boolean guardAction(Player player) {
        if (this.isVerified(player) || this.hasBypass(player)) {
            return true;
        }
        this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.locked-action", "&cKamu belum verifikasi Discord. Ketik &e/verify&c."));
        this.sendPendingReminder(player, false);
        return false;
    }

    private boolean isAllowedBeforeVerify(String cmd) {
        for (String allowed : this.getConfig().getStringList("loginsecurity.allowed-before-verify-commands")) {
            String clean = allowed.toLowerCase(Locale.ROOT).replace("/", "").trim();
            if (!cmd.equals(clean)) continue;
            return true;
        }
        return false;
    }

    private boolean hasBypass(Player player) {
        return player.hasPermission("rumahkita.verify.bypass");
    }

    private boolean isVerified(Player player) {
        return this.data.contains("verified." + this.key(player.getUniqueId()));
    }

    private boolean isUuidVerified(UUID uuid) {
        return this.data.contains("verified." + this.key(uuid));
    }

    private void sendPendingReminder(Player player, boolean force) {
        long now = System.currentTimeMillis();
        long cooldown = Math.max(5L, this.getConfig().getLong("lock.send-reminder-every-seconds", 20L)) * 1000L;
        long last = this.reminderCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (!force && now - last < cooldown) {
            return;
        }
        this.reminderCooldown.put(player.getUniqueId(), now);
        for (String line : this.getConfig().getStringList("messages.pending-reminder")) {
            this.msg((CommandSender)player, this.pref() + line);
        }
    }

    private void handleVerifyCommand(Player player) {
        if (this.isVerified(player)) {
            this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.already-verified", "&aAkun Minecraft kamu sudah terverifikasi dengan Discord."));
            return;
        }
        PendingVerify existing = this.pendingByUuid.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (existing != null && existing.expiresAt > now && this.getConfig().getBoolean("code.reuse-existing-pending-code", true)) {
            this.sendCodeMessage(player, existing.code);
            return;
        }
        if (existing != null) {
            this.removePending(existing);
        }
        String code = this.generateUniqueCode();
        long expireMs = now + Math.max(30L, this.getConfig().getLong("code.expire-seconds", 300L)) * 1000L;
        PendingVerify pending = new PendingVerify(player.getUniqueId(), player.getName(), this.getIp(player), this.subnetOf(this.getIp(player)), this.getFloodgateXuid(player), code, expireMs);
        this.pendingByUuid.put(player.getUniqueId(), pending);
        this.pendingByCode.put(code, player.getUniqueId());
        this.savePendingToData();
        this.saveData();
        this.sendCodeMessage(player, code);
    }

    private void sendCodeMessage(Player player, String code) {
        int seconds = Math.max(30, this.getConfig().getInt("code.expire-seconds", 300));
        String channel = this.getConfig().getString("discord.verify-channel-id", "-");
        for (String line : this.getConfig().getStringList("messages.generate-code")) {
            line = line.replace("%code%", code).replace("%channel%", channel).replace("%seconds%", String.valueOf(seconds));
            this.msg((CommandSender)player, this.pref() + line);
        }
    }

    private String generateUniqueCode() {
        int length = Math.max(4, Math.min(8, this.getConfig().getInt("code.length", 5)));
        int max = (int)Math.pow(10.0, length);
        int min = (int)Math.pow(10.0, length - 1);
        for (int i = 0; i < 1000; ++i) {
            String code = String.valueOf(min + this.random.nextInt(max - min));
            if (this.pendingByCode.containsKey(code)) continue;
            return code;
        }
        return String.valueOf(System.currentTimeMillis()).substring(8, 8 + length);
    }

    private void cleanupExpiredCodes() {
        long now = System.currentTimeMillis();
        ArrayList<PendingVerify> expired = new ArrayList<PendingVerify>();
        for (PendingVerify p : this.pendingByUuid.values()) {
            if (p.expiresAt > now) continue;
            expired.add(p);
        }
        for (PendingVerify p : expired) {
            this.removePending(p);
        }
        if (!expired.isEmpty()) {
            this.savePendingToData();
            this.saveData();
        }
    }

    private void removePending(PendingVerify p) {
        this.pendingByUuid.remove(p.uuid);
        this.pendingByCode.remove(p.code);
        this.data.set("pending." + this.key(p.uuid), null);
    }

    private void pollDiscordMessages() {
        if (!this.enabled()) {
            return;
        }
        String token = this.getConfig().getString("discord.bot-token", "");
        String channel = this.getConfig().getString("discord.verify-channel-id", "");
        if (token == null || token.isBlank() || token.contains("PASTE_BOT_TOKEN") || channel == null || channel.isBlank()) {
            return;
        }
        try {
            String url = "https://discord.com/api/v10/channels/" + channel + "/messages?limit=" + Math.max(1, this.getConfig().getInt("discord.read-message-limit", 20));
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).header("Authorization", "Bot " + token).header("User-Agent", "RumahKitaDiscordVerify/1.0.1").GET().build();
            HttpResponse<String> res = this.http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                this.getLogger().warning("Discord poll failed HTTP " + res.statusCode() + ": " + res.body());
                return;
            }
            this.processDiscordJson(res.body(), false);
        }
        catch (Exception e) {
            this.getLogger().warning("Discord poll error: " + e.getMessage());
        }
    }

    private void primeDiscordMessages() {
        String token = this.getConfig().getString("discord.bot-token", "");
        String channel = this.getConfig().getString("discord.verify-channel-id", "");
        if (token == null || token.isBlank() || token.contains("PASTE_BOT_TOKEN") || channel == null || channel.isBlank()) {
            return;
        }
        try {
            String url = "https://discord.com/api/v10/channels/" + channel + "/messages?limit=50";
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).header("Authorization", "Bot " + token).header("User-Agent", "RumahKitaDiscordVerify/1.0.1").GET().build();
            HttpResponse<String> res = this.http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                this.processDiscordJson(res.body(), true);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void processDiscordJson(String json, boolean primeOnly) {
        for (String obj : this.splitTopLevelObjects(json)) {
            String messageId = this.readJsonStringField(obj, "id");
            if (messageId.isBlank() || this.processedDiscordMessages.contains(messageId)) continue;
            if (primeOnly) {
                this.markProcessed(messageId);
                continue;
            }
            String content = this.unescapeJson(this.readJsonStringField(obj, "content"));
            String authorObj = this.readJsonObjectField(obj, "author");
            String authorId = this.readJsonStringField(authorObj, "id");
            String botValue = this.readJsonRawField(authorObj, "bot");
            if ("true".equalsIgnoreCase(botValue)) {
                this.markProcessed(messageId);
                continue;
            }
            Matcher m = FIVE_DIGITS.matcher(content == null ? "" : content);
            while (m.find()) {
                String code = m.group(1);
                UUID uuid = this.pendingByCode.get(code);
                if (uuid == null) continue;
                Bukkit.getScheduler().runTask((Plugin)this, () -> this.completeVerificationFromDiscord(code, authorId, messageId));
                break;
            }
            this.markProcessed(messageId);
        }
    }

    private void completeVerificationFromDiscord(String code, String discordId, String messageId) {
        UUID uuid = this.pendingByCode.get(code);
        if (uuid == null) {
            return;
        }
        PendingVerify pending = this.pendingByUuid.get(uuid);
        if (pending == null) {
            return;
        }
        if (pending.expiresAt <= System.currentTimeMillis()) {
            this.removePending(pending);
            this.savePendingToData();
            this.saveData();
            Player p = Bukkit.getPlayer((UUID)uuid);
            if (p != null) {
                this.msg((CommandSender)p, this.pref() + this.getConfig().getString("messages.code-expired", "&cKode kamu sudah expired. Ketik /verify lagi."));
            }
            return;
        }
        LimitResult limit = this.checkLimits(uuid, pending, discordId);
        if (!limit.allowed) {
            Player p = Bukkit.getPlayer((UUID)uuid);
            if (p != null) {
                this.msg((CommandSender)p, this.pref() + this.getConfig().getString("messages.limit-exceeded", "&cVerifikasi ditolak. Limit akun/IP terdeteksi, hubungi staff."));
                p.kickPlayer(this.color(this.getConfig().getString("messages.limit-exceeded", "&cVerifikasi ditolak. Limit akun/IP terdeteksi, hubungi staff.")));
            }
            this.sendDiscordMessageAsync(this.getConfig().getString("discord.failed-limit-message", "\u274c Verifikasi gagal untuk <@%discord_id%>.").replace("%discord_id%", discordId));
            this.alertStaff("&cVerify limit blocked: &f" + pending.name + " &7Discord=&f" + discordId + " &7Reason=&e" + limit.reason);
            this.maybeRunRkban(pending, limit.reason);
            this.removePending(pending);
            this.savePendingToData();
            this.saveData();
            return;
        }
        this.verifyPlayer(pending, discordId);
        this.markProcessed(messageId);
    }

    private void verifyPlayer(PendingVerify pending, String discordId) {
        String path = "verified." + this.key(pending.uuid);
        this.data.set(path + ".uuid", (Object)pending.uuid.toString());
        this.data.set(path + ".last-name", (Object)pending.name);
        this.data.set(path + ".discord-id", (Object)discordId);
        this.data.set(path + ".last-ip", (Object)pending.ip);
        this.data.set(path + ".last-subnet", (Object)pending.subnet);
        this.data.set(path + ".bedrock-xuid", (Object)pending.bedrockXuid);
        this.data.set(path + ".verified-at", (Object)this.now());
        this.data.set(path + ".last-seen", (Object)this.now());
        this.removePending(pending);
        this.savePendingToData();
        this.saveData();
        Player player = Bukkit.getPlayer((UUID)pending.uuid);
        if (player != null) {
            this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.verified-success", "&aVerifikasi berhasil! Selamat bermain di RumahKita S2."));
            this.updateVerifiedLoginInfo(player);
            this.runVerifiedCommands(player, discordId);
        }
        String discordMsg = this.getConfig().getString("discord.success-message", "\u2705 Selamat <@%discord_id%>, kamu sudah terverifikasi sebagai **%player%**.").replace("%discord_id%", discordId).replace("%player%", pending.name);
        this.sendDiscordMessageAsync(discordMsg);
        this.alertStaff("&aVerify success: &f" + pending.name + " &7Discord=&f" + discordId);
    }

    private void updateVerifiedLoginInfo(Player player) {
        String path = "verified." + this.key(player.getUniqueId());
        if (!this.data.contains(path)) {
            return;
        }
        this.data.set(path + ".last-name", (Object)player.getName());
        this.data.set(path + ".last-ip", (Object)this.getIp(player));
        this.data.set(path + ".last-subnet", (Object)this.subnetOf(this.getIp(player)));
        this.data.set(path + ".bedrock-xuid", (Object)this.getFloodgateXuid(player));
        this.data.set(path + ".last-seen", (Object)this.now());
        this.saveData();
    }

    private LimitResult checkLimits(UUID currentUuid, PendingVerify pending, String discordId) {
        if (!this.getConfig().getBoolean("anti-alt.enabled", true)) {
            return new LimitResult(true, "disabled");
        }
        int discordCount = 0;
        int ipCount = 0;
        int subnetCount = 0;
        int xuidCount = 0;
        ConfigurationSection sec = this.data.getConfigurationSection("verified");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                String path = "verified." + key;
                String uuidString = this.data.getString(path + ".uuid", "");
                if (uuidString.equalsIgnoreCase(currentUuid.toString())) continue;
                String d = this.data.getString(path + ".discord-id", "");
                String ip = this.data.getString(path + ".last-ip", "");
                String subnet = this.data.getString(path + ".last-subnet", "");
                String xuid = this.data.getString(path + ".bedrock-xuid", "");
                if (!discordId.isBlank() && discordId.equals(d)) {
                    ++discordCount;
                }
                if (!pending.ip.isBlank() && pending.ip.equals(ip)) {
                    ++ipCount;
                }
                if (!pending.subnet.isBlank() && pending.subnet.equals(subnet)) {
                    ++subnetCount;
                }
                if (pending.bedrockXuid.isBlank() || !pending.bedrockXuid.equals(xuid)) continue;
                ++xuidCount;
            }
        }
        int maxDiscord = Math.max(1, this.getConfig().getInt("anti-alt.max-accounts-per-discord", 2));
        int maxIp = Math.max(1, this.getConfig().getInt("anti-alt.max-accounts-per-ip", 2));
        int maxSubnet = Math.max(1, this.getConfig().getInt("anti-alt.max-accounts-per-subnet", 4));
        int maxXuid = Math.max(1, this.getConfig().getInt("anti-alt.max-accounts-per-bedrock-xuid", 1));
        if (discordCount >= maxDiscord) {
            return new LimitResult(false, "discord_limit");
        }
        if (ipCount >= maxIp) {
            return new LimitResult(false, "ip_limit");
        }
        if (subnetCount >= maxSubnet) {
            return new LimitResult(false, "subnet_limit");
        }
        if (this.getConfig().getBoolean("anti-alt.count-bedrock-xuid", true) && !pending.bedrockXuid.isBlank() && xuidCount >= maxXuid) {
            return new LimitResult(false, "bedrock_xuid_limit");
        }
        return new LimitResult(true, "ok");
    }

    private void maybeRunRkban(PendingVerify pending, String reason) {
        if (!this.getConfig().getBoolean("securityban-integration.enabled", true)) {
            return;
        }
        if (!this.getConfig().getBoolean("securityban-integration.auto-rkban-on-limit-exceeded", false)) {
            return;
        }
        String banReason = this.getConfig().getString("securityban-integration.ban-reason", "Verify limit exceeded / suspected alt or VPN") + " (" + reason + ")";
        Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)("rkban " + pending.name + " " + banReason));
    }

    private void runVerifiedCommands(Player player, String discordId) {
        if (!this.getConfig().getBoolean("on-verified-commands.enabled", true)) {
            return;
        }
        for (String cmd : this.getConfig().getStringList("on-verified-commands.console-commands")) {
            cmd = cmd.replace("%player%", player.getName()).replace("%uuid%", player.getUniqueId().toString()).replace("%discord_id%", discordId);
            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)cmd);
        }
    }

    private void sendDiscordMessageAsync(String message) {
        String token = this.getConfig().getString("discord.bot-token", "");
        String channel = this.getConfig().getString("discord.verify-channel-id", "");
        if (token == null || token.isBlank() || token.contains("PASTE_BOT_TOKEN") || channel == null || channel.isBlank()) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this, () -> {
            try {
                String json = "{\"content\":\"" + this.jsonEscape(message) + "\"}";
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("https://discord.com/api/v10/channels/" + channel + "/messages")).header("Authorization", "Bot " + token).header("Content-Type", "application/json; charset=UTF-8").header("User-Agent", "RumahKitaDiscordVerify/1.0.1").POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
                this.http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            }
            catch (Exception e) {
                this.getLogger().warning("Discord send error: " + e.getMessage());
            }
        });
    }

    private void alertStaff(String message) {
        if (!this.getConfig().getBoolean("securityban-integration.alert-staff", true)) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("rumahkita.verify.notify")) continue;
            this.msg((CommandSender)p, this.pref() + message);
        }
        this.getLogger().info(ChatColor.stripColor((String)this.color(message)));
    }

    private String getIp(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address == null || address.getAddress() == null) {
            return "";
        }
        return address.getAddress().getHostAddress();
    }

    private String getFloodgateXuid(Player player) {
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiClass.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
            Method isFloodgate = apiClass.getMethod("isFloodgatePlayer", UUID.class);
            Object result = isFloodgate.invoke(api, player.getUniqueId());
            if (!(result instanceof Boolean) || !((Boolean)result).booleanValue()) {
                return "";
            }
            Method getPlayer = apiClass.getMethod("getPlayer", UUID.class);
            Object floodgatePlayer = getPlayer.invoke(api, player.getUniqueId());
            if (floodgatePlayer == null) {
                return "";
            }
            try {
                Method xuid = floodgatePlayer.getClass().getMethod("getXuid", new Class[0]);
                Object x = xuid.invoke(floodgatePlayer, new Object[0]);
                return x == null ? "" : String.valueOf(x);
            }
            catch (NoSuchMethodException ignored) {
                return "";
            }
        }
        catch (Throwable ignored) {
            return "";
        }
    }

    private String subnetOf(String ip) {
        if (ip == null || ip.isBlank()) {
            return "";
        }
        if (ip.contains(":")) {
            return this.ipv6Subnet(ip, this.getConfig().getInt("anti-alt.ipv6-subnet-prefix", 64));
        }
        return this.ipv4Subnet(ip, this.getConfig().getInt("anti-alt.ipv4-subnet-prefix", 24));
    }

    private String ipv4Subnet(String ip, int prefix) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return ip.toLowerCase(Locale.ROOT);
            }
            int raw = 0;
            for (String p : parts) {
                raw = raw << 8 | Integer.parseInt(p) & 0xFF;
            }
            int mask = prefix <= 0 ? 0 : -1 << 32 - prefix;
            int net = raw & mask;
            return (net >>> 24 & 0xFF) + "." + (net >>> 16 & 0xFF) + "." + (net >>> 8 & 0xFF) + "." + (net & 0xFF) + "/" + prefix;
        }
        catch (Exception e) {
            return ip.toLowerCase(Locale.ROOT);
        }
    }

    private String ipv6Subnet(String ip, int prefix) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            byte[] bytes = address.getAddress();
            int fullBytes = prefix / 8;
            int bits = prefix % 8;
            for (int i = fullBytes + (bits > 0 ? 1 : 0); i < bytes.length; ++i) {
                bytes[i] = 0;
            }
            if (bits > 0 && fullBytes < bytes.length) {
                int mask = 255 << 8 - bits & 0xFF;
                bytes[fullBytes] = (byte)(bytes[fullBytes] & mask);
            }
            return InetAddress.getByAddress(bytes).getHostAddress().toLowerCase(Locale.ROOT) + "/" + prefix;
        }
        catch (Exception e) {
            return ip.toLowerCase(Locale.ROOT) + "/" + prefix;
        }
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private String key(UUID uuid) {
        return uuid.toString().toLowerCase(Locale.ROOT);
    }

    private void loadPending() {
        this.pendingByUuid.clear();
        this.pendingByCode.clear();
        ConfigurationSection sec = this.data.getConfigurationSection("pending");
        if (sec == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (String uuidString : sec.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                String path = "pending." + uuidString;
                String code = this.data.getString(path + ".code", "");
                long expires = this.data.getLong(path + ".expires-at", 0L);
                if (code.isBlank() || expires <= now) {
                    this.data.set(path, null);
                    continue;
                }
                PendingVerify p = new PendingVerify(uuid, this.data.getString(path + ".name", "Unknown"), this.data.getString(path + ".ip", ""), this.data.getString(path + ".subnet", ""), this.data.getString(path + ".bedrock-xuid", ""), code, expires);
                this.pendingByUuid.put(uuid, p);
                this.pendingByCode.put(code, uuid);
            }
            catch (Exception exception) {}
        }
        this.saveData();
    }

    private void savePendingToData() {
        this.data.set("pending", null);
        for (PendingVerify p : this.pendingByUuid.values()) {
            String path = "pending." + this.key(p.uuid);
            this.data.set(path + ".uuid", (Object)p.uuid.toString());
            this.data.set(path + ".name", (Object)p.name);
            this.data.set(path + ".ip", (Object)p.ip);
            this.data.set(path + ".subnet", (Object)p.subnet);
            this.data.set(path + ".bedrock-xuid", (Object)p.bedrockXuid);
            this.data.set(path + ".code", (Object)p.code);
            this.data.set(path + ".expires-at", (Object)p.expiresAt);
        }
    }

    private void loadProcessedMessages() {
        this.processedDiscordMessages.clear();
        this.processedDiscordMessages.addAll(this.data.getStringList("processed-discord-messages"));
    }

    private void markProcessed(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return;
        }
        this.processedDiscordMessages.add(messageId);
        if (this.processedDiscordMessages.size() > 250) {
            this.saveProcessedMessages();
        }
    }

    private void saveProcessedMessages() {
        ArrayList<String> list = new ArrayList<String>(this.processedDiscordMessages);
        while (list.size() > 200) {
            list.remove(0);
        }
        this.data.set("processed-discord-messages", list);
        this.saveData();
    }

    private void saveData() {
        try {
            this.data.save(this.dataFile);
        }
        catch (Exception e) {
            this.getLogger().warning("Failed to save data.yml: " + e.getMessage());
        }
    }

    private List<String> splitTopLevelObjects(String json) {
        ArrayList<String> out = new ArrayList<String>();
        if (json == null || json.isBlank()) {
            return out;
        }
        boolean inString = false;
        boolean escape = false;
        int depth = 0;
        int start = -1;
        for (int i = 0; i < json.length(); ++i) {
            char c = json.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '\"') {
                inString = !inString;
                continue;
            }
            if (inString) continue;
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                ++depth;
                continue;
            }
            if (c != '}' || --depth != 0 || start < 0) continue;
            out.add(json.substring(start, i + 1));
            start = -1;
        }
        return out;
    }

    private String readJsonStringField(String json, String field) {
        if (json == null) {
            return "";
        }
        String needle = "\"" + field + "\"";
        int i = json.indexOf(needle);
        if (i < 0) {
            return "";
        }
        int colon = json.indexOf(58, i + needle.length());
        if (colon < 0) {
            return "";
        }
        int q1 = json.indexOf(34, colon + 1);
        if (q1 < 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int p = q1 + 1; p < json.length(); ++p) {
            char c = json.charAt(p);
            if (escape) {
                sb.append('\\').append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '\"') break;
            sb.append(c);
        }
        return sb.toString();
    }

    private String readJsonRawField(String json, String field) {
        int end;
        if (json == null) {
            return "";
        }
        String needle = "\"" + field + "\"";
        int i = json.indexOf(needle);
        if (i < 0) {
            return "";
        }
        int colon = json.indexOf(58, i + needle.length());
        if (colon < 0) {
            return "";
        }
        for (end = colon + 1; end < json.length() && Character.isWhitespace(json.charAt(end)); ++end) {
        }
        int start = end;
        while (end < json.length() && ",}\n\r\t ".indexOf(json.charAt(end)) < 0) {
            ++end;
        }
        return json.substring(start, end).replace("\"", "").trim();
    }

    private String readJsonObjectField(String json, String field) {
        if (json == null) {
            return "";
        }
        String needle = "\"" + field + "\"";
        int i = json.indexOf(needle);
        if (i < 0) {
            return "";
        }
        int colon = json.indexOf(58, i + needle.length());
        if (colon < 0) {
            return "";
        }
        int start = json.indexOf(123, colon + 1);
        if (start < 0) {
            return "";
        }
        boolean inString = false;
        boolean escape = false;
        int depth = 0;
        for (int p = start; p < json.length(); ++p) {
            char c = json.charAt(p);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '\"') {
                inString = !inString;
                continue;
            }
            if (inString) continue;
            if (c == '{') {
                ++depth;
                continue;
            }
            if (c != '}' || --depth != 0) continue;
            return json.substring(start, p + 1);
        }
        return "";
    }

    private String unescapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String jsonEscape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private String color(String input) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)(input == null ? "" : input));
    }

    private void msg(CommandSender sender, String input) {
        sender.sendMessage(this.color(input));
    }

    private String pref() {
        return this.getConfig().getString("messages.prefix", "&8[&bVerify&8] ");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("verify")) {
            if (!(sender instanceof Player)) {
                this.msg(sender, this.pref() + "&cCommand ini hanya untuk player.");
                return true;
            }
            Player player = (Player)sender;
            this.handleVerifyCommand(player);
            return true;
        }
        if (!sender.hasPermission("rumahkita.verify.admin")) {
            this.msg(sender, this.pref() + this.getConfig().getString("messages.no-permission", "&cKamu tidak punya permission."));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            this.msg(sender, this.pref() + "&7Enabled: &f" + this.getConfig().getBoolean("enabled", true));
            this.msg(sender, this.pref() + "&7Verified: &f" + this.countSection("verified"));
            this.msg(sender, this.pref() + "&7Pending: &f" + this.pendingByUuid.size());
            this.msg(sender, this.pref() + "&7Processed Discord Messages: &f" + this.processedDiscordMessages.size());
            this.msg(sender, this.pref() + "&7Channel: &f" + this.getConfig().getString("discord.verify-channel-id", "-"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload": {
                this.reloadConfig();
                this.data = YamlConfiguration.loadConfiguration((File)this.dataFile);
                this.loadPending();
                this.loadProcessedMessages();
                this.startDiscordPoller();
                this.msg(sender, this.pref() + this.getConfig().getString("messages.reloaded", "&aDiscordVerify berhasil direload."));
                break;
            }
            case "check": {
                if (args.length < 2) {
                    this.msg(sender, "&e/rkverify check <player>");
                    return true;
                }
                this.checkPlayer(sender, args[1]);
                break;
            }
            case "unverify": {
                if (args.length < 2) {
                    this.msg(sender, "&e/rkverify unverify <player>");
                    return true;
                }
                UUID uuid = this.findUuid(args[1]);
                if (uuid == null) {
                    this.msg(sender, this.pref() + "&cPlayer tidak ditemukan.");
                    return true;
                }
                this.data.set("verified." + this.key(uuid), null);
                this.saveData();
                Player p = Bukkit.getPlayer((UUID)uuid);
                if (p != null) {
                    this.msg((CommandSender)p, this.pref() + "&cStatus verifikasi kamu dihapus staff. Ketik /verify lagi.");
                }
                this.msg(sender, this.pref() + "&aVerifikasi dihapus.");
                break;
            }
            case "forceverify": {
                if (args.length < 3) {
                    this.msg(sender, "&e/rkverify forceverify <player> <discordId>");
                    return true;
                }
                Player p = Bukkit.getPlayerExact((String)args[1]);
                if (p == null) {
                    this.msg(sender, this.pref() + "&cPlayer harus online untuk forceverify.");
                    return true;
                }
                PendingVerify fake = new PendingVerify(p.getUniqueId(), p.getName(), this.getIp(p), this.subnetOf(this.getIp(p)), this.getFloodgateXuid(p), "00000", System.currentTimeMillis() + 10000L);
                this.verifyPlayer(fake, args[2]);
                this.msg(sender, this.pref() + "&aForce verify berhasil.");
                break;
            }
            case "clearpending": {
                this.pendingByUuid.clear();
                this.pendingByCode.clear();
                this.data.set("pending", null);
                this.saveData();
                this.msg(sender, this.pref() + "&aSemua pending code dihapus.");
                break;
            }
            case "resyncdiscord": {
                this.processedDiscordMessages.clear();
                this.data.set("processed-discord-messages", new ArrayList());
                this.saveData();
                Bukkit.getScheduler().runTaskAsynchronously((Plugin)this, this::primeDiscordMessages);
                this.msg(sender, this.pref() + "&aDiscord messages di-resync. Pesan lama akan diabaikan.");
                break;
            }
            default: {
                this.help(sender);
            }
        }
        return true;
    }

    private int countSection(String path) {
        ConfigurationSection sec = this.data.getConfigurationSection(path);
        return sec == null ? 0 : sec.getKeys(false).size();
    }

    private void checkPlayer(CommandSender sender, String name) {
        PendingVerify p;
        UUID uuid = this.findUuid(name);
        if (uuid == null) {
            this.msg(sender, this.pref() + "&cPlayer tidak ditemukan online/data.");
            return;
        }
        String path = "verified." + this.key(uuid);
        this.msg(sender, "&8&m-----------------------------");
        this.msg(sender, "&bVerify Check: &f" + name);
        this.msg(sender, "&7UUID: &f" + String.valueOf(uuid));
        this.msg(sender, "&7Verified: " + (this.data.contains(path) ? "&aYES" : "&cNO"));
        if (this.data.contains(path)) {
            this.msg(sender, "&7Discord ID: &f" + this.data.getString(path + ".discord-id", "-"));
            this.msg(sender, "&7Last Name: &f" + this.data.getString(path + ".last-name", "-"));
            this.msg(sender, "&7Last IP: &f" + this.data.getString(path + ".last-ip", "-"));
            this.msg(sender, "&7Subnet: &f" + this.data.getString(path + ".last-subnet", "-"));
            this.msg(sender, "&7Bedrock XUID: &f" + this.data.getString(path + ".bedrock-xuid", "-"));
            this.msg(sender, "&7Verified At: &f" + this.data.getString(path + ".verified-at", "-"));
            this.msg(sender, "&7Last Seen: &f" + this.data.getString(path + ".last-seen", "-"));
        }
        if ((p = this.pendingByUuid.get(uuid)) != null) {
            this.msg(sender, "&7Pending Code: &e" + p.code + " &7expires in &f" + Math.max(0L, (p.expiresAt - System.currentTimeMillis()) / 1000L) + "s");
        }
        this.msg(sender, "&8&m-----------------------------");
    }

    private UUID findUuid(String name) {
        Player p = Bukkit.getPlayerExact((String)name);
        if (p != null) {
            return p.getUniqueId();
        }
        try {
            return UUID.fromString(name);
        }
        catch (Exception exception) {
            ConfigurationSection sec = this.data.getConfigurationSection("verified");
            if (sec != null) {
                for (String k : sec.getKeys(false)) {
                    String path = "verified." + k;
                    if (!this.data.getString(path + ".last-name", "").equalsIgnoreCase(name)) continue;
                    try {
                        return UUID.fromString(this.data.getString(path + ".uuid", k));
                    }
                    catch (Exception exception2) {
                    }
                }
            }
            return null;
        }
    }

    private void help(CommandSender sender) {
        this.msg(sender, "&8&m-----------------------------");
        this.msg(sender, "&bRumahKitaDiscordVerify");
        this.msg(sender, "&e/verify");
        this.msg(sender, "&e/rkverify status");
        this.msg(sender, "&e/rkverify reload");
        this.msg(sender, "&e/rkverify check <player>");
        this.msg(sender, "&e/rkverify unverify <player>");
        this.msg(sender, "&e/rkverify forceverify <player> <discordId>");
        this.msg(sender, "&e/rkverify clearpending");
        this.msg(sender, "&e/rkverify resyncdiscord");
        this.msg(sender, "&8&m-----------------------------");
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (alias.equalsIgnoreCase("verify")) {
            return List.of();
        }
        if (args.length == 1) {
            return List.of("status", "reload", "check", "unverify", "forceverify", "clearpending", "resyncdiscord");
        }
        if (args.length == 2 && List.of("check", "unverify", "forceverify").contains(args[0].toLowerCase(Locale.ROOT))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }

    private record PendingVerify(UUID uuid, String name, String ip, String subnet, String bedrockXuid, String code, long expiresAt) {
    }

    private record LimitResult(boolean allowed, String reason) {
    }
}

