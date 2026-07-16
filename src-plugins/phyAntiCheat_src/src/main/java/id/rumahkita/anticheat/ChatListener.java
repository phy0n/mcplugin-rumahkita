package id.rumahkita.anticheat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatListener implements Listener {

    private final PhyAntiCheat plugin;
    private final Map<UUID, Long> lastChatTime = new HashMap<>();
    private final Map<UUID, String> lastChatMessage = new HashMap<>();

    public ChatListener(PhyAntiCheat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();

        if (PACCommand.mutedPlayers.contains(p.getUniqueId())) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "[PAC] You are muted and cannot speak.");
            return;
        }

        if (p.hasPermission(plugin.getConfig().getString("settings.bypass-permission", "phyanticheat.bypass"))) return;

        if (!plugin.getConfig().getBoolean("modules.chat.anti-spam", true)) return;

        long now = System.currentTimeMillis();
        long lastTime = lastChatTime.getOrDefault(p.getUniqueId(), 0L);
        String lastMsg = lastChatMessage.getOrDefault(p.getUniqueId(), "");
        long cooldown = plugin.getConfig().getLong("modules.chat.cooldown-ms", 2000);
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("settings.prefix", "&8[&cPAC&8] &7"));

        if (now - lastTime < cooldown) {
            e.setCancelled(true);
            p.sendMessage(prefix + ChatColor.RED + "Please do not spam the chat.");
            return;
        }

        if (e.getMessage().equalsIgnoreCase(lastMsg)) {
            e.setCancelled(true);
            p.sendMessage(prefix + ChatColor.RED + "Please do not repeat the same message.");
            return;
        }

        lastChatTime.put(p.getUniqueId(), now);
        lastChatMessage.put(p.getUniqueId(), e.getMessage());
    }
}
