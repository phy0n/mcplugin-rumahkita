package id.rumahkita.trade;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TradeCommand implements CommandExecutor, TabCompleter {
    private final TradeManager tradeManager;

    public TradeCommand(TradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Hanya pemain yang bisa menggunakan command ini.");
            return true;
        }

        Player p = (Player) sender;

        if (args.length == 0) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a--- &lRumahKitaTrade &a---"));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/trade <nama_player> &8- &7Mengajak trade."));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/trade accept &8- &7Menerima ajakan trade."));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/trade deny &8- &7Menolak ajakan trade."));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("accept")) {
            tradeManager.acceptTrade(p);
            return true;
        }

        if (sub.equals("deny")) {
            tradeManager.denyTrade(p);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "Pemain tersebut tidak ditemukan atau sedang offline.");
            return true;
        }

        if (target.equals(p)) {
            p.sendMessage(ChatColor.RED + "Kamu tidak bisa trade dengan dirimu sendiri.");
            return true;
        }

        tradeManager.inviteTrade(p, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("accept");
            completions.add("deny");
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
            String search = args[0].toLowerCase();
            return completions.stream().filter(s -> s.toLowerCase().startsWith(search)).collect(Collectors.toList());
        }
        return completions;
    }
}
