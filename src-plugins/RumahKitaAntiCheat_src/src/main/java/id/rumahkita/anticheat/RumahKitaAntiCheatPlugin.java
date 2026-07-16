/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package id.rumahkita.anticheat;

import id.rumahkita.anticheat.AntiCheatCommand;
import id.rumahkita.anticheat.AntiCheatListener;
import id.rumahkita.anticheat.ExemptManager;
import id.rumahkita.anticheat.LogManager;
import id.rumahkita.anticheat.Text;
import id.rumahkita.anticheat.ViolationTracker;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class RumahKitaAntiCheatPlugin
extends JavaPlugin {
    private ExemptManager exemptManager;
    private ViolationTracker violationTracker;
    private LogManager logManager;

    public void onEnable() {
        this.saveDefaultConfig();
        this.exemptManager = new ExemptManager();
        this.violationTracker = new ViolationTracker();
        this.logManager = new LogManager(this);
        AntiCheatListener listener = new AntiCheatListener(this, this.exemptManager, this.violationTracker);
        Bukkit.getPluginManager().registerEvents((Listener)listener, (Plugin)this);
        listener.startAuditTask();
        AntiCheatCommand command = new AntiCheatCommand(this, this.exemptManager, this.violationTracker);
        this.getCommand("rkac").setExecutor((CommandExecutor)command);
        this.getCommand("rkac").setTabCompleter((TabCompleter)command);
        Bukkit.getScheduler().runTaskTimer((Plugin)this, () -> this.exemptManager.cleanup(), 600L, 600L);
        this.getLogger().info("RumahKitaAntiCheat v1.2.0 enabled.");
    }

    public void onDisable() {
        this.getLogger().info("RumahKitaAntiCheat disabled.");
    }

    public boolean isEnabledInConfig() {
        return this.getConfig().getBoolean("settings.enabled", true);
    }

    public void handleViolation(Player player, String type, String detail, int vl) {
        String alert = this.getConfig().getString("settings.prefix", "&8[&cRumahKitaAC&8] ") + Text.replace(this.getConfig().getString("messages.alert"), "%player%", player.getName(), "%type%", type, "%detail%", detail, "%vl%", String.valueOf(vl));
        if (this.getConfig().getBoolean("actions.staff-alerts", true)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission(this.getConfig().getString("settings.notify-permission", "rumahkita.anticheat.notify"))) continue;
                Text.msg((CommandSender)online, alert);
            }
        }
        this.logManager.log(player.getName() + " detected=" + type + " VL=" + vl + " detail=" + detail + " ping=" + player.getPing() + " gm=" + String.valueOf(player.getGameMode()) + " loc=" + this.loc(player));
    }

    public void handleKick(Player player, String type) {
        String msg = this.getConfig().getString("settings.prefix", "&8[&cRumahKitaAC&8] ") + Text.replace(this.getConfig().getString("messages.kicked"), "%player%", player.getName(), "%type%", type);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission(this.getConfig().getString("settings.notify-permission", "rumahkita.anticheat.notify"))) continue;
            Text.msg((CommandSender)online, msg);
        }
        this.logManager.log("KICK " + player.getName() + " reason=" + type + " loc=" + this.loc(player));
    }

    public void staffAlert(Player player, String type, String detail) {
        String alert = this.getConfig().getString("settings.prefix", "&8[&cRumahKitaAC&8] ") + Text.replace(this.getConfig().getString("messages.alert"), "%player%", player.getName(), "%type%", type, "%detail%", detail, "%vl%", "-");
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission(this.getConfig().getString("settings.notify-permission", "rumahkita.anticheat.notify"))) continue;
            Text.msg((CommandSender)online, alert);
        }
        this.logManager.log(player.getName() + " alert=" + type + " detail=" + detail + " loc=" + this.loc(player));
    }

    private String loc(Player p) {
        return p.getWorld().getName() + " " + String.format(Locale.US, "%.2f %.2f %.2f", p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ());
    }
}

