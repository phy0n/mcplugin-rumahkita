package id.rumahkita.trade;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import id.rumahkita.economy.RumahKitaEconomyRupiahPlugin;

public class TradeSession {
    private final Player p1;
    private final Player p2;
    private final Inventory invP1;
    private final Inventory invP2;

    private boolean p1Ready = false;
    private boolean p2Ready = false;
    private boolean locked = false;

    private long p1Money = 0;
    private long p2Money = 0;
    public boolean p1Typing = false;
    public boolean p2Typing = false;

    // LEFT side slots
    public static final int[] LEFT_SLOTS = {0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30};
    // RIGHT side slots
    public static final int[] RIGHT_SLOTS = {5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35};
    // Divider slots
    private static final int[] DIVIDER_SLOTS = {4, 13, 22, 31, 40, 49};
    
    // Control slots
    public static final int LEFT_ACCEPT_SLOT = 45;
    public static final int LEFT_CANCEL_SLOT = 46;
    public static final int LEFT_MONEY_SLOT = 47;
    public static final int RIGHT_ACCEPT_SLOT = 53;
    public static final int RIGHT_CANCEL_SLOT = 52;
    public static final int RIGHT_MONEY_SLOT = 51;

    public TradeSession(Player p1, Player p2) {
        this.p1 = p1;
        this.p2 = p2;
        this.invP1 = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', "&8Trade: &4" + p2.getName()));
        this.invP2 = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', "&8Trade: &4" + p1.getName()));
        setupBaseInventory(invP1);
        setupBaseInventory(invP2);
        updateButtons();
    }

