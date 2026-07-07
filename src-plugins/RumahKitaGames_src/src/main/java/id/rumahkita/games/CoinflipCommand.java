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

public class CoinflipCommand implements CommandExecutor, TabCompleter {

    private RumahKitaGamesPlugin plugin;

    public CoinflipCommand(RumahKitaGamesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Hanya player yang bisa main coinflip.");
            return true;
        }

        Player p = (Player) sender;
        CoinflipManager manager = plugin.getCoinflipManager();

        if (args.length == 0) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e=== &6Bantuan Coinflip &e==="));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/cf create <amount> <heads/tails> &7- Buat room coinflip"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/cf join <nama_player> &7- Lawan player yang open cf"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/cf invite <player> <amount> <heads/tails> &7- Ajak player tertentu adu cf"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/cf accept/deny <player> &7- Terima/Tolak ajakan cf"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/cf list &7- Buka menu list coinflip aktif"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/cf cancel &7- Batalkan room coinflip milikmu"));
            return true;
        }

        String sub = args[0].toLowerCase();
        
        if (sub.equals("list")) {
            manager.openGameList(p);
            return true;
        }
        
        if (sub.equals("help")) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e=== &6Bantuan Coinflip &e==="));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/cf create <amount> <heads/tails> &7- Buat room coinflip"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/cf join <nama_player> &7- Lawan player yang open cf"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/cf invite <player> <amount> <heads/tails> &7- Ajak player tertentu adu cf"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/cf accept/deny <player> &7- Terima/Tolak ajakan cf"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/cf list &7- Buka menu list coinflip aktif"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/cf cancel &7- Batalkan room coinflip milikmu"));
            return true;
        }

        if (sub.equals("create")) {
            if (args.length < 3) {
                p.sendMessage(ChatColor.RED + "Penggunaan: /cf create <jumlah> <heads/tails>");
                return true;
            }
            long amount;
            try {
                amount = Long.parseLong(args[1]);
            } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.RED + "Jumlah uang harus berupa angka yang valid.");
                return true;
            }

            long minBet = plugin.getConfig().getLong("coinflip.min_bet", 1000L);
            if (amount < minBet) {
                p.sendMessage(ChatColor.RED + "Minimal taruhan adalah Rp" + minBet + ".");
                return true;
            }

            String side = args[2].toLowerCase();
            if (!side.equals("heads") && !side.equals("tails")) {
                p.sendMessage(ChatColor.RED + "Sisi koin harus 'heads' (kepala) atau 'tails' (ekor).");
                return true;
            }

            manager.createGame(p, amount, side);
            return true;
        }

        if (sub.equals("join")) {
            if (args.length < 2) {
                p.sendMessage(ChatColor.RED + "Penggunaan: /cf join <nama_player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                p.sendMessage(ChatColor.RED + "Player tersebut tidak online.");
                return true;
            }

            manager.joinGame(p, target);
            return true;
        }

        if (sub.equals("cancel")) {
            manager.cancelGame(p);
            return true;
        }
        
        if (sub.equals("invite")) {
            if (args.length < 4) {
                p.sendMessage(ChatColor.RED + "Penggunaan: /cf invite <nama_player> <jumlah> <heads/tails>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                p.sendMessage(ChatColor.RED + "Player tersebut tidak online.");
                return true;
            }
            long amount;
            try {
                amount = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.RED + "Jumlah uang harus berupa angka yang valid.");
                return true;
            }
            long minBet = plugin.getConfig().getLong("coinflip.min_bet", 1000L);
            if (amount < minBet) {
                p.sendMessage(ChatColor.RED + "Minimal taruhan adalah Rp" + minBet + ".");
                return true;
            }
            String side = args[3].toLowerCase();
            if (!side.equals("heads") && !side.equals("tails")) {
                p.sendMessage(ChatColor.RED + "Sisi koin harus 'heads' (kepala) atau 'tails' (ekor).");
                return true;
            }
            manager.inviteGame(p, target, amount, side);
            return true;
        }

        if (sub.equals("accept")) {
            if (args.length < 2) {
                p.sendMessage(ChatColor.RED + "Penggunaan: /cf accept <nama_player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                p.sendMessage(ChatColor.RED + "Player tersebut tidak online.");
                return true;
            }
            manager.joinGame(p, target);
            return true;
        }
        
        if (sub.equals("deny")) {
            if (args.length < 2) {
                p.sendMessage(ChatColor.RED + "Penggunaan: /cf deny <nama_player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                p.sendMessage(ChatColor.RED + "Player tersebut tidak online.");
                return true;
            }
            manager.denyGame(p, target);
            return true;
        }

        p.sendMessage(ChatColor.RED + "Sub command tidak diketahui. Ketik /cf untuk bantuan.");
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
            if (sub.equals("create")) {
                return Arrays.asList("heads", "tails").stream()
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
                return Arrays.asList("heads", "tails").stream()
                        .filter(s -> s.startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}
