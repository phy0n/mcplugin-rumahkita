/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityExplodeEvent
 *  org.bukkit.event.entity.EntitySpawnEvent
 *  org.bukkit.event.player.PlayerBucketEmptyEvent
 *  org.bukkit.event.player.PlayerBucketFillEvent
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.projectiles.ProjectileSource
 */
package id.rumahkita.pvp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

public final class RumahKitaPvP1v1Plugin
extends JavaPlugin
implements Listener,
TabExecutor {
    private final Map<UUID, Duel> activeDuels = new HashMap<UUID, Duel>();
    private final Map<UUID, Invite> invitesByTarget = new HashMap<UUID, Invite>();
    private final Queue<UUID> quickQueue = new ArrayDeque<UUID>();
    private int cleanupTask = -1;

    public void onEnable() {
        this.saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        if (this.getCommand("pvp") != null) {
            this.getCommand("pvp").setExecutor((CommandExecutor)this);
            this.getCommand("pvp").setTabCompleter((TabCompleter)this);
        }
        this.cleanupTask = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this, this::cleanup, 20L, 20L);
        this.getLogger().info("RumahKitaPvP1v1 v1.0.1 enabled.");
    }

    public void onDisable() {
        if (this.cleanupTask != -1) {
            Bukkit.getScheduler().cancelTask(this.cleanupTask);
        }
        HashSet<Duel> duels = new HashSet<Duel>(this.activeDuels.values());
        for (Duel duel : duels) {
            this.forceEnd(duel, null, "Server reload/restart");
        }
        this.quickQueue.clear();
        this.invitesByTarget.clear();
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        int expire = Math.max(5, this.getConfig().getInt("queue.invite-expire-seconds", 60));
        this.invitesByTarget.entrySet().removeIf(entry -> now - ((Invite)entry.getValue()).createdAt > (long)expire * 1000L);
        this.quickQueue.removeIf(uuid -> Bukkit.getPlayer((UUID)uuid) == null || this.activeDuels.containsKey(uuid));
        int maxDuration = Math.max(30, this.getConfig().getInt("match.max-duration-seconds", 300));
        HashSet<Duel> duels = new HashSet<Duel>(this.activeDuels.values());
        for (Duel duel : duels) {
            if (now - duel.startedAt <= (long)maxDuration * 1000L || duel.ending) continue;
            Player p1 = Bukkit.getPlayer((UUID)duel.p1);
            Player p2 = Bukkit.getPlayer((UUID)duel.p2);
            this.broadcastToDuel(duel, this.cc(this.pref() + "&eWaktu duel habis. Duel seri."));
            this.endDuel(duel, null, null, false);
            if (p1 != null) {
                this.msg((CommandSender)p1, this.pref() + "&eDuel selesai karena waktu habis.");
            }
            if (p2 == null) continue;
            this.msg((CommandSender)p2, this.pref() + "&eDuel selesai karena waktu habis.");
        }
    }

    private void invite(Player inviter, Player target) {
        if (!this.canUse(inviter)) {
            return;
        }
        if (inviter.equals((Object)target)) {
            this.msg((CommandSender)inviter, this.pref() + "&cTidak bisa invite diri sendiri.");
            return;
        }
        if (this.activeDuels.containsKey(inviter.getUniqueId())) {
            this.msg((CommandSender)inviter, this.pref() + this.getConfig().getString("messages.already-in-duel"));
            return;
        }
        if (this.activeDuels.containsKey(target.getUniqueId())) {
            this.msg((CommandSender)inviter, this.pref() + this.getConfig().getString("messages.target-busy"));
            return;
        }
        if (this.isArenaBusy()) {
            this.msg((CommandSender)inviter, this.pref() + this.getConfig().getString("messages.arena-busy"));
            return;
        }
        this.invitesByTarget.put(target.getUniqueId(), new Invite(inviter.getUniqueId(), target.getUniqueId(), System.currentTimeMillis()));
        this.msg((CommandSender)inviter, this.pref() + this.replace(this.getConfig().getString("messages.invite-sent"), "%target%", target.getName()));
        this.msg((CommandSender)target, this.pref() + this.replace(this.getConfig().getString("messages.invite-received"), "%player%", inviter.getName()));
    }

    private void accept(Player target, String inviterName) {
        if (!this.canUse(target)) {
            return;
        }
        Invite invite = this.invitesByTarget.get(target.getUniqueId());
        if (invite == null) {
            this.msg((CommandSender)target, this.pref() + "&cTidak ada invite PvP yang aktif.");
            return;
        }
        Player inviter = Bukkit.getPlayer((UUID)invite.inviter);
        if (inviter == null) {
            this.invitesByTarget.remove(target.getUniqueId());
            this.msg((CommandSender)target, this.pref() + "&cPlayer yang invite sudah offline.");
            return;
        }
        if (inviterName != null && !inviterName.isEmpty() && !inviter.getName().equalsIgnoreCase(inviterName)) {
            this.msg((CommandSender)target, this.pref() + "&cInvite aktif kamu bukan dari player itu.");
            return;
        }
        int expire = Math.max(5, this.getConfig().getInt("queue.invite-expire-seconds", 60));
        if (System.currentTimeMillis() - invite.createdAt > (long)expire * 1000L) {
            this.invitesByTarget.remove(target.getUniqueId());
            this.msg((CommandSender)target, this.pref() + this.getConfig().getString("messages.invite-expired"));
            return;
        }
        this.invitesByTarget.remove(target.getUniqueId());
        this.startDuel(inviter, target, "invite");
    }

    private void quickJoin(Player player) {
        if (!this.canUse(player)) {
            return;
        }
        if (!this.getConfig().getBoolean("queue.quickjoin-enabled", true)) {
            this.msg((CommandSender)player, this.pref() + "&cQuickjoin sedang dimatikan.");
            return;
        }
        if (this.activeDuels.containsKey(player.getUniqueId())) {
            this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.already-in-duel"));
            return;
        }
        if (this.isArenaBusy()) {
            this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.arena-busy"));
            return;
        }
        this.quickQueue.remove(player.getUniqueId());
        while (!this.quickQueue.isEmpty()) {
            UUID otherId = this.quickQueue.poll();
            Player other = Bukkit.getPlayer((UUID)otherId);
            if (other == null || other.equals((Object)player) || this.activeDuels.containsKey(otherId)) continue;
            this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.quickjoin-found"));
            this.msg((CommandSender)other, this.pref() + this.getConfig().getString("messages.quickjoin-found"));
            this.startDuel(other, player, "quickjoin");
            return;
        }
        this.quickQueue.add(player.getUniqueId());
        this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.quickjoin-waiting"));
    }

    private boolean startDuel(Player p1, Player p2, String reason) {
        if (!this.getConfig().getBoolean("general.enabled", true)) {
            this.msg((CommandSender)p1, this.pref() + "&cPvP system sedang dimatikan.");
            return false;
        }
        if (this.isArenaBusy()) {
            this.msg((CommandSender)p1, this.pref() + this.getConfig().getString("messages.arena-busy"));
            this.msg((CommandSender)p2, this.pref() + this.getConfig().getString("messages.arena-busy"));
            return false;
        }
        if (this.activeDuels.containsKey(p1.getUniqueId()) || this.activeDuels.containsKey(p2.getUniqueId())) {
            this.msg((CommandSender)p1, this.pref() + this.getConfig().getString("messages.target-busy"));
            this.msg((CommandSender)p2, this.pref() + this.getConfig().getString("messages.target-busy"));
            return false;
        }
        Location s1 = this.readLocation("arena.spawn1");
        Location s2 = this.readLocation("arena.spawn2");
        if (s1 == null || s2 == null) {
            this.msg((CommandSender)p1, this.pref() + "&cSpawn arena belum valid. Admin harus cek config.yml.");
            this.msg((CommandSender)p2, this.pref() + "&cSpawn arena belum valid. Admin harus cek config.yml.");
            return false;
        }
        Duel duel = new Duel(p1.getUniqueId(), p2.getUniqueId(), p1.getLocation().clone(), p2.getLocation().clone());
        this.activeDuels.put(p1.getUniqueId(), duel);
        this.activeDuels.put(p2.getUniqueId(), duel);
        this.prepPlayer(p1);
        this.prepPlayer(p2);
        p1.teleport(s1);
        p2.teleport(s2);
        p1.setNoDamageTicks(60);
        p2.setNoDamageTicks(60);
        int countdown = Math.max(0, this.getConfig().getInt("match.countdown-seconds", 3));
        this.msg((CommandSender)p1, this.pref() + this.replace(this.getConfig().getString("messages.match-starting"), "%seconds%", String.valueOf(countdown)));
        this.msg((CommandSender)p2, this.pref() + this.replace(this.getConfig().getString("messages.match-starting"), "%seconds%", String.valueOf(countdown)));
        p1.sendTitle(this.cc("&cPvP 1v1"), this.cc("&eLawan: &f" + p2.getName()), 5, 35, 10);
        p2.sendTitle(this.cc("&cPvP 1v1"), this.cc("&eLawan: &f" + p1.getName()), 5, 35, 10);
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
            if (!duel.ending && this.activeDuels.get(p1.getUniqueId()) == duel && this.activeDuels.get(p2.getUniqueId()) == duel) {
                duel.canDamage = true;
                this.msg((CommandSender)p1, this.pref() + this.replace(this.getConfig().getString("messages.match-started"), "%opponent%", p2.getName()));
                this.msg((CommandSender)p2, this.pref() + this.replace(this.getConfig().getString("messages.match-started"), "%opponent%", p1.getName()));
                p1.sendTitle(this.cc("&aMULAI!"), this.cc("&fKalahkan &e" + p2.getName()), 5, 25, 5);
                p2.sendTitle(this.cc("&aMULAI!"), this.cc("&fKalahkan &e" + p1.getName()), 5, 25, 5);
            }
        }, (long)countdown * 20L);
        return true;
    }

    private void prepPlayer(Player player) {
        if (this.getConfig().getBoolean("match.heal-on-start", true)) {
            player.setHealth(Math.max(1.0, player.getMaxHealth()));
        }
        if (this.getConfig().getBoolean("match.clear-fire-on-start", true)) {
            player.setFireTicks(0);
        }
        int food = this.getConfig().getInt("match.food-level-on-start", 20);
        player.setFoodLevel(Math.max(0, Math.min(20, food)));
        player.setSaturation(20.0f);
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    private void endDuel(Duel duel, Player winner, Player loser, boolean forfeit) {
        if (duel == null || duel.ending) {
            return;
        }
        duel.ending = true;
        this.activeDuels.remove(duel.p1);
        this.activeDuels.remove(duel.p2);
        Player p1 = Bukkit.getPlayer((UUID)duel.p1);
        Player p2 = Bukkit.getPlayer((UUID)duel.p2);
        if (winner != null && loser != null) {
            this.msg((CommandSender)winner, this.pref() + this.replace(this.getConfig().getString("messages.match-win"), "%loser%", loser.getName()));
            this.msg((CommandSender)loser, this.pref() + this.replace(this.getConfig().getString("messages.match-lose"), "%winner%", winner.getName()));
            winner.sendTitle(this.cc("&aMENANG!"), this.cc("&fMelawan &e" + loser.getName()), 5, 35, 10);
            loser.sendTitle(this.cc("&cKALAH"), this.cc("&fMelawan &e" + winner.getName()), 5, 35, 10);
        }
        if (this.getConfig().getBoolean("protection.remove-projectiles-on-end", true)) {
            this.removeNearbyProjectiles();
        }
        long delay = Math.max(0L, this.getConfig().getLong("arena.end-delay-ticks", 40L));
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
            this.restorePlayer(p1, duel.p1Return);
            this.restorePlayer(p2, duel.p2Return);
        }, delay);
    }

    private void forceEnd(Duel duel, Player winner, String reason) {
        if (duel == null) {
            return;
        }
        Player p1 = Bukkit.getPlayer((UUID)duel.p1);
        Player p2 = Bukkit.getPlayer((UUID)duel.p2);
        if (p1 != null) {
            this.msg((CommandSender)p1, this.pref() + "&eDuel dihentikan. " + reason);
        }
        if (p2 != null) {
            this.msg((CommandSender)p2, this.pref() + "&eDuel dihentikan. " + reason);
        }
        this.endDuel(duel, null, null, false);
    }

    private void restorePlayer(Player player, Location returnLocation) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (this.getConfig().getBoolean("match.heal-on-end", true)) {
            player.setHealth(Math.max(1.0, player.getMaxHealth()));
        }
        if (this.getConfig().getBoolean("match.clear-fire-on-end", true)) {
            player.setFireTicks(0);
        }
        int food = this.getConfig().getInt("match.food-level-on-end", 20);
        player.setFoodLevel(Math.max(0, Math.min(20, food)));
        player.setSaturation(20.0f);
        player.setNoDamageTicks(60);
        if (this.getConfig().getBoolean("arena.return-to-original-location", true) && returnLocation != null && returnLocation.getWorld() != null) {
            player.teleport(returnLocation);
        }
    }

    private void removeNearbyProjectiles() {
        World world = Bukkit.getWorld((String)this.getConfig().getString("arena.world", "world"));
        if (world == null) {
            return;
        }
        int minX = Math.min(this.getConfig().getInt("arena.pos1.x"), this.getConfig().getInt("arena.pos2.x")) - 8;
        int maxX = Math.max(this.getConfig().getInt("arena.pos1.x"), this.getConfig().getInt("arena.pos2.x")) + 8;
        int minZ = Math.min(this.getConfig().getInt("arena.pos1.z"), this.getConfig().getInt("arena.pos2.z")) - 8;
        int maxZ = Math.max(this.getConfig().getInt("arena.pos1.z"), this.getConfig().getInt("arena.pos2.z")) + 8;
        int minY = this.getConfig().getInt("arena.min-y", 140) - 8;
        int maxY = this.getConfig().getInt("arena.max-y", 230) + 8;
        for (Entity entity : world.getEntities()) {
            Location l;
            if (!(entity instanceof Projectile) || (l = entity.getLocation()).getBlockX() < minX || l.getBlockX() > maxX || l.getBlockY() < minY || l.getBlockY() > maxY || l.getBlockZ() < minZ || l.getBlockZ() > maxZ) continue;
            entity.remove();
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player)event.getEntity();
        Duel duel = this.activeDuels.get(victim.getUniqueId());
        if (duel == null) {
            return;
        }
        if (!duel.canDamage) {
            event.setCancelled(true);
            return;
        }
        if (!this.getConfig().getBoolean("match.prevent-real-death", true)) {
            return;
        }
        double finalHealth = victim.getHealth() - event.getFinalDamage();
        if (finalHealth > 0.0) {
            return;
        }
        event.setCancelled(true);
        Player killer = this.getAttacker(event);
        Player winner = null;
        if (killer != null && duel.isParticipant(killer.getUniqueId()) && !killer.getUniqueId().equals(victim.getUniqueId())) {
            winner = killer;
        } else {
            UUID otherId = duel.other(victim.getUniqueId());
            winner = Bukkit.getPlayer((UUID)otherId);
        }
        Player loser = victim;
        this.endDuel(duel, winner, loser, false);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player)event.getEntity();
        Player attacker = this.getAttacker((EntityDamageEvent)event);
        if (attacker == null) {
            return;
        }
        Duel victimDuel = this.activeDuels.get(victim.getUniqueId());
        Duel attackerDuel = this.activeDuels.get(attacker.getUniqueId());
        boolean onlyDuel = this.getConfig().getBoolean("protection.only-duel-participants-can-pvp-in-arena", true);
        if (victimDuel != null || attackerDuel != null) {
            if (victimDuel == null || attackerDuel == null || victimDuel != attackerDuel) {
                event.setCancelled(true);
                this.msg((CommandSender)attacker, this.pref() + this.getConfig().getString("messages.cannot-hit"));
                return;
            }
            if (!victimDuel.canDamage) {
                event.setCancelled(true);
            }
            return;
        }
        if (onlyDuel && (this.isInArena(victim.getLocation()) || this.isInArena(attacker.getLocation()))) {
            event.setCancelled(true);
            this.msg((CommandSender)attacker, this.pref() + this.getConfig().getString("messages.cannot-hit"));
        }
    }

    private Player getAttacker(EntityDamageEvent event) {
        ProjectileSource source;
        if (!(event instanceof EntityDamageByEntityEvent)) {
            return null;
        }
        Entity damager = ((EntityDamageByEntityEvent)event).getDamager();
        if (damager instanceof Player) {
            return (Player)damager;
        }
        if (damager instanceof Projectile && (source = ((Projectile)damager).getShooter()) instanceof Player) {
            return (Player)source;
        }
        return null;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player quitter = event.getPlayer();
        this.quickQueue.remove(quitter.getUniqueId());
        this.invitesByTarget.remove(quitter.getUniqueId());
        Duel duel = this.activeDuels.get(quitter.getUniqueId());
        if (duel != null) {
            Player winner = Bukkit.getPlayer((UUID)duel.other(quitter.getUniqueId()));
            if (winner != null) {
                this.msg((CommandSender)winner, this.pref() + this.replace(this.getConfig().getString("messages.match-forfeit"), "%player%", quitter.getName()));
            }
            this.endDuel(duel, winner, quitter, true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event) {
        if (this.getConfig().getBoolean("protection.block-break-place-in-arena", true) && this.isInArena(event.getBlock().getLocation()) && !event.getPlayer().hasPermission("rumahkita.pvp.bypass")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPlace(BlockPlaceEvent event) {
        if (this.getConfig().getBoolean("protection.block-break-place-in-arena", true) && this.isInArena(event.getBlock().getLocation()) && !event.getPlayer().hasPermission("rumahkita.pvp.bypass")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (this.getConfig().getBoolean("protection.block-buckets-in-arena", true) && this.isInArena(event.getBlock().getLocation()) && !event.getPlayer().hasPermission("rumahkita.pvp.bypass")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (this.getConfig().getBoolean("protection.block-buckets-in-arena", true) && this.isInArena(event.getBlock().getLocation()) && !event.getPlayer().hasPermission("rumahkita.pvp.bypass")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onMobSpawn(EntitySpawnEvent event) {
        if (!this.getConfig().getBoolean("protection.deny-mob-spawn-in-arena", true)) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            return;
        }
        if (this.isInArena(event.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!this.getConfig().getBoolean("protection.deny-explosions-in-arena", true)) {
            return;
        }
        if (this.isInArena(event.getLocation())) {
            event.setCancelled(true);
        } else {
            event.blockList().removeIf(block -> this.isInArena(block.getLocation()));
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String root;
        if (!this.getConfig().getBoolean("commands.block-during-duel", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (!this.activeDuels.containsKey(player.getUniqueId())) {
            return;
        }
        String raw = event.getMessage().toLowerCase(Locale.ROOT).trim();
        if (raw.startsWith("/")) {
            raw = raw.substring(1);
        }
        if ((root = raw.split(" ")[0]).equals("pvp") || root.equals("duel") || root.equals("rkduel") || root.equals("rkpvp")) {
            return;
        }
        List blocked = this.getConfig().getStringList("commands.blocked-during-duel");
        for (String b : blocked) {
            if (!root.equalsIgnoreCase(b)) continue;
            event.setCancelled(true);
            this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.command-blocked"));
            return;
        }
    }

    private boolean isInArena(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        String worldName = this.getConfig().getString("arena.world", "world");
        if (!location.getWorld().getName().equalsIgnoreCase(worldName)) {
            return false;
        }
        int x1 = this.getConfig().getInt("arena.pos1.x");
        int z1 = this.getConfig().getInt("arena.pos1.z");
        int x2 = this.getConfig().getInt("arena.pos2.x");
        int z2 = this.getConfig().getInt("arena.pos2.z");
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        int minY = this.getConfig().getInt("arena.min-y", Math.min(this.getConfig().getInt("arena.pos1.y"), this.getConfig().getInt("arena.pos2.y")));
        int maxY = this.getConfig().getInt("arena.max-y", Math.max(this.getConfig().getInt("arena.pos1.y"), this.getConfig().getInt("arena.pos2.y")));
        return location.getBlockX() >= minX && location.getBlockX() <= maxX && location.getBlockY() >= minY && location.getBlockY() <= maxY && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }

    private boolean isArenaBusy() {
        return !this.activeDuels.isEmpty();
    }

    private Location readLocation(String path) {
        String worldName = this.getConfig().getString("arena.world", "world");
        World world = Bukkit.getWorld((String)worldName);
        if (world == null) {
            return null;
        }
        ConfigurationSection s = this.getConfig().getConfigurationSection(path);
        if (s == null) {
            return null;
        }
        return new Location(world, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"), (float)s.getDouble("yaw", 0.0), (float)s.getDouble("pitch", 0.0));
    }

    private void setLocation(String path, Location loc, boolean includeYawPitch) {
        this.getConfig().set(path + ".x", (Object)loc.getX());
        this.getConfig().set(path + ".y", (Object)loc.getY());
        this.getConfig().set(path + ".z", (Object)loc.getZ());
        if (includeYawPitch) {
            this.getConfig().set(path + ".yaw", (Object)Float.valueOf(loc.getYaw()));
            this.getConfig().set(path + ".pitch", (Object)Float.valueOf(loc.getPitch()));
        }
        this.getConfig().set("arena.world", (Object)loc.getWorld().getName());
        this.saveConfig();
    }

    private boolean canUse(Player player) {
        if (!player.hasPermission("rumahkita.pvp.use")) {
            this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.no-permission"));
            return false;
        }
        return true;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub;
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.cc(this.pref() + this.getConfig().getString("messages.not-player")));
            return true;
        }
        Player player = (Player)sender;
        if (args.length == 0) {
            this.sendHelp(player);
            return true;
        }
        switch (sub = args[0].toLowerCase(Locale.ROOT)) {
            case "invite": {
                if (args.length < 2) {
                    this.msg((CommandSender)player, this.pref() + "&cPakai: /pvp invite <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer((String)args[1]);
                if (target == null) {
                    this.msg((CommandSender)player, this.pref() + "&cPlayer tidak online.");
                    return true;
                }
                this.invite(player, target);
                return true;
            }
            case "accept": {
                this.accept(player, args.length >= 2 ? args[1] : null);
                return true;
            }
            case "deny": 
            case "cancel": {
                this.invitesByTarget.remove(player.getUniqueId());
                this.quickQueue.remove(player.getUniqueId());
                this.msg((CommandSender)player, this.pref() + "&eInvite/queue PvP dibatalkan.");
                return true;
            }
            case "quickjoin": 
            case "quick": {
                this.quickJoin(player);
                return true;
            }
            case "leave": 
            case "forfeit": {
                Duel duel = this.activeDuels.get(player.getUniqueId());
                if (duel == null) {
                    this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.not-in-duel"));
                    return true;
                }
                Player winner = Bukkit.getPlayer((UUID)duel.other(player.getUniqueId()));
                if (winner != null) {
                    this.msg((CommandSender)winner, this.pref() + this.replace(this.getConfig().getString("messages.match-forfeit"), "%player%", player.getName()));
                }
                this.endDuel(duel, winner, player, true);
                return true;
            }
            case "status": {
                if (!player.hasPermission("rumahkita.pvp.admin")) {
                    this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.no-permission"));
                    return true;
                }
                this.msg((CommandSender)player, this.pref() + "&eArena busy: &f" + this.isArenaBusy());
                this.msg((CommandSender)player, this.pref() + "&eQueue: &f" + this.quickQueue.size());
                this.msg((CommandSender)player, this.pref() + "&eRegion: &f" + this.getConfig().getString("arena.world") + " " + this.getConfig().getInt("arena.pos1.x") + "," + this.getConfig().getInt("arena.min-y") + "," + this.getConfig().getInt("arena.pos1.z") + " -> " + this.getConfig().getInt("arena.pos2.x") + "," + this.getConfig().getInt("arena.max-y") + "," + this.getConfig().getInt("arena.pos2.z"));
                return true;
            }
            case "reload": {
                if (!player.hasPermission("rumahkita.pvp.admin")) {
                    this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.no-permission"));
                    return true;
                }
                this.reloadConfig();
                this.msg((CommandSender)player, this.pref() + "&aConfig PvP reloaded.");
                return true;
            }
            case "setspawn1": 
            case "setspawn2": 
            case "setpos1": 
            case "setpos2": {
                if (!player.hasPermission("rumahkita.pvp.admin")) {
                    this.msg((CommandSender)player, this.pref() + this.getConfig().getString("messages.no-permission"));
                    return true;
                }
                Location loc = player.getLocation();
                if (sub.startsWith("setspawn")) {
                    this.setLocation("arena." + sub.substring(3), loc, true);
                    this.msg((CommandSender)player, this.pref() + "&a" + sub + " disimpan.");
                } else {
                    String key = "arena." + sub.substring(3);
                    this.getConfig().set(key + ".x", (Object)loc.getBlockX());
                    this.getConfig().set(key + ".y", (Object)loc.getBlockY());
                    this.getConfig().set(key + ".z", (Object)loc.getBlockZ());
                    this.getConfig().set("arena.world", (Object)loc.getWorld().getName());
                    this.saveConfig();
                    this.msg((CommandSender)player, this.pref() + "&a" + sub + " disimpan. Jangan lupa cek min-y/max-y.");
                }
                return true;
            }
        }
        this.sendHelp(player);
        return true;
    }

    private void sendHelp(Player player) {
        this.msg((CommandSender)player, "&c&lRumahKita PvP 1v1");
        this.msg((CommandSender)player, "&f/pvp invite <player> &7- ajak player duel");
        this.msg((CommandSender)player, "&f/pvp accept <player> &7- terima duel");
        this.msg((CommandSender)player, "&f/pvp quickjoin &7- cari lawan otomatis");
        this.msg((CommandSender)player, "&f/pvp leave &7- menyerah / keluar duel");
        this.msg((CommandSender)player, "&f/pvp cancel &7- batal queue/invite");
        if (player.hasPermission("rumahkita.pvp.admin")) {
            this.msg((CommandSender)player, "&eAdmin: &f/pvp setpos1, setpos2, setspawn1, setspawn2, status, reload");
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            ArrayList<String> base = new ArrayList<String>(Arrays.asList("invite", "accept", "deny", "cancel", "quickjoin", "leave"));
            if (sender.hasPermission("rumahkita.pvp.admin")) {
                base.addAll(Arrays.asList("status", "reload", "setpos1", "setpos2", "setspawn1", "setspawn2"));
            }
            return this.filter(base, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("accept"))) {
            ArrayList<String> names = new ArrayList<String>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return this.filter(names, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> input, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        ArrayList<String> out = new ArrayList<String>();
        for (String s : input) {
            if (!s.toLowerCase(Locale.ROOT).startsWith(p)) continue;
            out.add(s);
        }
        return out;
    }

    private void broadcastToDuel(Duel duel, String message) {
        Player p1 = Bukkit.getPlayer((UUID)duel.p1);
        Player p2 = Bukkit.getPlayer((UUID)duel.p2);
        if (p1 != null) {
            p1.sendMessage(message);
        }
        if (p2 != null) {
            p2.sendMessage(message);
        }
    }

    private String pref() {
        return this.getConfig().getString("general.prefix", "&8[&cPvP&8] &f");
    }

    private void msg(CommandSender sender, String text) {
        sender.sendMessage(this.cc(text == null ? "" : text));
    }

    private String cc(String s) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)(s == null ? "" : s));
    }

    private String replace(String src, String key, String value) {
        return (src == null ? "" : src).replace(key, value == null ? "" : value);
    }

    private static final class Duel {
        final UUID p1;
        final UUID p2;
        final Location p1Return;
        final Location p2Return;
        final long startedAt = System.currentTimeMillis();
        boolean canDamage = false;
        boolean ending = false;

        Duel(UUID p1, UUID p2, Location p1Return, Location p2Return) {
            this.p1 = p1;
            this.p2 = p2;
            this.p1Return = p1Return;
            this.p2Return = p2Return;
        }

        boolean isParticipant(UUID uuid) {
            return this.p1.equals(uuid) || this.p2.equals(uuid);
        }

        UUID other(UUID uuid) {
            return this.p1.equals(uuid) ? this.p2 : this.p1;
        }
    }

    private static final class Invite {
        final UUID inviter;
        final UUID target;
        final long createdAt;

        Invite(UUID inviter, UUID target, long createdAt) {
            this.inviter = inviter;
            this.target = target;
            this.createdAt = createdAt;
        }
    }
}

