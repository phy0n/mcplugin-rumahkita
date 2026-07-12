package id.rumahkita.warps;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WarpCommand implements CommandExecutor, TabCompleter {

    private final RumahKitaWarpsPlugin plugin;

    public WarpCommand(RumahKitaWarpsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player p = (Player) sender;
        WarpManager manager = plugin.getWarpManager();
        String prefix = manager.getPrefix();

        if (args.length == 0) {
            manager.openWarpMenu(p, 1);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("list")) {
            manager.openWarpMenu(p, 1);
            return true;
        }

        if (sub.equals("help")) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8==== &b&lPlayer Warp &8===="));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a /pwarp create <warp_name> &7- Create new pwarp"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a /pwarp delete <warp_name> &7- Delete your warp"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a /pwarp list &7- Open warp list menu"));
            return true;
        }

        if (sub.equals("create")) {
            if (args.length < 2) {
                p.sendMessage(prefix + ChatColor.RED + "Usage: /pwarp create <warp_name>");
                return true;
            }
            
            String warpName = args[1];
            if (!warpName.matches("^[a-zA-Z0-9_]+$")) {
                p.sendMessage(prefix + ChatColor.RED + "Warp name can only contain letters, numbers, and underscores.");
                return true;
            }
            if (warpName.length() > 16) {
                p.sendMessage(prefix + ChatColor.RED + "Warp name max 16 characters.");
                return true;
            }
            
            manager.createWarp(p, warpName);
            return true;
        }

        if (sub.equals("delete")) {
            if (args.length < 2) {
                p.sendMessage(prefix + ChatColor.RED + "Usage: /pwarp delete <warp_name>");
                return true;
            }
            manager.deleteWarp(p, args[1]);
            return true;
        }
        manager.teleportToWarp(p, sub);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> options = new ArrayList<>();
            options.add("create");
            options.add("delete");
            options.add("list");
            options.add("help");

            WarpManager manager = plugin.getWarpManager();
            options.addAll(manager.getWarpNames());
            
            for (String opt : options) {
                if (opt.toLowerCase().startsWith(partial)) {
                    completions.add(opt);
                }
            }
        }
        
        return completions;
    }
}
