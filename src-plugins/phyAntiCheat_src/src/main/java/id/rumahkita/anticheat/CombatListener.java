package id.rumahkita.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatListener implements Listener {

    private final PhyAntiCheat plugin;
    private final Map<UUID, Integer> clicks = new HashMap<>();
    private final Map<UUID, Long> lastClickTime = new HashMap<>();

    public CombatListener(PhyAntiCheat plugin) {
        this.plugin = plugin;
        
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getConfig().getBoolean("modules.combat.anti-autoclicker", true)) return;
            
            for (UUID uuid : clicks.keySet()) {
                int cps = clicks.get(uuid);
                int maxCps = plugin.getConfig().getInt("modules.combat.max-cps", 20);
                if (cps > maxCps) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && !p.hasPermission(plugin.getConfig().getString("settings.bypass-permission", "phyanticheat.bypass"))) {
                        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("settings.prefix", "&8[&cPAC&8] &7"));
                        String msg = prefix + ChatColor.YELLOW + p.getName() + ChatColor.GRAY + " was flagged for AutoClicker (" + ChatColor.RED + cps + " CPS" + ChatColor.GRAY + ").";
                        for (Player admin : Bukkit.getOnlinePlayers()) {
                            if (admin.hasPermission(plugin.getConfig().getString("settings.alert-permission", "phyanticheat.admin"))) {
                                admin.sendMessage(msg);
                            }
                        }
                        plugin.getLogManager().logViolation(p.getName(), "AutoClicker", cps + " CPS");
                    }
                }
            }
            clicks.clear();
        }, 20L, 20L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!plugin.getConfig().getBoolean("modules.combat.anti-autoclicker", true)) return;
        
        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            Player p = e.getPlayer();
            if (p.hasPermission(plugin.getConfig().getString("settings.bypass-permission", "phyanticheat.bypass"))) return;

            clicks.put(p.getUniqueId(), clicks.getOrDefault(p.getUniqueId(), 0) + 1);

            int cancelCps = plugin.getConfig().getInt("modules.combat.cancel-cps", 30);
            if (clicks.get(p.getUniqueId()) > cancelCps) {
                e.setCancelled(true);
            }
        }
    }
}
