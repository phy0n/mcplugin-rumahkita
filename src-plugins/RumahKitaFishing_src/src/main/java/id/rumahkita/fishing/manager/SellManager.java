/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package id.rumahkita.fishing.manager;

import id.rumahkita.fishing.RumahKitaFishingPlugin;
import id.rumahkita.fishing.model.CaughtFish;
import id.rumahkita.fishing.model.PlayerFishingData;
import id.rumahkita.fishing.util.NumberUtil;
import id.rumahkita.fishing.util.Text;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class SellManager {
    private final RumahKitaFishingPlugin plugin;

    public SellManager(RumahKitaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public void sellHand(Player player) {
        long totalPrice;
        if (!player.hasPermission("rumahkitafishing.sell")) {
            Text.send((CommandSender)player, this.plugin.messagesConfig().get(), "no-permission", Map.of());
            return;
        }
        PlayerFishingData data = this.plugin.playerDataManager().get(player);
        ItemStack item = player.getInventory().getItemInMainHand();
        Optional<CaughtFish> optionalFish = this.plugin.fishItemFactory().read(item, data.level());
        if (optionalFish.isEmpty()) {
            Text.send((CommandSender)player, this.plugin.messagesConfig().get(), "not-custom-fish", Map.of());
            return;
        }
        CaughtFish fish = optionalFish.get();
        int amount = Math.max(1, item.getAmount());
        if (!this.checkDailyLimit(player, amount, totalPrice = (long)fish.price() * (long)amount)) {
            return;
        }
        if (!this.plugin.economyManager().deposit(player, this.safeMoney(totalPrice))) {
            Text.send((CommandSender)player, this.plugin.messagesConfig().get(), "economy-failed", Map.of());
            return;
        }
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        this.plugin.dailyLimitManager().add(player, amount, totalPrice);
        this.plugin.playerDataManager().addSold(player, amount, totalPrice);
        Text.send((CommandSender)player, this.plugin.messagesConfig().get(), "sell-hand-success", Map.of("amount", String.valueOf(amount), "fish_name", Text.stripColor(fish.definition().displayName()), "price", NumberUtil.money(totalPrice)));
    }

    public void sellAll(Player player) {
        if (!player.hasPermission("rumahkitafishing.sell")) {
            Text.send((CommandSender)player, this.plugin.messagesConfig().get(), "no-permission", Map.of());
            return;
        }
        PlayerFishingData data = this.plugin.playerDataManager().get(player);
        ArrayList<SellEntry> entries = new ArrayList<SellEntry>();
        ItemStack[] contents = player.getInventory().getStorageContents();
        long amount = 0L;
        long totalPrice = 0L;
        for (int slot = 0; slot < contents.length; ++slot) {
            ItemStack item = contents[slot];
            Optional<CaughtFish> optionalFish = this.plugin.fishItemFactory().read(item, data.level());
            if (optionalFish.isEmpty()) continue;
            CaughtFish fish = optionalFish.get();
            int stackAmount = Math.max(1, item.getAmount());
            long stackPrice = (long)fish.price() * (long)stackAmount;
            entries.add(new SellEntry(slot, stackAmount, stackPrice));
            amount += (long)stackAmount;
            totalPrice += stackPrice;
        }
        if (entries.isEmpty()) {
            Text.send((CommandSender)player, this.plugin.messagesConfig().get(), "no-fish-in-inventory", Map.of());
            return;
        }
        if (!this.checkDailyLimit(player, amount, totalPrice)) {
            return;
        }
        if (!this.plugin.economyManager().deposit(player, this.safeMoney(totalPrice))) {
            Text.send((CommandSender)player, this.plugin.messagesConfig().get(), "economy-failed", Map.of());
            return;
        }
        for (SellEntry entry : entries) {
            player.getInventory().setItem(entry.slot(), null);
        }
        this.plugin.dailyLimitManager().add(player, amount, totalPrice);
        this.plugin.playerDataManager().addSold(player, amount, totalPrice);
        Text.send((CommandSender)player, this.plugin.messagesConfig().get(), "sell-all-success", Map.of("amount", String.valueOf(amount), "price", NumberUtil.money(totalPrice)));
    }

    private boolean checkDailyLimit(Player player, long fishAmount, long money) {
        if (!this.plugin.dailyLimitManager().canSellFish(player, fishAmount)) {
            Text.send((CommandSender)player, this.plugin.messagesConfig().get(), "daily-limit-fish", Map.of("limit", NumberUtil.money(this.plugin.dailyLimitManager().maxFishPerDay())));
            return false;
        }
        if (!this.plugin.dailyLimitManager().canEarn(player, money)) {
            Text.send((CommandSender)player, this.plugin.messagesConfig().get(), "daily-limit-money", Map.of("limit", NumberUtil.money(this.plugin.dailyLimitManager().maxMoneyPerDay())));
            return false;
        }
        return true;
    }

    private int safeMoney(long totalPrice) {
        if (totalPrice > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (totalPrice <= 0L) {
            return 0;
        }
        return (int)totalPrice;
    }

    private record SellEntry(int slot, int amount, long price) {
    }
}

