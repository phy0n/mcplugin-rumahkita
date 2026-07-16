/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.inventory.InventoryMoveItemEvent
 *  org.bukkit.event.inventory.InventoryType$SlotType
 */
package id.rumahkita.fishing.listener;

import id.rumahkita.fishing.RumahKitaFishingPlugin;
import id.rumahkita.fishing.menu.FishMenuHolder;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;

public final class GuiListener
implements Listener {
    private final RumahKitaFishingPlugin plugin;

    public GuiListener(RumahKitaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FishMenuHolder)) {
            return;
        }
        event.setCancelled(true);
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getSlotType() == InventoryType.SlotType.OUTSIDE) {
            return;
        }
        this.plugin.fishMarketMenu().handleClick(player, event.getRawSlot());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof FishMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof FishMenuHolder || event.getDestination().getHolder() instanceof FishMenuHolder) {
            event.setCancelled(true);
        }
    }
}

