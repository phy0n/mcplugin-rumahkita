/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.entity.Player
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.Guild;
import id.rumahkita.guilds.GuildChatManager;
import id.rumahkita.guilds.GuildGui;
import id.rumahkita.guilds.GuildHomeManager;
import id.rumahkita.guilds.GuildManager;
import id.rumahkita.guilds.GuildRole;
import id.rumahkita.guilds.GuildWarManager;
import id.rumahkita.guilds.RumahKitaGuildsPlugin;
import id.rumahkita.guilds.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class GuildCommand
implements TabExecutor {
    private final RumahKitaGuildsPlugin plugin;
    private final GuildManager guildManager;
    private final GuildHomeManager homeManager;
    private final GuildChatManager chatManager;
    private final GuildGui gui;
    private final GuildWarManager warManager;
    private final Map<UUID, Invite> invites = new HashMap<UUID, Invite>();

    public GuildCommand(RumahKitaGuildsPlugin plugin, GuildManager guildManager, GuildHomeManager homeManager, GuildChatManager chatManager, GuildGui gui, GuildWarManager warManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.homeManager = homeManager;
        this.chatManager = chatManager;
        this.gui = gui;
        this.warManager = warManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub;
        if (command.getName().equalsIgnoreCase("guildchat")) {
            Player player = this.requirePlayer(sender);
            if (player == null || !this.hasUse(sender)) {
                return true;
            }
            if (args.length == 0) {
                this.chatManager.toggle(player);
            } else {
                this.chatManager.sendGuildMessage(player, this.join(args, 0));
            }
            return true;
        }
        if (args.length == 0) {
            Player player = this.requirePlayer(sender);
            if (player != null) {
                this.gui.open(player);
            }
            return true;
        }
        switch (sub = args[0].toLowerCase(Locale.ROOT)) {
            case "create": {
                this.create(sender, args);
                break;
            }
            case "invite": {
                this.invite(sender, args);
                break;
            }
            case "accept": {
                this.accept(sender, args);
                break;
            }
            case "members": {
                this.members(sender);
                break;
            }
            case "list": {
                this.list(sender);
                break;
            }
            case "leave": {
                this.leave(sender);
                break;
            }
            case "kick": {
                this.kick(sender, args);
                break;
            }
            case "promote": {
                this.promote(sender, args);
                break;
            }
            case "demote": {
                this.demote(sender, args);
                break;
            }
            case "role": {
                this.role(sender, args);
                break;
            }
            case "transfer": {
                this.transfer(sender, args);
                break;
            }
            case "rename": {
                this.rename(sender, args);
                break;
            }
            case "settag": {
                this.settag(sender, args);
                break;
            }
            case "sethome": {
                this.sethome(sender);
                break;
            }
            case "home": {
                this.home(sender);
                break;
            }
            case "delhome": {
                this.delhome(sender);
                break;
            }
            case "chat": 
            case "guildchat": 
            case "gc": {
                this.guildChat(sender, args);
                break;
            }
            case "war": {
                this.warManager.handleCommand(sender, args);
                break;
            }
            case "wallet": {
                this.warManager.handleWallet(sender, args);
                break;
            }
            case "disband": {
                this.disband(sender);
                break;
            }
            case "adminreload": 
            case "reload": {
                this.adminReload(sender);
                break;
            }
            case "save": {
                this.adminSave(sender);
                break;
            }
            case "info": {
                this.adminInfo(sender, args);
                break;
            }
            case "forcedisband": {
                this.adminForceDisband(sender, args);
                break;
            }
            default: {
                this.help(sender);
            }
        }
        return true;
    }

    private Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            Text.msg(sender, Text.prefixed(this.plugin, "only-player"));
            return null;
        }
        Player player = (Player)sender;
        return player;
    }

    private boolean hasUse(CommandSender sender) {
        if (!sender.hasPermission("rumahkitaguilds.use")) {
            Text.msg(sender, Text.prefixed(this.plugin, "no-permission"));
            return false;
        }
        return true;
    }

    private void guildChat(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        if (args.length <= 1) {
            this.chatManager.toggle(player);
        } else {
            this.chatManager.sendGuildMessage(player, this.join(args, 1));
        }
    }

    private void create(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        if (!player.hasPermission("rumahkitaguilds.create")) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        if (args.length < 3) {
            Text.msg((CommandSender)player, "&eGunakan: /guild create <TAG> <Nama Guild>");
            return;
        }
        String tag = args[1];
        String name = this.join(args, 2);
        GuildManager.CreateResult result = this.guildManager.createGuild(player, tag, name);
        switch (result) {
            case SUCCESS: {
                Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "guild-created"), "%name%", name, "%tag%", tag.toUpperCase()));
                break;
            }
            case ALREADY_IN_GUILD: {
                Text.msg((CommandSender)player, Text.prefixed(this.plugin, "already-in-guild"));
                break;
            }
            case INVALID_TAG: {
                Text.msg((CommandSender)player, Text.prefixed(this.plugin, "invalid-tag"));
                break;
            }
            case INVALID_NAME: {
                Text.msg((CommandSender)player, Text.prefixed(this.plugin, "invalid-name"));
                break;
            }
            case DUPLICATE_TAG: {
                Text.msg((CommandSender)player, Text.prefixed(this.plugin, "duplicate-tag"));
                break;
            }
            case DUPLICATE_NAME: {
                Text.msg((CommandSender)player, Text.prefixed(this.plugin, "duplicate-name"));
                break;
            }
            case NOT_ENOUGH_COST: {
                Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "create-cost-missing"), "%amount%", String.valueOf(this.plugin.getConfig().getInt("settings.create-cost.amount", 5)), "%material%", this.plugin.getConfig().getString("settings.create-cost.material", "DIAMOND")));
            }
        }
    }

    private void invite(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        if (args.length < 2) {
            Text.msg((CommandSender)player, "&eGunakan: /guild invite <player>");
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        if (!guild.getRole(player.getUniqueId()).atLeast(GuildRole.ADMIN)) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "player-not-found"));
            return;
        }
        if (this.guildManager.hasGuild(target.getUniqueId())) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "target-already-guild"));
            return;
        }
        if (guild.size() >= this.guildManager.getMaxMembers()) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "max-members"));
            return;
        }
        long expireAt = System.currentTimeMillis() + (long)this.plugin.getConfig().getInt("settings.invite-expire-seconds", 120) * 1000L;
        this.invites.put(target.getUniqueId(), new Invite(guild.getTag(), expireAt, player.getUniqueId()));
        Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "invite-sent"), "%player%", target.getName()));
        Text.msg((CommandSender)target, Text.replace(Text.prefixed(this.plugin, "invite-received"), "%player%", player.getName(), "%guild%", guild.getName(), "%tag%", guild.getTag()));
    }

    private void accept(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        if (args.length < 2) {
            Text.msg((CommandSender)player, "&eGunakan: /guild accept <TAG>");
            return;
        }
        if (this.guildManager.hasGuild(player.getUniqueId())) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "already-in-guild"));
            return;
        }
        Invite invite = this.invites.get(player.getUniqueId());
        if (invite == null || !invite.guildTag.equalsIgnoreCase(args[1]) || invite.expireAt < System.currentTimeMillis()) {
            this.invites.remove(player.getUniqueId());
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "invite-expired"));
            return;
        }
        Guild guild = this.guildManager.getGuildByTag(invite.guildTag);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "guild-not-found"));
            return;
        }
        if (guild.size() >= this.guildManager.getMaxMembers()) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "max-members"));
            return;
        }
        this.guildManager.addMember(guild, player);
        this.invites.remove(player.getUniqueId());
        Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "joined-guild"), "%guild%", guild.getName()));
    }

    private void members(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        this.gui.openMembers(player, guild);
    }

    private void list(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player)sender;
            this.gui.openGuildList(player);
        } else {
            for (Guild guild : this.guildManager.getGuilds()) {
                Text.msg(sender, "&b" + guild.getTag() + " &7- &f" + guild.getName() + " &8(" + guild.size() + " member)");
            }
        }
    }

    private void leave(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        if (guild.getRole(player.getUniqueId()) == GuildRole.LEADER) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "leave-leader"));
            return;
        }
        this.guildManager.removeMember(guild, player.getUniqueId());
        this.chatManager.removeToggle(player.getUniqueId());
        Text.msg((CommandSender)player, Text.prefixed(this.plugin, "left-guild"));
    }

    private void kick(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        if (args.length < 2) {
            Text.msg((CommandSender)player, "&eGunakan: /guild kick <player>");
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        GuildRole actorRole = guild.getRole(player.getUniqueId());
        if (!actorRole.atLeast(GuildRole.ADMIN)) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        UUID targetUuid = this.guildManager.findMember(guild, args[1]);
        if (targetUuid == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "player-not-found"));
            return;
        }
        GuildRole targetRole = guild.getRole(targetUuid);
        if (targetRole == GuildRole.LEADER) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "cannot-kick-leader"));
            return;
        }
        if (actorRole == GuildRole.ADMIN && targetRole != GuildRole.MEMBER) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        this.guildManager.removeMember(guild, targetUuid);
        this.chatManager.removeToggle(targetUuid);
        Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "kicked"), "%player%", this.guildManager.getOfflineName(targetUuid)));
    }

    private void promote(CommandSender sender, String[] args) {
        this.setRoleCommand(sender, args, true);
    }

    private void demote(CommandSender sender, String[] args) {
        this.setRoleCommand(sender, args, false);
    }

    private void setRoleCommand(CommandSender sender, String[] args, boolean promote) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        if (guild.getRole(player.getUniqueId()) != GuildRole.LEADER) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        if (args.length < 2) {
            Text.msg((CommandSender)player, promote ? "&eGunakan: /guild promote <player>" : "&eGunakan: /guild demote <player>");
            return;
        }
        UUID targetUuid = this.guildManager.findMember(guild, args[1]);
        if (targetUuid == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "player-not-found"));
            return;
        }
        if (promote) {
            if (guild.getRole(targetUuid) != GuildRole.MEMBER) {
                Text.msg((CommandSender)player, "&cPlayer itu bukan Member.");
                return;
            }
            guild.setRole(targetUuid, GuildRole.ADMIN);
            this.guildManager.save();
            Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "promoted"), "%player%", this.guildManager.getOfflineName(targetUuid)));
        } else {
            if (guild.getRole(targetUuid) != GuildRole.ADMIN) {
                Text.msg((CommandSender)player, "&cPlayer itu bukan Admin.");
                return;
            }
            guild.setRole(targetUuid, GuildRole.MEMBER);
            this.guildManager.save();
            Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "demoted"), "%player%", this.guildManager.getOfflineName(targetUuid)));
        }
    }

    private void role(CommandSender sender, String[] args) {
        UUID target;
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        UUID uUID = target = args.length >= 2 ? this.guildManager.findMember(guild, args[1]) : player.getUniqueId();
        if (target == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "player-not-found"));
            return;
        }
        Text.msg((CommandSender)player, "&eRole &f" + this.guildManager.getOfflineName(target) + "&e: &f" + guild.getRole(target).displayName(this.plugin));
    }

    private void transfer(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        if (guild.getRole(player.getUniqueId()) != GuildRole.LEADER) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        if (args.length < 2) {
            Text.msg((CommandSender)player, "&eGunakan: /guild transfer <player>");
            return;
        }
        UUID targetUuid = this.guildManager.findMember(guild, args[1]);
        if (targetUuid == null || targetUuid.equals(player.getUniqueId())) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "player-not-found"));
            return;
        }
        guild.setRole(targetUuid, GuildRole.LEADER);
        guild.setRole(player.getUniqueId(), GuildRole.ADMIN);
        this.guildManager.save();
        Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "transfer-done"), "%player%", this.guildManager.getOfflineName(targetUuid)));
    }

    private void rename(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.leaderGuild(player);
        if (guild == null) {
            return;
        }
        if (args.length < 2) {
            Text.msg((CommandSender)player, "&eGunakan: /guild rename <nama baru>");
            return;
        }
        String newName = this.join(args, 1);
        if (this.guildManager.renameGuild(guild, newName)) {
            Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "rename-done"), "%name%", newName));
        } else {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "invalid-name"));
        }
    }

    private void settag(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.leaderGuild(player);
        if (guild == null) {
            return;
        }
        if (args.length < 2) {
            Text.msg((CommandSender)player, "&eGunakan: /guild settag <tag baru>");
            return;
        }
        if (this.guildManager.changeTag(guild, args[1])) {
            Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "tag-done"), "%tag%", args[1].toUpperCase()));
        } else {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "invalid-tag"));
        }
    }

    private Guild leaderGuild(Player player) {
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return null;
        }
        if (guild.getRole(player.getUniqueId()) != GuildRole.LEADER) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return null;
        }
        return guild;
    }

    private void sethome(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        if (!guild.getRole(player.getUniqueId()).atLeast(GuildRole.ADMIN)) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        guild.setHome(player.getLocation());
        this.guildManager.save();
        Text.msg((CommandSender)player, Text.prefixed(this.plugin, "home-set"));
    }

    private void home(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        this.homeManager.teleport(player, guild);
    }

    private void delhome(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        if (!guild.getRole(player.getUniqueId()).atLeast(GuildRole.ADMIN)) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        guild.setHome(null);
        this.guildManager.save();
        Text.msg((CommandSender)player, Text.prefixed(this.plugin, "home-deleted"));
    }

    private void disband(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.leaderGuild(player);
        if (guild == null) {
            return;
        }
        String name = guild.getName();
        this.guildManager.disband(guild);
        Bukkit.broadcastMessage((String)Text.color(Text.replace(Text.prefixed(this.plugin, "disbanded"), "%guild%", name)));
    }

    private void adminReload(CommandSender sender) {
        if (!sender.hasPermission("rumahkitaguilds.admin")) {
            Text.msg(sender, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        this.plugin.reloadAll();
        Text.msg(sender, Text.prefixed(this.plugin, "reloaded"));
    }

    private void adminSave(CommandSender sender) {
        if (!sender.hasPermission("rumahkitaguilds.admin")) {
            Text.msg(sender, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        this.guildManager.save();
        Text.msg(sender, Text.prefixed(this.plugin, "admin-saved"));
    }

    private void adminInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rumahkitaguilds.admin")) {
            Text.msg(sender, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        if (args.length < 2) {
            Text.msg(sender, "&eGunakan: /guild info <tag>");
            return;
        }
        Guild guild = this.guildManager.getGuildByTag(args[1]);
        if (guild == null) {
            Text.msg(sender, Text.prefixed(this.plugin, "guild-not-found"));
            return;
        }
        Text.msg(sender, "&bGuild: &f" + guild.getName() + " &8[&b" + guild.getTag() + "&8]");
        Text.msg(sender, "&7Leader: &f" + this.guildManager.getOfflineName(guild.getLeader()));
        Text.msg(sender, "&7Members: &f" + guild.size());
        Text.msg(sender, "&7Home: &f" + (guild.getHome() == null ? "Belum diset" : "Sudah diset"));
    }

    private void adminForceDisband(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rumahkitaguilds.admin")) {
            Text.msg(sender, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        if (args.length < 2) {
            Text.msg(sender, "&eGunakan: /guild forcedisband <tag>");
            return;
        }
        Guild guild = this.guildManager.getGuildByTag(args[1]);
        if (guild == null) {
            Text.msg(sender, Text.prefixed(this.plugin, "guild-not-found"));
            return;
        }
        String name = guild.getName();
        this.guildManager.disband(guild);
        Text.msg(sender, "&aGuild " + name + " berhasil dibubarkan paksa.");
    }

    private void help(CommandSender sender) {
        Text.msg(sender, "&8&m----------------------------");
        Text.msg(sender, "&bRumahKitaGuilds Commands");
        Text.msg(sender, "&e/guild &7- buka GUI");
        Text.msg(sender, "&e/guild create <TAG> <Nama>");
        Text.msg(sender, "&e/guild list");
        Text.msg(sender, "&e/guild chat <pesan>");
        Text.msg(sender, "&e/guildchat &7- toggle guild chat");
        Text.msg(sender, "&e/guild war <tag> &7- ajukan Guild War");
        Text.msg(sender, "&e/guild wallet &7- wallet guild");
        Text.msg(sender, "&e/guild home / sethome / delhome");
        Text.msg(sender, "&8&m----------------------------");
    }

    private String join(String[] args, int start) {
        return Arrays.stream(args).skip(start).collect(Collectors.joining(" ")).trim();
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("guildchat")) {
            return List.of();
        }
        if (args.length == 1) {
            ArrayList<String> subs = new ArrayList<String>(List.of("create", "invite", "accept", "members", "list", "leave", "kick", "promote", "demote", "role", "transfer", "rename", "settag", "sethome", "home", "delhome", "chat", "war", "wallet", "disband"));
            if (sender.hasPermission("rumahkitaguilds.admin")) {
                subs.addAll(List.of("adminreload", "save", "info", "forcedisband"));
            }
            return this.filter(subs, args[0]);
        }
        if (args.length == 2 && List.of("invite", "kick", "promote", "demote", "role", "transfer").contains(args[0].toLowerCase())) {
            return this.filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("war")) {
            return this.filter(this.guildManager.getGuilds().stream().map(Guild::getTag).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 2 && List.of("accept", "info", "forcedisband").contains(args[0].toLowerCase())) {
            return this.filter(this.guildManager.getGuilds().stream().map(Guild::getTag).collect(Collectors.toList()), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return list.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lower)).sorted().collect(Collectors.toList());
    }

    private record Invite(String guildTag, long expireAt, UUID inviter) {
    }
}

