package id.rumahkita.anticheat;

import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class IllegalItemListener implements Listener {

    private final PhyAntiCheat plugin;

    public IllegalItemListener(PhyAntiCheat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!plugin.getConfig().getBoolean("modules.items.anti-illegal-enchants", true)) return;
        
        ItemStack item = e.getCurrentItem();
        if (checkAndClearIllegalItem(item)) {
            e.setCancelled(true);
            e.setCurrentItem(null); // Remove the item
            if (e.getWhoClicked() instanceof Player) {
                alertAndLog((Player) e.getWhoClicked(), "Illegal Item (Inventory)", item.getType().toString());
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!plugin.getConfig().getBoolean("modules.items.anti-illegal-enchants", true)) return;

        if (e.getEntity() instanceof Player) {
            ItemStack item = e.getItem().getItemStack();
            if (checkAndClearIllegalItem(item)) {
                e.setCancelled(true);
                e.getItem().remove();
                alertAndLog((Player) e.getEntity(), "Illegal Item (Pickup)", item.getType().toString());
            }
        }
    }

    private boolean checkAndClearIllegalItem(ItemStack item) {
        if (item == null || item.getEnchantments().isEmpty()) return false;

        int maxAllowed = plugin.getConfig().getInt("modules.items.max-enchant-level", 5);
        boolean isIllegal = false;

        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            if (entry.getValue() > maxAllowed && entry.getValue() > entry.getKey().getMaxLevel()) {
                isIllegal = true;
                break;
            }
        }
        return isIllegal;
    }

    private void alertAndLog(Player p, String module, String details) {
        if (p.hasPermission(plugin.getConfig().getString("settings.bypass-permission", "phyanticheat.bypass"))) return;
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("settings.prefix", "&8[&cPAC&8] &7"));
        String msg = prefix + ChatColor.YELLOW + p.getName() + ChatColor.GRAY + " was flagged for: " + ChatColor.RED + module;
        
        for (Player admin : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission(plugin.getConfig().getString("settings.alert-permission", "phyanticheat.admin"))) {
                admin.sendMessage(msg);
            }
        }
        
        plugin.getLogManager().logViolation(p.getName(), module, details);
    }
}
