/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityPickupItemEvent
 *  org.bukkit.event.entity.EntityTargetLivingEntityEvent
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffectType
 */
package id.rumahkita.utilities;

import id.rumahkita.utilities.RumahKitaUtilitiesPlugin;
import id.rumahkita.utilities.Text;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

public final class VanishManager
implements Listener,
TabExecutor {
    private final RumahKitaUtilitiesPlugin plugin;
    private final Set<UUID> vanished = new HashSet<UUID>();
    private final File file;
    private YamlConfiguration data;

    public VanishManager(RumahKitaUtilitiesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "vanish.yml");
        this.reloadData();
    }

    public void reloadData() {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }
        if (!this.file.exists()) {
            try {
                this.file.createNewFile();
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("Gagal membuat vanish.yml: " + e.getMessage());
            }
        }
        this.data = YamlConfiguration.loadConfiguration((File)this.file);
        this.vanished.clear();
        for (String raw : this.data.getStringList("vanished")) {
            try {
                this.vanished.add(UUID.fromString(raw));
            }
            catch (Exception exception) {}
        }
    }

    public void saveData() {
        List ids = this.vanished.stream().map(UUID::toString).sorted().collect(Collectors.toList());
        this.data.set("vanished", ids);
        try {
            this.data.save(this.file);
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Gagal save vanish.yml: " + e.getMessage());
        }
    }

    public boolean isVanished(UUID uuid) {
        return this.vanished.contains(uuid);
    }

    public int vanishedCount() {
        return this.vanished.size();
    }

    public void reapplyAll() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!this.isVanished(online.getUniqueId())) continue;
            this.applyVanishState(online, false);
        }
        this.refreshVisibilityForAll();
    }

    public void showAllOnDisable() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (UUID id : this.vanished) {
                Player target = Bukkit.getPlayer((UUID)id);
                if (target == null) continue;
                viewer.showPlayer((Plugin)this.plugin, target);
            }
        }
    }

    private void setVanished(Player target, boolean value, boolean save) {
        if (value) {
            this.vanished.add(target.getUniqueId());
            this.applyVanishState(target, true);
        } else {
            this.vanished.remove(target.getUniqueId());
            this.applyUnvanishState(target, true);
        }
        if (save && this.plugin.getConfig().getBoolean("vanish.persist", true)) {
            this.saveData();
        }
        this.refreshVisibilityForAll();
    }

    private void applyVanishState(Player target, boolean sendMessage) {
        if (this.plugin.getConfig().getBoolean("vanish.remove-invisibility-effect", true)) {
            target.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
        if (this.plugin.getConfig().getBoolean("vanish.disable-collision", true)) {
            target.setCollidable(false);
        }
        if (!this.plugin.getConfig().getBoolean("vanish.silent-game-mode", false) || target.getGameMode() != GameMode.SPECTATOR) {
            // empty if block
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            this.updateVisibility(viewer, target);
        }
        if (sendMessage) {
            this.msg(target, "enabled");
        }
    }

    private void applyUnvanishState(Player target, boolean sendMessage) {
        target.setCollidable(true);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.showPlayer((Plugin)this.plugin, target);
        }
        if (sendMessage) {
            this.msg(target, "disabled");
        }
    }

    private void refreshVisibilityForAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (UUID id : this.vanished) {
                Player target = Bukkit.getPlayer((UUID)id);
                if (target == null) continue;
                this.updateVisibility(viewer, target);
            }
        }
    }

    private void updateVisibility(Player viewer, Player target) {
        if (viewer.getUniqueId().equals(target.getUniqueId())) {
            viewer.showPlayer((Plugin)this.plugin, target);
            return;
        }
        if (!this.isVanished(target.getUniqueId())) {
            viewer.showPlayer((Plugin)this.plugin, target);
            return;
        }
        if (viewer.hasPermission(this.getSeePermission())) {
            viewer.showPlayer((Plugin)this.plugin, target);
        } else {
            viewer.hidePlayer((Plugin)this.plugin, target);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (this.isVanished(joined.getUniqueId())) {
                this.applyVanishState(joined, false);
                if (this.plugin.getConfig().getBoolean("vanish.hide-join-quit-message", true)) {
                    event.setJoinMessage(null);
                }
            }
            this.refreshVisibilityForAll();
        }, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (this.isVanished(event.getPlayer().getUniqueId()) && this.plugin.getConfig().getBoolean("vanish.hide-join-quit-message", true)) {
            event.setQuitMessage(null);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onPickup(EntityPickupItemEvent event) {
        Player player;
        if (!this.plugin.getConfig().getBoolean("vanish.block-item-pickup", true)) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player && this.isVanished((player = (Player)entity).getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        Player player;
        if (!this.plugin.getConfig().getBoolean("vanish.prevent-mob-target", true)) {
            return;
        }
        LivingEntity target = event.getTarget();
        if (target instanceof Player && this.isVanished((player = (Player)target).getUniqueId())) {
            event.setCancelled(true);
            event.setTarget(null);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!this.plugin.getConfig().getBoolean("vanish.block-chat-while-vanished", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (!this.isVanished(player.getUniqueId())) {
            return;
        }
        if (player.hasPermission("rumahkita.vanish.chatbypass")) {
            return;
        }
        event.setCancelled(true);
        Text.msg((CommandSender)player, this.plugin.getConfig().getString("vanish.messages.chat-blocked", "&cKamu sedang vanish. Chat diblok agar tidak ketahuan."));
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!this.plugin.getConfig().getBoolean("vanish.enabled", true)) {
            Text.msg(sender, this.plugin.getConfig().getString("vanish.messages.feature-disabled", "&cRKVanish sedang dimatikan."));
            return true;
        }
        if (args.length > 0) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("reload")) {
                if (!sender.hasPermission("rumahkita.vanish.admin") && !sender.hasPermission("rumahkita.utilities.admin")) {
                    Text.msg(sender, this.plugin.getConfig().getString("messages.no-permission", "&cKamu tidak punya permission."));
                    return true;
                }
                this.plugin.reloadAll();
                this.reloadData();
                this.reapplyAll();
                Text.msg(sender, this.plugin.getConfig().getString("messages.prefix", "") + this.plugin.getConfig().getString("messages.reloaded", "&aConfig berhasil direload."));
                return true;
            }
            if (sub.equals("list")) {
                if (!sender.hasPermission("rumahkita.vanish.see") && !sender.hasPermission("rumahkita.vanish.admin")) {
                    Text.msg(sender, this.plugin.getConfig().getString("messages.no-permission", "&cKamu tidak punya permission."));
                    return true;
                }
                ArrayList<String> names = new ArrayList<String>();
                for (UUID id : this.vanished) {
                    Player p = Bukkit.getPlayer((UUID)id);
                    names.add(p != null ? p.getName() : id.toString());
                }
                Text.msg(sender, this.plugin.getConfig().getString("vanish.messages.list", "&bVanished: &f%list%").replace("%list%", names.isEmpty() ? "-" : String.join((CharSequence)", ", names)));
                return true;
            }
            Player target = Bukkit.getPlayerExact((String)args[0]);
            if (target == null) {
                target = Bukkit.getPlayer((String)args[0]);
            }
            if (target != null) {
                if (!sender.hasPermission("rumahkita.vanish.others") && !sender.hasPermission("rumahkita.utilities.admin")) {
                    Text.msg(sender, this.plugin.getConfig().getString("messages.no-permission", "&cKamu tidak punya permission."));
                    return true;
                }
                boolean next = !this.isVanished(target.getUniqueId());
                this.setVanished(target, next, true);
                if (!sender.equals((Object)target)) {
                    Text.msg(sender, this.plugin.getConfig().getString("vanish.messages.toggled-other", "&aRKVanish %player%: %state%").replace("%player%", target.getName()).replace("%state%", next ? "ON" : "OFF"));
                }
                this.notifyStaff(target, next);
                return true;
            }
        }
        if (!(sender instanceof Player)) {
            Text.msg(sender, "&cConsole gunakan: /rkvanish <player>");
            return true;
        }
        Player player = (Player)sender;
        if (!player.hasPermission("rumahkita.vanish.use") && !player.hasPermission("rumahkita.utilities.admin")) {
            Text.msg(sender, this.plugin.getConfig().getString("messages.no-permission", "&cKamu tidak punya permission."));
            return true;
        }
        boolean next = !this.isVanished(player.getUniqueId());
        this.setVanished(player, next, true);
        this.notifyStaff(player, next);
        return true;
    }

    private void notifyStaff(Player target, boolean vanishedNow) {
        if (!this.plugin.getConfig().getBoolean("vanish.notify-staff", true)) {
            return;
        }
        String msg = this.plugin.getConfig().getString("vanish.messages.staff-notify", "&8[&7RKVanish&8] &f%player% &7%state%").replace("%player%", target.getName()).replace("%state%", vanishedNow ? "vanished" : "unvanished");
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!staff.hasPermission("rumahkita.vanish.see") && !staff.hasPermission("rumahkita.vanish.admin")) continue;
            Text.msg((CommandSender)staff, msg);
        }
    }

    private String getSeePermission() {
        return this.plugin.getConfig().getString("vanish.see-permission", "rumahkita.vanish.see");
    }

    private void msg(Player player, String key) {
        String raw = this.plugin.getConfig().getString("vanish.messages." + key, "&aVanish " + key);
        Text.msg((CommandSender)player, raw);
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            ArrayList<String> out = new ArrayList<String>();
            if (sender.hasPermission("rumahkita.vanish.admin")) {
                out.add("reload");
                out.add("list");
            }
            if (sender.hasPermission("rumahkita.vanish.others")) {
                out.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            }
            return out;
        }
        return List.of();
    }
}

