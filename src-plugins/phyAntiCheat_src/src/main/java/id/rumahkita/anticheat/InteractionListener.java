package id.rumahkita.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;

public class InteractionListener implements Listener {

    private final PhyAntiCheat plugin;

    public InteractionListener(PhyAntiCheat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        
        if (!plugin.getConfig().getBoolean("modules.interaction.anti-freecam", true)) return;
        
        Player p = e.getPlayer();
        if (p.hasPermission(plugin.getConfig().getString("settings.bypass-permission", "phyanticheat.bypass")) || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;

        Block clicked = e.getClickedBlock();
        if (clicked == null) return;

        org.bukkit.Location eyeLoc = p.getEyeLocation();
        org.bukkit.Location blockLoc = clicked.getLocation().add(0.5, 0.5, 0.5);
        org.bukkit.util.Vector dir = blockLoc.toVector().subtract(eyeLoc.toVector()).normalize();
        double distance = eyeLoc.distance(blockLoc);

        // Tarik garis langsung dari mata ke blok yang dipukul
        RayTraceResult result = p.getWorld().rayTraceBlocks(eyeLoc, dir, distance, org.bukkit.FluidCollisionMode.NEVER, true);
        
        if (result != null && result.getHitBlock() != null) {
            Block hit = result.getHitBlock();
            // Jika mengenai blok solid lain sebelum sampai ke blok tujuan = Nembus Dinding (Freecam)
            if (!hit.getLocation().equals(clicked.getLocation()) && hit.getType().isSolid() && !hit.getType().isTransparent()) {
                e.setCancelled(true);
                String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("settings.prefix", "&8[&cPAC&8] &7"));
                String alertPerm = plugin.getConfig().getString("settings.alert-permission", "phyanticheat.admin");
                Bukkit.broadcast(prefix + ChatColor.YELLOW + p.getName() + ChatColor.GRAY + " was prevented from interacting through blocks (Freecam/Nuker).", alertPerm);
                plugin.getLogManager().logViolation(p.getName(), "Freecam/Nuker", "Interacted with " + clicked.getType() + " through " + hit.getType());
            }
        }
    }
}
