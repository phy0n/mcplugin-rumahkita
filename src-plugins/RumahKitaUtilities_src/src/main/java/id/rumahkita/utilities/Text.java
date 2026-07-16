/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 */
package id.rumahkita.utilities;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Text {
    private Text() {
    }

    public static String color(String input) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)(input == null ? "" : input));
    }

    public static void msg(CommandSender sender, String input) {
        sender.sendMessage(Text.color(input));
    }
}

