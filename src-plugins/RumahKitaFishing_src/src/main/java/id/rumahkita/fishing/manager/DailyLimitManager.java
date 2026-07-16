/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 */
package id.rumahkita.fishing.manager;

import id.rumahkita.fishing.RumahKitaFishingPlugin;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class DailyLimitManager {
    private final RumahKitaFishingPlugin plugin;
    private final File file;
    private FileConfiguration configuration;

    public DailyLimitManager(RumahKitaFishingPlugin plugin) {
        this.plugin = plugin;
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create fishing data folder.");
        }
        this.file = new File(dataFolder, "daily-limits.yml");
        this.reload();
    }

    public void reload() {
        if (!this.file.exists()) {
            try {
                if (!this.file.createNewFile()) {
                    this.plugin.getLogger().warning("daily-limits.yml already exists but could not be initialized.");
                }
            }
            catch (IOException exception) {
                this.plugin.getLogger().warning("Failed to create daily-limits.yml: " + exception.getMessage());
            }
        }
        this.configuration = YamlConfiguration.loadConfiguration((File)this.file);
        if (!this.configuration.isConfigurationSection("limits")) {
            this.configuration.set("limits", null);
        }
    }

    public boolean enabled() {
        return this.plugin.getConfig().getBoolean("daily-sell-limit.enabled", true);
    }

    public String today() {
        String zone = this.plugin.getConfig().getString("daily-sell-limit.reset-timezone", "Asia/Jakarta");
        return LocalDate.now(ZoneId.of(zone)).toString();
    }

    public long soldToday(UUID uuid) {
        return this.configuration.getLong("limits." + this.today() + "." + String.valueOf(uuid) + ".sold", 0L);
    }

    public long earnedToday(UUID uuid) {
        return this.configuration.getLong("limits." + this.today() + "." + String.valueOf(uuid) + ".earned", 0L);
    }

    public boolean canSellFish(Player player, long fishAmount) {
        if (!this.enabled()) {
            return true;
        }
        long max = this.plugin.getConfig().getLong("daily-sell-limit.max-fish-sold-per-day", 500L);
        return this.soldToday(player.getUniqueId()) + fishAmount <= max;
    }

    public boolean canEarn(Player player, long money) {
        if (!this.enabled()) {
            return true;
        }
        long max = this.plugin.getConfig().getLong("daily-sell-limit.max-money-earned-per-day", 250000L);
        return this.earnedToday(player.getUniqueId()) + money <= max;
    }

    public long maxFishPerDay() {
        return this.plugin.getConfig().getLong("daily-sell-limit.max-fish-sold-per-day", 500L);
    }

    public long maxMoneyPerDay() {
        return this.plugin.getConfig().getLong("daily-sell-limit.max-money-earned-per-day", 250000L);
    }

    public void add(Player player, long fishAmount, long money) {
        if (!this.enabled()) {
            return;
        }
        String path = "limits." + this.today() + "." + String.valueOf(player.getUniqueId());
        this.configuration.set(path + ".name", (Object)player.getName());
        this.configuration.set(path + ".sold", (Object)(this.soldToday(player.getUniqueId()) + fishAmount));
        this.configuration.set(path + ".earned", (Object)(this.earnedToday(player.getUniqueId()) + money));
        this.save();
    }

    public void reset(Player player) {
        this.configuration.set("limits." + this.today() + "." + String.valueOf(player.getUniqueId()), null);
        this.save();
    }

    public void reset(UUID uuid) {
        this.configuration.set("limits." + this.today() + "." + String.valueOf(uuid), null);
        this.save();
    }

    public void resetAllToday() {
        this.configuration.set("limits." + this.today(), null);
        this.save();
    }

    public void clearOldDaysExceptToday() {
        ConfigurationSection section = this.configuration.getConfigurationSection("limits");
        if (section == null) {
            return;
        }
        String today = this.today();
        for (String day : section.getKeys(false)) {
            if (day.equals(today)) continue;
            this.configuration.set("limits." + day, null);
        }
        this.save();
    }

    public void save() {
        try {
            this.configuration.save(this.file);
        }
        catch (IOException exception) {
            this.plugin.getLogger().warning("Failed to save daily-limits.yml: " + exception.getMessage());
        }
    }
}

