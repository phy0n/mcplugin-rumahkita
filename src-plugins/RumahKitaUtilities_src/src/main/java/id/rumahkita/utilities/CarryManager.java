/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityDismountEvent
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerTeleportEvent
 *  org.bukkit.event.player.PlayerToggleSneakEvent
 *  org.bukkit.plugin.Plugin
 */
package id.rumahkita.utilities;

import id.rumahkita.utilities.RumahKitaUtilitiesPlugin;
import id.rumahkita.utilities.Text;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.Plugin;

public final class CarryManager
implements Listener,
TabExecutor {
    private final RumahKitaUtilitiesPlugin plugin;
    private final Map<UUID, UUID> carriedToCarrier = new HashMap<UUID, UUID>();
    private final Map<UUID, UUID> carrierToCarried = new HashMap<UUID, UUID>();

    public CarryManager(RumahKitaUtilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled=true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!this.plugin.getConfig().getBoolean("carry.enabled", true)) {
            return;
        }
        if (!this.plugin.getConfig().getBoolean("carry.sneak-right-click-entity", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("rumahkita.carry.use")) {
            return;
        }
        if (!player.isSneaking()) {
            return;
        }
        if (!this.isWorldAllowed(player.getWorld())) {
            return;
        }
        Entity target = event.getRightClicked();
        if (target.getUniqueId().equals(player.getUniqueId())) {
            return;
        }
        if (!this.canCarry(player, target, true)) {
            return;
        }
        event.setCancelled(true);
        this.carry(player, target);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        if (this.plugin.getConfig().getBoolean("carry.drop-when-carrier-sneak", true) && this.carrierToCarried.containsKey(player.getUniqueId())) {
            this.dropByCarrier(player.getUniqueId(), true);
        }
        if (this.plugin.getConfig().getBoolean("carry.drop-when-passenger-sneak", true) && this.carriedToCarrier.containsKey(player.getUniqueId())) {
            UUID carrier = this.carriedToCarrier.get(player.getUniqueId());
            this.dropByCarrier(carrier, true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.dropByCarrier(event.getPlayer().getUniqueId(), false);
        UUID carrier = this.carriedToCarrier.get(event.getPlayer().getUniqueId());
        if (carrier != null) {
            this.dropByCarrier(carrier, false);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        this.dropByCarrier(event.getPlayer().getUniqueId(), false);
        UUID carrier = this.carriedToCarrier.get(event.getPlayer().getUniqueId());
        if (carrier != null) {
            this.dropByCarrier(carrier, false);
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        UUID carried = event.getEntity().getUniqueId();
        UUID carrier = this.carriedToCarrier.get(carried);
        if (carrier != null) {
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.dropByCarrier(carrier, false));
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onDamage(EntityDamageEvent event) {
        if (!this.plugin.getConfig().getBoolean("carry.cancel-damage-while-carrying", false)) {
            return;
        }
        UUID id = event.getEntity().getUniqueId();
        if (this.carriedToCarrier.containsKey(id) || this.carrierToCarried.containsKey(id)) {
            event.setCancelled(true);
        }
    }

    private boolean canCarry(Player carrier, Entity target, boolean sendMessage) {
        if (!this.plugin.getConfig().getBoolean("carry.enabled", true)) {
            if (sendMessage) {
                this.msg(carrier, "disabled");
            }
            return false;
        }
        if (!this.isWorldAllowed(carrier.getWorld())) {
            return false;
        }
        if (!carrier.getWorld().equals((Object)target.getWorld())) {
            return false;
        }
        double max = this.plugin.getConfig().getDouble("carry.max-distance", 4.0);
        if (carrier.getLocation().distanceSquared(target.getLocation()) > max * max) {
            if (sendMessage) {
                this.msg(carrier, "too-far");
            }
            return false;
        }
        if (target instanceof Player && !this.plugin.getConfig().getBoolean("carry.allow-carry-player", true)) {
            if (sendMessage) {
                this.msg(carrier, "blocked");
            }
            return false;
        }
        if (!(target instanceof Player) && !this.plugin.getConfig().getBoolean("carry.allow-carry-entity", true)) {
            if (sendMessage) {
                this.msg(carrier, "blocked");
            }
            return false;
        }
        if (!(target instanceof Player) && !this.isEntityTypeAllowed(target.getType())) {
            if (sendMessage) {
                this.msg(carrier, "too-heavy");
            }
            return false;
        }
        if (!this.canCarryInLand(carrier, target)) {
            if (sendMessage) {
                this.msg(carrier, "land-denied");
            }
            return false;
        }
        if (this.carrierToCarried.containsKey(carrier.getUniqueId()) || this.carriedToCarrier.containsKey(carrier.getUniqueId())) {
            if (sendMessage) {
                Text.msg((CommandSender)carrier, "&cKamu sedang carry / sedang digendong.");
            }
            return false;
        }
        if (this.carriedToCarrier.containsKey(target.getUniqueId())) {
            if (sendMessage) {
                Text.msg((CommandSender)carrier, "&cTarget sedang digendong orang lain.");
            }
            return false;
        }
        return true;
    }

    private boolean isEntityTypeAllowed(EntityType type) {
        List blocked = this.plugin.getConfig().getStringList("carry.blocked-entity-types");
        if (this.containsIgnoreCase(blocked, type.name())) {
            return false;
        }
        boolean whitelistEnabled = this.plugin.getConfig().getBoolean("carry.allowed-entity-whitelist-enabled", true);
        if (!whitelistEnabled) {
            return true;
        }
        List allowed = this.plugin.getConfig().getStringList("carry.allowed-entity-types");
        return this.containsIgnoreCase(allowed, type.name());
    }

    private boolean canCarryInLand(Player player, Entity target) {
        if (target instanceof Player) {
            return true;
        }
        if (player.hasPermission("rumahkita.carry.bypassland") || player.hasPermission("rumahkita.carry.admin") || player.hasPermission("rumahkita.utilities.admin")) {
            return true;
        }
        if (!this.plugin.getConfig().getBoolean("carry.protection.enabled", true)) {
            return true;
        }
        if (this.plugin.getConfig().getBoolean("carry.protection.griefprevention.enabled", true) && Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
            ProtectionResult gp = this.checkGriefPrevention(player, target.getLocation());
            if (gp == ProtectionResult.ALLOW) {
                return true;
            }
            if (gp == ProtectionResult.DENY) {
                return false;
            }
            return !this.plugin.getConfig().getBoolean("carry.protection.deny-if-hook-error", true);
        }
        return true;
    }

    private ProtectionResult checkGriefPrevention(Player player, Location loc) {
        try {
            Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            Field instanceField = gpClass.getField("instance");
            Object gp = instanceField.get(null);
            if (gp == null) {
                return ProtectionResult.UNKNOWN;
            }
            Field dataStoreField = gpClass.getField("dataStore");
            Object dataStore = dataStoreField.get(gp);
            if (dataStore == null) {
                return ProtectionResult.UNKNOWN;
            }
            Method getClaimAt = dataStore.getClass().getMethod("getClaimAt", Location.class, Boolean.TYPE, Class.forName("me.ryanhamshire.GriefPrevention.Claim"));
            Object claim = getClaimAt.invoke(dataStore, loc, true, null);
            if (claim == null) {
                return ProtectionResult.ALLOW;
            }
            try {
                Method allowContainers = claim.getClass().getMethod("allowContainers", Player.class);
                Object result = allowContainers.invoke(claim, player);
                return result == null ? ProtectionResult.ALLOW : ProtectionResult.DENY;
            }
            catch (NoSuchMethodException allowContainers) {
                try {
                    UUID uuid;
                    Field owner = claim.getClass().getField("ownerID");
                    Object ownerId = owner.get(claim);
                    if (ownerId instanceof UUID && (uuid = (UUID)ownerId).equals(player.getUniqueId())) {
                        return ProtectionResult.ALLOW;
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
                return ProtectionResult.DENY;
            }
        }
        catch (Throwable t) {
            if (this.plugin.getConfig().getBoolean("carry.protection.debug", false)) {
                this.plugin.getLogger().warning("GriefPrevention hook error: " + t.getClass().getSimpleName() + " " + t.getMessage());
            }
            return ProtectionResult.UNKNOWN;
        }
    }

    private boolean containsIgnoreCase(List<String> list, String value) {
        for (String s : list) {
            if (!s.equalsIgnoreCase(value)) continue;
            return true;
        }
        return false;
    }

    private void carry(Player carrier, Entity target) {
        target.leaveVehicle();
        if (carrier.addPassenger(target)) {
            this.carrierToCarried.put(carrier.getUniqueId(), target.getUniqueId());
            this.carriedToCarrier.put(target.getUniqueId(), carrier.getUniqueId());
            if (target instanceof Player) {
                Player p = (Player)target;
                Text.msg((CommandSender)carrier, this.message("picked-player").replace("%target%", p.getName()));
                Text.msg((CommandSender)p, "&8[&dCarry&8] &7Kamu sedang digendong oleh &f" + carrier.getName() + "&7. Shift untuk turun.");
            } else {
                Text.msg((CommandSender)carrier, this.message("picked-entity").replace("%target%", target.getType().name()));
            }
        } else {
            Text.msg((CommandSender)carrier, "&cGagal carry target.");
        }
    }

    private void dropByCarrier(UUID carrierId, boolean notify) {
        Player p;
        UUID carriedId = this.carrierToCarried.remove(carrierId);
        if (carriedId == null) {
            return;
        }
        this.carriedToCarrier.remove(carriedId);
        Entity carrier = Bukkit.getEntity((UUID)carrierId);
        Entity carried = Bukkit.getEntity((UUID)carriedId);
        if (carried != null) {
            carried.leaveVehicle();
            if (carrier != null) {
                carried.teleport(carrier.getLocation());
            }
        }
        if (notify && carrier instanceof Player) {
            p = (Player)carrier;
            this.msg(p, "dropped");
        }
        if (notify && carried instanceof Player) {
            p = (Player)carried;
            this.msg(p, "dropped");
        }
    }

    public void dropAll() {
        for (UUID carrier : new ArrayList<UUID>(this.carrierToCarried.keySet())) {
            this.dropByCarrier(carrier, false);
        }
        this.carriedToCarrier.clear();
        this.carrierToCarried.clear();
    }

    private boolean isWorldAllowed(World world) {
        List worlds = this.plugin.getConfig().getStringList("carry.allowed-worlds");
        return worlds.isEmpty() || worlds.contains(world.getName());
    }

    private String message(String key) {
        return Text.color(this.plugin.getConfig().getString("carry.messages." + key, key));
    }

    private void msg(Player player, String key) {
        Text.msg((CommandSender)player, this.plugin.getConfig().getString("carry.messages." + key, key));
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean adminCmd = label.equalsIgnoreCase("rkcarry");
        if (adminCmd && !sender.hasPermission("rumahkita.carry.admin") && !sender.hasPermission("rumahkita.utilities.admin")) {
            Text.msg(sender, this.plugin.getConfig().getString("carry.messages.no-permission", "&cKamu tidak punya permission."));
            return true;
        }
        if (!adminCmd && !sender.hasPermission("rumahkita.carry.use")) {
            Text.msg(sender, this.plugin.getConfig().getString("carry.messages.no-permission", "&cKamu tidak punya permission."));
            return true;
        }
        if (args.length == 0) {
            Text.msg(sender, "&e/carry <player|drop>");
            Text.msg(sender, "&eSneak + klik kanan entity untuk carry mob/player.");
            return true;
        }
        if (args[0].equalsIgnoreCase("drop")) {
            if (!(sender instanceof Player)) {
                Text.msg(sender, this.plugin.getConfig().getString("carry.messages.only-player", "&cCommand ini hanya bisa dipakai player."));
                return true;
            }
            Player p = (Player)sender;
            this.dropByCarrier(p.getUniqueId(), true);
            UUID carrier = this.carriedToCarrier.get(p.getUniqueId());
            if (carrier != null) {
                this.dropByCarrier(carrier, true);
            }
            return true;
        }
        if (adminCmd && args[0].equalsIgnoreCase("reload")) {
            this.plugin.reloadAll();
            Text.msg(sender, this.plugin.getConfig().getString("messages.prefix", "") + this.plugin.getConfig().getString("messages.reloaded", "&aConfig berhasil direload."));
            return true;
        }
        if (adminCmd && args[0].equalsIgnoreCase("toggle")) {
            boolean now = !this.plugin.getConfig().getBoolean("carry.enabled", true);
            this.plugin.getConfig().set("carry.enabled", (Object)now);
            this.plugin.saveConfig();
            Text.msg(sender, this.plugin.getConfig().getString("messages.prefix", "") + (now ? "&aCarry ON." : "&cCarry OFF."));
            return true;
        }
        if (!(sender instanceof Player)) {
            Text.msg(sender, this.plugin.getConfig().getString("carry.messages.only-player", "&cCommand ini hanya bisa dipakai player."));
            return true;
        }
        Player player = (Player)sender;
        Player target = Bukkit.getPlayerExact((String)args[0]);
        if (target == null) {
            this.msg(player, "not-found");
            return true;
        }
        if (this.canCarry(player, (Entity)target, true)) {
            this.carry(player, (Entity)target);
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            ArrayList<String> base = new ArrayList<String>();
            base.add("drop");
            if (alias.equalsIgnoreCase("rkcarry")) {
                base.add("toggle");
                base.add("reload");
            }
            base.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            return base;
        }
        return List.of();
    }

    private static enum ProtectionResult {
        ALLOW,
        DENY,
        UNKNOWN;

    }
}

