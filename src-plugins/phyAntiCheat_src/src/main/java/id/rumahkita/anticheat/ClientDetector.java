package id.rumahkita.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;

public class ClientDetector implements PluginMessageListener, Listener {

    private final PhyAntiCheat plugin;

    public ClientDetector(PhyAntiCheat plugin) {
        this.plugin = plugin;
        try {
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "minecraft:brand", this);
        } catch (Exception e) {}
        try {
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "minecraft:register", this);
        } catch (Exception e) {}
        
        // Daftarkan event pendeteksi jalur rahasia
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChannelRegister(PlayerRegisterChannelEvent e) {
        checkAndKick(e.getPlayer(), e.getChannel().toLowerCase());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        String data = new String(message, StandardCharsets.UTF_8).toLowerCase();
        checkAndKick(player, data);
    }
    
    private void checkAndKick(Player player, String data) {
        if (data == null || data.isEmpty()) return;
        
        String bypassPerm = plugin.getConfig().getString("settings.bypass-permission", "phyanticheat.bypass");
        if (player.hasPermission(bypassPerm)) return;
        
        for (String bad : plugin.getConfig().getStringList("modules.client-detector.blacklisted-brands")) {
            if (data.contains(bad.toLowerCase())) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.kickPlayer(ChatColor.RED + "Illegal mods/clients are strictly prohibited on this server!\n\n" + ChatColor.GRAY + "Detected: " + ChatColor.YELLOW + bad);
                        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("settings.prefix", "&8[&cPAC&8] &7"));
                        String alertPerm = plugin.getConfig().getString("settings.alert-permission", "phyanticheat.admin");
                        Bukkit.broadcast(prefix + ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " was kicked for using an illegal client/mod (" + bad + ").", alertPerm);
                        plugin.getLogManager().logViolation(player.getName(), "Illegal Client/Mod", "Brand/Channel detected: " + bad);
                    }
                });
                break;
            }
        }
    }
}
