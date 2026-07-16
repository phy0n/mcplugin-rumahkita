/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.entity.Player
 */
package id.rumahkita.anticheat;

import id.rumahkita.anticheat.ExemptManager;
import id.rumahkita.anticheat.RumahKitaAntiCheatPlugin;
import id.rumahkita.anticheat.Text;
import id.rumahkita.anticheat.ViolationTracker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class AntiCheatCommand
implements TabExecutor {
    private final RumahKitaAntiCheatPlugin plugin;
    private final ExemptManager exemptManager;
    private final ViolationTracker violationTracker;

    public AntiCheatCommand(RumahKitaAntiCheatPlugin plugin, ExemptManager exemptManager, ViolationTracker violationTracker) {
        this.plugin = plugin;
        this.exemptManager = exemptManager;
        this.violationTracker = violationTracker;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String adminPerm = this.plugin.getConfig().getString("settings.admin-permission", "rumahkita.anticheat.admin");
        if (!sender.hasPermission(adminPerm)) {
            Text.msg(sender, this.pref() + this.plugin.getConfig().getString("messages.no-permission", "&cNo permission."));
            return true;
        }
        if (args.length == 0) {
            this.help(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status": {
                this.status(sender);
                break;
            }
            case "on": {
                this.setEnabled(sender, true);
                break;
            }
            case "off": {
                this.setEnabled(sender, false);
                break;
            }
            case "reload": {
                this.plugin.reloadConfig();
                Text.msg(sender, this.pref() + this.plugin.getConfig().getString("messages.reloaded", "&aReloaded."));
                break;
            }
            case "check": {
                this.check(sender, args);
                break;
            }
            case "exempt": {
                this.exempt(sender, args);
                break;
            }
            case "unexempt": {
                this.unexempt(sender, args);
                break;
            }
            case "violations": 
            case "vl": {
                this.violations(sender, args);
                break;
            }
            case "clearvl": {
                this.clearVl(sender, args);
                break;
            }
            default: {
                this.help(sender);
            }
        }
        return true;
    }

    private void status(CommandSender sender) {
        String text = Text.replace(this.plugin.getConfig().getString("messages.status", "%status%"), "%status%", this.plugin.isEnabledInConfig() ? "&aON" : "&cOFF", "%online%", String.valueOf(Bukkit.getOnlinePlayers().size()), "%exempt%", String.valueOf(this.exemptManager.size()));
        Text.msg(sender, this.pref() + text);
    }

    private void setEnabled(CommandSender sender, boolean enabled) {
        this.plugin.getConfig().set("settings.enabled", (Object)enabled);
        this.plugin.saveConfig();
        Text.msg(sender, this.pref() + this.plugin.getConfig().getString(enabled ? "messages.enabled" : "messages.disabled"));
    }

    private void check(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Text.msg(sender, "&eGunakan: /rkac check <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target == null) {
            Text.msg(sender, "&cPlayer tidak ditemukan.");
            return;
        }
        String bypassPerm = this.plugin.getConfig().getString("settings.bypass-permission", "rumahkita.anticheat.bypass");
        String text = Text.replace(this.plugin.getConfig().getString("messages.check"), "%player%", target.getName(), "%ping%", String.valueOf(target.getPing()), "%bypass%", String.valueOf(target.hasPermission(bypassPerm)), "%exempt%", this.exemptManager.isTimedExempt(target.getUniqueId()) + " " + this.exemptManager.getRemainingSeconds(target.getUniqueId()) + "s", "%allowflight%", String.valueOf(target.getAllowFlight()), "%flying%", String.valueOf(target.isFlying()), "%gamemode%", target.getGameMode().name());
        Text.msg(sender, this.pref() + text);
    }

    private void exempt(CommandSender sender, String[] args) {
        long seconds;
        if (args.length < 3) {
            Text.msg(sender, "&eGunakan: /rkac exempt <player> <seconds> [reason]");
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target == null) {
            Text.msg(sender, "&cPlayer tidak ditemukan.");
            return;
        }
        try {
            seconds = Long.parseLong(args[2]);
        }
        catch (NumberFormatException ex) {
            Text.msg(sender, "&cSeconds harus angka.");
            return;
        }
        String reason = args.length >= 4 ? String.join((CharSequence)" ", Arrays.copyOfRange(args, 3, args.length)) : "manual";
        this.exemptManager.exempt(target, seconds, reason);
        String text = Text.replace(this.plugin.getConfig().getString("messages.exempt-added"), "%player%", target.getName(), "%seconds%", String.valueOf(seconds), "%reason%", reason);
        Text.msg(sender, this.pref() + text);
    }

    private void unexempt(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Text.msg(sender, "&eGunakan: /rkac unexempt <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target == null) {
            Text.msg(sender, "&cPlayer tidak ditemukan.");
            return;
        }
        if (this.exemptManager.remove(target.getUniqueId())) {
            Text.msg(sender, this.pref() + Text.replace(this.plugin.getConfig().getString("messages.exempt-removed"), "%player%", target.getName()));
        } else {
            Text.msg(sender, "&cPlayer tidak sedang exempt.");
        }
    }

    private void violations(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Text.msg(sender, "&eGunakan: /rkac violations <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target == null) {
            Text.msg(sender, "&cPlayer tidak ditemukan.");
            return;
        }
        Text.msg(sender, this.pref() + "&7VL &f" + target.getName() + "&7: &e" + this.violationTracker.summary(target.getUniqueId()));
    }

    private void clearVl(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Text.msg(sender, "&eGunakan: /rkac clearvl <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target == null) {
            Text.msg(sender, "&cPlayer tidak ditemukan.");
            return;
        }
        this.violationTracker.clear(target.getUniqueId());
        Text.msg(sender, this.pref() + "&aViolation " + target.getName() + " dibersihkan.");
    }

    private void help(CommandSender sender) {
        Text.msg(sender, "&8&m-----------------------------");
        Text.msg(sender, "&cRumahKitaAntiCheat &7v1.2.0");
        Text.msg(sender, "&e/rkac status");
        Text.msg(sender, "&e/rkac on");
        Text.msg(sender, "&e/rkac off");
        Text.msg(sender, "&e/rkac reload");
        Text.msg(sender, "&e/rkac check <player>");
        Text.msg(sender, "&e/rkac exempt <player> <seconds> [reason]");
        Text.msg(sender, "&e/rkac unexempt <player>");
        Text.msg(sender, "&e/rkac violations <player>");
        Text.msg(sender, "&e/rkac clearvl <player>");
        Text.msg(sender, "&8&m-----------------------------");
    }

    private String pref() {
        return this.plugin.getConfig().getString("settings.prefix", "&8[&cRumahKitaAC&8] ");
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return this.filter(List.of("status", "on", "off", "reload", "check", "exempt", "unexempt", "violations", "clearvl"), args[0]);
        }
        if (args.length == 2 && List.of("check", "exempt", "unexempt", "violations", "clearvl").contains(args[0].toLowerCase(Locale.ROOT))) {
            return this.filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("exempt")) {
            return this.filter(List.of("5", "10", "15", "30", "60"), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        String low = prefix.toLowerCase(Locale.ROOT);
        ArrayList<String> out = new ArrayList<String>();
        for (String value : values) {
            if (!value.toLowerCase(Locale.ROOT).startsWith(low)) continue;
            out.add(value);
        }
        return out;
    }
}

