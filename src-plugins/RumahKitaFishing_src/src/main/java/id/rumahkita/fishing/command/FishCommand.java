/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.entity.Player
 */
package id.rumahkita.fishing.command;

import id.rumahkita.fishing.RumahKitaFishingPlugin;
import id.rumahkita.fishing.model.PlayerFishingData;
import id.rumahkita.fishing.util.NumberUtil;
import id.rumahkita.fishing.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class FishCommand
implements TabExecutor {
    private final RumahKitaFishingPlugin plugin;

    public FishCommand(RumahKitaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub;
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            Text.send(sender, this.plugin.messagesConfig().get(), "help", Map.of());
            return true;
        }
        switch (sub = args[0].toLowerCase(Locale.ROOT)) {
            case "sell": {
                this.handleSell(sender, args);
                break;
            }
            case "shop": 
            case "market": {
                this.handleShop(sender);
                break;
            }
            case "stats": {
                this.handleStats(sender);
                break;
            }
            case "top": 
            case "leaderboard": {
                this.handleTop(sender, args);
                break;
            }
            case "reload": {
                this.handleReload(sender);
                break;
            }
            default: {
                Text.send(sender, this.plugin.messagesConfig().get(), "unknown-command", Map.of());
            }
        }
        return true;
    }

    private void handleSell(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            Text.send(sender, this.plugin.messagesConfig().get(), "player-only", Map.of());
            return;
        }
        Player player = (Player)sender;
        if (args.length < 2) {
            Text.send(sender, this.plugin.messagesConfig().get(), "help", Map.of());
            return;
        }
        if (args[1].equalsIgnoreCase("hand")) {
            this.plugin.sellManager().sellHand(player);
            return;
        }
        if (args[1].equalsIgnoreCase("all")) {
            this.plugin.sellManager().sellAll(player);
            return;
        }
        Text.send((CommandSender)player, this.plugin.messagesConfig().get(), "help", Map.of());
    }

    private void handleShop(CommandSender sender) {
        if (!(sender instanceof Player)) {
            Text.send(sender, this.plugin.messagesConfig().get(), "player-only", Map.of());
            return;
        }
        Player player = (Player)sender;
        if (!player.hasPermission("rumahkitafishing.shop")) {
            Text.send((CommandSender)player, this.plugin.messagesConfig().get(), "no-permission", Map.of());
            return;
        }
        this.plugin.fishMarketMenu().open(player);
    }

    private void handleStats(CommandSender sender) {
        if (!(sender instanceof Player)) {
            Text.send(sender, this.plugin.messagesConfig().get(), "player-only", Map.of());
            return;
        }
        Player player = (Player)sender;
        if (!player.hasPermission("rumahkitafishing.stats")) {
            Text.send((CommandSender)player, this.plugin.messagesConfig().get(), "no-permission", Map.of());
            return;
        }
        PlayerFishingData data = this.plugin.playerDataManager().get(player);
        this.sendStats((CommandSender)player, data);
    }

    public void sendStats(CommandSender sender, PlayerFishingData data) {
        Text.send(sender, this.plugin.messagesConfig().get(), "stats-header", Map.of("player", data.name()));
        Text.send(sender, this.plugin.messagesConfig().get(), "stats-line-level", Map.of("level", String.valueOf(data.level()), "exp", String.valueOf(data.exp()), "required_exp", String.valueOf(this.plugin.fishManager().requiredExp(data.level()))));
        Text.send(sender, this.plugin.messagesConfig().get(), "stats-line-catches", Map.of("total_catches", NumberUtil.money(data.totalCatches())));
        Text.send(sender, this.plugin.messagesConfig().get(), "stats-line-sold", Map.of("total_sold", NumberUtil.money(data.totalSold())));
        Text.send(sender, this.plugin.messagesConfig().get(), "stats-line-earned", Map.of("total_earned", NumberUtil.money(data.totalEarned())));
        Text.send(sender, this.plugin.messagesConfig().get(), "stats-line-biggest", Map.of("biggest_fish_name", data.biggestFishName(), "biggest_fish_weight", NumberUtil.weight(data.biggestFishWeight())));
    }

    private void handleTop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rumahkitafishing.top")) {
            Text.send(sender, this.plugin.messagesConfig().get(), "no-permission", Map.of());
            return;
        }
        String type = args.length >= 2 ? args[1] : "weight";
        this.plugin.leaderboardManager().sendLeaderboard(sender, type);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("rumahkitafishing.admin")) {
            Text.send(sender, this.plugin.messagesConfig().get(), "no-permission", Map.of());
            return;
        }
        this.plugin.reloadEverything();
        Text.send(sender, this.plugin.messagesConfig().get(), "reload-success", Map.of());
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return this.filter(List.of("help", "shop", "sell", "stats", "top", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            return this.filter(List.of("hand", "all"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            return this.filter(List.of("weight", "catches", "money", "level"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        ArrayList<String> result = new ArrayList<String>();
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (!value.toLowerCase(Locale.ROOT).startsWith(lower)) continue;
            result.add(value);
        }
        return result;
    }
}

