/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.AsyncPlayerPreLoginEvent
 *  org.bukkit.event.player.AsyncPlayerPreLoginEvent$Result
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package id.rumahkita.securityban;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class RumahKitaSecurityBanPlugin
extends JavaPlugin
implements Listener,
TabExecutor {
    private final Map<String, BanRecord> uuidBans = new ConcurrentHashMap<String, BanRecord>();
    private final Map<String, BanRecord> nameBans = new ConcurrentHashMap<String, BanRecord>();
    private final Map<String, BanRecord> ipBans = new ConcurrentHashMap<String, BanRecord>();
    private final Map<String, BanRecord> subnetBans = new ConcurrentHashMap<String, BanRecord>();
    private File dataFile;
    private YamlConfiguration data;

    public void onEnable() {
        this.saveDefaultConfig();
        this.saveResource("data.yml", false);
        this.dataFile = new File(this.getDataFolder(), "data.yml");
        this.data = YamlConfiguration.loadConfiguration((File)this.dataFile);
        this.loadBans();
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        this.getCommand("rksec").setExecutor((CommandExecutor)this);
        this.getCommand("rksec").setTabCompleter((TabCompleter)this);
        this.getCommand("rkban").setExecutor((CommandExecutor)this);
        this.getCommand("rkban").setTabCompleter((TabCompleter)this);
        this.getCommand("rkipban").setExecutor((CommandExecutor)this);
        this.getCommand("rkipban").setTabCompleter((TabCompleter)this);
        this.getCommand("rkunban").setExecutor((CommandExecutor)this);
        this.getCommand("rkunban").setTabCompleter((TabCompleter)this);
        this.getLogger().info("RumahKitaSecurityBan v1.0.3 enabled.");
    }

    public void onDisable() {
        this.saveData();
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (!this.getConfig().getBoolean("enabled", true)) {
            return;
        }
        String uuid = e.getUniqueId().toString().toLowerCase(Locale.ROOT);
        String name = this.normalizeName(e.getName());
        String ip = e.getAddress().getHostAddress().toLowerCase(Locale.ROOT);
        String subnet = this.subnetOf(ip).toLowerCase(Locale.ROOT);
        BanRecord hit = null;
        String type = "";
        if (this.getConfig().getBoolean("security.check-uuid", true)) {
            hit = this.uuidBans.get(uuid);
            type = "UUID";
        }
        if (hit == null && this.getConfig().getBoolean("security.check-name", true)) {
            hit = this.nameBans.get(name);
            type = "NAME";
        }
        if (hit == null && this.getConfig().getBoolean("security.check-ip", true)) {
            hit = this.ipBans.get(ip);
            type = "IP";
        }
        if (hit == null && this.getConfig().getBoolean("security.check-subnet", true)) {
            hit = this.subnetBans.get(subnet);
            type = "SUBNET";
        }
        if (hit != null) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, this.denyMessage(type, hit.reason));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!this.getConfig().getBoolean("history.enabled", true)) {
            return;
        }
        Player p = e.getPlayer();
        InetSocketAddress a = p.getAddress();
        String ip = a == null || a.getAddress() == null ? "" : a.getAddress().getHostAddress();
        this.updateHistory(p.getUniqueId().toString(), p.getName(), ip);
    }

    private void updateHistory(String uuid, String name, String ip) {
        String path = "history.players." + this.enc(uuid.toLowerCase(Locale.ROOT));
        this.data.set(path + ".uuid", (Object)uuid);
        this.data.set(path + ".last-name", (Object)name);
        this.data.set(path + ".last-ip", (Object)ip);
        this.data.set(path + ".last-seen", (Object)this.now());
        if (this.getConfig().getBoolean("history.save-ip-history", true) && ip != null && !ip.isBlank()) {
            this.data.set(path + ".ips", this.addLimited(this.data.getStringList(path + ".ips"), ip, this.getConfig().getInt("history.max-ips-per-player", 15)));
        }
        if (this.getConfig().getBoolean("history.save-name-history", true)) {
            this.data.set(path + ".names", this.addLimited(this.data.getStringList(path + ".names"), name, this.getConfig().getInt("history.max-names-per-player", 10)));
        }
        this.saveData();
    }

    private List<String> addLimited(List<String> old, String value, int max) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        set.add(value);
        for (String s : old) {
            if (s == null || s.equalsIgnoreCase(value)) continue;
            set.add(s);
        }
        ArrayList<String> out = new ArrayList<String>(set);
        while (out.size() > max) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    private void loadBans() {
        this.uuidBans.clear();
        this.nameBans.clear();
        this.ipBans.clear();
        this.subnetBans.clear();
        this.loadType("uuid", this.uuidBans);
        this.loadType("name", this.nameBans);
        this.loadType("ip", this.ipBans);
        this.loadType("subnet", this.subnetBans);
    }

    private void loadType(String type, Map<String, BanRecord> map) {
        ConfigurationSection sec = this.data.getConfigurationSection("bans." + type);
        if (sec == null) {
            return;
        }
        for (String key : sec.getKeys(false)) {
            String path = "bans." + type + "." + key;
            String value = this.data.getString(path + ".value", "").toLowerCase(Locale.ROOT);
            if (value.isBlank()) continue;
            map.put(value, new BanRecord(type, value, this.data.getString(path + ".reason", "Banned"), this.data.getString(path + ".by", "Console"), this.data.getString(path + ".time", "-")));
        }
    }

    private void addBan(String type, String value, String reason, String by) {
        String val = this.normalizeValue(type, value);
        String path = "bans." + type + "." + this.enc(val);
        this.data.set(path + ".value", (Object)val);
        this.data.set(path + ".reason", (Object)reason);
        this.data.set(path + ".by", (Object)by);
        this.data.set(path + ".time", (Object)this.now());
        BanRecord rec = new BanRecord(type, val, reason, by, this.now());
        if (type.equals("uuid")) {
            this.uuidBans.put(val, rec);
        } else if (type.equals("name")) {
            this.nameBans.put(val, rec);
        } else if (type.equals("ip")) {
            this.ipBans.put(val, rec);
        } else if (type.equals("subnet")) {
            this.subnetBans.put(val, rec);
        }
        this.saveData();
    }

    private boolean removeBan(String value) {
        String uuid;
        String raw = value.trim();
        boolean r = false;
        r |= this.removeFrom("uuid", raw, this.uuidBans);
        r |= this.removeFrom("name", raw, this.nameBans);
        r |= this.removeFrom("ip", raw, this.ipBans);
        r |= this.removeFrom("subnet", raw, this.subnetBans);
        if (!raw.contains("/") && this.looksLikeIp(raw)) {
            r |= this.removeFrom("subnet", this.subnetOf(raw), this.subnetBans);
        }
        if (!(uuid = this.resolveUuid(raw)).isBlank()) {
            r |= this.removeFrom("uuid", uuid, this.uuidBans);
            String path = "history.players." + this.enc(uuid.toLowerCase(Locale.ROOT));
            String lastName = this.data.getString(path + ".last-name", "");
            if (!lastName.isBlank()) {
                r |= this.removeFrom("name", lastName, this.nameBans);
            }
            for (String oldName : this.data.getStringList(path + ".names")) {
                r |= this.removeFrom("name", oldName, this.nameBans);
            }
            String lastIp = this.data.getString(path + ".last-ip", "");
            if (!lastIp.isBlank()) {
                r |= this.removeFrom("ip", lastIp, this.ipBans);
                r |= this.removeFrom("subnet", this.subnetOf(lastIp), this.subnetBans);
            }
            for (String oldIp : this.data.getStringList(path + ".ips")) {
                r |= this.removeFrom("ip", oldIp, this.ipBans);
                r |= this.removeFrom("subnet", this.subnetOf(oldIp), this.subnetBans);
            }
        }
        if (r) {
            this.saveData();
            this.loadBans();
        }
        return r;
    }

    private String resolveUuid(String value) {
        Player online = Bukkit.getPlayerExact((String)value);
        if (online == null) {
            online = Bukkit.getPlayer((String)value);
        }
        if (online != null) {
            return online.getUniqueId().toString();
        }
        if (value.matches("[0-9a-fA-F-]{32,36}")) {
            return value.toLowerCase(Locale.ROOT);
        }
        return this.findUuidFromHistory(value);
    }

    private boolean removeFrom(String type, String value, Map<String, BanRecord> map) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String val = this.normalizeValue(type, value);
        boolean existed = map.remove(val) != null;
        String path = "bans." + type + "." + this.enc(val);
        if (this.data.contains(path)) {
            this.data.set(path, null);
            existed = true;
        }
        return existed;
    }

    private void banPlayer(CommandSender sender, String target, String reason) {
        Player online = Bukkit.getPlayerExact((String)target);
        if (online == null) {
            online = Bukkit.getPlayer((String)target);
        }
        String uuid = "";
        String name = target;
        String ip = "";
        if (online != null) {
            if (online.hasPermission("rumahkita.securityban.bypass")) {
                this.Text(sender, this.pref() + "&eTarget punya bypass.");
                return;
            }
            uuid = online.getUniqueId().toString();
            name = online.getName();
            InetSocketAddress a = online.getAddress();
            if (a != null && a.getAddress() != null) {
                ip = a.getAddress().getHostAddress();
            }
            this.updateHistory(uuid, name, ip);
        } else {
            uuid = target.matches("[0-9a-fA-F-]{32,36}") ? target : this.findUuidFromHistory(target);
        }
        if (!uuid.isBlank()) {
            this.addBan("uuid", uuid, reason, this.senderName(sender));
        }
        if (this.getConfig().getBoolean("security.auto-ban-name", true)) {
            this.addBan("name", name, reason, this.senderName(sender));
        }
        ArrayList<String> ips = new ArrayList<String>();
        if (!ip.isBlank()) {
            ips.add(ip);
        }
        if (!uuid.isBlank()) {
            ips.addAll(this.findIpsFromHistory(uuid));
        }
        if (this.getConfig().getBoolean("security.auto-ban-last-ip", true)) {
            for (String one : ips) {
                if (one == null || one.isBlank()) continue;
                this.addBan("ip", one, reason, this.senderName(sender));
            }
        }
        if (this.getConfig().getBoolean("security.auto-ban-subnet", true)) {
            for (String one : ips) {
                if (one == null || one.isBlank()) continue;
                this.addBan("subnet", this.subnetOf(one), reason, this.senderName(sender));
            }
        }
        if (online != null && this.getConfig().getBoolean("security.kick-online-target", true)) {
            online.kickPlayer(this.denyMessage("STRICT", reason));
        }
        this.Text(sender, this.pref() + this.getConfig().getString("messages.banned", "&cTarget berhasil diban ketat."));
    }

    private void banIp(CommandSender sender, String value, String reason) {
        if (value.contains("/")) {
            this.addBan("subnet", value, reason, this.senderName(sender));
        } else {
            this.addBan("ip", value, reason, this.senderName(sender));
            if (this.getConfig().getBoolean("security.auto-ban-subnet", true)) {
                this.addBan("subnet", this.subnetOf(value), reason, this.senderName(sender));
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            String pip;
            InetSocketAddress a = p.getAddress();
            if (a == null || a.getAddress() == null || !(pip = a.getAddress().getHostAddress()).equalsIgnoreCase(value) && !this.orSubnet(value, pip) || p.hasPermission("rumahkita.securityban.bypass")) continue;
            p.kickPlayer(this.denyMessage("IP", reason));
        }
        this.Text(sender, this.pref() + this.getConfig().getString("messages.ip-banned", "&cIP/Subnet berhasil diban."));
    }

    private boolean orSubnet(String target, String playerIp) {
        return target.equalsIgnoreCase(this.subnetOf(playerIp)) || this.subnetBans.containsKey(this.subnetOf(playerIp).toLowerCase(Locale.ROOT));
    }

    private void check(CommandSender sender, String q) {
        this.Text(sender, "&8&m-----------------------------");
        this.Text(sender, "&cSecurityBan Check: &f" + q);
        this.show(sender, "Direct UUID", this.uuidBans.get(q.toLowerCase(Locale.ROOT)));
        this.show(sender, "Direct NAME", this.nameBans.get(this.normalizeName(q)));
        this.show(sender, "Direct IP", this.ipBans.get(q.toLowerCase(Locale.ROOT)));
        this.show(sender, "Direct SUBNET", this.subnetBans.get(this.looksLikeIp(q) && !q.contains("/") ? this.subnetOf(q).toLowerCase(Locale.ROOT) : q.toLowerCase(Locale.ROOT)));
        String uuid = this.resolveUuid(q);
        if (!uuid.isBlank()) {
            List ips;
            List names;
            this.Text(sender, "&7Resolved UUID: &f" + uuid);
            this.show(sender, "Linked UUID", this.uuidBans.get(uuid.toLowerCase(Locale.ROOT)));
            String path = "history.players." + this.enc(uuid.toLowerCase(Locale.ROOT));
            String lastName = this.data.getString(path + ".last-name", "");
            if (!lastName.isBlank()) {
                this.show(sender, "Linked NAME " + lastName, this.nameBans.get(this.normalizeName(lastName)));
            }
            if (!(names = this.data.getStringList(path + ".names")).isEmpty()) {
                this.Text(sender, "&7History Names: &f" + String.join((CharSequence)", ", names));
            }
            if (!(ips = this.data.getStringList(path + ".ips")).isEmpty()) {
                this.Text(sender, "&7History IPs: &f" + String.join((CharSequence)", ", ips));
                for (String ip : ips) {
                    this.show(sender, "Linked IP " + ip, this.ipBans.get(ip.toLowerCase(Locale.ROOT)));
                    this.show(sender, "Linked SUBNET " + this.subnetOf(ip), this.subnetBans.get(this.subnetOf(ip).toLowerCase(Locale.ROOT)));
                }
            }
        }
        this.Text(sender, "&8&m-----------------------------");
    }

    private void show(CommandSender s, String t, BanRecord r) {
        this.Text(s, r == null ? "&7" + t + ": &aNot banned" : "&7" + t + ": &cBANNED &8| &f" + r.value + " &8| &e" + r.reason);
    }

    private void list(CommandSender s) {
        this.Text(s, "&cBan list: &7UUID=&f" + this.uuidBans.size() + " &7Name=&f" + this.nameBans.size() + " &7IP=&f" + this.ipBans.size() + " &7Subnet=&f" + this.subnetBans.size());
    }

    private void history(CommandSender s, String q) {
        String uuid = this.findUuidFromHistory(q);
        Player p = Bukkit.getPlayerExact((String)q);
        if (p != null) {
            uuid = p.getUniqueId().toString();
        }
        if (uuid.isBlank()) {
            this.Text(s, this.pref() + this.getConfig().getString("messages.player-not-found"));
            return;
        }
        String path = "history.players." + this.enc(uuid.toLowerCase(Locale.ROOT));
        this.Text(s, "&8&m-----------------------------");
        this.Text(s, "&cSecurity History");
        this.Text(s, "&7UUID: &f" + this.data.getString(path + ".uuid", uuid));
        this.Text(s, "&7Last Name: &f" + this.data.getString(path + ".last-name", "-"));
        this.Text(s, "&7Last IP: &f" + this.data.getString(path + ".last-ip", "-"));
        this.Text(s, "&7Last Seen: &f" + this.data.getString(path + ".last-seen", "-"));
        this.Text(s, "&7Names: &f" + String.join((CharSequence)", ", this.data.getStringList(path + ".names")));
        this.Text(s, "&7IPs: &f" + String.join((CharSequence)", ", this.data.getStringList(path + ".ips")));
        this.Text(s, "&8&m-----------------------------");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("rumahkita.securityban.admin")) {
            this.Text(sender, this.pref() + this.getConfig().getString("messages.no-permission"));
            return true;
        }
        String l = label.toLowerCase(Locale.ROOT);
        if (l.equals("rkban")) {
            if (args.length < 1) {
                this.Text(sender, "&e/rkban <player> [reason]");
            } else {
                this.banPlayer(sender, args[0], this.reason(args, 1));
            }
            return true;
        }
        if (l.equals("rkipban")) {
            if (args.length < 1) {
                this.Text(sender, "&e/rkipban <ip|cidr> [reason]");
            } else {
                this.banIp(sender, args[0], this.reason(args, 1));
            }
            return true;
        }
        if (l.equals("rkunban")) {
            if (args.length < 1) {
                this.Text(sender, "&e/rkunban <uuid|name|ip|cidr>");
            } else {
                this.Text(sender, this.pref() + (this.removeBan(args[0]) ? this.getConfig().getString("messages.unbanned") : this.getConfig().getString("messages.not-banned")));
            }
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            this.Text(sender, this.pref() + "&7Enabled: &f" + this.getConfig().getBoolean("enabled", true));
            this.list(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload": {
                this.reloadConfig();
                this.data = YamlConfiguration.loadConfiguration((File)this.dataFile);
                this.loadBans();
                this.Text(sender, this.pref() + this.getConfig().getString("messages.reloaded"));
                break;
            }
            case "ban": {
                if (args.length < 2) {
                    this.Text(sender, "&e/rksec ban <player> [reason]");
                    break;
                }
                this.banPlayer(sender, args[1], this.reason(args, 2));
                break;
            }
            case "banip": {
                if (args.length < 2) {
                    this.Text(sender, "&e/rksec banip <ip|cidr> [reason]");
                    break;
                }
                this.banIp(sender, args[1], this.reason(args, 2));
                break;
            }
            case "unban": {
                if (args.length < 2) {
                    this.Text(sender, "&e/rksec unban <target>");
                    break;
                }
                this.Text(sender, this.pref() + (this.removeBan(args[1]) ? this.getConfig().getString("messages.unbanned") : this.getConfig().getString("messages.not-banned")));
                break;
            }
            case "check": {
                if (args.length < 2) {
                    this.Text(sender, "&e/rksec check <target>");
                    break;
                }
                this.check(sender, args[1]);
                break;
            }
            case "history": {
                if (args.length < 2) {
                    this.Text(sender, "&e/rksec history <player>");
                    break;
                }
                this.history(sender, args[1]);
                break;
            }
            case "list": {
                this.list(sender);
                break;
            }
            default: {
                this.help(sender);
            }
        }
        return true;
    }

    private void help(CommandSender s) {
        this.Text(s, "&8&m-----------------------------");
        this.Text(s, "&cRumahKitaSecurityBan");
        this.Text(s, "&e/rkban <player> [reason]");
        this.Text(s, "&e/rkipban <ip|cidr> [reason]");
        this.Text(s, "&e/rkunban <uuid|name|ip|cidr>");
        this.Text(s, "&e/rksec check <target>");
        this.Text(s, "&e/rksec history <player>");
        this.Text(s, "&e/rksec list");
        this.Text(s, "&e/rksec reload");
        this.Text(s, "&8&m-----------------------------");
    }

    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (args.length == 1 && a.equalsIgnoreCase("rksec")) {
            return List.of("status", "reload", "ban", "banip", "unban", "check", "history", "list");
        }
        if (args.length == 1 && a.equalsIgnoreCase("rkban")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2 && a.equalsIgnoreCase("rksec") && List.of("ban", "check", "history").contains(args[0].toLowerCase(Locale.ROOT))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }

    private String normalizeName(String n) {
        String v = n == null ? "" : n.trim();
        return this.getConfig().getBoolean("security.name-case-insensitive", true) ? v.toLowerCase(Locale.ROOT) : v;
    }

    private String normalizeValue(String type, String v) {
        return type.equals("name") ? this.normalizeName(v) : v.trim().toLowerCase(Locale.ROOT);
    }

    private String denyMessage(String type, String reason) {
        List lines = this.getConfig().getStringList("messages.deny-message");
        ArrayList<String> out = new ArrayList<String>();
        for (String line : lines) {
            out.add(this.color(line.replace("%reason%", reason).replace("%type%", type)));
        }
        return String.join((CharSequence)"\n", out);
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private String enc(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private String senderName(CommandSender s) {
        String string;
        if (s instanceof Player) {
            Player p = (Player)s;
            string = p.getName();
        } else {
            string = "Console";
        }
        return string;
    }

    private String reason(String[] args, int start) {
        if (args.length <= start) {
            return "Banned by staff";
        }
        StringBuilder b = new StringBuilder();
        for (int i = start; i < args.length; ++i) {
            if (i > start) {
                b.append(' ');
            }
            b.append(args[i]);
        }
        return b.toString();
    }

    private boolean looksLikeIp(String in) {
        return in.matches("[0-9a-fA-F:.]+") || in.contains("/");
    }

    private String findUuidFromHistory(String q) {
        String n = this.normalizeName(q);
        ConfigurationSection sec = this.data.getConfigurationSection("history.players");
        if (sec == null) {
            return "";
        }
        for (String key : sec.getKeys(false)) {
            String path = "history.players." + key;
            String uuid = this.data.getString(path + ".uuid", "");
            if (!uuid.isBlank() && uuid.equalsIgnoreCase(q)) {
                return uuid;
            }
            if (this.normalizeName(this.data.getString(path + ".last-name", "")).equals(n)) {
                return uuid;
            }
            for (String old : this.data.getStringList(path + ".names")) {
                if (!this.normalizeName(old).equals(n)) continue;
                return uuid;
            }
        }
        return "";
    }

    private List<String> findIpsFromHistory(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return List.of();
        }
        return this.data.getStringList("history.players." + this.enc(uuid.toLowerCase(Locale.ROOT)) + ".ips");
    }

    private void saveData() {
        try {
            if (this.data != null && this.dataFile != null) {
                this.data.save(this.dataFile);
            }
        }
        catch (Exception e) {
            this.getLogger().warning("Gagal save data.yml: " + e.getMessage());
        }
    }

    private String pref() {
        return this.getConfig().getString("messages.prefix", "");
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)(s == null ? "" : s));
    }

    private void Text(CommandSender s, String m) {
        s.sendMessage(this.color(m));
    }

    private String subnetOf(String ip) {
        if (ip == null || ip.isBlank()) {
            return "";
        }
        if (ip.contains(":")) {
            return this.ipv6Subnet(ip, this.getConfig().getInt("security.ipv6-subnet-prefix", 64));
        }
        return this.ipv4Subnet(ip, this.getConfig().getInt("security.ipv4-subnet-prefix", 24));
    }

    private String ipv4Subnet(String ip, int prefix) {
        try {
            String[] p = ip.split("\\.");
            if (p.length != 4) {
                return ip.toLowerCase(Locale.ROOT);
            }
            int raw = 0;
            for (String part : p) {
                raw = raw << 8 | Integer.parseInt(part) & 0xFF;
            }
            int mask = prefix <= 0 ? 0 : -1 << 32 - prefix;
            int net = raw & mask;
            return ((net >>> 24 & 0xFF) + "." + (net >>> 16 & 0xFF) + "." + (net >>> 8 & 0xFF) + "." + (net & 0xFF) + "/" + prefix).toLowerCase(Locale.ROOT);
        }
        catch (Exception e) {
            return ip.toLowerCase(Locale.ROOT);
        }
    }

    private String ipv6Subnet(String ip, int prefix) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            byte[] bytes = address.getAddress();
            int full = prefix / 8;
            int bits = prefix % 8;
            for (int i = full + (bits > 0 ? 1 : 0); i < bytes.length; ++i) {
                bytes[i] = 0;
            }
            if (bits > 0 && full < bytes.length) {
                int mask = 255 << 8 - bits & 0xFF;
                bytes[full] = (byte)(bytes[full] & mask);
            }
            return (InetAddress.getByAddress(bytes).getHostAddress() + "/" + prefix).toLowerCase(Locale.ROOT);
        }
        catch (Exception e) {
            return (ip + "/" + prefix).toLowerCase(Locale.ROOT);
        }
    }

    private record BanRecord(String type, String value, String reason, String by, String time) {
    }
}

