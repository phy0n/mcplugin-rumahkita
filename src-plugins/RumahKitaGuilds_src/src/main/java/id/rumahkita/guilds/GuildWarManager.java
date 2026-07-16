/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.projectiles.ProjectileSource
 *  org.bukkit.scheduler.BukkitTask
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.Guild;
import id.rumahkita.guilds.GuildManager;
import id.rumahkita.guilds.GuildRole;
import id.rumahkita.guilds.RumahKitaGuildsPlugin;
import id.rumahkita.guilds.Text;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

public final class GuildWarManager
implements Listener {
    private final RumahKitaGuildsPlugin plugin;
    private final GuildManager guildManager;
    private final Map<String, Challenge> challenges = new HashMap<String, Challenge>();
    private War activeWar;
    private BukkitTask ticker;

    public GuildWarManager(RumahKitaGuildsPlugin plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
    }

    public void reload() {
    }

    public void shutdown() {
        if (this.activeWar != null) {
            this.endWar("Server restart / plugin disable", null);
        }
        if (this.ticker != null) {
            this.ticker.cancel();
        }
    }

    public void handleCommand(CommandSender sender, String[] args) {
        String sub;
        Player player = this.requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (!this.plugin.getConfig().getBoolean("guild-war.enabled", true)) {
            Text.msg((CommandSender)player, this.pref() + "&cGuild War sedang dimatikan di config.");
            return;
        }
        if (args.length < 2) {
            this.help(player);
            return;
        }
        switch (sub = args[1].toLowerCase(Locale.ROOT)) {
            case "accept": {
                this.accept(player, args);
                break;
            }
            case "deny": 
            case "decline": {
                this.deny(player);
                break;
            }
            case "status": {
                this.status(player);
                break;
            }
            case "stop": {
                this.stop(player);
                break;
            }
            default: {
                this.challenge(player, args[1]);
            }
        }
    }

    public void handleWallet(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        GuildRole role = guild.getRole(player.getUniqueId());
        if (args.length >= 2 && args[1].equalsIgnoreCase("withdraw")) {
            int amount;
            if (!role.atLeast(GuildRole.ADMIN)) {
                Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
                return;
            }
            if (args.length < 3) {
                Text.msg((CommandSender)player, "&eGunakan: /guild wallet withdraw <amount|all>");
                return;
            }
            if (args[2].equalsIgnoreCase("all")) {
                amount = guild.getEmeraldWallet();
            } else {
                try {
                    amount = Integer.parseInt(args[2]);
                }
                catch (NumberFormatException ex) {
                    Text.msg((CommandSender)player, "&cAmount harus angka atau all.");
                    return;
                }
            }
            if (amount <= 0) {
                Text.msg((CommandSender)player, "&cWallet guild tidak punya emerald untuk diambil.");
                return;
            }
            if (!guild.withdrawEmeraldWallet(amount)) {
                Text.msg((CommandSender)player, "&cEmerald wallet guild tidak cukup.");
                return;
            }
            HashMap left = player.getInventory().addItem(new ItemStack[]{new ItemStack(Material.EMERALD, amount)});
            for (ItemStack item : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            this.guildManager.save();
            Text.msg((CommandSender)player, this.pref() + "&aKamu mengambil &e" + amount + " Emerald &adari Guild Wallet.");
        } else {
            Text.msg((CommandSender)player, "&8&m------------------------");
            Text.msg((CommandSender)player, "&bGuild Wallet &8[&f" + guild.getTag() + "&8]");
            Text.msg((CommandSender)player, "&7Emerald: &a" + guild.getEmeraldWallet());
            Text.msg((CommandSender)player, "&7Leader/Admin bisa pakai: &e/guild wallet withdraw <amount|all>");
            Text.msg((CommandSender)player, "&8&m------------------------");
        }
    }

    private void challenge(Player player, String enemyTag) {
        if (this.activeWar != null) {
            Text.msg((CommandSender)player, this.pref() + "&cSedang ada Guild War berjalan.");
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        if (!this.canManageWar(guild, player.getUniqueId())) {
            Text.msg((CommandSender)player, this.pref() + "&cHanya Leader atau Admin guild yang bisa mengajukan Guild War.");
            return;
        }
        Guild enemy = this.guildManager.getGuildByTag(enemyTag);
        if (enemy == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "guild-not-found"));
            return;
        }
        if (enemy == guild) {
            Text.msg((CommandSender)player, this.pref() + "&cTidak bisa war melawan guild sendiri.");
            return;
        }
        int minOnline = this.plugin.getConfig().getInt("guild-war.min-online-per-guild", 1);
        if (this.onlineMembers(guild).size() < minOnline || this.onlineMembers(enemy).size() < minOnline) {
            Text.msg((CommandSender)player, this.pref() + "&cMasing-masing guild minimal harus punya &e" + minOnline + " &cplayer online.");
            return;
        }
        long expire = System.currentTimeMillis() + (long)this.plugin.getConfig().getInt("guild-war.challenge-expire-seconds", 120) * 1000L;
        Challenge challenge = new Challenge(guild.getTag(), enemy.getTag(), player.getUniqueId(), expire);
        this.challenges.put(enemy.getTag().toUpperCase(Locale.ROOT), challenge);
        Text.msg((CommandSender)player, this.pref() + "&aGuild War diajukan ke guild &e" + enemy.getTag() + "&a.");
        for (Player p : this.onlineMembers(enemy)) {
            GuildRole role = enemy.getRole(p.getUniqueId());
            if (!role.atLeast(GuildRole.ADMIN)) continue;
            Text.msg((CommandSender)p, this.pref() + "&eGuild &b" + guild.getTag() + " &emengajak Guild War. Ketik &a/guild war accept " + guild.getTag() + " &euntuk mulai.");
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        }
    }

    private void accept(Player player, String[] args) {
        if (this.activeWar != null) {
            Text.msg((CommandSender)player, this.pref() + "&cSedang ada Guild War berjalan.");
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        if (!this.canManageWar(guild, player.getUniqueId())) {
            Text.msg((CommandSender)player, this.pref() + "&cHanya Leader atau Admin guild yang bisa accept Guild War.");
            return;
        }
        if (args.length < 3) {
            Text.msg((CommandSender)player, "&eGunakan: /guild war accept <tag musuh>");
            return;
        }
        Challenge ch = this.challenges.get(guild.getTag().toUpperCase(Locale.ROOT));
        if (ch == null || !ch.challengerTag.equalsIgnoreCase(args[2]) || ch.expireAt < System.currentTimeMillis()) {
            this.challenges.remove(guild.getTag().toUpperCase(Locale.ROOT));
            Text.msg((CommandSender)player, this.pref() + "&cChallenge tidak ditemukan atau sudah expired.");
            return;
        }
        Guild challenger = this.guildManager.getGuildByTag(ch.challengerTag);
        if (challenger == null) {
            Text.msg((CommandSender)player, this.pref() + "&cGuild penantang tidak ditemukan.");
            return;
        }
        this.challenges.remove(guild.getTag().toUpperCase(Locale.ROOT));
        this.startCountdown(challenger, guild);
    }

    private void deny(Player player) {
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        if (!this.canManageWar(guild, player.getUniqueId())) {
            Text.msg((CommandSender)player, this.pref() + "&cHanya Leader atau Admin guild yang bisa menolak Guild War.");
            return;
        }
        this.challenges.remove(guild.getTag().toUpperCase(Locale.ROOT));
        Text.msg((CommandSender)player, this.pref() + "&cChallenge Guild War ditolak.");
    }

    private void startCountdown(Guild g1, Guild g2) {
        int countdown;
        List<Player> side1 = this.onlineMembers(g1);
        List<Player> side2 = this.onlineMembers(g2);
        if (side1.isEmpty() || side2.isEmpty()) {
            Bukkit.broadcastMessage((String)Text.color(this.pref() + "&cGuild War batal karena salah satu guild tidak punya member online."));
            return;
        }
        this.activeWar = new War(g1.getTag(), g2.getTag());
        Location loc1 = this.sideLocation("side1");
        Location loc2 = this.sideLocation("side2");
        for (Player p : side1) {
            this.preparePlayer(p, loc1, g1.getTag());
        }
        for (Player p : side2) {
            this.preparePlayer(p, loc2, g2.getTag());
        }
        this.activeWar.countdownLeft = countdown = this.plugin.getConfig().getInt("guild-war.countdown-seconds", 10);
        this.activeWar.running = false;
        Bukkit.broadcastMessage((String)Text.color(this.pref() + "&dGuild War &f" + g1.getTag() + " &7vs &f" + g2.getTag() + " &edimulai dalam &c" + countdown + " detik&e!"));
        this.ticker = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, this::tickWar, 20L, 20L);
    }

    private void preparePlayer(Player p, Location target, String guildTag) {
        this.activeWar.returnLocations.put(p.getUniqueId(), p.getLocation().clone());
        this.activeWar.playerGuild.put(p.getUniqueId(), guildTag.toUpperCase(Locale.ROOT));
        this.activeWar.participants.add(p.getUniqueId());
        this.activeWar.kills.put(p.getUniqueId(), 0);
        if (this.plugin.getConfig().getBoolean("guild-war.reset-health-food-on-start", true)) {
            p.setHealth(Math.min(p.getMaxHealth(), 20.0));
            p.setFoodLevel(20);
            p.setSaturation(10.0f);
            p.setFireTicks(0);
        }
        p.teleport(target);
        p.sendTitle(Text.color("&d&lGUILD WAR"), Text.color("&fTeam: &b" + guildTag), 10, 40, 10);
    }

    private void tickWar() {
        if (this.activeWar == null) {
            if (this.ticker != null) {
                this.ticker.cancel();
            }
            return;
        }
        if (!this.activeWar.running) {
            if (this.activeWar.countdownLeft <= 0) {
                this.activeWar.running = true;
                this.activeWar.endsAt = System.currentTimeMillis() + (long)this.plugin.getConfig().getInt("guild-war.duration-seconds", 300) * 1000L;
                this.broadcastWar("&aGuild War dimulai! Inventory tidak dihapus. Fight!");
            } else {
                for (Player p : this.activePlayers()) {
                    p.sendTitle(Text.color("&d&lGUILD WAR"), Text.color("&eMulai dalam &c" + this.activeWar.countdownLeft + " &edetik"), 0, 25, 5);
                }
                --this.activeWar.countdownLeft;
            }
            return;
        }
        long left = Math.max(0L, (this.activeWar.endsAt - System.currentTimeMillis()) / 1000L);
        if (this.plugin.getConfig().getBoolean("guild-war.actionbar", true)) {
            String bar = Text.color("&dGuild War &8| &f" + this.activeWar.guild1 + " &e" + this.activeWar.score(this.activeWar.guild1) + " &7vs &f" + this.activeWar.guild2 + " &e" + this.activeWar.score(this.activeWar.guild2) + " &8| &7" + left + "s");
            for (Player p : this.activePlayers()) {
                p.sendActionBar((Component)Component.text((String)bar));
            }
        }
        if (left <= 0L) {
            this.endByScore();
        }
    }

    private void endByScore() {
        int s2;
        int s1 = this.activeWar.score(this.activeWar.guild1);
        if (s1 > (s2 = this.activeWar.score(this.activeWar.guild2))) {
            this.endWar("Score " + s1 + " - " + s2, this.activeWar.guild1);
        } else if (s2 > s1) {
            this.endWar("Score " + s1 + " - " + s2, this.activeWar.guild2);
        } else {
            this.endWar("Score seri " + s1 + " - " + s2, null);
        }
    }

    private void endWar(String reason, String winnerTag) {
        if (this.ticker != null) {
            this.ticker.cancel();
            this.ticker = null;
        }
        if (this.activeWar == null) {
            return;
        }
        War war = this.activeWar;
        this.activeWar = null;
        int reward = this.plugin.getConfig().getInt("guild-war.reward-emerald", 2);
        if (winnerTag != null) {
            Guild winner = this.guildManager.getGuildByTag(winnerTag);
            if (winner != null) {
                winner.addEmeraldWallet(reward);
                this.guildManager.save();
            }
            Bukkit.broadcastMessage((String)Text.color(this.pref() + "&aGuild War selesai! Pemenang: &e" + winnerTag + " &7(" + reason + ") &a+" + reward + " Emerald ke Guild Wallet."));
        } else {
            Bukkit.broadcastMessage((String)Text.color(this.pref() + "&eGuild War selesai seri! &7(" + reason + ") &fTidak ada reward."));
        }
        if (this.plugin.getConfig().getBoolean("guild-war.restore-location-after-war", true)) {
            for (UUID uuid : war.participants) {
                Player p = Bukkit.getPlayer((UUID)uuid);
                Location back = war.returnLocations.get(uuid);
                if (p == null || !p.isOnline() || back == null) continue;
                p.teleport(back);
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (this.activeWar == null) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player victim = (Player)entity;
        Player attacker = this.findAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        if (!this.isInWar(victim) || !this.isInWar(attacker)) {
            return;
        }
        String vg = this.activeWar.playerGuild.get(victim.getUniqueId());
        String ag = this.activeWar.playerGuild.get(attacker.getUniqueId());
        if (vg == null || ag == null) {
            return;
        }
        if (vg.equalsIgnoreCase(ag) && this.plugin.getConfig().getBoolean("guild-war.prevent-friendly-fire", true)) {
            event.setCancelled(true);
            return;
        }
        if (!this.activeWar.running) {
            event.setCancelled(true);
        }
    }

    private Player findAttacker(Object damager) {
        Projectile proj;
        ProjectileSource projectileSource;
        if (damager instanceof Player) {
            Player p = (Player)damager;
            return p;
        }
        if (damager instanceof Projectile && (projectileSource = (proj = (Projectile)damager).getShooter()) instanceof Player) {
            Player p = (Player)projectileSource;
            return p;
        }
        return null;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player killer;
        if (this.activeWar == null) {
            return;
        }
        Player victim = event.getEntity();
        if (!this.isInWar(victim)) {
            return;
        }
        if (this.plugin.getConfig().getBoolean("guild-war.keep-inventory-during-war", true)) {
            event.setKeepInventory(true);
            event.getDrops().clear();
        }
        if (this.plugin.getConfig().getBoolean("guild-war.keep-level-during-war", true)) {
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
        if ((killer = victim.getKiller()) != null && this.isInWar(killer)) {
            String kg = this.activeWar.playerGuild.get(killer.getUniqueId());
            String vg = this.activeWar.playerGuild.get(victim.getUniqueId());
            if (kg != null && vg != null && !kg.equalsIgnoreCase(vg)) {
                this.activeWar.kills.put(killer.getUniqueId(), this.activeWar.kills.getOrDefault(killer.getUniqueId(), 0) + 1);
                if (this.plugin.getConfig().getBoolean("guild-war.announce-kills", true)) {
                    this.broadcastWar("&e" + killer.getName() + " &7membunuh &c" + victim.getName() + " &8(&b" + kg + " &7+1&8)");
                }
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (this.activeWar == null) {
            return;
        }
        Player p = event.getPlayer();
        if (!this.isInWar(p)) {
            return;
        }
        if (!this.plugin.getConfig().getBoolean("guild-war.respawn-continue", true)) {
            return;
        }
        String tag = this.activeWar.playerGuild.get(p.getUniqueId());
        Location loc = tag != null && tag.equalsIgnoreCase(this.activeWar.guild1) ? this.sideLocation("side1") : this.sideLocation("side2");
        event.setRespawnLocation(loc);
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (p.isOnline() && this.activeWar != null && this.isInWar(p)) {
                p.setHealth(Math.min(p.getMaxHealth(), 20.0));
                p.setFoodLevel(20);
                p.setSaturation(10.0f);
                p.sendTitle(Text.color("&cKamu mati!"), Text.color("&7Respawn ke spawn guild, lanjut main."), 5, 35, 10);
            }
        }, 2L);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onCommandDuringWar(PlayerCommandPreprocessEvent event) {
        if (this.activeWar == null) {
            return;
        }
        Player player = event.getPlayer();
        if (!this.isInWar(player)) {
            return;
        }
        String raw = event.getMessage();
        if (raw == null || raw.isBlank()) {
            return;
        }
        String first = raw.split("\\s+")[0].toLowerCase(Locale.ROOT);
        if (first.startsWith("/")) {
            first = first.substring(1);
        }
        if (first.contains(":")) {
            first = first.substring(first.indexOf(58) + 1);
        }
        if (this.isCommandAllowedDuringWar(first, player)) {
            return;
        }
        event.setCancelled(true);
        Text.msg((CommandSender)player, this.pref() + this.plugin.getConfig().getString("guild-war.command-blocker.block-message", "&cKamu tidak bisa memakai command saat Guild War."));
    }

    private boolean isCommandAllowedDuringWar(String command, Player player) {
        if (player.hasPermission("rumahkitaguilds.war.commandbypass") || player.hasPermission("rumahkitaguilds.admin") || player.hasPermission("rumahkitaguilds.war.admin")) {
            return true;
        }
        if (!this.plugin.getConfig().getBoolean("guild-war.command-blocker.enabled", true)) {
            return true;
        }
        if (this.plugin.getConfig().getBoolean("guild-war.command-blocker.block-all", true)) {
            List allowed = this.plugin.getConfig().getStringList("guild-war.command-blocker.allowed-commands");
            for (String allow : allowed) {
                String clean = allow.toLowerCase(Locale.ROOT).replace("/", "").trim();
                if (!command.equals(clean)) continue;
                return true;
            }
            return false;
        }
        List blocked = this.plugin.getConfig().getStringList("guild-war.command-blocker.blocked-commands");
        for (String block : blocked) {
            String clean = block.toLowerCase(Locale.ROOT).replace("/", "").trim();
            if (!command.equals(clean)) continue;
            return false;
        }
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
    }

    private void status(Player player) {
        if (this.activeWar == null) {
            Text.msg((CommandSender)player, this.pref() + "&7Tidak ada Guild War aktif.");
            return;
        }
        Text.msg((CommandSender)player, "&8&m------------------------");
        Text.msg((CommandSender)player, "&dGuild War: &f" + this.activeWar.guild1 + " &7vs &f" + this.activeWar.guild2);
        Text.msg((CommandSender)player, "&7Status: " + (this.activeWar.running ? "&aRUNNING" : "&eCOUNTDOWN"));
        Text.msg((CommandSender)player, "&7Score: &f" + this.activeWar.guild1 + " &e" + this.activeWar.score(this.activeWar.guild1) + " &7- &f" + this.activeWar.guild2 + " &e" + this.activeWar.score(this.activeWar.guild2));
        Text.msg((CommandSender)player, "&8&m------------------------");
    }

    private void stop(Player player) {
        boolean allowed;
        Guild guild = this.guildManager.getGuild(player);
        boolean bl = allowed = player.hasPermission("rumahkitaguilds.war.admin") || player.hasPermission("rumahkitaguilds.admin");
        if (!allowed && this.activeWar != null && guild != null && this.canManageWar(guild, player.getUniqueId())) {
            String tag = guild.getTag();
            boolean bl2 = allowed = tag.equalsIgnoreCase(this.activeWar.guild1) || tag.equalsIgnoreCase(this.activeWar.guild2);
        }
        if (!allowed) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        if (this.activeWar == null) {
            Text.msg((CommandSender)player, this.pref() + "&cTidak ada Guild War aktif.");
            return;
        }
        this.endWar("Dihentikan manual", null);
    }

    private void help(Player player) {
        Text.msg((CommandSender)player, "&8&m------------------------");
        Text.msg((CommandSender)player, "&dGuild War Commands");
        Text.msg((CommandSender)player, "&e/guild war <tag> &7- leader/admin ajukan war ke guild lain");
        Text.msg((CommandSender)player, "&e/guild war accept <tag> &7- leader/admin accept war");
        Text.msg((CommandSender)player, "&e/guild war deny &7- leader/admin tolak war");
        Text.msg((CommandSender)player, "&e/guild war status &7- cek war aktif");
        Text.msg((CommandSender)player, "&e/guild wallet &7- cek wallet guild");
        Text.msg((CommandSender)player, "&e/guild wallet withdraw <amount|all> &7- leader/admin ambil emerald");
        Text.msg((CommandSender)player, "&8&m------------------------");
    }

    private List<Player> onlineMembers(Guild guild) {
        return guild.getMembers().keySet().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).filter(OfflinePlayer::isOnline).collect(Collectors.toList());
    }

    private List<Player> activePlayers() {
        if (this.activeWar == null) {
            return List.of();
        }
        return this.activeWar.participants.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).filter(OfflinePlayer::isOnline).collect(Collectors.toList());
    }

    private boolean isInWar(Player player) {
        return this.activeWar != null && this.activeWar.participants.contains(player.getUniqueId());
    }

    private Location sideLocation(String side) {
        String base = "guild-war.arena." + side;
        String worldName = this.plugin.getConfig().getString("guild-war.arena.world", "world");
        World world = Bukkit.getWorld((String)worldName);
        if (world == null) {
            world = (World)Bukkit.getWorlds().get(0);
        }
        return new Location(world, this.plugin.getConfig().getDouble(base + ".x"), this.plugin.getConfig().getDouble(base + ".y"), this.plugin.getConfig().getDouble(base + ".z"), (float)this.plugin.getConfig().getDouble(base + ".yaw"), (float)this.plugin.getConfig().getDouble(base + ".pitch"));
    }

    private void broadcastWar(String msg) {
        for (Player p : this.activePlayers()) {
            Text.msg((CommandSender)p, this.pref() + msg);
        }
    }

    private Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            Text.msg(sender, Text.prefixed(this.plugin, "only-player"));
            return null;
        }
        Player p = (Player)sender;
        return p;
    }

    private boolean canManageWar(Guild guild, UUID uuid) {
        return guild != null && guild.getRole(uuid).atLeast(GuildRole.ADMIN);
    }

    private String pref() {
        return this.plugin.getConfig().getString("settings.prefix", "&8[&bRumahKitaGuilds&8] ");
    }

    private static final class War {
        final String guild1;
        final String guild2;
        final Set<UUID> participants = new HashSet<UUID>();
        final Map<UUID, String> playerGuild = new HashMap<UUID, String>();
        final Map<UUID, Location> returnLocations = new HashMap<UUID, Location>();
        final Map<UUID, Integer> kills = new HashMap<UUID, Integer>();
        boolean running;
        int countdownLeft;
        long endsAt;

        War(String guild1, String guild2) {
            this.guild1 = guild1.toUpperCase(Locale.ROOT);
            this.guild2 = guild2.toUpperCase(Locale.ROOT);
        }

        int score(String guildTag) {
            int total = 0;
            for (Map.Entry<UUID, Integer> e : this.kills.entrySet()) {
                String g = this.playerGuild.get(e.getKey());
                if (g == null || !g.equalsIgnoreCase(guildTag)) continue;
                total += e.getValue().intValue();
            }
            return total;
        }
    }

    private record Challenge(String challengerTag, String targetTag, UUID sender, long expireAt) {
    }
}

