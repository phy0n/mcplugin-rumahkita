package id.rumahkita.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementListener implements Listener {

    private final PhyAntiCheat plugin;

    public MovementListener(PhyAntiCheat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        String bypassPerm = plugin.getConfig().getString("settings.bypass-permission", "phyanticheat.bypass");

        if (p.hasPermission(bypassPerm) || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR || p.isFlying() || p.isGliding()) return;

        Location from = e.getFrom();
        Location to = e.getTo();

        if (to == null || from.getWorld() != to.getWorld()) return;

        double distance = from.distance(to);
        
        // Anti Speed / Fly
        if (plugin.getConfig().getBoolean("modules.movement.anti-speed-fly", true)) {
            double maxDist = plugin.getConfig().getDouble("modules.movement.max-distance-per-tick", 1.5);
            if (distance > maxDist && p.getVelocity().length() < 1.0) {
                e.setTo(from);
                alertAndLog(p, "Irregular Movement (Speed/Fly)", "Distance: " + String.format("%.2f", distance));
            }
        }

        // Anti-Jesus (Water Walk)
        if (plugin.getConfig().getBoolean("modules.movement.anti-jesus", true)) {
            if (to.getBlock().getType().toString().contains("WATER") && !p.isSwimming() && !p.isInsideVehicle()) {
                // If standing exactly on top of water (y % 1 == 0 or very close)
                if (to.getY() % 1.0 < 0.1 && to.clone().subtract(0, 0.1, 0).getBlock().getType().toString().contains("WATER")) {
                    e.setTo(from.clone().subtract(0, 1.0, 0)); // Pull them down into the water
                    alertAndLog(p, "Irregular Movement (Jesus/WaterWalk)", "Walking on water");
                }
            }
        }
    }

    private void alertAndLog(Player p, String module, String details) {
        // Prevent chat spam, 10% chance to alert chat for movement
        if (Math.random() < 0.1) {
            String prefix = org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("settings.prefix", "&8[&cPAC&8] &7"));
            String msg = prefix + org.bukkit.ChatColor.YELLOW + p.getName() + org.bukkit.ChatColor.GRAY + " flagged for: " + org.bukkit.ChatColor.RED + module;
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission(plugin.getConfig().getString("settings.alert-permission", "phyanticheat.admin"))) {
                    admin.sendMessage(msg);
                }
            }
        }
        plugin.getLogManager().logViolation(p.getName(), module, details);
    }
}
