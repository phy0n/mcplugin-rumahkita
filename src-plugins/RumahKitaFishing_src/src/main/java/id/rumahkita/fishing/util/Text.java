/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.FileConfiguration
 */
package id.rumahkita.fishing.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public final class Text {
    private Text() {
    }

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes((char)'&', (String)text);
    }

    public static List<String> colorList(List<String> lines) {
        ArrayList<String> colored = new ArrayList<String>();
        if (lines == null) {
            return colored;
        }
        for (String line : lines) {
            colored.add(Text.color(line));
        }
        return colored;
    }

    public static String stripColor(String text) {
        return ChatColor.stripColor((String)Text.color(text));
    }

    public static String format(String text, Map<String, String> placeholders) {
        String result;
        String string = result = text == null ? "" : text;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : (CharSequence)entry.getValue());
            }
        }
        return Text.color(result);
    }

    public static void send(CommandSender sender, FileConfiguration messages, String path, Map<String, String> placeholders) {
        String prefix = messages.getString("prefix", "&bFishing &8\u00bb &r");
        if (messages.isList(path)) {
            List lines = messages.getStringList(path);
            for (String line : lines) {
                sender.sendMessage(Text.format(line, Text.mergePrefix(prefix, placeholders)));
            }
            return;
        }
        String raw = messages.getString(path, path);
        sender.sendMessage(Text.format(raw, Text.mergePrefix(prefix, placeholders)));
    }

    public static Map<String, String> mergePrefix(String prefix, Map<String, String> placeholders) {
        HashMap<String, String> merged = new HashMap<String, String>();
        if (placeholders != null) {
            merged.putAll(placeholders);
        }
        merged.put("prefix", prefix);
        return merged;
    }
}

