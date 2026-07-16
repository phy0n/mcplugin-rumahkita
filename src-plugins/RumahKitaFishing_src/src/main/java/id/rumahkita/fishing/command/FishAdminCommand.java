/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package id.rumahkita.fishing.command;

import id.rumahkita.fishing.RumahKitaFishingPlugin;
import id.rumahkita.fishing.model.CaughtFish;
import id.rumahkita.fishing.model.FishDefinition;
import id.rumahkita.fishing.model.PlayerFishingData;
import id.rumahkita.fishing.model.Rarity;
import id.rumahkita.fishing.util.NumberUtil;
import id.rumahkita.fishing.util.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class FishAdminCommand
implements TabExecutor {
    private final RumahKitaFishingPlugin plugin;

    public FishAdminCommand(RumahKitaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rumahkitafishing.admin")) {
            Text.send(sender, this.plugin.messagesConfig().get(), "no-permission", Map.of());
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            Text.send(sender, this.plugin.messagesConfig().get(), "admin-help", Map.of());
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give": {
                this.handleGive(sender, args);
                break;
            }
            case "resetstats": {
                this.handleResetStats(sender, args);
                break;
            }
            case "resetlimit": {
                this.handleResetLimit(sender, args);
                break;
            }
            case "reload": {
                this.handleReload(sender);
                break;
            }
            case "debug": {
                this.handleDebug(sender, args);
                break;
            }
            default: {
                Text.send(sender, this.plugin.messagesConfig().get(), "admin-help", Map.of());
            }
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("random")) {
            this.handleGiveRandom(sender, args);
            return;
        }
        if (args.length < 3) {
            Text.send(sender, this.plugin.messagesConfig().get(), "admin-help", Map.of());
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target == null) {
            Text.send(sender, this.plugin.messagesConfig().get(), "admin-player-not-found", Map.of());
            return;
        }
        String fishId = args[2].toLowerCase(Locale.ROOT);
        Optional<FishDefinition> optionalDefinition = this.plugin.fishManager().find(fishId);
        if (optionalDefinition.isEmpty()) {
            Text.send(sender, this.plugin.messagesConfig().get(), "admin-fish-not-found", Map.of("fish_id", fishId));
            return;
        }
        FishDefinition definition = optionalDefinition.get();
        double weight = args.length >= 4 ? this.parseDouble(args[3], this.plugin.fishManager().rollWeight(definition, this.plugin.playerDataManager().get(target).level())) : this.plugin.fishManager().rollWeight(definition, this.plugin.playerDataManager().get(target).level());
        this.giveFish(sender, target, definition, NumberUtil.round2(weight));
    }

    private void handleGiveRandom(CommandSender sender, String[] args) {
        if (args.length < 4) {
            Text.send(sender, this.plugin.messagesConfig().get(), "admin-help", Map.of());
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[2]);
        if (target == null) {
            Text.send(sender, this.plugin.messagesConfig().get(), "admin-player-not-found", Map.of());
            return;
        }
        Rarity rarity = Rarity.fromString(args[3]);
        List<FishDefinition> definitions = this.plugin.fishManager().fishes().stream().filter(fish -> fish.rarity() == rarity).toList();
        if (definitions.isEmpty()) {
            sender.sendMessage(Text.color("&cTidak ada ikan dengan rarity itu."));
            return;
        }
        FishDefinition definition = definitions.get(ThreadLocalRandom.current().nextInt(definitions.size()));
        double weight = this.plugin.fishManager().rollWeight(definition, this.plugin.playerDataManager().get(target).level());
        this.giveFish(sender, target, definition, weight);
    }

    private void giveFish(CommandSender sender, Player target, FishDefinition definition, double weight) {
        int level = this.plugin.playerDataManager().get(target).level();
        int price = this.plugin.fishManager().calculatePrice(definition, weight, level);
        CaughtFish caughtFish = new CaughtFish(UUID.randomUUID(), definition, weight, price, target.getUniqueId(), target.getName(), System.currentTimeMillis(), target.getLocation().getBlock().getBiome().name(), target.getWorld().getName());
        ItemStack item = this.plugin.fishItemFactory().create(caughtFish);
        HashMap leftover = target.getInventory().addItem(new ItemStack[]{item});
        for (ItemStack stack : leftover.values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), stack);
        }
        Text.send(sender, this.plugin.messagesConfig().get(), "admin-give-success", Map.of("fish_name", Text.stripColor(definition.displayName()), "weight", NumberUtil.weight(weight), "player", target.getName()));
    }

    private void handleResetStats(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Text.send(sender, this.plugin.messagesConfig().get(), "admin-help", Map.of());
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target == null) {
            Text.send(sender, this.plugin.messagesConfig().get(), "admin-player-not-found", Map.of());
            return;
        }
        this.plugin.playerDataManager().reset(target);
        Text.send(sender, this.plugin.messagesConfig().get(), "admin-resetstats-success", Map.of("player", target.getName()));
    }

    private void handleResetLimit(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Text.send(sender, this.plugin.messagesConfig().get(), "admin-help", Map.of());
            return;
        }
        if (args[1].equalsIgnoreCase("all")) {
            this.plugin.dailyLimitManager().resetAllToday();
            Text.send(sender, this.plugin.messagesConfig().get(), "admin-resetlimit-all-success", Map.of());
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target == null) {
            Text.send(sender, this.plugin.messagesConfig().get(), "admin-player-not-found", Map.of());
            return;
        }
        this.plugin.dailyLimitManager().reset(target);
        Text.send(sender, this.plugin.messagesConfig().get(), "admin-resetlimit-player-success", Map.of("player", target.getName()));
    }

    private void handleReload(CommandSender sender) {
        this.plugin.reloadEverything();
        Text.send(sender, this.plugin.messagesConfig().get(), "reload-success", Map.of());
    }

    private void handleDebug(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Text.send(sender, this.plugin.messagesConfig().get(), "admin-help", Map.of());
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target == null) {
            Text.send(sender, this.plugin.messagesConfig().get(), "admin-player-not-found", Map.of());
            return;
        }
        PlayerFishingData data = this.plugin.playerDataManager().get(target);
        sender.sendMessage(Text.color("&bFishing debug for &f" + target.getName()));
        sender.sendMessage(Text.color("&7Level: &f" + data.level() + " &7EXP: &f" + data.exp()));
        sender.sendMessage(Text.color("&7Sold today: &f" + this.plugin.dailyLimitManager().soldToday(target.getUniqueId())));
        sender.sendMessage(Text.color("&7Earned today: &fRp " + NumberUtil.money(this.plugin.dailyLimitManager().earnedToday(target.getUniqueId()))));
        sender.sendMessage(Text.color("&7Economy mode: &f" + this.plugin.economyManager().mode()));
        sender.sendMessage(Text.color("&7Loaded fishes: &f" + this.plugin.fishManager().fishes().size()));
    }

    private double parseDouble(String raw, double fallback) {
        try {
            return Double.parseDouble(raw);
        }
        catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rumahkitafishing.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return this.filter(List.of("give", "resetstats", "resetlimit", "reload", "debug"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            ArrayList<String> values = new ArrayList<String>();
            values.add("random");
            for (Player player2 : Bukkit.getOnlinePlayers()) {
                values.add(player2.getName());
            }
            return this.filter(values, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give") && !args[1].equalsIgnoreCase("random")) {
            return this.filter(this.plugin.fishManager().fishes().stream().map(FishDefinition::id).toList(), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give") && args[1].equalsIgnoreCase("random")) {
            return this.filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("give") && args[1].equalsIgnoreCase("random")) {
            return this.filter(List.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC"), args[3]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("resetstats") || args[0].equalsIgnoreCase("debug"))) {
            return this.filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("resetlimit")) {
            ArrayList<String> values = new ArrayList<String>();
            values.add("all");
            Bukkit.getOnlinePlayers().forEach(player -> values.add(player.getName()));
            return this.filter(values, args[1]);
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

