/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Particle
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerFishEvent
 *  org.bukkit.event.player.PlayerFishEvent$State
 *  org.bukkit.inventory.ItemStack
 */
package id.rumahkita.fishing.listener;

import id.rumahkita.fishing.RumahKitaFishingPlugin;
import id.rumahkita.fishing.model.CaughtFish;
import id.rumahkita.fishing.model.FishDefinition;
import id.rumahkita.fishing.model.PlayerFishingData;
import id.rumahkita.fishing.model.Rarity;
import id.rumahkita.fishing.util.NumberUtil;
import id.rumahkita.fishing.util.Text;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

public final class FishingListener
implements Listener {
    private final RumahKitaFishingPlugin plugin;

    public FishingListener(RumahKitaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();
        Entity caught = event.getCaught();
        if (!(caught instanceof Item)) {
            return;
        }
        Item caughtItem = (Item)caught;
        this.plugin.antiAfkManager().registerCatch(player);
        if (!this.plugin.antiAfkManager().canReceiveCustomFish(player)) {
            Text.send((CommandSender)player, this.plugin.messagesConfig().get(), "afk-blocked", Map.of());
            if (this.plugin.antiAfkManager().shouldCancelWhenDetected()) {
                event.setCancelled(true);
                caught.remove();
            }
            return;
        }
        Location location = caughtItem.getLocation();
        Optional<FishDefinition> optionalFish = this.plugin.fishManager().selectFish(player, location);
        if (optionalFish.isEmpty()) {
            return;
        }
        FishDefinition definition = optionalFish.get();
        PlayerFishingData data = this.plugin.playerDataManager().get(player);
        double weight = this.plugin.fishManager().rollWeight(definition, data.level());
        int price = this.plugin.fishManager().calculatePrice(definition, weight, data.level());
        CaughtFish fish = new CaughtFish(UUID.randomUUID(), definition, weight, price, player.getUniqueId(), player.getName(), System.currentTimeMillis(), location.getBlock().getBiome().name(), player.getWorld().getName());
        ItemStack item = this.plugin.fishItemFactory().create(fish);
        caughtItem.setItemStack(item);
        caughtItem.setCustomName(Text.color(definition.rarity().color() + Text.stripColor(definition.displayName())));
        caughtItem.setCustomNameVisible(true);
        player.playSound(player.getLocation(), definition.rarity().sound(), 1.0f, 1.0f);
        Particle particle = this.resolveParticle(definition.rarity().particleNames());
        if (particle != null) {
            player.getWorld().spawnParticle(particle, player.getLocation().add(0.0, 1.0, 0.0), 24, 0.4, 0.4, 0.4, 0.02);
        }
        this.plugin.playerDataManager().addCatch(player, fish);
        Text.send((CommandSender)player, this.plugin.messagesConfig().get(), "fish-caught", Map.of("rarity_color", definition.rarity().color(), "fish_name", Text.stripColor(definition.displayName()), "weight", NumberUtil.weight(weight), "price", NumberUtil.money(price)));
        if (definition.rarity() == Rarity.LEGENDARY) {
            Bukkit.broadcastMessage((String)Text.format(this.plugin.messagesConfig().get().getString("fish-caught-broadcast-legendary"), Map.of("player", player.getName(), "fish_name", Text.stripColor(definition.displayName()), "weight", NumberUtil.weight(weight))));
        }
        if (definition.rarity() == Rarity.MYTHIC) {
            Bukkit.broadcastMessage((String)Text.format(this.plugin.messagesConfig().get().getString("fish-caught-broadcast-mythic"), Map.of("player", player.getName(), "fish_name", Text.stripColor(definition.displayName()), "weight", NumberUtil.weight(weight))));
        }
    }

    private Particle resolveParticle(String[] names) {
        if (names == null) {
            return null;
        }
        for (String name : names) {
            try {
                return Particle.valueOf((String)name);
            }
            catch (IllegalArgumentException illegalArgumentException) {
            }
        }
        return null;
    }
}

