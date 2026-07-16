/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.Guild;
import id.rumahkita.guilds.GuildManager;
import id.rumahkita.guilds.GuildRole;
import id.rumahkita.guilds.RumahKitaGuildsPlugin;
import id.rumahkita.guilds.Text;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GuildChatManager {
    private final RumahKitaGuildsPlugin plugin;
    private final GuildManager guildManager;
    private final Set<UUID> toggled = new HashSet<UUID>();

    public GuildChatManager(RumahKitaGuildsPlugin plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
    }

    public boolean isToggled(Player player) {
        return this.toggled.contains(player.getUniqueId());
    }

    public void toggle(Player player) {
        if (this.toggled.contains(player.getUniqueId())) {
            this.toggled.remove(player.getUniqueId());
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "guild-chat-toggle-off"));
        } else {
            this.toggled.add(player.getUniqueId());
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "guild-chat-toggle-on"));
        }
    }

    public void removeToggle(UUID uuid) {
        this.toggled.remove(uuid);
    }

    public boolean sendGuildMessage(Player sender, String message) {
        if (!this.plugin.getConfig().getBoolean("guild-chat.enabled", true)) {
            Text.msg((CommandSender)sender, Text.prefixed(this.plugin, "guild-chat-disabled"));
            return false;
        }
        if (message == null || message.isBlank()) {
            Text.msg((CommandSender)sender, Text.prefixed(this.plugin, "guild-chat-empty"));
            return false;
        }
        Guild guild = this.guildManager.getGuild(sender);
        if (guild == null) {
            Text.msg((CommandSender)sender, Text.prefixed(this.plugin, "not-in-guild"));
            return false;
        }
        GuildRole role = guild.getRole(sender.getUniqueId());
        String formatted = this.plugin.getConfig().getString("guild-chat.format", "&8[&bGuild &8| &f%guild_tag%&8] &e%role% &f%player% &8\u00bb &f%message%");
        formatted = Text.replace(formatted, "%guild_tag%", guild.getTag(), "%guild_name%", guild.getName(), "%role%", role.displayName(this.plugin), "%player%", sender.getName(), "%message%", message);
        for (UUID uuid : guild.getMembers().keySet()) {
            Player target = Bukkit.getPlayer((UUID)uuid);
            if (target == null || !target.isOnline()) continue;
            Text.msg((CommandSender)target, formatted);
        }
        if (this.plugin.getConfig().getBoolean("guild-chat.admin-spy", false)) {
            String spy = this.plugin.getConfig().getString("guild-chat.spy-format", "&8[&cGuildSpy &8| &f%guild_tag%&8] &7%player% &8\u00bb &f%message%");
            spy = Text.replace(spy, "%guild_tag%", guild.getTag(), "%guild_name%", guild.getName(), "%role%", role.displayName(this.plugin), "%player%", sender.getName(), "%message%", message);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (guild.isMember(online.getUniqueId()) || !online.hasPermission("rumahkitaguilds.spy")) continue;
                Text.msg((CommandSender)online, spy);
            }
        }
        return true;
    }
}

