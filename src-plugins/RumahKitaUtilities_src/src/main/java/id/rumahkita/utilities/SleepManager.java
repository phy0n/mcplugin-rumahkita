/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.World
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerBedEnterEvent
 *  org.bukkit.event.player.PlayerBedEnterEvent$BedEnterResult
 *  org.bukkit.event.player.PlayerBedLeaveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 */
package id.rumahkita.utilities;

import id.rumahkita.utilities.RumahKitaUtilitiesPlugin;
import id.rumahkita.utilities.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class SleepManager
implements Listener,
TabExecutor {
    private final RumahKitaUtilitiesPlugin plugin;

    public SleepManager(RumahKitaUtilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled=true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (!this.plugin.getConfig().getBoolean("sleep.enabled", true)) {
            return;
        }
        if (!event.getPlayer().hasPermission("rumahkita.sleep.use")) {
            return;
        }
        if (!this.isWorldAllowed(event.getPlayer().getWorld())) {
            return;
        }
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }
        int delay = Math.max(1, this.plugin.getConfig().getInt("sleep.delay-after-bed-enter-ticks", 20));
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.checkWorld(event.getPlayer().getWorld(), event.getPlayer()), (long)delay);
    }

    @EventHandler
    public void onBedLeave(PlayerBedLeaveEvent event) {
        if (!this.plugin.getConfig().getBoolean("sleep.enabled", true)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.checkWorld(event.getPlayer().getWorld(), null), 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!this.plugin.getConfig().getBoolean("sleep.enabled", true)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            World world = event.getPlayer().getWorld();
            if (world != null) {
                this.checkWorld(world, null);
            }
        }, 5L);
    }

    private void checkWorld(World world, Player sleeper) {
        int needed;
        if (world == null || !this.isWorldAllowed(world)) {
            return;
        }
        if (!this.isNightOrThunder(world)) {
            return;
        }
        ArrayList<Player> counted = new ArrayList<Player>();
        int sleeping = 0;
        for (Player player : world.getPlayers()) {
            if (!this.countsForSleep(player)) continue;
            counted.add(player);
            if (!player.isSleeping()) continue;
            ++sleeping;
        }
        int online = counted.size();
        if (online <= 0) {
            return;
        }
        String mode = this.plugin.getConfig().getString("sleep.mode", "SINGLE_PLAYER").toUpperCase(Locale.ROOT);
        if (mode.equals("PERCENTAGE")) {
            int percent = Math.max(1, this.plugin.getConfig().getInt("sleep.required-percentage", 25));
            needed = (int)Math.ceil((double)online * ((double)percent / 100.0));
            needed = Math.max(needed, this.plugin.getConfig().getInt("sleep.min-sleeping-players", 1));
        } else {
            needed = Math.max(1, this.plugin.getConfig().getInt("sleep.min-sleeping-players", 1));
        }
        if (sleeping >= needed) {
            this.skip(world, sleeper);
        }
    }

    private boolean countsForSleep(Player player) {
        GameMode gm = player.getGameMode();
        if (gm == GameMode.SURVIVAL && this.plugin.getConfig().getBoolean("sleep.count-survival", true)) {
            return true;
        }
        if (gm == GameMode.ADVENTURE && this.plugin.getConfig().getBoolean("sleep.count-adventure", true)) {
            return true;
        }
        if (gm == GameMode.CREATIVE && !this.plugin.getConfig().getBoolean("sleep.ignore-creative", true)) {
            return true;
        }
        return gm == GameMode.SPECTATOR && !this.plugin.getConfig().getBoolean("sleep.ignore-spectator", true);
    }

    private boolean isNightOrThunder(World world) {
        long time = world.getTime();
        return world.isThundering() || time >= 12541L && time <= 23458L;
    }

    private void skip(World world, Player sleeper) {
        if (this.plugin.getConfig().getBoolean("sleep.skip-night", true)) {
            world.setTime(this.plugin.getConfig().getLong("sleep.set-time", 0L));
        }
        if (this.plugin.getConfig().getBoolean("sleep.clear-thunder", true)) {
            world.setThundering(false);
        }
        if (this.plugin.getConfig().getBoolean("sleep.clear-rain", true)) {
            world.setStorm(false);
        }
        for (Player player : world.getPlayers()) {
            if (!player.isSleeping()) continue;
            try {
                player.wakeup(false);
            }
            catch (Exception exception) {}
        }
        String name = sleeper == null ? "Seseorang" : sleeper.getName();
        String broadcast = this.plugin.getConfig().getString("sleep.broadcast", "&8[&bSleep&8] &f%sleeper% &7tidur. Malam dilewati.").replace("%sleeper%", name);
        Bukkit.broadcastMessage((String)Text.color(broadcast));
        if (this.plugin.getConfig().getBoolean("sleep.title-enabled", true)) {
            String title = this.plugin.getConfig().getString("sleep.title", "&b&lSelamat Pagi!").replace("%sleeper%", name);
            String subtitle = this.plugin.getConfig().getString("sleep.subtitle", "&7Malam dilewati karena &f%sleeper% &7tidur.").replace("%sleeper%", name);
            for (Player player : world.getPlayers()) {
                player.sendTitle(Text.color(title), Text.color(subtitle), 10, 50, 20);
            }
        }
    }

    private boolean isWorldAllowed(World world) {
        List worlds = this.plugin.getConfig().getStringList("sleep.worlds");
        return worlds.isEmpty() || worlds.contains(world.getName());
    }

    private String pref() {
        return this.plugin.getConfig().getString("messages.prefix", "&8[&bRumahKitaUtilities&8] ");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rumahkita.utilities.admin")) {
            Text.msg(sender, this.pref() + this.plugin.getConfig().getString("messages.no-permission", "&cKamu tidak punya permission."));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            Text.msg(sender, this.pref() + "&7Sleep: &f" + this.plugin.getConfig().getBoolean("sleep.enabled", true) + " &8| &7Mode: &f" + this.plugin.getConfig().getString("sleep.mode", "SINGLE_PLAYER"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            this.plugin.reloadAll();
            Text.msg(sender, this.pref() + this.plugin.getConfig().getString("messages.reloaded", "&aConfig berhasil direload."));
            return true;
        }
        if (args[0].equalsIgnoreCase("on")) {
            this.plugin.getConfig().set("sleep.enabled", (Object)true);
            this.plugin.saveConfig();
            Text.msg(sender, this.pref() + this.plugin.getConfig().getString("messages.enabled", "&aFitur diaktifkan."));
            return true;
        }
        if (args[0].equalsIgnoreCase("off")) {
            this.plugin.getConfig().set("sleep.enabled", (Object)false);
            this.plugin.saveConfig();
            Text.msg(sender, this.pref() + this.plugin.getConfig().getString("messages.disabled", "&cFitur dimatikan."));
            return true;
        }
        Text.msg(sender, "&e/rksleep <status|reload|on|off>");
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("status", "reload", "on", "off");
        }
        return List.of();
    }
}

