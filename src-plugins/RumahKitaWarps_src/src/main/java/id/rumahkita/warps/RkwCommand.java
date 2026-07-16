package id.rumahkita.warps;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.List;

public class RkwCommand implements CommandExecutor, TabCompleter {
    private final RumahKitaWarpsPlugin plugin;

    public RkwCommand(RumahKitaWarpsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rumahkitawarps.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Type /rkw help for help.");
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8==== &b&lRumahKita Warps &8===="));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a /warp [name] &7- Open Server Warp GUI"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a /setwarp <name> &7- Create Server Warp"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a /delwarp <name> &7- Delete Server Warp"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a /editwarp <name> <icon/slot/lore> &7- Edit Server Warp"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a /pwarp &7- Open Player Warp menu"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a /rtp &7- Random Teleport"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a /rkw reload &7- Reload config"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&aRumahKitaWarps&8] &aConfig reloaded successfully."));
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Command not found. Type /rkw for help.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if ("reload".startsWith(args[0].toLowerCase())) completions.add("reload");
            if ("help".startsWith(args[0].toLowerCase())) completions.add("help");
        }
        return completions;
    }
}
