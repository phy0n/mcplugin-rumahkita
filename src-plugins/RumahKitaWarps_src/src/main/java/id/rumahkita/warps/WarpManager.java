package id.rumahkita.warps;

import id.rumahkita.economy.RumahKitaEconomyRupiahPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WarpManager implements Listener {

    private final RumahKitaWarpsPlugin plugin;
    private File warpsFile;
    private FileConfiguration warpsConfig;
    private final Map<String, PlayerWarp> warps = new HashMap<>();

    public WarpManager(RumahKitaWarpsPlugin plugin) {
        this.plugin = plugin;
        setupFiles();
    }

    private void setupFiles() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        warpsFile = new File(plugin.getDataFolder(), "warps.yml");
        if (!warpsFile.exists()) {
            try {
                warpsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);
    }

    public void loadWarps() {
        warps.clear();
        ConfigurationSection section = warpsConfig.getConfigurationSection("warps");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String name = section.getString(key + ".name");
                String ownerIdStr = section.getString(key + ".owner");
                if (ownerIdStr == null) continue;
                UUID owner = UUID.fromString(ownerIdStr);
                String world = section.getString(key + ".world");
                double x = section.getDouble(key + ".x");
                double y = section.getDouble(key + ".y");
                double z = section.getDouble(key + ".z");
                float yaw = (float) section.getDouble(key + ".yaw");
                float pitch = (float) section.getDouble(key + ".pitch");
                
                Location loc = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
                warps.put(name.toLowerCase(), new PlayerWarp(name, owner, loc));
            }
        }
        plugin.getLogger().info("Loaded " + warps.size() + " warps.");
    }

    public void saveWarps() {
        warpsConfig.set("warps", null);
        for (PlayerWarp warp : warps.values()) {
            String path = "warps." + warp.name.toLowerCase();
            warpsConfig.set(path + ".name", warp.name);
            warpsConfig.set(path + ".owner", warp.owner.toString());
            warpsConfig.set(path + ".world", warp.location.getWorld().getName());
            warpsConfig.set(path + ".x", warp.location.getX());
            warpsConfig.set(path + ".y", warp.location.getY());
            warpsConfig.set(path + ".z", warp.location.getZ());
            warpsConfig.set(path + ".yaw", warp.location.getYaw());
            warpsConfig.set(path + ".pitch", warp.location.getPitch());
        }
        try {
            warpsConfig.save(warpsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean createWarp(Player p, String name) {
        String key = name.toLowerCase();
        if (warps.containsKey(key)) {
            p.sendMessage(getPrefix() + ChatColor.RED + "Nama warp '" + name + "' sudah dipakai! Silakan pilih nama lain.");
            return false;
        }

        for (PlayerWarp w : warps.values()) {
            if (w.owner.equals(p.getUniqueId())) {
                p.sendMessage(getPrefix() + ChatColor.RED + "Kamu sudah memiliki warp! Hapus dulu dengan /pwarp delete " + w.name);
                return false;
            }
        }

        long cost = plugin.getConfig().getLong("cost.create", 50000);
        RumahKitaEconomyRupiahPlugin economy = RumahKitaEconomyRupiahPlugin.getInstance();
        
        if (economy.getBalance(p.getUniqueId()) < cost) {
            p.sendMessage(getPrefix() + ChatColor.RED + "Uangmu tidak cukup! Biaya membuat pwarp adalah " + economy.formatRp(cost));
            return false;
        }

        economy.takeBalance(p.getUniqueId(), cost);
        warps.put(key, new PlayerWarp(name, p.getUniqueId(), p.getLocation()));
        saveWarps();

        p.sendMessage(getPrefix() + ChatColor.GREEN + "Berhasil membuat warp " + ChatColor.YELLOW + name + ChatColor.GREEN + " dengan biaya " + economy.formatRp(cost) + "!");
        return true;
    }

    public boolean deleteWarp(Player p, String name) {
        String key = name.toLowerCase();
        PlayerWarp warp = warps.get(key);
        
        if (warp == null) {
            p.sendMessage(getPrefix() + ChatColor.RED + "Warp tidak ditemukan.");
            return false;
        }

        if (!warp.owner.equals(p.getUniqueId()) && !p.hasPermission("pwarp.admin")) {
            p.sendMessage(getPrefix() + ChatColor.RED + "Ini bukan warp milikmu.");
            return false;
        }

        warps.remove(key);
        saveWarps();
        p.sendMessage(getPrefix() + ChatColor.GREEN + "Warp " + ChatColor.YELLOW + warp.name + ChatColor.GREEN + " telah dihapus.");
        return true;
    }

    public void openWarpMenu(Player p) {
        int size = 54;
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.DARK_GRAY + "Player Warps");

        for (PlayerWarp warp : warps.values()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(warp.owner);
                meta.setOwningPlayer(owner);
                meta.setDisplayName(ChatColor.YELLOW + warp.name);
                
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Pemilik: " + ChatColor.WHITE + (owner.getName() != null ? owner.getName() : "Unknown"));
                lore.add("");
                lore.add(ChatColor.GREEN + "Klik untuk teleport!");
                
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            inv.addItem(head);
        }

        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals(ChatColor.DARK_GRAY + "Player Warps")) {
            e.setCancelled(true);
            if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                Player p = (Player) e.getWhoClicked();
                SkullMeta meta = (SkullMeta) e.getCurrentItem().getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String warpName = ChatColor.stripColor(meta.getDisplayName());
                    PlayerWarp warp = warps.get(warpName.toLowerCase());
                    if (warp != null) {
                        p.closeInventory();
                        teleportToWarp(p, warpName);
                    } else {
                        p.sendMessage(getPrefix() + ChatColor.RED + "Warp tidak ditemukan.");
                        p.closeInventory();
                    }
                }
            }
        }
    }

    public void teleportToWarp(Player p, String name) {
        String key = name.toLowerCase();
        PlayerWarp warp = warps.get(key);
        
        if (warp == null) {
            p.sendMessage(getPrefix() + ChatColor.RED + "Warp " + name + " tidak ditemukan.");
            return;
        }

        p.sendMessage(getPrefix() + ChatColor.YELLOW + "Teleportasi dalam 3 detik. Jangan bergerak!");
        
        Location startLoc = p.getLocation();

        new BukkitRunnable() {
            int countdown = 3;

            @Override
            public void run() {
                if (!p.isOnline()) {
                    this.cancel();
                    return;
                }

                if (startLoc.distanceSquared(p.getLocation()) > 1.0) {
                    p.sendMessage(getPrefix() + ChatColor.RED + "Teleportasi dibatalkan karena kamu bergerak.");
                    this.cancel();
                    return;
                }

                if (countdown > 0) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    p.sendMessage(getPrefix() + ChatColor.GREEN + countdown + "...");
                    countdown--;
                } else {
                    p.teleport(warp.location);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    
                    OfflinePlayer owner = Bukkit.getOfflinePlayer(warp.owner);
                    String ownerName = owner.getName() != null ? owner.getName() : "Unknown";
                    
                    p.sendTitle(
                        ChatColor.translateAlternateColorCodes('&', "&e&lWelcome to &a&l" + warp.name),
                        ChatColor.translateAlternateColorCodes('&', "&fMilik &b" + ownerName),
                        10, 60, 10
                    );
                    
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "&8[&ePWarp&8] "));
    }

    public List<String> getWarpNames() {
        List<String> names = new ArrayList<>();
        for (PlayerWarp warp : warps.values()) {
            names.add(warp.name);
        }
        return names;
    }

    private static class PlayerWarp {
        public String name;
        public UUID owner;
        public Location location;

        public PlayerWarp(String name, UUID owner, Location location) {
            this.name = name;
            this.owner = owner;
            this.location = location;
        }
    }
}
