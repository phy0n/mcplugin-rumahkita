/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  id.rumahkita.economy.RkePlaceholderExpansion
 *  id.rumahkita.economy.RumahKitaEconomyRupiahPlugin
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.OfflinePlayer
 */
package id.rumahkita.economy;

import id.rumahkita.economy.RumahKitaEconomyRupiahPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public final class RkePlaceholderExpansion
extends PlaceholderExpansion {
    private final RumahKitaEconomyRupiahPlugin plugin;

    public RkePlaceholderExpansion(RumahKitaEconomyRupiahPlugin plugin) {
        this.plugin = plugin;
    }

    public String getIdentifier() {
        return "rke";
    }

    public String getAuthor() {
        return "RumahKita";
    }

    public String getVersion() {
        return "2.1.0";
    }

    public boolean persist() {
        return true;
    }

    public boolean canRegister() {
        return true;
    }

    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "-";
        }
        if (params.equalsIgnoreCase("balance")) {
            return this.plugin.getBalanceFormatted(player);
        }
        if (params.equalsIgnoreCase("balance_raw")) {
            return this.plugin.getBalanceRaw(player);
        }
        if (params.equalsIgnoreCase("balance_short")) {
            return this.plugin.getBalanceShort(player);
        }
        if (params.equalsIgnoreCase("currency")) {
            return "Rp";
        }
        if (params.equalsIgnoreCase("voucher")) {
            return this.plugin.getVoucherPlaceholder(player);
        }
        if (params.equalsIgnoreCase("voucher_percent")) {
            String v = this.plugin.getVoucherPlaceholder(player);
            return v.equals("-") ? "0" : v.replace("%", "");
        }
        return null;
    }
}

