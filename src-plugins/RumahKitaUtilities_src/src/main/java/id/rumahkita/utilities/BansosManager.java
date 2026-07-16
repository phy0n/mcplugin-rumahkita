/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.Sound
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package id.rumahkita.utilities;

import id.rumahkita.utilities.RumahKitaUtilitiesPlugin;
import id.rumahkita.utilities.Text;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class BansosManager
implements TabExecutor {
    private final RumahKitaUtilitiesPlugin plugin;
    private final File dataFile;
    private YamlConfiguration data;
    private int taskId = -1;

    public BansosManager(RumahKitaUtilitiesPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.reloadData();
    }

    public void reloadData() {
        this.data = YamlConfiguration.loadConfiguration((File)this.dataFile);
    }

    public void saveData() {
        try {
            this.data.save(this.dataFile);
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Gagal save data.yml: " + e.getMessage());
        }
    }

    public void startScheduler() {
        if (this.taskId != -1) {
            Bukkit.getScheduler().cancelTask(this.taskId);
        }
        this.taskId = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, this::checkSchedule, 100L, 1200L).getTaskId();
    }

    private void checkSchedule() {
        String last;
        boolean shouldRun;
        if (!this.plugin.getConfig().getBoolean("bansos.enabled", true)) {
            return;
        }
        ZoneId zone = ZoneId.of(this.plugin.getConfig().getString("bansos.timezone", "Asia/Jakarta"));
        ZonedDateTime now = ZonedDateTime.now(zone);
        DayOfWeek targetDay = DayOfWeek.valueOf(this.plugin.getConfig().getString("bansos.day-of-week", "FRIDAY").toUpperCase(Locale.ROOT));
        if (now.getDayOfWeek() != targetDay) {
            return;
        }
        LocalTime targetTime = LocalTime.parse(this.plugin.getConfig().getString("bansos.time", "20:00"));
        boolean catchUp = this.plugin.getConfig().getBoolean("bansos.catch-up-same-day", true);
        if (catchUp) {
            shouldRun = !now.toLocalTime().isBefore(targetTime);
        } else {
            boolean bl = shouldRun = now.toLocalTime().getHour() == targetTime.getHour() && now.toLocalTime().getMinute() == targetTime.getMinute();
        }
        if (!shouldRun) {
            return;
        }
        String today = now.toLocalDate().toString();
        if (today.equals(last = this.data.getString("bansos.last-run-date", ""))) {
            return;
        }
        this.runBansos("scheduled");
        this.data.set("bansos.last-run-date", (Object)today);
        this.data.set("bansos.last-run-time", (Object)now.toString());
        this.saveData();
    }

    private void runBansos(String reason) {
        List<Reward> rewards = this.loadRewards();
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Reward reward : rewards) {
                HashMap leftover = player.getInventory().addItem(new ItemStack[]{new ItemStack(reward.material, reward.amount)});
                for (ItemStack item : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
            if (this.plugin.getConfig().getBoolean("bansos.title-enabled", true)) {
                player.sendTitle(Text.color(this.plugin.getConfig().getString("bansos.title", "&a&lBANSOS JUMAT")), Text.color(this.plugin.getConfig().getString("bansos.subtitle", "&b+5 Diamond &a+10 Emerald &c+20 Beef")), 10, 70, 25);
            }
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);
            }
            catch (Exception exception) {
                // empty catch block
            }
            ++count;
        }
        if (this.plugin.getConfig().getBoolean("bansos.broadcast-enabled", true)) {
            for (String line : this.plugin.getConfig().getStringList("bansos.broadcast-lines")) {
                Bukkit.broadcastMessage((String)Text.color(line));
            }
        }
        if (this.plugin.getConfig().getBoolean("bansos.discord.enabled", true)) {
            this.sendWebhook(count, rewards, reason);
        }
        this.plugin.getLogger().info("Bansos dijalankan. Player online: " + count);
    }

    private List<Reward> loadRewards() {
        ArrayList<Reward> list = new ArrayList<Reward>();
        List raw = this.plugin.getConfig().getMapList("bansos.rewards");
        for (Map map : raw) {
            String materialRaw = map.containsKey("material") ? map.get("material") : "DIAMOND";
            String amountRaw = map.containsKey("amount") ? map.get("amount") : "1";
            String materialName = String.valueOf(materialRaw).toUpperCase(Locale.ROOT);
            int amount = Integer.parseInt(String.valueOf(amountRaw));
            Material material = Material.matchMaterial((String)materialName);
            if (material == null || material.isAir()) {
                this.plugin.getLogger().warning("Material bansos invalid: " + materialName);
                continue;
            }
            list.add(new Reward(material, Math.max(1, amount)));
        }
        if (list.isEmpty()) {
            list.add(new Reward(Material.DIAMOND, 5));
            list.add(new Reward(Material.EMERALD, 10));
            list.add(new Reward(Material.COOKED_BEEF, 20));
        }
        return list;
    }

    private void sendWebhook(int online, List<Reward> rewards, String reason) {
        String url = this.plugin.getConfig().getString("bansos.discord.webhook-url", "");
        if (url == null || url.isBlank() || url.contains("PASTE_WEBHOOK")) {
            this.plugin.getLogger().warning("Webhook bansos belum diisi di config.yml.");
            return;
        }
        String content = this.plugin.getConfig().getString("bansos.discord.content", "Bansos sudah dibagikan!");
        String title = this.plugin.getConfig().getString("bansos.discord.embed-title", "Bansos Jumat RumahKita S2");
        String desc = this.plugin.getConfig().getString("bansos.discord.embed-description", "Bansos sudah dibagikan.");
        int color = this.plugin.getConfig().getInt("bansos.discord.embed-color", 5763719);
        String username = this.plugin.getConfig().getString("bansos.discord.username", "RumahKita Bansos");
        String avatar = this.plugin.getConfig().getString("bansos.discord.avatar-url", "");
        String roleId = this.plugin.getConfig().getString("bansos.discord.role-id", "");
        StringBuilder rewardText = new StringBuilder();
        for (Reward r : rewards) {
            if (!rewardText.isEmpty()) {
                rewardText.append("\\n");
            }
            rewardText.append("\u2022 ").append(r.amount).append("x ").append(r.material.name());
        }
        String json = "{\"username\":\"" + BansosManager.esc(username) + "\"," + (String)(avatar == null || avatar.isBlank() ? "" : "\"avatar_url\":\"" + BansosManager.esc(avatar) + "\",") + "\"content\":\"" + BansosManager.esc(content) + "\",\"allowed_mentions\":{\"roles\":[\"" + BansosManager.esc(roleId) + "\"]},\"embeds\":[{\"title\":\"" + BansosManager.esc(title) + "\",\"description\":\"" + BansosManager.esc(desc) + "\",\"color\":" + color + ",\"fields\":[{\"name\":\"Player Online\",\"value\":\"" + online + "\",\"inline\":true},{\"name\":\"Reward\",\"value\":\"" + BansosManager.esc(rewardText.toString()) + "\",\"inline\":false},{\"name\":\"Reason\",\"value\":\"" + BansosManager.esc(reason) + "\",\"inline\":true}],\"footer\":{\"text\":\"RumahKita S2\"}}]}";
        try {
            HttpClient.newHttpClient().sendAsync(HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString()).thenAccept(res -> {
                if (res.statusCode() < 200 || res.statusCode() >= 300) {
                    this.plugin.getLogger().warning("Webhook bansos gagal. HTTP " + res.statusCode() + " " + (String)res.body());
                }
            });
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Gagal kirim webhook bansos: " + e.getMessage());
        }
    }

    private static String esc(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private String pref() {
        return this.plugin.getConfig().getString("messages.prefix", "&8[&bRumahKitaUtilities&8] ");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rumahkita.bansos.admin") && !sender.hasPermission("rumahkita.utilities.admin")) {
            Text.msg(sender, this.pref() + this.plugin.getConfig().getString("messages.no-permission", "&cKamu tidak punya permission."));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            ZoneId zone = ZoneId.of(this.plugin.getConfig().getString("bansos.timezone", "Asia/Jakarta"));
            Text.msg(sender, this.pref() + "&7Bansos: &f" + this.plugin.getConfig().getBoolean("bansos.enabled", true));
            Text.msg(sender, "&7Jadwal: &f" + this.plugin.getConfig().getString("bansos.day-of-week", "FRIDAY") + " " + this.plugin.getConfig().getString("bansos.time", "20:00") + " " + String.valueOf(zone));
            Text.msg(sender, "&7Last Run: &f" + this.data.getString("bansos.last-run-date", "-"));
            return true;
        }
        if (args[0].equalsIgnoreCase("now")) {
            this.runBansos("manual");
            Text.msg(sender, this.pref() + "&aBansos manual dijalankan.");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            this.plugin.reloadAll();
            Text.msg(sender, this.pref() + this.plugin.getConfig().getString("messages.reloaded", "&aConfig berhasil direload."));
            return true;
        }
        Text.msg(sender, "&e/rkbansos <status|now|reload>");
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("status", "now", "reload");
        }
        return List.of();
    }

    private record Reward(Material material, int amount) {
    }
}

