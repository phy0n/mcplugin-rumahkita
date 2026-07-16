/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.scheduler.BukkitTask
 */
package id.rumahkita.ramguard;

import id.rumahkita.ramguard.CleanupReport;
import id.rumahkita.ramguard.MemoryInfo;
import id.rumahkita.ramguard.RamGuardService;
import id.rumahkita.ramguard.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class RumahKitaRamGuardPlugin
extends JavaPlugin
implements TabExecutor {
    private RamGuardService ramGuardService;
    private BukkitTask monitorTask;

    public void onEnable() {
        this.saveDefaultConfig();
        this.ramGuardService = new RamGuardService(this);
        if (this.getCommand("ramguard") != null) {
            this.getCommand("ramguard").setExecutor((CommandExecutor)this);
            this.getCommand("ramguard").setTabCompleter((TabCompleter)this);
        }
        this.startMonitorTask();
        this.log("RumahKitaRamGuard aktif. Gunakan /ramguard status untuk cek RAM.");
    }

    public void onDisable() {
        if (this.monitorTask != null) {
            this.monitorTask.cancel();
            this.monitorTask = null;
        }
        this.log("RumahKitaRamGuard dimatikan.");
    }

    private void startMonitorTask() {
        if (this.monitorTask != null) {
            this.monitorTask.cancel();
        }
        long intervalSeconds = Math.max(5L, this.getConfig().getLong("settings.check-interval-seconds", 30L));
        long intervalTicks = intervalSeconds * 20L;
        this.monitorTask = Bukkit.getScheduler().runTaskTimer((Plugin)this, () -> this.ramGuardService.checkMemory(), 100L, intervalTicks);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rumahkitaramguard.admin")) {
            sender.sendMessage(Text.color("&cKamu tidak punya permission untuk command ini."));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            this.ramGuardService.sendStatus(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("cleanup")) {
            MemoryInfo info = MemoryInfo.current();
            CleanupReport report = this.ramGuardService.runCleanup(RamGuardService.CleanupLevel.CLEANUP, info);
            sender.sendMessage(Text.color("&aManual cleanup selesai: &f" + report.summary()));
            this.log("Manual cleanup oleh " + sender.getName() + ": " + report.summary());
            return true;
        }
        if (args[0].equalsIgnoreCase("emergency")) {
            MemoryInfo info = MemoryInfo.current();
            CleanupReport report = this.ramGuardService.runCleanup(RamGuardService.CleanupLevel.EMERGENCY, info);
            sender.sendMessage(Text.color("&cManual emergency cleanup selesai: &f" + report.summary()));
            this.log("Manual emergency cleanup oleh " + sender.getName() + ": " + report.summary());
            return true;
        }
        if (args[0].equalsIgnoreCase("restore")) {
            this.ramGuardService.restoreViewDistance();
            sender.sendMessage(Text.color("&aView distance dan simulation distance dikembalikan ke config normal."));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            this.reloadConfig();
            this.startMonitorTask();
            sender.sendMessage(Text.color("&aConfig RumahKitaRamGuard berhasil direload."));
            return true;
        }
        sender.sendMessage(Text.color("&eGunakan: &f/ramguard status&7, &f/ramguard cleanup&7, &f/ramguard emergency&7, &f/ramguard restore&7, &f/ramguard reload"));
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("rumahkitaramguard.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> options = Arrays.asList("status", "cleanup", "emergency", "restore", "reload");
            ArrayList<String> result = new ArrayList<String>();
            String prefix = args[0].toLowerCase();
            for (String option : options) {
                if (!option.startsWith(prefix)) continue;
                result.add(option);
            }
            return result;
        }
        return List.of();
    }

    public void sendAlert(String message) {
        if (this.getConfig().getBoolean("settings.notify-admins", true)) {
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (player.isOp() || player.hasPermission("rumahkitaramguard.notify")) {
                    player.sendMessage(Text.color(message));
                }
            });
        }
        if (this.getConfig().getBoolean("settings.log-to-console", true)) {
            Bukkit.getConsoleSender().sendMessage(Text.color(message));
        }
    }

    public String message(String key, MemoryInfo info) {
        String prefix = this.getConfig().getString("messages.prefix", "&8[&aRamGuard&8] ");
        String message = this.getConfig().getString("messages." + key, "");
        return (prefix + message).replace("%percent%", info.getPercentFormatted()).replace("%used%", info.getUsedFormatted()).replace("%max%", info.getMaxFormatted());
    }

    public void log(String message) {
        if (this.getConfig().getBoolean("settings.log-to-console", true)) {
            this.getLogger().info(message);
        }
    }
}

