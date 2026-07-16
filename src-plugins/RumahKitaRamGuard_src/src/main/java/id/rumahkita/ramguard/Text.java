/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 */
package id.rumahkita.ramguard;

import org.bukkit.ChatColor;

public final class Text {
    private Text() {
    }

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes((char)'&', (String)text);
    }
}

