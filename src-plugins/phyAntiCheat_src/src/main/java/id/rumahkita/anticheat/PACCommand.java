package id.rumahkita.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.BanList;

public class PACCommand implements CommandExecutor {

    private final PhyAntiCheat plugin;
    public static final Set<UUID> mutedPlayers = new HashSet<>();

    public PACCommand(PhyAntiCheat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("phyanticheat.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8==== &c&l[PAC] Anti-Cheat &8===="));
            sender.sendMessage(ChatColor.GREEN + " /pac kick <player> [reason]");
            sender.sendMessage(ChatColor.GREEN + " /pac ban <player> [reason]");
            sender.sendMessage(ChatColor.GREEN + " /pac unban <player>");
            sender.sendMessage(ChatColor.GREEN + " /pac mute <player>");
            sender.sendMessage(ChatColor.GREEN + " /pac unmute <player>");
            sender.sendMessage(ChatColor.GREEN + " /pac freeze <player>");
            sender.sendMessage(ChatColor.GREEN + " /pac unfreeze <player>");
            sender.sendMessage(ChatColor.GREEN + " /pac check <player>");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("kick")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /pac kick <player> [reason]");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player offline.");
                return true;
            }
            String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Kicked by Admin.";
            target.kickPlayer(ChatColor.RED + "[PAC] " + ChatColor.WHITE + "You have been kicked.\n" + ChatColor.GRAY + "Reason: " + ChatColor.YELLOW + reason);
            Bukkit.broadcast(ChatColor.RED + "[PAC] " + ChatColor.YELLOW + target.getName() + ChatColor.GRAY + " has been kicked for: " + reason, "phyanticheat.admin");
            return true;
        }

        if (sub.equals("ban")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /pac ban <player> [reason]");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Banned by PAC.";
            Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(target.getName(), ChatColor.RED + "[PAC] " + ChatColor.WHITE + reason, null, sender.getName());
            if (target.isOnline()) {
                ((Player) target).kickPlayer(ChatColor.RED + "[PAC] " + ChatColor.WHITE + "You have been permanently banned.\n" + ChatColor.GRAY + "Reason: " + ChatColor.YELLOW + reason);
            }
            Bukkit.broadcast(ChatColor.RED + "[PAC] " + ChatColor.YELLOW + target.getName() + ChatColor.GRAY + " has been banned for: " + reason, "phyanticheat.admin");
            return true;
        }

        if (sub.equals("mute")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /pac mute <player>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            mutedPlayers.add(target.getUniqueId());
            sender.sendMessage(ChatColor.GREEN + "Muted " + target.getName() + ".");
            return true;
        }

        if (sub.equals("unmute")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /pac unmute <player>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            mutedPlayers.remove(target.getUniqueId());
            sender.sendMessage(ChatColor.GREEN + "Unmuted " + target.getName() + ".");
            return true;
        }

        if (sub.equals("unban")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /pac unban <player>");
                return true;
            }
            String targetName = args[1];
            if (Bukkit.getBanList(BanList.Type.NAME).isBanned(targetName)) {
                Bukkit.getBanList(BanList.Type.NAME).pardon(targetName);
                sender.sendMessage(ChatColor.GREEN + "Unbanned " + targetName + ".");
                Bukkit.broadcast(ChatColor.GREEN + "[PAC] " + ChatColor.YELLOW + targetName + ChatColor.GRAY + " has been unbanned.", "phyanticheat.admin");
            } else {
                sender.sendMessage(ChatColor.RED + "Player is not banned.");
            }
            return true;
        }

        if (sub.equals("freeze")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /pac freeze <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player offline.");
                return true;
            }
            FreezeListener.frozenPlayers.add(target.getUniqueId());
            target.sendMessage(ChatColor.RED + "[PAC] " + ChatColor.WHITE + "You have been frozen by an admin! Do not log out.");
            sender.sendMessage(ChatColor.GREEN + "Froze " + target.getName() + ".");
            return true;
        }

        if (sub.equals("unfreeze")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /pac unfreeze <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player offline.");
                return true;
            }
            FreezeListener.frozenPlayers.remove(target.getUniqueId());
            target.sendMessage(ChatColor.GREEN + "[PAC] " + ChatColor.WHITE + "You have been unfrozen.");
            sender.sendMessage(ChatColor.GREEN + "Unfroze " + target.getName() + ".");
            return true;
        }

        if (sub.equals("check")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /pac check <player>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8==== &c[PAC] " + target.getName() + " &8===="));
            sender.sendMessage(ChatColor.GRAY + "Status Muted: " + (mutedPlayers.contains(target.getUniqueId()) ? ChatColor.RED + "Yes" : ChatColor.GREEN + "No"));
            sender.sendMessage(ChatColor.GRAY + "Status Frozen: " + (FreezeListener.frozenPlayers.contains(target.getUniqueId()) ? ChatColor.RED + "Yes" : ChatColor.GREEN + "No"));
            sender.sendMessage(ChatColor.GRAY + "Status Banned: " + (Bukkit.getBanList(BanList.Type.NAME).isBanned(target.getName()) ? ChatColor.RED + "Yes" : ChatColor.GREEN + "No"));
            if (target.isOnline()) {
                Player p = (Player) target;
                sender.sendMessage(ChatColor.GRAY + "Ping: " + ChatColor.YELLOW + p.getPing() + "ms");
                sender.sendMessage(ChatColor.GRAY + "Gamemode: " + ChatColor.YELLOW + p.getGameMode().toString());
            } else {
                sender.sendMessage(ChatColor.GRAY + "Player is offline.");
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Unknown command.");
        return true;
    }
}
