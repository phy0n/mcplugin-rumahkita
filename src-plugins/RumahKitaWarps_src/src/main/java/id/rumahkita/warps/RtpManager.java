package id.rumahkita.warps;

import id.rumahkita.economy.RumahKitaEconomyRupiahPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RtpManager {
    private final RumahKitaWarpsPlugin plugin;
    private final Map<UUID, Long> rtpCooldowns = new HashMap<>();
    private final long cooldownTime = 60000; 
    private final Random random = new Random();

    public RtpManager(RumahKitaWarpsPlugin plugin) {
        this.plugin = plugin;
    }

    private String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', "&8[&b&lRTP&8] &r");
    }

    public void randomTeleport(Player p) {
        World.Environment env = p.getWorld().getEnvironment();
        String worldName = p.getWorld().getName().toLowerCase();
        
        if (env == World.Environment.THE_END || env == World.Environment.NETHER || worldName.contains("nether") || worldName.contains("end")) {
            p.sendMessage(getPrefix() + ChatColor.RED + "You cannot use RTP in this dimension!");
            return;
        }

        if (rtpCooldowns.containsKey(p.getUniqueId())) {
            long timePassed = System.currentTimeMillis() - rtpCooldowns.get(p.getUniqueId());
            if (timePassed < cooldownTime) {
                int secondsLeft = (int) ((cooldownTime - timePassed) / 1000);
                p.sendTitle(ChatColor.translateAlternateColorCodes('&', "&c&lWait!"), ChatColor.YELLOW + "Cooldown: " + secondsLeft + " seconds", 5, 20, 5);
                p.sendMessage(getPrefix() + ChatColor.RED + "Wait " + secondsLeft + " seconds before RTP again.");
                return;
            }
        }

        long rtpCost = plugin.getConfig().getLong("rtp.cost", 1000);

        RumahKitaEconomyRupiahPlugin economy = RumahKitaEconomyRupiahPlugin.getInstance();
        if (economy.getBalance(p.getUniqueId()) < rtpCost) {
            p.sendMessage(getPrefix() + ChatColor.RED + "Not enough money! RTP cost is " + economy.formatRp(rtpCost));
            return;
        }

        p.sendMessage(getPrefix() + ChatColor.YELLOW + "Searching for a safe location...");

        new BukkitRunnable() {
            int attempts = 0;
            
            @Override
            public void run() {
                Location loc = findSafeLocation(p.getWorld());
                if (loc != null) {
                    this.cancel();
                    
                    if (!p.isOnline()) return;
                    
                    if (economy.getBalance(p.getUniqueId()) < rtpCost) {
                        p.sendMessage(getPrefix() + ChatColor.RED + "Not enough money to pay for RTP.");
                        return;
                    }
                    
                    economy.takeBalance(p.getUniqueId(), rtpCost);
                    rtpCooldowns.put(p.getUniqueId(), System.currentTimeMillis());
                    teleportWithCountdown(p, loc, rtpCost);
                    
                } else {
                    attempts++;
                    if (attempts > 10) {
                        this.cancel();
                        p.sendMessage(getPrefix() + ChatColor.RED + "Failed to find a safe location, try again.");
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
    
    private Location findSafeLocation(World world) {
        int radius = plugin.getConfig().getInt("rtp.radius", 3000);
        int x = random.nextInt(radius * 2) - radius;
        int z = random.nextInt(radius * 2) - radius;
        
        int y;
        if (world.getEnvironment() == World.Environment.NETHER) {
            y = -1;
            for (int i = 110; i > 32; i--) {
                Block b = world.getBlockAt(x, i, z);
                Block a1 = world.getBlockAt(x, i + 1, z);
                Block a2 = world.getBlockAt(x, i + 2, z);
                if (isSafeBlock(b.getType()) && a1.getType().isAir() && a2.getType().isAir()) {
                    y = i;
                    break;
                }
            }
            if (y == -1) return null;
        } else {
            y = world.getHighestBlockYAt(x, z);
            if (y <= 0) return null;
        }
        
        Block highest = world.getBlockAt(x, y, z);
        Block above = world.getBlockAt(x, y + 1, z);
        Block above2 = world.getBlockAt(x, y + 2, z);
        
        if (isSafeBlock(highest.getType()) && above.getType().isAir() && above2.getType().isAir()) {
            return new Location(world, x + 0.5, y + 1.0, z + 0.5);
        }
        return null;
    }
    
    private boolean isSafeBlock(Material mat) {
        if (mat.isInteractable() || mat == Material.LAVA || mat == Material.WATER || mat == Material.BEDROCK) return false;
        if (mat == Material.CACTUS || mat == Material.MAGMA_BLOCK || mat == Material.FIRE || mat == Material.CAMPFIRE) return false;
        return mat.isSolid();
    }

    private void teleportWithCountdown(Player p, Location targetLoc, long rtpCost) {
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
                    p.sendMessage(getPrefix() + ChatColor.RED + "RTP cancelled because you moved.");
                    this.cancel();
                    RumahKitaEconomyRupiahPlugin.getInstance().addBalance(p.getUniqueId(), rtpCost);
                    p.sendMessage(getPrefix() + ChatColor.GRAY + "Your money " + RumahKitaEconomyRupiahPlugin.getInstance().formatRp(rtpCost) + " was refunded.");
                    return;
                }

                if (countdown > 0) {
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    p.sendTitle(ChatColor.translateAlternateColorCodes('&', "&a&l" + countdown), ChatColor.YELLOW + "Do not move!", 0, 25, 0);
                    countdown--;
                } else {
                    p.teleport(targetLoc);
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    p.sendTitle(ChatColor.translateAlternateColorCodes('&', "&a&lWUSHH!"), ChatColor.YELLOW + "Successfully RTP to random location!", 5, 20, 10);
                    p.sendMessage(getPrefix() + ChatColor.GREEN + "Successfully RTP to random coordinates!");
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
