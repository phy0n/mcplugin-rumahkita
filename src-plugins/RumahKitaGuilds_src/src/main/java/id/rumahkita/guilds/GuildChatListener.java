/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.Guild;
import id.rumahkita.guilds.GuildChatManager;
import id.rumahkita.guilds.GuildManager;
import id.rumahkita.guilds.RumahKitaGuildsPlugin;
import id.rumahkita.guilds.Text;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class GuildChatListener
implements Listener {
    private final RumahKitaGuildsPlugin plugin;
    private final GuildManager guildManager;
    private final GuildChatManager chatManager;

    public GuildChatListener(RumahKitaGuildsPlugin plugin, GuildManager guildManager, GuildChatManager chatManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.chatManager = chatManager;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (this.chatManager.isToggled(event.getPlayer())) {
            event.setCancelled(true);
            String message = event.getMessage();
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.chatManager.sendGuildMessage(event.getPlayer(), message));
            return;
        }
        if (!this.plugin.getConfig().getBoolean("chat-format.enabled", false)) {
            return;
        }
        if (!event.getPlayer().hasPermission("rumahkitaguilds.chatformat")) {
            return;
        }
        Guild guild = this.guildManager.getGuild(event.getPlayer());
        if (guild == null) {
            return;
        }
        String tag = Text.color(this.plugin.getConfig().getString("placeholder.tag-format", "&8[&b%tag%&8]").replace("%tag%", guild.getTag()));
        String format = this.plugin.getConfig().getString("chat-format.format", "%guild_tag% &7%player% &8\u00bb &f%message%");
        format = format.replace("%guild_tag%", tag).replace("%guild_name%", guild.getName()).replace("%guild_role%", guild.getRole(event.getPlayer().getUniqueId()).displayName(this.plugin)).replace("%player%", "%1$s").replace("%message%", "%2$s");
        event.setFormat(Text.color(format));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.chatManager.removeToggle(event.getPlayer().getUniqueId());
    }
}

