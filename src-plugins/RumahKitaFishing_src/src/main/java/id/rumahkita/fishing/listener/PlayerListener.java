/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 */
package id.rumahkita.fishing.listener;

import id.rumahkita.fishing.RumahKitaFishingPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener
implements Listener {
    private final RumahKitaFishingPlugin plugin;

    public PlayerListener(RumahKitaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.plugin.playerDataManager().get(event.getPlayer());
        this.plugin.antiAfkManager().trackMovement(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.plugin.playerDataManager().unload(event.getPlayer());
        this.plugin.antiAfkManager().forget(event.getPlayer());
    }

    @EventHandler(ignoreCancelled=true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        this.plugin.antiAfkManager().trackMovement(event.getPlayer(), event.getTo());
    }
}

