/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.OfflinePlayer
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
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package id.rumahkita.guildadmin;

import java.io.File;
import java.io.FileWriter;
import java.lang.invoke.CallSite;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
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
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class RumahKitaGuildAdminToolsPlugin
extends JavaPlugin
implements Listener,
TabExecutor {
    public void onEnable() {
        this.saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        if (this.getCommand("rkgadmin") != null) {
            this.getCommand("rkgadmin").setExecutor((CommandExecutor)this);
            this.getCommand("rkgadmin").setTabCompleter((TabCompleter)this);
        }
        this.getLogger().info("RumahKitaGuildAdminTools v1.0.0 enabled.");
    }

    public void onDisable() {
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onGuildCommandWhileFrozen(PlayerCommandPreprocessEvent event) {
        if (!this.getConfig().getBoolean("frozen-guild-command-block.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("rumahkita.guild.admin.bypass")) {
            return;
        }
        String raw = event.getMessage();
        String lower = raw.toLowerCase(Locale.ROOT).trim();
        if (!this.isGuildCommand(lower)) {
            return;
        }
        if (!this.isBlockedSubCommand(lower)) {
            return;
        }
        GuildRef ref = this.findGuildByPlayer(player.getName(), player.getUniqueId().toString());
        if (ref == null) {
            return;
        }
        if (!this.isFrozen(ref)) {
            return;
        }
        event.setCancelled(true);
        this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.guild-frozen", "&cGuild kamu sedang dibekukan oleh admin."));
    }

    private boolean isGuildCommand(String lower) {
        for (String cmd : this.getConfig().getStringList("frozen-guild-command-block.commands")) {
            Object c = cmd.toLowerCase(Locale.ROOT).trim();
            if (!((String)c).startsWith("/")) {
                c = "/" + (String)c;
            }
            if (!lower.equals(c) && !lower.startsWith((String)c + " ")) continue;
            return true;
        }
        return false;
    }

    private boolean isBlockedSubCommand(String lower) {
        String[] split = lower.split("\\s+");
        if (split.length < 2) {
            return false;
        }
        String sub = split[1].toLowerCase(Locale.ROOT);
        for (String blocked : this.getConfig().getStringList("frozen-guild-command-block.blocked-subcommands")) {
            if (!sub.equals(blocked.toLowerCase(Locale.ROOT))) continue;
            return true;
        }
        return false;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!this.has(sender, "rumahkita.guild.admin")) {
            this.msg(sender, this.pref() + this.getConfig().getString("messages.no-permission", "&cNo permission."));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            this.sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        try {
            switch (sub) {
                case "reload": {
                    if (!this.has(sender, "rumahkita.guild.admin.reload")) {
                        return this.noPerm(sender);
                    }
                    this.reloadConfig();
                    this.msg(sender, this.pref() + this.getConfig().getString("messages.reloaded", "&aReloaded."));
                    break;
                }
                case "scan": {
                    this.scan(sender);
                    break;
                }
                case "list": {
                    this.listGuilds(sender);
                    break;
                }
                case "info": {
                    if (args.length < 2) {
                        return this.usage(sender, "/rkgadmin info <guild>");
                    }
                    GuildRef ref = this.findGuild(args[1]);
                    if (ref == null) {
                        return this.notFound(sender);
                    }
                    this.sendInfo(sender, ref);
                    break;
                }
                case "members": {
                    if (args.length < 2) {
                        return this.usage(sender, "/rkgadmin members <guild>");
                    }
                    GuildRef ref = this.findGuild(args[1]);
                    if (ref == null) {
                        return this.notFound(sender);
                    }
                    this.showMembers(sender, ref);
                    break;
                }
                case "freeze": {
                    if (!this.has(sender, "rumahkita.guild.admin.freeze")) {
                        return this.noPerm(sender);
                    }
                    if (args.length < 2) {
                        return this.usage(sender, "/rkgadmin freeze <guild>");
                    }
                    GuildRef ref = this.findGuild(args[1]);
                    if (ref == null) {
                        return this.notFound(sender);
                    }
                    this.backup(ref.file, sender);
                    ref.yml.set(ref.path + ".admin-frozen", (Object)true);
                    ref.yml.set(ref.path + ".frozen", (Object)true);
                    ref.yml.set(ref.path + ".freeze-reason", (Object)("Frozen by admin " + sender.getName()));
                    ref.yml.set(ref.path + ".frozen-at", (Object)this.time());
                    this.save(ref);
                    this.log(sender, "FREEZE guild=" + ref.name);
                    this.msg(sender, this.pref() + "&cGuild &f" + ref.name + " &cdibekukan.");
                    break;
                }
                case "unfreeze": {
                    if (!this.has(sender, "rumahkita.guild.admin.freeze")) {
                        return this.noPerm(sender);
                    }
                    if (args.length < 2) {
                        return this.usage(sender, "/rkgadmin unfreeze <guild>");
                    }
                    GuildRef ref = this.findGuild(args[1]);
                    if (ref == null) {
                        return this.notFound(sender);
                    }
                    this.backup(ref.file, sender);
                    ref.yml.set(ref.path + ".admin-frozen", (Object)false);
                    ref.yml.set(ref.path + ".frozen", (Object)false);
                    ref.yml.set(ref.path + ".freeze-reason", null);
                    this.save(ref);
                    this.log(sender, "UNFREEZE guild=" + ref.name);
                    this.msg(sender, this.pref() + "&aGuild &f" + ref.name + " &adibuka freeze-nya.");
                    break;
                }
                case "disband": {
                    if (!this.has(sender, "rumahkita.guild.admin.disband")) {
                        return this.noPerm(sender);
                    }
                    if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
                        return this.confirm(sender, "/rkgadmin disband <guild> confirm");
                    }
                    GuildRef ref = this.findGuild(args[1]);
                    if (ref == null) {
                        return this.notFound(sender);
                    }
                    this.backup(ref.file, sender);
                    ref.yml.set(ref.path, null);
                    this.save(ref);
                    this.log(sender, "DISBAND guild=" + ref.name);
                    this.msg(sender, this.pref() + "&cGuild &f" + ref.name + " &csudah dibubarkan paksa.");
                    break;
                }
                case "delhome": {
                    if (!this.has(sender, "rumahkita.guild.admin.home")) {
                        return this.noPerm(sender);
                    }
                    if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
                        return this.confirm(sender, "/rkgadmin delhome <guild> confirm");
                    }
                    GuildRef ref = this.findGuild(args[1]);
                    if (ref == null) {
                        return this.notFound(sender);
                    }
                    this.backup(ref.file, sender);
                    this.clearHome(ref);
                    this.save(ref);
                    this.log(sender, "DELHOME guild=" + ref.name);
                    this.msg(sender, this.pref() + "&aHome guild &f" + ref.name + " &asudah dihapus.");
                    break;
                }
                case "sethome": {
                    if (!this.has(sender, "rumahkita.guild.admin.home")) {
                        return this.noPerm(sender);
                    }
                    if (!(sender instanceof Player)) {
                        this.msg(sender, this.pref() + "&cCommand ini harus dari player.");
                        return true;
                    }
                    Player p = (Player)sender;
                    if (args.length < 2) {
                        return this.usage(sender, "/rkgadmin sethome <guild>");
                    }
                    GuildRef ref = this.findGuild(args[1]);
                    if (ref == null) {
                        return this.notFound(sender);
                    }
                    this.backup(ref.file, sender);
                    this.setHome(ref, p.getLocation());
                    this.save(ref);
                    this.log(sender, "SETHOME guild=" + ref.name + " loc=" + this.locString(p.getLocation()));
                    this.msg(sender, this.pref() + "&aHome guild &f" + ref.name + " &adisimpan di lokasi kamu.");
                    break;
                }
                case "kick": {
                    if (!this.has(sender, "rumahkita.guild.admin.player")) {
                        return this.noPerm(sender);
                    }
                    if (args.length < 4 || !args[3].equalsIgnoreCase("confirm")) {
                        return this.confirm(sender, "/rkgadmin kick <guild> <player> confirm");
                    }
                    GuildRef ref = this.findGuild(args[1]);
                    if (ref == null) {
                        return this.notFound(sender);
                    }
                    this.backup(ref.file, sender);
                    int removed = this.removePlayerFromSection(ref.yml, ref.path, args[2]);
                    this.save(ref);
                    this.log(sender, "KICK guild=" + ref.name + " player=" + args[2] + " removed=" + removed);
                    this.msg(sender, this.pref() + "&aRemoved &f" + removed + " &adata player dari guild &f" + ref.name + "&a.");
                    break;
                }
                case "removeplayer": {
                    if (!this.has(sender, "rumahkita.guild.admin.player")) {
                        return this.noPerm(sender);
                    }
                    if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
                        return this.confirm(sender, "/rkgadmin removeplayer <player> confirm");
                    }
                    int total = 0;
                    for (GuildRef ref : this.allGuildRefs()) {
                        int removed = this.removePlayerFromSection(ref.yml, ref.path, args[1]);
                        if (removed <= 0) continue;
                        this.backup(ref.file, sender);
                        this.save(ref);
                        total += removed;
                    }
                    this.log(sender, "REMOVEPLAYER player=" + args[1] + " removed=" + total);
                    this.msg(sender, this.pref() + "&aRemoved &f" + total + " &adata milik player &f" + args[1] + "&a.");
                    break;
                }
                case "setleader": {
                    if (!this.has(sender, "rumahkita.guild.admin.player")) {
                        return this.noPerm(sender);
                    }
                    if (args.length < 4 || !args[3].equalsIgnoreCase("confirm")) {
                        return this.confirm(sender, "/rkgadmin setleader <guild> <player> confirm");
                    }
                    GuildRef ref = this.findGuild(args[1]);
                    if (ref == null) {
                        return this.notFound(sender);
                    }
                    this.backup(ref.file, sender);
                    this.setLeader(ref, args[2]);
                    this.save(ref);
                    this.log(sender, "SETLEADER guild=" + ref.name + " leader=" + args[2]);
                    this.msg(sender, this.pref() + "&aLeader guild &f" + ref.name + " &adiubah ke &f" + args[2]);
                    break;
                }
                case "rename": {
                    if (!this.has(sender, "rumahkita.guild.admin.disband")) {
                        return this.noPerm(sender);
                    }
                    if (args.length < 4 || !args[3].equalsIgnoreCase("confirm")) {
                        return this.confirm(sender, "/rkgadmin rename <guild> <namaBaru> confirm");
                    }
                    GuildRef ref = this.findGuild(args[1]);
                    if (ref == null) {
                        return this.notFound(sender);
                    }
                    this.backup(ref.file, sender);
                    this.renameGuild(ref, args[2]);
                    this.log(sender, "RENAME guild=" + args[1] + " new=" + args[2]);
                    this.msg(sender, this.pref() + "&aGuild rename dicoba ke &f" + args[2] + "&a. Cek data setelah restart.");
                    break;
                }
                case "setprefix": {
                    if (!this.has(sender, "rumahkita.guild.admin.disband")) {
                        return this.noPerm(sender);
                    }
                    if (args.length < 3) {
                        return this.usage(sender, "/rkgadmin setprefix <guild> <prefix>");
                    }
                    GuildRef ref = this.findGuild(args[1]);
                    if (ref == null) {
                        return this.notFound(sender);
                    }
                    this.backup(ref.file, sender);
                    ref.yml.set(ref.path + ".prefix", (Object)args[2]);
                    ref.yml.set(ref.path + ".tag", (Object)args[2]);
                    this.save(ref);
                    this.log(sender, "SETPREFIX guild=" + ref.name + " prefix=" + args[2]);
                    this.msg(sender, this.pref() + "&aPrefix guild &f" + ref.name + " &adiubah.");
                    break;
                }
                case "backup": {
                    int count = 0;
                    for (File f : this.dataFiles()) {
                        if (!f.exists()) continue;
                        this.backup(f, sender);
                        ++count;
                    }
                    this.msg(sender, this.pref() + "&aBackup manual selesai. File: &f" + count);
                    break;
                }
                default: {
                    this.sendHelp(sender);
                    break;
                }
            }
        }
        catch (Exception e) {
            this.msg(sender, this.pref() + "&cError: &f" + e.getMessage());
            this.getLogger().warning("Command error: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    private boolean noPerm(CommandSender s) {
        this.msg(s, this.pref() + this.getConfig().getString("messages.no-permission", "&cNo permission."));
        return true;
    }

    private boolean usage(CommandSender s, String u) {
        this.msg(s, this.pref() + "&eUsage: &f" + u);
        return true;
    }

    private boolean confirm(CommandSender s, String u) {
        this.msg(s, this.pref() + this.getConfig().getString("messages.need-confirm", "&eNeed confirm."));
        this.msg(s, "&f" + u);
        return true;
    }

    private boolean notFound(CommandSender s) {
        this.msg(s, this.pref() + this.getConfig().getString("messages.not-found", "&cNot found."));
        return true;
    }

    private void sendHelp(CommandSender s) {
        this.msg(s, "&8&m--------------------------------");
        this.msg(s, "&bRumahKita Guild Admin Tools");
        this.msg(s, "&e/rkgadmin list");
        this.msg(s, "&e/rkgadmin info <guild>");
        this.msg(s, "&e/rkgadmin members <guild>");
        this.msg(s, "&e/rkgadmin freeze <guild>");
        this.msg(s, "&e/rkgadmin unfreeze <guild>");
        this.msg(s, "&e/rkgadmin disband <guild> confirm");
        this.msg(s, "&e/rkgadmin delhome <guild> confirm");
        this.msg(s, "&e/rkgadmin sethome <guild>");
        this.msg(s, "&e/rkgadmin kick <guild> <player> confirm");
        this.msg(s, "&e/rkgadmin removeplayer <player> confirm");
        this.msg(s, "&e/rkgadmin setleader <guild> <player> confirm");
        this.msg(s, "&e/rkgadmin rename <guild> <namaBaru> confirm");
        this.msg(s, "&e/rkgadmin setprefix <guild> <prefix>");
        this.msg(s, "&e/rkgadmin backup / reload / scan");
        this.msg(s, "&8&m--------------------------------");
    }

    private void scan(CommandSender s) {
        this.msg(s, this.pref() + "&7Folder target: &fplugins/" + this.getConfig().getString("guild-plugin-folder", "RumahKitaGuilds"));
        for (File f : this.dataFiles()) {
            this.msg(s, "&7- " + f.getName() + ": " + (f.exists() ? "&aFOUND" : "&cmissing"));
            if (!f.exists()) continue;
            YamlConfiguration yml = YamlConfiguration.loadConfiguration((File)f);
            this.msg(s, "  &8top keys: &f" + String.valueOf(yml.getKeys(false)));
        }
    }

    private void listGuilds(CommandSender s) {
        List<GuildRef> refs = this.allGuildRefs();
        this.msg(s, this.pref() + "&aTotal guild terdeteksi: &f" + refs.size());
        int shown = 0;
        for (GuildRef ref : refs) {
            this.msg(s, "&e" + ++shown + ". &f" + ref.name + " &8(" + ref.file.getName() + " : " + ref.path + ")" + (this.isFrozen(ref) ? " &c[FROZEN]" : ""));
            if (shown < 20) continue;
            this.msg(s, "&7...pakai /rkgadmin info <guild> untuk detail");
            break;
        }
    }

    private void sendInfo(CommandSender s, GuildRef ref) {
        this.msg(s, "&8&m--------------------------------");
        this.msg(s, "&bGuild Info: &f" + ref.name);
        this.msg(s, "&7File: &f" + ref.file.getName());
        this.msg(s, "&7Path: &f" + ref.path);
        this.msg(s, "&7Frozen: &f" + this.isFrozen(ref));
        this.msg(s, "&7Leader: &f" + this.firstExistingString(ref, "leader", "owner", "leader-name", "owner-name"));
        this.msg(s, "&7Prefix: &f" + this.firstExistingString(ref, "prefix", "tag"));
        this.msg(s, "&7Home: &f" + this.homeString(ref));
        this.msg(s, "&7Keys: &f" + this.keys(ref));
        this.msg(s, "&8&m--------------------------------");
    }

    private void showMembers(CommandSender s, GuildRef ref) {
        LinkedHashSet<String> values = new LinkedHashSet<String>();
        this.collectMemberLike(ref.yml.getConfigurationSection(ref.path), values, 0);
        this.msg(s, this.pref() + "&aMember-like data untuk &f" + ref.name + "&a:");
        int i = 0;
        for (String v : values) {
            if (++i > 40) {
                this.msg(s, "&7...terlalu banyak, dipotong.");
                break;
            }
            this.msg(s, "&7- &f" + v);
        }
    }

    private List<File> dataFiles() {
        File folder = new File("plugins", this.getConfig().getString("guild-plugin-folder", "RumahKitaGuilds"));
        ArrayList<File> files = new ArrayList<File>();
        for (String name : this.getConfig().getStringList("data-files")) {
            files.add(new File(folder, name));
        }
        return files;
    }

    private List<GuildRef> allGuildRefs() {
        ArrayList<GuildRef> refs = new ArrayList<GuildRef>();
        HashSet<CallSite> seen = new HashSet<CallSite>();
        for (File file : this.dataFiles()) {
            if (!file.exists()) continue;
            YamlConfiguration yml = YamlConfiguration.loadConfiguration((File)file);
            for (String rootPath : this.getConfig().getStringList("root-paths")) {
                YamlConfiguration rootSec;
                Object object = rootSec = rootPath.isBlank() ? yml : yml.getConfigurationSection(rootPath);
                if (rootSec == null) continue;
                for (String key : rootSec.getKeys(false)) {
                    String path;
                    String string = path = rootPath.isBlank() ? key : rootPath + "." + key;
                    if (!yml.isConfigurationSection(path)) continue;
                    String name = yml.getString(path + ".name", key);
                    String id = file.getAbsolutePath() + "|" + path;
                    if (!seen.add((CallSite)((Object)id))) continue;
                    refs.add(new GuildRef(file, yml, path, name));
                }
            }
        }
        return refs;
    }

    private GuildRef findGuild(String name) {
        String target = this.clean(name);
        for (GuildRef ref : this.allGuildRefs()) {
            if (this.clean(ref.name).equals(target) || this.clean(this.lastPath(ref.path)).equals(target)) {
                return ref;
            }
            String alt = ref.yml.getString(ref.path + ".display-name", "");
            if (!this.clean(alt).equals(target)) continue;
            return ref;
        }
        return null;
    }

    private GuildRef findGuildByPlayer(String playerName, String uuid) {
        String n = this.clean(playerName);
        String u = this.clean(uuid);
        for (GuildRef ref : this.allGuildRefs()) {
            ConfigurationSection sec = ref.yml.getConfigurationSection(ref.path);
            if (sec == null || !this.containsPlayer(sec, n, u, 0)) continue;
            return ref;
        }
        return null;
    }

    private boolean containsPlayer(Object obj, String n, String u, int depth) {
        if (obj == null || depth > 6) {
            return false;
        }
        if (obj instanceof ConfigurationSection) {
            ConfigurationSection sec = (ConfigurationSection)obj;
            for (String key : sec.getKeys(false)) {
                if (this.clean(key).equals(n) || this.clean(key).equals(u)) {
                    return true;
                }
                if (!this.containsPlayer(sec.get(key), n, u, depth + 1)) continue;
                return true;
            }
            return false;
        }
        if (obj instanceof Iterable) {
            Iterable it = (Iterable)obj;
            for (Object o : it) {
                if (!this.containsPlayer(o, n, u, depth + 1)) continue;
                return true;
            }
            return false;
        }
        String v = this.clean(String.valueOf(obj));
        return v.equals(n) || v.equals(u);
    }

    private int removePlayerFromSection(YamlConfiguration yml, String path, String player) {
        UUID id;
        OfflinePlayer off = Bukkit.getOfflinePlayer((String)player);
        HashSet<String> targets = new HashSet<String>();
        targets.add(this.clean(player));
        if (off.getName() != null) {
            targets.add(this.clean(off.getName()));
        }
        if ((id = off.getUniqueId()) != null) {
            targets.add(this.clean(id.toString()));
        }
        return this.removeTargets(yml, path, targets, 0);
    }

    private int removeTargets(YamlConfiguration yml, String path, Set<String> targets, int depth) {
        if (depth > 8) {
            return 0;
        }
        int removed = 0;
        Object obj = yml.get(path);
        if (obj instanceof ConfigurationSection) {
            ConfigurationSection sec = (ConfigurationSection)obj;
            for (String key : new ArrayList(sec.getKeys(false))) {
                String str;
                String child;
                String string = child = path.isBlank() ? key : path + "." + key;
                if (targets.contains(this.clean(key))) {
                    yml.set(child, null);
                    ++removed;
                    continue;
                }
                Object val = yml.get(child);
                if (val instanceof String && targets.contains(this.clean(str = (String)val))) {
                    String lowKey = key.toLowerCase(Locale.ROOT);
                    if (!lowKey.contains("member") && !lowKey.contains("player") && !lowKey.contains("leader") && !lowKey.contains("owner") && !lowKey.contains("uuid") && !lowKey.contains("name")) continue;
                    yml.set(child, null);
                    ++removed;
                    continue;
                }
                if (val instanceof List) {
                    List list = (List)val;
                    ArrayList newList = new ArrayList();
                    for (Object item : list) {
                        if (targets.contains(this.clean(String.valueOf(item)))) {
                            ++removed;
                            continue;
                        }
                        newList.add(item);
                    }
                    if (removed <= 0) continue;
                    yml.set(child, newList);
                    continue;
                }
                if (!(val instanceof ConfigurationSection)) continue;
                removed += this.removeTargets(yml, child, targets, depth + 1);
            }
        }
        return removed;
    }

    private void clearHome(GuildRef ref) {
        for (String p : Arrays.asList("home", "guildHome", "base", "spawn", "homes", "home-location", "homeLocation")) {
            ref.yml.set(ref.path + "." + p, null);
        }
        for (String p : Arrays.asList("home-world", "home-x", "home-y", "home-z", "home-yaw", "home-pitch", "world", "x", "y", "z", "yaw", "pitch")) {
            String low = p.toLowerCase(Locale.ROOT);
            if (!low.startsWith("home") && !ref.yml.contains(ref.path + ".home." + p)) continue;
            ref.yml.set(ref.path + "." + p, null);
        }
    }

    private void setHome(GuildRef ref, Location loc) {
        String base = ref.path + ".home";
        ref.yml.set(base + ".world", (Object)loc.getWorld().getName());
        ref.yml.set(base + ".x", (Object)loc.getX());
        ref.yml.set(base + ".y", (Object)loc.getY());
        ref.yml.set(base + ".z", (Object)loc.getZ());
        ref.yml.set(base + ".yaw", (Object)Float.valueOf(loc.getYaw()));
        ref.yml.set(base + ".pitch", (Object)Float.valueOf(loc.getPitch()));
        ref.yml.set(ref.path + ".home-world", (Object)loc.getWorld().getName());
        ref.yml.set(ref.path + ".home-x", (Object)loc.getX());
        ref.yml.set(ref.path + ".home-y", (Object)loc.getY());
        ref.yml.set(ref.path + ".home-z", (Object)loc.getZ());
        ref.yml.set(ref.path + ".home-yaw", (Object)Float.valueOf(loc.getYaw()));
        ref.yml.set(ref.path + ".home-pitch", (Object)Float.valueOf(loc.getPitch()));
    }

    private void setLeader(GuildRef ref, String player) {
        OfflinePlayer off = Bukkit.getOfflinePlayer((String)player);
        ref.yml.set(ref.path + ".leader", (Object)player);
        ref.yml.set(ref.path + ".owner", (Object)player);
        if (off.getUniqueId() != null) {
            ref.yml.set(ref.path + ".leader-uuid", (Object)off.getUniqueId().toString());
            ref.yml.set(ref.path + ".owner-uuid", (Object)off.getUniqueId().toString());
        }
    }

    private void renameGuild(GuildRef ref, String newName) throws Exception {
        String parent = this.parentPath(ref.path);
        String newPath = parent.isBlank() ? newName : parent + "." + newName;
        Object old = ref.yml.get(ref.path);
        ref.yml.set(newPath, old);
        ref.yml.set(newPath + ".name", (Object)newName);
        ref.yml.set(ref.path, null);
        ref.yml.save(ref.file);
    }

    private void backup(File file, CommandSender sender) throws Exception {
        if (!this.getConfig().getBoolean("backup.enabled", true) || !file.exists()) {
            return;
        }
        File folder = new File(this.getDataFolder(), this.getConfig().getString("backup.folder", "backups"));
        folder.mkdirs();
        File dest = new File(folder, file.getName() + "." + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".bak");
        Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        this.msg(sender, this.pref() + this.getConfig().getString("messages.backup-made", "&aBackup made: %file%").replace("%file%", dest.getName()));
    }

    private void save(GuildRef ref) throws Exception {
        ref.yml.save(ref.file);
    }

    private boolean isFrozen(GuildRef ref) {
        return ref.yml.getBoolean(ref.path + ".admin-frozen", false) || ref.yml.getBoolean(ref.path + ".frozen", false);
    }

    private void collectMemberLike(Object obj, Set<String> out, int depth) {
        block5: {
            block4: {
                if (obj == null || depth > 5) {
                    return;
                }
                if (!(obj instanceof ConfigurationSection)) break block4;
                ConfigurationSection sec = (ConfigurationSection)obj;
                for (String key : sec.getKeys(false)) {
                    String low = key.toLowerCase(Locale.ROOT);
                    Object val = sec.get(key);
                    if (low.contains("member") || low.contains("player") || low.contains("leader") || low.contains("owner")) {
                        out.add(key + " = " + String.valueOf(val));
                    }
                    this.collectMemberLike(val, out, depth + 1);
                }
                break block5;
            }
            if (!(obj instanceof Iterable)) break block5;
            Iterable it = (Iterable)obj;
            for (Object o : it) {
                out.add(String.valueOf(o));
            }
        }
    }

    private String firstExistingString(GuildRef ref, String ... keys) {
        for (String k : keys) {
            String v = ref.yml.getString(ref.path + "." + k);
            if (v == null || v.isBlank()) continue;
            return v;
        }
        return "-";
    }

    private String keys(GuildRef ref) {
        ConfigurationSection sec = ref.yml.getConfigurationSection(ref.path);
        return sec == null ? "-" : sec.getKeys(false).toString();
    }

    private String homeString(GuildRef ref) {
        if (ref.yml.isConfigurationSection(ref.path + ".home")) {
            String b = ref.path + ".home";
            return ref.yml.getString(b + ".world", "?") + " " + ref.yml.getString(b + ".x", "?") + " " + ref.yml.getString(b + ".y", "?") + " " + ref.yml.getString(b + ".z", "?");
        }
        return "-";
    }

    private String locString(Location l) {
        return l.getWorld().getName() + " " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ();
    }

    private String clean(String s) {
        return s == null ? "" : s.replace("-", "").replace("_", "").replace(" ", "").toLowerCase(Locale.ROOT);
    }

    private String lastPath(String path) {
        int i = path.lastIndexOf(46);
        return i >= 0 ? path.substring(i + 1) : path;
    }

    private String parentPath(String path) {
        int i = path.lastIndexOf(46);
        return i >= 0 ? path.substring(0, i) : "";
    }

    private String time() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private String pref() {
        return this.getConfig().getString("messages.prefix", "&8[&bGuildAdmin&8] ");
    }

    private void msg(CommandSender s, String m) {
        s.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)(m == null ? "" : m)));
    }

    private boolean has(CommandSender s, String perm) {
        return s.hasPermission("rumahkita.guild.admin") || s.hasPermission(perm);
    }

    private void log(CommandSender sender, String action) {
        if (!this.getConfig().getBoolean("logs.enabled", true)) {
            return;
        }
        try {
            File file = new File(this.getDataFolder(), this.getConfig().getString("logs.file", "admin-actions.log"));
            file.getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(file, true);){
                fw.write(this.time() + " | admin=" + sender.getName() + " | " + action + System.lineSeparator());
            }
        }
        catch (Exception e) {
            this.getLogger().warning("Gagal tulis log: " + e.getMessage());
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return this.filter(args[0], List.of("help", "scan", "list", "info", "members", "freeze", "unfreeze", "disband", "delhome", "sethome", "kick", "removeplayer", "setleader", "rename", "setprefix", "backup", "reload"));
        }
        if (args.length == 2 && List.of("info", "members", "freeze", "unfreeze", "disband", "delhome", "sethome", "kick", "setleader", "rename", "setprefix").contains(args[0].toLowerCase(Locale.ROOT))) {
            ArrayList<String> names = new ArrayList<String>();
            for (GuildRef ref : this.allGuildRefs()) {
                names.add(ref.name);
            }
            return this.filter(args[1], names);
        }
        if (args.length == 3 && List.of("kick", "removeplayer", "setleader").contains(args[0].toLowerCase(Locale.ROOT))) {
            return this.filter(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        return List.of();
    }

    private List<String> filter(String prefix, List<String> all) {
        String p = prefix.toLowerCase(Locale.ROOT);
        ArrayList<String> out = new ArrayList<String>();
        for (String s : all) {
            if (!s.toLowerCase(Locale.ROOT).startsWith(p)) continue;
            out.add(s);
        }
        return out;
    }

    private static final class GuildRef {
        final File file;
        final YamlConfiguration yml;
        final String path;
        final String name;

        GuildRef(File file, YamlConfiguration yml, String path, String name) {
            this.file = file;
            this.yml = yml;
            this.path = path;
            this.name = name;
        }
    }
}

