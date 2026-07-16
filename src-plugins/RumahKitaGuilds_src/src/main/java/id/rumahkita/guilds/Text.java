/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.RumahKitaGuildsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Text {
    private Text() {
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)(text == null ? "" : text));
    }

    public static void msg(CommandSender sender, String message) {
        sender.sendMessage(Text.color(message));
    }

    public static String prefixed(RumahKitaGuildsPlugin plugin, String path) {
        return plugin.getConfig().getString("settings.prefix", "&8[&bRumahKitaGuilds&8] ") + plugin.getConfig().getString("messages." + path, path);
    }

    public static String replace(String text, String ... replacements) {
        String out = text == null ? "" : text;
        int i = 0;
        while (i + 1 < replacements.length) {
            out = out.replace(replacements[i], replacements[i + 1]);
            i += 2;
        }
        return out;
    }
}

