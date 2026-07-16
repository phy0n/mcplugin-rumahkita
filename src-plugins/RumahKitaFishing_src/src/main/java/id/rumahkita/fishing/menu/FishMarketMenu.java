/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package id.rumahkita.fishing.menu;

import id.rumahkita.fishing.RumahKitaFishingPlugin;
import id.rumahkita.fishing.command.FishCommand;
import id.rumahkita.fishing.menu.FishMenuHolder;
import id.rumahkita.fishing.util.Text;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class FishMarketMenu {
    private final RumahKitaFishingPlugin plugin;

    public FishMarketMenu(RumahKitaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String title = Text.color(this.plugin.guiConfig().get().getString("fish-market.title", "&8RumahKita Fish Market"));
        int size = this.normalizeSize(this.plugin.guiConfig().get().getInt("fish-market.size", 27));
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new FishMenuHolder(), (int)size, (String)title);
        if (this.plugin.guiConfig().get().getBoolean("fish-market.filler.enabled", true)) {
            ItemStack filler = this.createItem(this.plugin.guiConfig().get().getString("fish-market.filler.material", "GRAY_STAINED_GLASS_PANE"), this.plugin.guiConfig().get().getString("fish-market.filler.name", " "), this.plugin.guiConfig().get().getStringList("fish-market.filler.lore"));
            for (int slot = 0; slot < size; ++slot) {
                inventory.setItem(slot, filler);
            }
        }
        this.addButton(inventory, "sell-hand");
        this.addButton(inventory, "sell-all");
        this.addButton(inventory, "stats");
        this.addButton(inventory, "leaderboard");
        this.addButton(inventory, "close");
        player.openInventory(inventory);
    }

    public void handleClick(Player player, int rawSlot) {
        ConfigurationSection buttons = this.plugin.guiConfig().get().getConfigurationSection("fish-market.buttons");
        if (buttons == null) {
            return;
        }
        if (rawSlot == buttons.getInt("sell-hand.slot", 10)) {
            player.closeInventory();
            this.plugin.sellManager().sellHand(player);
            return;
        }
        if (rawSlot == buttons.getInt("sell-all.slot", 12)) {
            player.closeInventory();
            this.plugin.sellManager().sellAll(player);
            return;
        }
        if (rawSlot == buttons.getInt("stats.slot", 14)) {
            player.closeInventory();
            new FishCommand(this.plugin).sendStats((CommandSender)player, this.plugin.playerDataManager().get(player));
            return;
        }
        if (rawSlot == buttons.getInt("leaderboard.slot", 16)) {
            player.closeInventory();
            this.plugin.leaderboardManager().sendLeaderboard((CommandSender)player, "weight");
            return;
        }
        if (rawSlot == buttons.getInt("close.slot", 22)) {
            player.closeInventory();
        }
    }

    private void addButton(Inventory inventory, String key) {
        String path = "fish-market.buttons." + key;
        int slot = this.plugin.guiConfig().get().getInt(path + ".slot", -1);
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, this.createItem(this.plugin.guiConfig().get().getString(path + ".material", "STONE"), this.plugin.guiConfig().get().getString(path + ".name", key), this.plugin.guiConfig().get().getStringList(path + ".lore")));
    }

    private ItemStack createItem(String materialName, String name, List<String> lore) {
        ItemStack item;
        ItemMeta meta;
        Material material = Material.matchMaterial((String)(materialName == null ? "STONE" : materialName));
        if (material == null || !material.isItem()) {
            material = Material.STONE;
        }
        if ((meta = (item = new ItemStack(material)).getItemMeta()) != null) {
            meta.setDisplayName(Text.color(name));
            meta.setLore(Text.colorList(lore));
            meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS});
            item.setItemMeta(meta);
        }
        return item;
    }

    private int normalizeSize(int requested) {
        if (requested < 9) {
            return 9;
        }
        if (requested > 54) {
            return 54;
        }
        return (requested + 8) / 9 * 9;
    }
}

