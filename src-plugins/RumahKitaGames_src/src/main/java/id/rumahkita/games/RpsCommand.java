package id.rumahkita.games;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RpsCommand implements CommandExecutor, TabCompleter {

    private RumahKitaGamesPlugin plugin;

    public RpsCommand(RumahKitaGamesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can play RPS.");
            return true;
        }

        Player p = (Player) sender;
        RpsManager manager = plugin.getRpsManager();

        if (args.length == 0) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e=== &6RPS Help &e==="));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rps create <amount> <rock/paper/scissors> &7- Create RPS room"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rps join <player_name> <rock/paper/scissors> &7- Play against an open RPS"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rps invite <player> <amount> <rock/paper/scissors> &7- Invite player to RPS"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rps accept <player> <rock/paper/scissors> &7- Accept RPS invite"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rps deny <player> &7- Deny RPS invite"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rps list &7- Open active RPS list"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rps cancel &7- Cancel your RPS room"));
            return true;
        }

        String sub = args[0].toLowerCase();
        
        if (sub.equals("list")) {
            manager.openGameList(p);
            return true;
        }
        
        if (sub.equals("help")) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e=== &6RPS Help &e==="));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rps create <amount> <rock/paper/scissors> &7- Create RPS room"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rps join <player_name> <rock/paper/scissors> &7- Play against an open RPS"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rps invite <player> <amount> <rock/paper/scissors> &7- Invite player to RPS"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rps accept <player> <rock/paper/scissors> &7- Accept RPS invite"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rps deny <player> &7- Deny RPS invite"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rps list &7- Open active RPS list"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rps cancel &7- Cancel your RPS room"));
            return true;
        }

        if (sub.equals("create")) {
            if (args.length < 3) {
                p.sendMessage(ChatColor.RED + "Usage: /rps create <amount> <rock/paper/scissors>");
                return true;
            }
            long amount;
            try {
                amount = Long.parseLong(args[1]);
            } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.RED + "The amount must be a valid number.");
                return true;
            }

            long minBet = plugin.getConfig().getLong("rps.min_bet", 1000L);
            if (amount < minBet) {
                p.sendMessage(ChatColor.RED + "Minimum bet is Rp" + minBet + ".");
                return true;
            }

            String choice = args[2].toLowerCase();
            if (!manager.isValidChoice(choice)) {
                p.sendMessage(ChatColor.RED + "Choice must be 'rock', 'paper', or 'scissors'.");
                return true;
            }

            manager.createGame(p, amount, choice);
            return true;
        }

        if (sub.equals("join")) {
            if (args.length < 3) {
                p.sendMessage(ChatColor.RED + "Usage: /rps join <player_name> <rock/paper/scissors>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                p.sendMessage(ChatColor.RED + "That player is not online.");
                return true;
            }
            
            String choice = args[2].toLowerCase();
            if (!manager.isValidChoice(choice)) {
                p.sendMessage(ChatColor.RED + "Choice must be 'rock', 'paper', or 'scissors'.");
                return true;
            }

            manager.joinGame(p, target, choice);
            return true;
        }

        if (sub.equals("cancel")) {
            manager.cancelGame(p);
            return true;
        }
        
        if (sub.equals("invite")) {
            if (args.length < 4) {
                p.sendMessage(ChatColor.RED + "Usage: /rps invite <player_name> <amount> <rock/paper/scissors>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                p.sendMessage(ChatColor.RED + "That player is not online.");
                return true;
            }
            long amount;
            try {
                amount = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.RED + "The amount must be a valid number.");
                return true;
            }
            long minBet = plugin.getConfig().getLong("rps.min_bet", 1000L);
            if (amount < minBet) {
                p.sendMessage(ChatColor.RED + "Minimum bet is Rp" + minBet + ".");
                return true;
            }
            String choice = args[3].toLowerCase();
            if (!manager.isValidChoice(choice)) {
                p.sendMessage(ChatColor.RED + "Choice must be 'rock', 'paper', or 'scissors'.");
                return true;
            }
            manager.inviteGame(p, target, amount, choice);
            return true;
        }

        if (sub.equals("accept")) {
            if (args.length < 3) {
                p.sendMessage(ChatColor.RED + "Usage: /rps accept <player_name> <rock/paper/scissors>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                p.sendMessage(ChatColor.RED + "That player is not online.");
                return true;
            }
            
            String choice = args[2].toLowerCase();
            if (!manager.isValidChoice(choice)) {
                p.sendMessage(ChatColor.RED + "Choice must be 'rock', 'paper', or 'scissors'.");
                return true;
            }
            
            manager.joinGame(p, target, choice);
            return true;
        }
        
        if (sub.equals("deny")) {
            if (args.length < 2) {
                p.sendMessage(ChatColor.RED + "Usage: /rps deny <player_name>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                p.sendMessage(ChatColor.RED + "That player is not online.");
                return true;
            }
            manager.denyGame(p, target);
            return true;
        }

        p.sendMessage(ChatColor.RED + "Unknown sub-command. Type /rps for help.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "join", "invite", "accept", "deny", "list", "cancel", "help").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("join") || sub.equals("invite") || sub.equals("accept") || sub.equals("deny")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (sub.equals("create")) {
                return Arrays.asList("1000", "5000", "10000").stream()
                        .filter(s -> s.startsWith(args[1]))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("create") || sub.equals("join") || sub.equals("accept")) {
                return Arrays.asList("batu", "gunting", "kertas").stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (sub.equals("invite")) {
                return Arrays.asList("1000", "5000", "10000").stream()
                        .filter(s -> s.startsWith(args[2]))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 4) {
            String sub = args[0].toLowerCase();
            if (sub.equals("invite")) {
                return Arrays.asList("batu", "gunting", "kertas").stream()
                        .filter(s -> s.startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}