    private void setupBaseInventory(Inventory inv) {
        ItemStack divider = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = divider.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + "|");
            divider.setItemMeta(meta);
        }
        for (int slot : DIVIDER_SLOTS) {
            inv.setItem(slot, divider);
        }

        ItemStack bottomFiller = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bottomMeta = bottomFiller.getItemMeta();
        if (bottomMeta != null) {
            bottomMeta.setDisplayName(" ");
            bottomFiller.setItemMeta(bottomMeta);
        }
        for (int i = 45; i < 54; i++) {
            if (i != LEFT_ACCEPT_SLOT && i != LEFT_CANCEL_SLOT && i != LEFT_MONEY_SLOT &&
                i != RIGHT_ACCEPT_SLOT && i != RIGHT_CANCEL_SLOT && i != RIGHT_MONEY_SLOT && i != 49) {
                inv.setItem(i, bottomFiller);
            }
        }
    }

    public void syncInventories() {
        if (locked) return;
        // P1's items are in invP1 LEFT -> sync to invP2 RIGHT
        for (int i = 0; i < 16; i++) {
            invP2.setItem(RIGHT_SLOTS[i], invP1.getItem(LEFT_SLOTS[i]));
        }
        // P2's items are in invP2 LEFT -> sync to invP1 RIGHT
        for (int i = 0; i < 16; i++) {
            invP1.setItem(RIGHT_SLOTS[i], invP2.getItem(LEFT_SLOTS[i]));
        }
    }

    public void updateButtons() {
        // invP1 (Viewer: P1, Left: P1, Right: P2)
        invP1.setItem(LEFT_ACCEPT_SLOT, createButton(p1Ready, p1.getName()));
        invP1.setItem(LEFT_CANCEL_SLOT, createCancelButton(p1.getName()));
        invP1.setItem(LEFT_MONEY_SLOT, createMoneyButton(p1.getName(), p1Money));
        invP1.setItem(RIGHT_ACCEPT_SLOT, createButton(p2Ready, p2.getName()));
        invP1.setItem(RIGHT_CANCEL_SLOT, createCancelButton(p2.getName()));
        invP1.setItem(RIGHT_MONEY_SLOT, createMoneyButton(p2.getName(), p2Money));

        // invP2 (Viewer: P2, Left: P2, Right: P1)
        invP2.setItem(LEFT_ACCEPT_SLOT, createButton(p2Ready, p2.getName()));
        invP2.setItem(LEFT_CANCEL_SLOT, createCancelButton(p2.getName()));
        invP2.setItem(LEFT_MONEY_SLOT, createMoneyButton(p2.getName(), p2Money));
        invP2.setItem(RIGHT_ACCEPT_SLOT, createButton(p1Ready, p1.getName()));
        invP2.setItem(RIGHT_CANCEL_SLOT, createCancelButton(p1.getName()));
        invP2.setItem(RIGHT_MONEY_SLOT, createMoneyButton(p1.getName(), p1Money));
    }

    private ItemStack createMoneyButton(String owner, long amount) {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Uang Trade");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Milik: " + ChatColor.WHITE + owner);
            lore.add(ChatColor.GRAY + "Nominal: " + ChatColor.YELLOW + "Rp " + amount);
            lore.add("");
            lore.add(ChatColor.GREEN + "Klik untuk mengubah nominal!");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createButton(boolean isReady, String owner) {
        ItemStack item = new ItemStack(isReady ? Material.LIME_WOOL : Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', isReady ? "&a&lSIAP" : "&c&lBELUM SIAP"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Milik: " + ChatColor.WHITE + owner);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCancelButton(String owner) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lBATAL"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Milik: " + ChatColor.WHITE + owner);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open(Player p) {
        if (p.getUniqueId().equals(p1.getUniqueId())) {
            p.openInventory(invP1);
        } else if (p.getUniqueId().equals(p2.getUniqueId())) {
            p.openInventory(invP2);
        }
    }

    public void open() {
        open(p1);
        open(p2);
    }

    public void closeSafely() {
        locked = true;
        if (p1.getOpenInventory().getTopInventory().equals(invP1)) p1.closeInventory();
        if (p2.getOpenInventory().getTopInventory().equals(invP2)) p2.closeInventory();
    }

    public void returnItems() {
        for (int slot : LEFT_SLOTS) {
            ItemStack item = invP1.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                giveItemOrDrop(p1, item);
                invP1.setItem(slot, null);
            }
        }
        for (int slot : LEFT_SLOTS) {
            ItemStack item = invP2.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                giveItemOrDrop(p2, item);
                invP2.setItem(slot, null);
            }
        }
    }

    public void finishTrade() {
        locked = true;
        java.util.List<String> p1ItemsLog = new java.util.ArrayList<>();
        java.util.List<String> p2ItemsLog = new java.util.ArrayList<>();

        // Give P1's items to P2
        for (int slot : LEFT_SLOTS) {
            ItemStack item = invP1.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                p1ItemsLog.add(item.getAmount() + "x " + item.getType().name());
                giveItemOrDrop(p2, item);
                invP1.setItem(slot, null);
            }
        }
        // Give P2's items to P1
        for (int slot : LEFT_SLOTS) {
            ItemStack item = invP2.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                p2ItemsLog.add(item.getAmount() + "x " + item.getType().name());
                giveItemOrDrop(p1, item);
                invP2.setItem(slot, null);
            }
        }

        RumahKitaEconomyRupiahPlugin eco = RumahKitaEconomyRupiahPlugin.getInstance();
        if (p1Money > 0) {
            eco.addBalance(p2.getUniqueId(), p1Money);
            eco.takeBalance(p1.getUniqueId(), p1Money);
        }
        if (p2Money > 0) {
            eco.addBalance(p1.getUniqueId(), p2Money);
            eco.takeBalance(p2.getUniqueId(), p2Money);
        }

        p1.sendMessage(ChatColor.GREEN + "Trade dengan " + p2.getName() + " berhasil diselesaikan!");
        p2.sendMessage(ChatColor.GREEN + "Trade dengan " + p1.getName() + " berhasil diselesaikan!");
        p1.playSound(p1.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        p2.playSound(p2.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        try {
            java.io.File logFile = new java.io.File(RumahKitaTradePlugin.getInstance().getDataFolder(), "trades.log");
            if (!logFile.exists()) {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            }
            java.io.FileWriter fw = new java.io.FileWriter(logFile, true);
            java.io.PrintWriter pw = new java.io.PrintWriter(fw);
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            pw.println("[" + timestamp + "] TRADE COMPLETED:");
            pw.println("Player 1: " + p1.getName() + " | Uang: Rp " + p1Money + " | Item: " + String.join(", ", p1ItemsLog));
            pw.println("Player 2: " + p2.getName() + " | Uang: Rp " + p2Money + " | Item: " + String.join(", ", p2ItemsLog));
            pw.println("-------------------------------------------------");
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        closeSafely();
    }

    private void giveItemOrDrop(Player p, ItemStack item) {
        if (!p.getInventory().addItem(item).isEmpty()) {
            p.getWorld().dropItemNaturally(p.getLocation(), item);
            p.sendMessage(ChatColor.RED + "Inventory kamu penuh! Item telah dijatuhkan ke tanah.");
        }
    }

    public boolean isP1(Player p) {
        return p.getUniqueId().equals(p1.getUniqueId());
    }

    public boolean isP2(Player p) {
        return p.getUniqueId().equals(p2.getUniqueId());
    }

    public boolean isLeftSlot(int slot) {
        for (int s : LEFT_SLOTS) if (s == slot) return true;
        return false;
    }

    public long getP1Money() { return p1Money; }
    public void setP1Money(long money) { this.p1Money = money; updateButtons(); }
    public long getP2Money() { return p2Money; }
    public void setP2Money(long money) { this.p2Money = money; updateButtons(); }

    public Player getP1() { return p1; }
    public Player getP2() { return p2; }
    public Inventory getInvP1() { return invP1; }
    public Inventory getInvP2() { return invP2; }
    
    public void toggleReady(Player p) {
        if (locked) return;
        if (isP1(p)) {
            p1Ready = !p1Ready;
        } else if (isP2(p)) {
            p2Ready = !p2Ready;
        }
        p1.playSound(p1.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        p2.playSound(p2.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        updateButtons();
    }
    
    public void resetReady() {
        p1Ready = false;
        p2Ready = false;
        updateButtons();
    }

    public boolean isBothReady() {
        return p1Ready && p2Ready;
    }

    public boolean isLocked() {
        return locked;
    }
    
    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
