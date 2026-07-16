/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 */
package id.rumahkita.fishing.manager;

import id.rumahkita.fishing.RumahKitaFishingPlugin;
import id.rumahkita.fishing.model.CaughtFish;
import id.rumahkita.fishing.model.PlayerFishingData;
import id.rumahkita.fishing.model.Rarity;
import id.rumahkita.fishing.util.NumberUtil;
import id.rumahkita.fishing.util.Text;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class PlayerDataManager {
    private final RumahKitaFishingPlugin plugin;
    private final File playersFolder;
    private final Map<UUID, PlayerFishingData> cache;

    public PlayerDataManager(RumahKitaFishingPlugin plugin) {
        this.plugin = plugin;
        this.playersFolder = new File(plugin.getDataFolder(), "data/players");
        this.cache = new HashMap<UUID, PlayerFishingData>();
        if (!this.playersFolder.exists() && !this.playersFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create player data folder.");
        }
    }

    public PlayerFishingData get(Player player) {
        PlayerFishingData data = this.get(player.getUniqueId(), player.getName());
        data.name(player.getName());
        return data;
    }

    public PlayerFishingData get(UUID uuid, String fallbackName) {
        PlayerFishingData cached = this.cache.get(uuid);
        if (cached != null) {
            return cached;
        }
        PlayerFishingData loaded = this.load(uuid, fallbackName);
        this.cache.put(uuid, loaded);
        return loaded;
    }

    public void unload(Player player) {
        PlayerFishingData data = this.cache.remove(player.getUniqueId());
        if (data != null) {
            this.save(data);
        }
    }

    public void addCatch(Player player, CaughtFish fish) {
        PlayerFishingData data = this.get(player);
        data.totalCatches(data.totalCatches() + 1L);
        data.discoveredFishes().add(fish.definition().id());
        if (fish.weight() > data.biggestFishWeight()) {
            data.biggestFishWeight(fish.weight());
            data.biggestFishName(Text.stripColor(fish.definition().displayName()));
        }
        if (fish.definition().rarity() == Rarity.LEGENDARY) {
            data.legendaryCatches(data.legendaryCatches() + 1L);
        }
        if (fish.definition().rarity() == Rarity.MYTHIC) {
            data.mythicCatches(data.mythicCatches() + 1L);
        }
        this.addExp(player, this.plugin.fishManager().expFor(fish.definition().rarity()));
        if (this.plugin.getConfig().getBoolean("settings.save-player-data-on-catch", false)) {
            this.save(data);
        }
    }

    public void addSold(Player player, long amount, long earned) {
        PlayerFishingData data = this.get(player);
        data.totalSold(data.totalSold() + amount);
        data.totalEarned(data.totalEarned() + earned);
    }

    public void addExp(Player player, int amount) {
        if (!this.plugin.getConfig().getBoolean("level-system.enabled", true)) {
            return;
        }
        PlayerFishingData data = this.get(player);
        data.exp(data.exp() + Math.max(0, amount));
        boolean leveled = false;
        while (data.exp() >= this.plugin.fishManager().requiredExp(data.level())) {
            data.exp(data.exp() - this.plugin.fishManager().requiredExp(data.level()));
            data.level(data.level() + 1);
            leveled = true;
        }
        if (leveled) {
            Text.send((CommandSender)player, this.plugin.messagesConfig().get(), "level-up", Map.of("level", String.valueOf(data.level())));
        }
    }

    public void reset(Player player) {
        PlayerFishingData data = this.get(player);
        data.reset();
        this.save(data);
    }

    public void reset(UUID uuid, String name) {
        PlayerFishingData data = this.get(uuid, name);
        data.reset();
        this.save(data);
    }

    public void saveAll() {
        for (PlayerFishingData data : this.cache.values()) {
            this.save(data);
        }
    }

    public File playersFolder() {
        return this.playersFolder;
    }

    public Map<UUID, PlayerFishingData> cache() {
        return this.cache;
    }

    public PlayerFishingData load(UUID uuid, String fallbackName) {
        File file = this.file(uuid);
        PlayerFishingData data = new PlayerFishingData(uuid, fallbackName == null ? uuid.toString() : fallbackName);
        if (!file.exists()) {
            return data;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration((File)file);
        data.name(config.getString("name", data.name()));
        data.level(config.getInt("level", 1));
        data.exp(config.getInt("exp", 0));
        data.totalCatches(config.getLong("total_catches", 0L));
        data.totalSold(config.getLong("total_sold", 0L));
        data.totalEarned(config.getLong("total_earned", 0L));
        data.biggestFishWeight(config.getDouble("biggest_fish_weight", 0.0));
        data.biggestFishName(config.getString("biggest_fish_name", "-"));
        data.legendaryCatches(config.getLong("legendary_catches", 0L));
        data.mythicCatches(config.getLong("mythic_catches", 0L));
        data.discoveredFishes().addAll(config.getStringList("discovered_fishes"));
        return data;
    }

    public void save(PlayerFishingData data) {
        File file = this.file(data.uuid());
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", (Object)data.name());
        config.set("level", (Object)data.level());
        config.set("exp", (Object)data.exp());
        config.set("total_catches", (Object)data.totalCatches());
        config.set("total_sold", (Object)data.totalSold());
        config.set("total_earned", (Object)data.totalEarned());
        config.set("biggest_fish_weight", (Object)NumberUtil.round2(data.biggestFishWeight()));
        config.set("biggest_fish_name", (Object)data.biggestFishName());
        config.set("legendary_catches", (Object)data.legendaryCatches());
        config.set("mythic_catches", (Object)data.mythicCatches());
        config.set("discovered_fishes", new ArrayList<String>(data.discoveredFishes()));
        try {
            config.save(file);
        }
        catch (IOException exception) {
            this.plugin.getLogger().warning("Failed to save player fishing data " + String.valueOf(data.uuid()) + ": " + exception.getMessage());
        }
    }

    private File file(UUID uuid) {
        return new File(this.playersFolder, String.valueOf(uuid) + ".yml");
    }
}

