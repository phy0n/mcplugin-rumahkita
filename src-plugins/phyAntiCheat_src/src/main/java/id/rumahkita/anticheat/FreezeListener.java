package id.rumahkita.anticheat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FreezeListener implements Listener {

    public static final Set<UUID> frozenPlayers = new HashSet<>();

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (frozenPlayers.contains(e.getPlayer().getUniqueId())) {
            // Allow looking around, but not moving X/Y/Z
            if (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getY() != e.getTo().getY() || e.getFrom().getZ() != e.getTo().getZ()) {
                e.setTo(e.getFrom());
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (frozenPlayers.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "[PAC] You cannot interact while frozen.");
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (frozenPlayers.contains(p.getUniqueId())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) {
            Player p = (Player) e.getDamager();
            if (frozenPlayers.contains(p.getUniqueId())) {
                e.setCancelled(true);
                p.sendMessage(ChatColor.RED + "[PAC] You cannot attack while frozen.");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // Punish player for logging out while frozen?
        if (frozenPlayers.contains(e.getPlayer().getUniqueId())) {
            // Optionally, ban them or kill them. Let's just log it.
            e.getPlayer().setHealth(0); // Kill them as punishment
            org.bukkit.Bukkit.broadcast(ChatColor.RED + "[PAC] " + ChatColor.YELLOW + e.getPlayer().getName() + ChatColor.RED + " logged out while frozen! They have been killed.", "phyanticheat.admin");
        }
    }
}
