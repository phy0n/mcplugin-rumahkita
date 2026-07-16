/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.plugin.Plugin
 */
package id.rumahkita.fishing.manager;

import id.rumahkita.fishing.RumahKitaFishingPlugin;
import id.rumahkita.fishing.model.PlayerFishingData;
import id.rumahkita.fishing.util.NumberUtil;
import id.rumahkita.fishing.util.Text;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class LeaderboardManager {
    private final RumahKitaFishingPlugin plugin;

    public LeaderboardManager(RumahKitaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendLeaderboard(CommandSender sender, String type) {
        Text.send(sender, this.plugin.messagesConfig().get(), "leaderboard-loading", Map.of());
        Runnable task = () -> {
            List<Row> rows = this.loadRows(type);
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.render(sender, type, rows));
        };
        if (this.plugin.getConfig().getBoolean("leaderboard.async-load", true)) {
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, task);
        } else {
            task.run();
        }
    }

    private List<Row> loadRows(String rawType) {
        String type = this.normalizeType(rawType);
        ArrayList<Row> rows = new ArrayList<Row>();
        for (PlayerFishingData data : this.plugin.playerDataManager().cache().values()) {
            rows.add(this.row(type, data.name(), data));
        }
        File folder = this.plugin.playerDataManager().playersFolder();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                    if (this.plugin.playerDataManager().cache().containsKey(uuid)) continue;
                    YamlConfiguration config = YamlConfiguration.loadConfiguration((File)file);
                    PlayerFishingData data = new PlayerFishingData(uuid, config.getString("name", uuid.toString()));
                    data.level(config.getInt("level", 1));
                    data.totalCatches(config.getLong("total_catches", 0L));
                    data.totalEarned(config.getLong("total_earned", 0L));
                    data.biggestFishWeight(config.getDouble("biggest_fish_weight", 0.0));
                    data.biggestFishName(config.getString("biggest_fish_name", "-"));
                    rows.add(this.row(type, data.name(), data));
                }
                catch (IllegalArgumentException ignored) {
                    this.plugin.getLogger().warning("Invalid player data filename: " + file.getName());
                }
            }
        }
        rows.sort(Comparator.comparingDouble(Row::value).reversed());
        int max = Math.max(1, this.plugin.getConfig().getInt("leaderboard.max-entries", 10));
        if (rows.size() > max) {
            return new ArrayList<Row>(rows.subList(0, max));
        }
        return rows;
    }

    private Row row(String type, String name, PlayerFishingData data) {
        return switch (type) {
            case "catches" -> new Row(name, data.totalCatches(), NumberUtil.money(data.totalCatches()) + " ikan");
            case "money" -> new Row(name, data.totalEarned(), "Rp " + NumberUtil.money(data.totalEarned()));
            case "level" -> new Row(name, data.level(), "Level " + data.level());
            default -> new Row(name, data.biggestFishWeight(), NumberUtil.weight(data.biggestFishWeight()) + " kg");
        };
    }

    private void render(CommandSender sender, String rawType, List<Row> rows) {
        String type = this.normalizeType(rawType);
        Text.send(sender, this.plugin.messagesConfig().get(), "leaderboard-header", Map.of("type", type));
        if (rows.isEmpty()) {
            Text.send(sender, this.plugin.messagesConfig().get(), "leaderboard-empty", Map.of());
            return;
        }
        int rank = 1;
        for (Row row : rows) {
            Text.send(sender, this.plugin.messagesConfig().get(), "leaderboard-line", Map.of("rank", String.valueOf(rank), "name", row.name(), "value", row.display()));
            ++rank;
        }
    }

    private String normalizeType(String type) {
        if (type == null) {
            return "weight";
        }
        String normalized = type.toLowerCase(Locale.ROOT);
        if (normalized.equals("catch")) {
            return "catches";
        }
        if (normalized.equals("earned") || normalized.equals("money")) {
            return "money";
        }
        if (normalized.equals("level")) {
            return "level";
        }
        return "weight";
    }

    private record Row(String name, double value, String display) {
    }
}

