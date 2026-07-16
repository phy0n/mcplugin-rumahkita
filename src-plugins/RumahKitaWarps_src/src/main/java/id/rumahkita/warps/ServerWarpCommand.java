package id.rumahkita.warps;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerWarpCommand implements CommandExecutor, TabCompleter {
    private final ServerWarpManager manager;

    public ServerWarpCommand(ServerWarpManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("warp")) {
            if (args.length == 0) {
                manager.openWarpMenu(p);
                return true;
            } else {
                ServerWarpManager.ServerWarp w = manager.getWarp(args[0]);
                if (w != null) {
                    manager.teleportToWarp(p, args[0]);
                } else {
                    p.sendMessage(ChatColor.RED + "Warp '" + args[0] + "' not found.");
                }
                return true;
            }
        }
        
        if (!p.hasPermission("rumahkitawarps.admin")) {
            p.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        if (cmd.equals("setwarp")) {
            if (args.length == 0) {
                p.sendMessage(ChatColor.RED + "Usage: /setwarp <nama>");
                return true;
            }
            manager.setWarp(args[0], p.getLocation());
            p.sendMessage(ChatColor.GREEN + "Warp " + ChatColor.YELLOW + args[0] + ChatColor.GREEN + " successfully set at your position!");
            return true;
        }

        if (cmd.equals("delwarp")) {
            if (args.length == 0) {
                p.sendMessage(ChatColor.RED + "Usage: /delwarp <nama>");
                return true;
            }
            if (manager.delWarp(args[0])) {
                p.sendMessage(ChatColor.GREEN + "Warp " + ChatColor.YELLOW + args[0] + ChatColor.GREEN + " successfully deleted.");
            } else {
                p.sendMessage(ChatColor.RED + "Warp not found.");
            }
            return true;
        }

        if (cmd.equals("editwarp")) {
            if (args.length < 3) {
                p.sendMessage(ChatColor.RED + "Usage: /editwarp <nama> <icon/slot/lore> <value>");
                return true;
            }
            ServerWarpManager.ServerWarp w = manager.getWarp(args[0]);
            if (w == null) {
                p.sendMessage(ChatColor.RED + "Warp not found.");
                return true;
            }

            String type = args[1].toLowerCase();
            if (type.equals("icon")) {
                Material m = Material.matchMaterial(args[2]);
                if (m != null) {
                    w.icon = m;
                    manager.saveWarps();
                    p.sendMessage(ChatColor.GREEN + "Icon changed to " + m.name());
                } else {
                    p.sendMessage(ChatColor.RED + "Invalid material.");
                }
            } else if (type.equals("slot")) {
                try {
                    int slot = Integer.parseInt(args[2]);
                    w.slot = slot;
                    manager.saveWarps();
                    p.sendMessage(ChatColor.GREEN + "Slot changed to " + slot);
                } catch (Exception e) {
                    p.sendMessage(ChatColor.RED + "Slot must be a number.");
                }
            } else if (type.equals("lore")) {
                String lore = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                if (lore.equalsIgnoreCase("clear")) {
                    w.lore.clear();
                } else {
                    w.lore.add(lore);
                }
                manager.saveWarps();
                p.sendMessage(ChatColor.GREEN + "Lore updated.");
            }
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("warp")) {
            if (args.length == 1) {
                for (String w : manager.getWarps().keySet()) {
                    if (w.toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(w);
                    }
                }
            }
        } else if (command.getName().equalsIgnoreCase("editwarp") && sender.hasPermission("rumahkitawarps.admin")) {
            if (args.length == 1) {
                for (String w : manager.getWarps().keySet()) {
                    if (w.toLowerCase().startsWith(args[0].toLowerCase())) completions.add(w);
                }
            } else if (args.length == 2) {
                List<String> opts = Arrays.asList("icon", "slot", "lore");
                for (String o : opts) {
                    if (o.startsWith(args[1].toLowerCase())) completions.add(o);
                }
            }
        } else if (command.getName().equalsIgnoreCase("delwarp") && sender.hasPermission("rumahkitawarps.admin")) {
            if (args.length == 1) {
                for (String w : manager.getWarps().keySet()) {
                    if (w.toLowerCase().startsWith(args[0].toLowerCase())) completions.add(w);
                }
            }
        }
        return completions;
    }
}
