/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.Guild;
import id.rumahkita.guilds.GuildManager;
import id.rumahkita.guilds.RumahKitaGuildsPlugin;
import id.rumahkita.guilds.Text;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class GuildPlaceholderExpansion
extends PlaceholderExpansion {
    private final RumahKitaGuildsPlugin plugin;
    private final GuildManager guildManager;

    public GuildPlaceholderExpansion(RumahKitaGuildsPlugin plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
    }

    @NotNull
    public String getIdentifier() {
        return "rumahkitaguilds";
    }

    @NotNull
    public String getAuthor() {
        return "HansM";
    }

    @NotNull
    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    public boolean persist() {
        return true;
    }

    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            return this.plugin.getConfig().getString("placeholder.no-guild", "");
        }
        return switch (params.toLowerCase()) {
            case "tag" -> Text.color(this.plugin.getConfig().getString("placeholder.tag-format", "&8[&b%tag%&8]").replace("%tag%", guild.getTag()));
            case "tag_raw" -> guild.getTag();
            case "name" -> guild.getName();
            case "role" -> guild.getRole(player.getUniqueId()).displayName(this.plugin);
            default -> "";
        };
    }
}

