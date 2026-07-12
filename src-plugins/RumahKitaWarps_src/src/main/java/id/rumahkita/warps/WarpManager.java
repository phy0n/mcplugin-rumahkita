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
            p.sendMessage(getPrefix() + ChatColor.RED + "Warp name '" + name + "' is already taken! Please choose another name.");
            return false;
        }

        for (PlayerWarp w : warps.values()) {
            if (w.owner.equals(p.getUniqueId())) {
                p.sendMessage(getPrefix() + ChatColor.RED + "You already have a warp! Delete it first using /pwarp delete " + w.name);
                return false;
            }
        }

        long cost = plugin.getConfig().getLong("cost.create", 50000);
        RumahKitaEconomyRupiahPlugin economy = RumahKitaEconomyRupiahPlugin.getInstance();
        
        if (economy.getBalance(p.getUniqueId()) < cost) {
            p.sendMessage(getPrefix() + ChatColor.RED + "Not enough money! Cost to create pwarp is " + economy.formatRp(cost));
            return false;
        }

        economy.takeBalance(p.getUniqueId(), cost);
        warps.put(key, new PlayerWarp(name, p.getUniqueId(), p.getLocation()));
        saveWarps();

        p.sendMessage(getPrefix() + ChatColor.GREEN + "Successfully created warp " + ChatColor.YELLOW + name + ChatColor.GREEN + " for a cost of " + economy.formatRp(cost) + "!");
        return true;
    }

    public boolean deleteWarp(Player p, String name) {
        String key = name.toLowerCase();
        PlayerWarp warp = warps.get(key);
        
        if (warp == null) {
            p.sendMessage(getPrefix() + ChatColor.RED + "Warp not found.");
            return false;
        }

        if (!warp.owner.equals(p.getUniqueId()) && !p.hasPermission("pwarp.admin")) {
            p.sendMessage(getPrefix() + ChatColor.RED + "This is not your warp.");
            return false;
        }

        warps.remove(key);
        saveWarps();
        p.sendMessage(getPrefix() + ChatColor.GREEN + "Warp " + ChatColor.YELLOW + warp.name + ChatColor.GREEN + " has been deleted.");
        return true;
    }

    public void openWarpMenu(Player p, int page) {
        int size = 54;
        String title = ChatColor.DARK_GRAY + "Player Warps - Page " + page;
        Inventory inv = Bukkit.createInventory(null, size, title);

        List<PlayerWarp> warpList = new ArrayList<>(warps.values());
        warpList.sort((w1, w2) -> w1.name.compareToIgnoreCase(w2.name));

        int maxItemsPerPage = 45;
        int totalPages = (int) Math.ceil((double) warpList.size() / maxItemsPerPage);
        if (totalPages == 0) totalPages = 1;

        if (page > totalPages) page = totalPages;

        int startIndex = (page - 1) * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, warpList.size());

        for (int i = startIndex; i < endIndex; i++) {
            PlayerWarp warp = warpList.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(warp.owner);
                meta.setOwningPlayer(owner);
                meta.setDisplayName(ChatColor.YELLOW + warp.name);
                
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Owner: " + ChatColor.WHITE + (owner.getName() != null ? owner.getName() : "Unknown"));
                lore.add("");
                lore.add(ChatColor.GREEN + "Click to teleport!");
                
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            inv.setItem(i - startIndex, head);
        }



        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            org.bukkit.inventory.meta.ItemMeta prevMeta = prev.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName(ChatColor.GREEN + "Previous Page");
                prev.setItemMeta(prevMeta);
            }
            inv.setItem(45, prev);
        }

        if (page < totalPages) {
            ItemStack next = new ItemStack(Material.ARROW);
            org.bukkit.inventory.meta.ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName(ChatColor.GREEN + "Next Page");
                next.setItemMeta(nextMeta);
            }
            inv.setItem(53, next);
        }

        ItemStack infoCreate = new ItemStack(Material.PAPER);
        org.bukkit.inventory.meta.ItemMeta metaCreate = infoCreate.getItemMeta();
        if (metaCreate != null) {
            metaCreate.setDisplayName(ChatColor.AQUA + "Create Warp");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.YELLOW + "/pwarp create <name>");
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.YELLOW + "Rp 50.000");
            metaCreate.setLore(lore);
            infoCreate.setItemMeta(metaCreate);
        }
        inv.setItem(48, infoCreate);

        ItemStack infoInfo = new ItemStack(Material.BOOK);
        org.bukkit.inventory.meta.ItemMeta metaInfo = infoInfo.getItemMeta();
        if (metaInfo != null) {
            metaInfo.setDisplayName(ChatColor.GOLD + "Pwarp Info");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Player Warp System.");
            lore.add(ChatColor.GRAY + "Type " + ChatColor.YELLOW + "/pwarp help" + ChatColor.GRAY + " for command list.");
            metaInfo.setLore(lore);
            infoInfo.setItemMeta(metaInfo);
        }
        inv.setItem(49, infoInfo);

        ItemStack infoDelete = new ItemStack(Material.BARRIER);
        org.bukkit.inventory.meta.ItemMeta metaDelete = infoDelete.getItemMeta();
        if (metaDelete != null) {
            metaDelete.setDisplayName(ChatColor.RED + "Delete Warp");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.YELLOW + "/pwarp delete <name>");
            metaDelete.setLore(lore);
            infoDelete.setItemMeta(metaDelete);
        }
        inv.setItem(50, infoDelete);

        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().startsWith(ChatColor.DARK_GRAY + "Player Warps - Page ")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            
            Player p = (Player) e.getWhoClicked();
            Material type = e.getCurrentItem().getType();
            
            if (type == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) e.getCurrentItem().getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String warpName = ChatColor.stripColor(meta.getDisplayName());
                    PlayerWarp warp = warps.get(warpName.toLowerCase());
                    if (warp != null) {
                        p.closeInventory();
                        teleportToWarp(p, warpName);
                    } else {
                        p.sendMessage(getPrefix() + ChatColor.RED + "Warp not found.");
                        p.closeInventory();
                    }
                }
            } else if (type == Material.ARROW) {
                String title = e.getView().getTitle();
                String name = e.getCurrentItem().getItemMeta().getDisplayName();
                try {
                    int currentPage = Integer.parseInt(title.split("Page ")[1]);
                    if (name.contains("Next Page")) {
                        openWarpMenu(p, currentPage + 1);
                    } else if (name.contains("Previous Page")) {
                        openWarpMenu(p, currentPage - 1);
                    }
                } catch (Exception ex) {
                    // Ignore
                }
            }
        }
    }

    public void teleportToWarp(Player p, String name) {
        String key = name.toLowerCase();
        PlayerWarp warp = warps.get(key);
        
        if (warp == null) {
            p.sendMessage(getPrefix() + ChatColor.RED + "Warp " + name + " not found.");
            return;
        }

        p.sendMessage(getPrefix() + ChatColor.YELLOW + "Teleporting in 5 seconds. Do not move!");
        
        Location startLoc = p.getLocation();

        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (!p.isOnline()) {
                    this.cancel();
                    return;
                }

                if (startLoc.distanceSquared(p.getLocation()) > 1.0) {
                    p.sendMessage(getPrefix() + ChatColor.RED + "Teleportation cancelled because you moved.");
                    this.cancel();
                    return;
                }

                if (countdown > 0) {
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    p.sendTitle(ChatColor.translateAlternateColorCodes('&', "&a&l" + countdown), ChatColor.YELLOW + "Do not move!", 0, 25, 0);
                    countdown--;
                } else {
                    p.teleport(warp.location);
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    
                    OfflinePlayer owner = Bukkit.getOfflinePlayer(warp.owner);
                    String ownerName = owner.getName() != null ? owner.getName() : "Unknown";
                    
                    p.sendTitle(
                        ChatColor.translateAlternateColorCodes('&', "&e&lWelcome to &a&l" + warp.name),
                        ChatColor.translateAlternateColorCodes('&', "&fOwner &b" + ownerName),
                        10, 60, 10
                    );
                    
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "&8[&b&lPWarp&8] "));
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
