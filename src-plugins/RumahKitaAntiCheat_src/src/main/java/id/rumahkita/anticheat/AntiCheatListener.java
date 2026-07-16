/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.player.PlayerChangedWorldEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.event.player.PlayerTeleportEvent
 *  org.bukkit.event.player.PlayerToggleFlightEvent
 *  org.bukkit.event.player.PlayerVelocityEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package id.rumahkita.anticheat;

import id.rumahkita.anticheat.ExemptManager;
import id.rumahkita.anticheat.MoveUtil;
import id.rumahkita.anticheat.PlayerData;
import id.rumahkita.anticheat.RumahKitaAntiCheatPlugin;
import id.rumahkita.anticheat.Text;
import id.rumahkita.anticheat.ViolationTracker;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class AntiCheatListener
implements Listener {
    private final RumahKitaAntiCheatPlugin plugin;
    private final ExemptManager exemptManager;
    private final ViolationTracker violationTracker;
    private final Map<UUID, PlayerData> data = new HashMap<UUID, PlayerData>();

    public AntiCheatListener(RumahKitaAntiCheatPlugin plugin, ExemptManager exemptManager, ViolationTracker violationTracker) {
        this.plugin = plugin;
        this.exemptManager = exemptManager;
        this.violationTracker = violationTracker;
    }

    public void startAuditTask() {
        Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!this.plugin.isEnabledInConfig()) {
                return;
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (this.shouldSkip(player)) continue;
                this.auditIllegalFlight(player);
                this.auditSuspiciousPermissions(player);
            }
        }, 40L, Math.max(5L, this.plugin.getConfig().getLong("checks.illegal-flight.audit-every-ticks", 20L)));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        int seconds = this.plugin.getConfig().getInt("exemptions.after-join-seconds", 6);
        this.exemptManager.exempt(p, (long)seconds, "join");
        this.data.put(p.getUniqueId(), new PlayerData(p.getLocation()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.data.remove(event.getPlayer().getUniqueId());
        this.violationTracker.clear(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) {
            return;
        }
        this.exemptManager.exempt(event.getPlayer(), (long)this.plugin.getConfig().getInt("exemptions.after-teleport-seconds", 5), "teleport");
        this.data.put(event.getPlayer().getUniqueId(), new PlayerData(event.getTo()));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        this.exemptManager.exempt(event.getPlayer(), (long)this.plugin.getConfig().getInt("exemptions.after-world-change-seconds", 5), "world-change");
        this.data.put(event.getPlayer().getUniqueId(), new PlayerData(event.getPlayer().getLocation()));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        this.exemptManager.exempt(event.getPlayer(), (long)this.plugin.getConfig().getInt("exemptions.after-respawn-seconds", 5), "respawn");
        this.data.put(event.getPlayer().getUniqueId(), new PlayerData(event.getRespawnLocation()));
    }

    @EventHandler
    public void onVelocity(PlayerVelocityEvent event) {
        this.exemptManager.exempt(event.getPlayer(), (long)this.plugin.getConfig().getInt("exemptions.after-velocity-seconds", 4), "velocity");
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Player player = (Player)entity;
            this.exemptManager.exempt(player, (long)this.plugin.getConfig().getInt("exemptions.after-damage-seconds", 3), "damage");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        PlayerData d = this.data.computeIfAbsent(p.getUniqueId(), uuid -> new PlayerData(p.getLocation()));
        d.lastBlockPlaceMillis = System.currentTimeMillis();
        d.lastSafeLocation = p.getLocation().clone();
        d.lastGroundMillis = System.currentTimeMillis();
        this.exemptManager.exempt(p, (long)this.plugin.getConfig().getInt("checks.build-safe.after-block-place-seconds", 5), "block-place");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        PlayerData d = this.data.computeIfAbsent(p.getUniqueId(), uuid -> new PlayerData(p.getLocation()));
        d.lastBlockBreakMillis = System.currentTimeMillis();
        d.lastSafeLocation = p.getLocation().clone();
        this.exemptManager.exempt(p, (long)this.plugin.getConfig().getInt("checks.build-safe.after-block-break-seconds", 3), "block-break");
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player p = event.getPlayer();
        if (!this.plugin.isEnabledInConfig() || this.shouldSkip(p)) {
            return;
        }
        if (!this.plugin.getConfig().getBoolean("checks.illegal-flight.enabled", true)) {
            return;
        }
        event.setCancelled(true);
        p.setFlying(false);
        p.setAllowFlight(false);
        this.flag(p, "ILLEGAL_FLIGHT", "toggle-flight", this.plugin.getConfig().getInt("checks.illegal-flight.kick-threshold", 1), this.plugin.getConfig().getBoolean("checks.illegal-flight.kick", true));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!this.plugin.isEnabledInConfig()) {
            return;
        }
        if (event.getTo() == null) {
            return;
        }
        Player player = event.getPlayer();
        PlayerData d = this.data.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerData(event.getFrom()));
        if (event.getFrom().getWorld() != event.getTo().getWorld()) {
            this.exemptManager.exempt(player, (long)this.plugin.getConfig().getInt("exemptions.after-world-change-seconds", 5), "world-change");
            this.resetMoveData(d, event.getTo());
            return;
        }
        if (this.shouldSkip(player)) {
            this.resetMoveData(d, event.getTo());
            return;
        }
        if (this.shouldResetVL(d)) {
            this.violationTracker.clear(player.getUniqueId());
            d.lastViolationReset = System.currentTimeMillis();
            d.speedBuffer = 0;
        }
        this.auditIllegalFlight(player);
        Location from = d.lastMoveLocation == null ? event.getFrom() : d.lastMoveLocation;
        Location to = event.getTo();
        long now = System.currentTimeMillis();
        double elapsedTicks = Math.max(1.0, (double)(now - d.lastMoveMillis) / 50.0);
        if (this.isHardMovementExempt(player)) {
            this.resetMoveData(d, to);
            return;
        }
        boolean grounded = MoveUtil.isStandingOnSolid(player);
        boolean nearSolid = MoveUtil.isNearSolidForBuild(player);
        if (grounded) {
            d.airTicks = 0;
            d.stableYTicks = 0;
            d.lastGroundMillis = now;
            d.lastSafeLocation = to.clone();
        }
        if (nearSolid) {
            d.lastNearSolidMillis = now;
        }
        this.checkJesus(event, player, d, from, to);
        if (!this.isBuildSafeExempt(player, d)) {
            this.checkSpeed(event, player, d, from, to, elapsedTicks);
            this.checkFlySustain(event, player, d, from, to);
        } else {
            d.speedBuffer = Math.max(0, d.speedBuffer - 1);
            if (grounded || nearSolid) {
                d.airTicks = 0;
                d.stableYTicks = 0;
            }
        }
        d.lastMoveLocation = to.clone();
        d.lastMoveMillis = now;
        if ((grounded || nearSolid) && !MoveUtil.isOnWaterSurface(player)) {
            d.lastSafeLocation = to.clone();
        }
    }

    private void checkSpeed(PlayerMoveEvent event, Player player, PlayerData d, Location from, Location to, double elapsedTicks) {
        if (!this.plugin.getConfig().getBoolean("checks.speed.enabled", true)) {
            return;
        }
        if (MoveUtil.isLiquidNear(player) || MoveUtil.isOnWaterSurface(player) || MoveUtil.isWeb(player)) {
            d.speedBuffer = Math.max(0, d.speedBuffer - 1);
            return;
        }
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double bpt = horizontal / elapsedTicks;
        boolean onGround = MoveUtil.isStandingOnSolid(player);
        double max = onGround ? this.plugin.getConfig().getDouble("checks.speed.max-ground-blocks-per-tick", 0.92) : this.plugin.getConfig().getDouble("checks.speed.max-air-blocks-per-tick", 1.18);
        PotionEffect speed = player.getPotionEffect(PotionEffectType.SPEED);
        if (speed != null) {
            max += (double)(speed.getAmplifier() + 1) * this.plugin.getConfig().getDouble("checks.speed.speed-potion-extra-per-level", 0.18);
        }
        if (MoveUtil.isOnIce(player)) {
            max += this.plugin.getConfig().getDouble("checks.speed.ice-extra", 0.75);
        }
        if (MoveUtil.isOnSoul(player)) {
            max += this.plugin.getConfig().getDouble("checks.speed.soul-speed-extra", 0.35);
        }
        int ping = Math.max(0, player.getPing());
        double pingExtra = Math.min(this.plugin.getConfig().getDouble("checks.speed.max-ping-leniency", 0.35), (double)ping / 100.0 * this.plugin.getConfig().getDouble("checks.speed.ping-leniency-per-100ms", 0.035));
        if (bpt > (max += pingExtra)) {
            ++d.speedBuffer;
            int minFlags = this.plugin.getConfig().getInt("checks.speed.min-consecutive-flags", 3);
            if (d.speedBuffer >= minFlags) {
                if (this.plugin.getConfig().getBoolean("checks.speed.cancel-move", true)) {
                    this.cancelToSafe(event, d);
                }
                String detail = String.format(Locale.US, "bpt=%.2f max=%.2f ping=%d ground=%s", bpt, max, ping, onGround);
                this.flag(player, "SPEED", detail, this.plugin.getConfig().getInt("checks.speed.kick-threshold", 5), this.plugin.getConfig().getBoolean("checks.speed.kick", true));
            }
        } else {
            d.speedBuffer = Math.max(0, d.speedBuffer - 1);
        }
    }

    private void checkFlySustain(PlayerMoveEvent event, Player player, PlayerData d, Location from, Location to) {
        if (!this.plugin.getConfig().getBoolean("checks.fly-sustain.enabled", true)) {
            return;
        }
        if (MoveUtil.isStandingOnSolid(player) || this.isBuildSafeExempt(player, d)) {
            d.airTicks = 0;
            d.stableYTicks = 0;
            return;
        }
        double dy = to.getY() - from.getY();
        d.stableYTicks = dy > -0.08 && dy < 0.12 ? ++d.stableYTicks : Math.max(0, d.stableYTicks - 1);
        ++d.airTicks;
        int maxAir = this.plugin.getConfig().getInt("checks.fly-sustain.max-air-ticks", 55);
        int maxStable = this.plugin.getConfig().getInt("checks.fly-sustain.max-stable-y-ticks", 18);
        if (d.airTicks > maxAir || d.stableYTicks > maxStable) {
            if (this.plugin.getConfig().getBoolean("checks.fly-sustain.cancel-move", true)) {
                this.cancelToSafe(event, d);
            }
            String detail = "airTicks=" + d.airTicks + " stableY=" + d.stableYTicks;
            this.flag(player, "FLY_SUSTAIN", detail, this.plugin.getConfig().getInt("checks.fly-sustain.kick-threshold", 4), this.plugin.getConfig().getBoolean("checks.fly-sustain.kick", true));
        }
    }

    private void checkJesus(PlayerMoveEvent event, Player player, PlayerData d, Location from, Location to) {
        if (!this.plugin.getConfig().getBoolean("checks.jesus.enabled", true)) {
            return;
        }
        d.jesusTicks = MoveUtil.isOnWaterSurface(player) && !player.isInsideVehicle() && !player.isSwimming() && Math.abs(to.getY() - from.getY()) < 0.08 ? ++d.jesusTicks : Math.max(0, d.jesusTicks - 2);
        int minTicks = this.plugin.getConfig().getInt("checks.jesus.min-water-walk-ticks", 12);
        if (d.jesusTicks >= minTicks) {
            if (this.plugin.getConfig().getBoolean("checks.jesus.cancel-move", true)) {
                this.cancelToSafe(event, d);
            }
            String detail = "waterWalkTicks=" + d.jesusTicks;
            this.flag(player, "JESUS", detail, this.plugin.getConfig().getInt("checks.jesus.kick-threshold", 3), this.plugin.getConfig().getBoolean("checks.jesus.kick", true));
        }
    }

    private void auditIllegalFlight(Player player) {
        if (!this.plugin.getConfig().getBoolean("checks.illegal-flight.enabled", true)) {
            return;
        }
        if (!MoveUtil.checkableGamemode(player)) {
            return;
        }
        if (player.getAllowFlight() || player.isFlying()) {
            if (this.plugin.getConfig().getBoolean("checks.illegal-flight.remove-allow-flight", true)) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
            String detail = "allowFlight=" + player.getAllowFlight() + " flying=" + player.isFlying() + " gm=" + String.valueOf(player.getGameMode());
            this.flag(player, "ILLEGAL_FLIGHT", detail, this.plugin.getConfig().getInt("checks.illegal-flight.kick-threshold", 1), this.plugin.getConfig().getBoolean("checks.illegal-flight.kick", true));
        }
    }

    private void auditSuspiciousPermissions(Player player) {
        if (!this.plugin.getConfig().getBoolean("checks.permission-audit.enabled", true)) {
            return;
        }
        if (!MoveUtil.checkableGamemode(player)) {
            return;
        }
        if (MoveUtil.hasAnyPermission(player, this.plugin.getConfig().getStringList("checks.permission-audit.suspicious-permissions"))) {
            this.plugin.staffAlert(player, "PERMISSION_AUDIT", "Player has suspicious fly/bypass permission. Check LuckPerms.");
        }
    }

    private boolean shouldSkip(Player player) {
        if (!player.isOnline()) {
            return true;
        }
        if (!MoveUtil.checkableGamemode(player)) {
            return true;
        }
        if (player.hasPermission(this.plugin.getConfig().getString("settings.bypass-permission", "rumahkita.anticheat.bypass"))) {
            return true;
        }
        if (this.exemptManager.isTimedExempt(player.getUniqueId())) {
            return true;
        }
        if (player.getPing() >= this.plugin.getConfig().getInt("exemptions.max-ping-exempt-ms", 900)) {
            return true;
        }
        for (String tag : this.plugin.getConfig().getStringList("exemptions.scoreboard-tags")) {
            if (!player.getScoreboardTags().contains(tag)) continue;
            return true;
        }
        for (String key : this.plugin.getConfig().getStringList("exemptions.metadata-keys")) {
            if (!player.hasMetadata(key)) continue;
            return true;
        }
        return false;
    }

    private boolean isBuildSafeExempt(Player player, PlayerData d) {
        if (!this.plugin.getConfig().getBoolean("checks.build-safe.enabled", true)) {
            return false;
        }
        long placeSeconds = this.plugin.getConfig().getLong("checks.build-safe.after-block-place-seconds", 5L);
        long breakSeconds = this.plugin.getConfig().getLong("checks.build-safe.after-block-break-seconds", 3L);
        if (d.recentlyPlacedBlock(placeSeconds)) {
            return true;
        }
        if (d.recentlyBrokeBlock(breakSeconds)) {
            return true;
        }
        if (this.plugin.getConfig().getBoolean("checks.build-safe.exempt-near-solid-block", true) && (MoveUtil.isNearSolidForBuild(player) || d.recentlyNearSolid(1200L))) {
            return true;
        }
        return this.plugin.getConfig().getBoolean("checks.build-safe.exempt-while-sneaking-on-edge", true) && player.isSneaking() && d.recentlyGrounded(2000L);
    }

    private boolean isHardMovementExempt(Player player) {
        return player.isInsideVehicle() || player.isGliding() || player.isRiptiding() || MoveUtil.isLiquidNear(player) || MoveUtil.isClimbable(player) || MoveUtil.isBubbleColumn(player) || MoveUtil.isOnSlimeHoney(player) || MoveUtil.hasMovementExemptEffect(player);
    }

    private boolean shouldResetVL(PlayerData d) {
        long seconds = this.plugin.getConfig().getLong("actions.reset-vl-after-seconds", 30L);
        return System.currentTimeMillis() - d.lastViolationReset > seconds * 1000L;
    }

    private void cancelToSafe(PlayerMoveEvent event, PlayerData d) {
        if (d.lastSafeLocation != null && d.lastSafeLocation.getWorld() == event.getTo().getWorld()) {
            event.setTo(d.lastSafeLocation);
        } else {
            event.setTo(event.getFrom());
        }
    }

    private void flag(Player player, String type, String detail, int kickThreshold, boolean kick) {
        int vl = this.violationTracker.add(player.getUniqueId(), type);
        this.plugin.handleViolation(player, type, detail, vl);
        if (kick && vl >= kickThreshold) {
            this.plugin.handleKick(player, type);
            player.kickPlayer(Text.color(this.plugin.getConfig().getString("actions.kick-reason", "&cIllegal movement.")));
            this.violationTracker.clear(player.getUniqueId());
        }
    }

    private void resetMoveData(PlayerData d, Location location) {
        d.lastMoveLocation = location == null ? null : location.clone();
        d.lastSafeLocation = location == null ? null : location.clone();
        d.lastMoveMillis = System.currentTimeMillis();
        d.airTicks = 0;
        d.stableYTicks = 0;
        d.jesusTicks = 0;
        d.speedBuffer = 0;
    }
}

