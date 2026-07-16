/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 */
package id.rumahkita.anticheat;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Text {
    private Text() {
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)(text == null ? "" : text));
    }

    public static void msg(CommandSender sender, String text) {
        sender.sendMessage(Text.color(text));
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

