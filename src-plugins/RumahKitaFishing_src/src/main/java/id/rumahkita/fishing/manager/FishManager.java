/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.block.Biome
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package id.rumahkita.fishing.manager;

import id.rumahkita.fishing.RumahKitaFishingPlugin;
import id.rumahkita.fishing.model.FishDefinition;
import id.rumahkita.fishing.model.PlayerFishingData;
import id.rumahkita.fishing.model.Rarity;
import id.rumahkita.fishing.util.NumberUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class FishManager {
    private final RumahKitaFishingPlugin plugin;
    private final FileConfiguration configuration;
    private final Map<String, FishDefinition> fishes;

    public FishManager(RumahKitaFishingPlugin plugin, FileConfiguration configuration) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.fishes = new HashMap<String, FishDefinition>();
    }

    public void load() {
        this.fishes.clear();
        ConfigurationSection section = this.configuration.getConfigurationSection("fishes");
        if (section == null) {
            this.plugin.getLogger().warning("fishes.yml does not contain fishes section.");
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection fishSection = section.getConfigurationSection(id);
            if (fishSection == null) continue;
            Material material = Material.matchMaterial((String)fishSection.getString("material", "COD"));
            if (material == null || !material.isItem()) {
                material = Material.COD;
                this.plugin.getLogger().warning("Invalid material for fish " + id + ", fallback to COD.");
            }
            FishDefinition definition = new FishDefinition(id.toLowerCase(Locale.ROOT), fishSection.getString("display-name", id), material, Rarity.fromString(fishSection.getString("rarity", "COMMON")), fishSection.getDouble("min-weight", 0.1), fishSection.getDouble("max-weight", 1.0), fishSection.getDouble("base-price-per-kg", 100.0), fishSection.getDouble("chance", 1.0), this.normalizeList(fishSection.getStringList("allowed-biomes")), this.normalizeList(fishSection.getStringList("allowed-worlds")), fishSection.getString("allowed-time", "ANY"), fishSection.getString("allowed-weather", "ANY"), fishSection.getInt("custom-model-data", 0), fishSection.getStringList("lore"));
            this.fishes.put(definition.id(), definition);
        }
        this.plugin.getLogger().info("Loaded " + this.fishes.size() + " custom fish definitions.");
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<String> result = new ArrayList<String>();
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            result.add(value.trim().toUpperCase(Locale.ROOT));
        }
        return result;
    }

    public Collection<FishDefinition> fishes() {
        return this.fishes.values().stream().sorted(Comparator.comparing(FishDefinition::id)).toList();
    }

    public Optional<FishDefinition> find(String fishId) {
        if (fishId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.fishes.get(fishId.toLowerCase(Locale.ROOT)));
    }

    public Optional<FishDefinition> selectFish(Player player, Location location) {
        if (!this.plugin.getConfig().getBoolean("custom-catch.enabled", true)) {
            return Optional.empty();
        }
        double globalChance = this.plugin.getConfig().getDouble("custom-catch.global-chance-percent", 85.0);
        globalChance += this.luckBonus(player);
        if (player.hasPermission("rumahkitafishing.bonus.vip")) {
            globalChance += this.plugin.getConfig().getDouble("custom-catch.vip-bonus-percent", 5.0);
        }
        if (ThreadLocalRandom.current().nextDouble(100.0) > Math.min(100.0, globalChance)) {
            return Optional.empty();
        }
        ArrayList<FishDefinition> valid = new ArrayList<FishDefinition>();
        for (FishDefinition definition : this.fishes.values()) {
            if (!this.matches(definition, location)) continue;
            valid.add(definition);
        }
        if (valid.isEmpty()) {
            return Optional.empty();
        }
        PlayerFishingData data = this.plugin.playerDataManager().get(player);
        double rareBonus = this.getRareChanceBonus(data.level());
        double totalWeight = 0.0;
        for (FishDefinition definition : valid) {
            totalWeight += this.effectiveChance(definition, rareBonus);
        }
        if (totalWeight <= 0.0) {
            return Optional.empty();
        }
        double random = ThreadLocalRandom.current().nextDouble(totalWeight);
        double current = 0.0;
        for (FishDefinition definition : valid) {
            if (!(random <= (current += this.effectiveChance(definition, rareBonus)))) continue;
            return Optional.of(definition);
        }
        return Optional.of((FishDefinition)valid.get(valid.size() - 1));
    }

    private double effectiveChance(FishDefinition definition, double rareBonus) {
        double chance = definition.chance();
        if (definition.rarity().isRareOrBetter()) {
            chance *= 1.0 + rareBonus / 100.0;
        }
        return Math.max(0.0, chance);
    }

    private boolean matches(FishDefinition definition, Location location) {
        boolean day;
        if (location == null || location.getWorld() == null) {
            return false;
        }
        World world = location.getWorld();
        String worldName = world.getName().toUpperCase(Locale.ROOT);
        if (!(definition.allowedWorlds().isEmpty() || definition.allowedWorlds().contains("ANY") || definition.allowedWorlds().contains("*") || definition.allowedWorlds().contains(worldName))) {
            return false;
        }
        Biome biome = location.getBlock().getBiome();
        String biomeName = biome.name().toUpperCase(Locale.ROOT);
        if (!(definition.allowedBiomes().isEmpty() || definition.allowedBiomes().contains("ANY") || definition.allowedBiomes().contains("*") || definition.allowedBiomes().contains(biomeName))) {
            return false;
        }
        String time = definition.allowedTime();
        long worldTime = world.getTime();
        boolean bl = day = worldTime < 12300L || worldTime > 23850L;
        if ("DAY".equals(time) && !day) {
            return false;
        }
        if ("NIGHT".equals(time) && day) {
            return false;
        }
        String weather = definition.allowedWeather();
        if ("CLEAR".equals(weather) && (world.hasStorm() || world.isThundering())) {
            return false;
        }
        if ("RAIN".equals(weather) && (!world.hasStorm() || world.isThundering())) {
            return false;
        }
        return !"THUNDER".equals(weather) || world.isThundering();
    }

    private double luckBonus(Player player) {
        int luckLevel = 0;
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        if (main.getType() == Material.FISHING_ROD) {
            luckLevel = Math.max(luckLevel, main.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA));
        }
        if (off.getType() == Material.FISHING_ROD) {
            luckLevel = Math.max(luckLevel, off.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA));
        }
        return (double)luckLevel * this.plugin.getConfig().getDouble("custom-catch.luck-of-the-sea-bonus-percent-per-level", 4.0);
    }

    public double rollWeight(FishDefinition definition, int playerLevel) {
        double min = definition.minWeight();
        double max = definition.maxWeight();
        double bonusCap = this.plugin.getConfig().getDouble("level-system.bonus.max-weight-percent-cap", 10.0);
        double weightBonus = Math.min(bonusCap, (double)this.getLevelBonusSteps(playerLevel) * this.plugin.getConfig().getDouble("level-system.bonus.max-weight-percent", 1.0));
        double random = NumberUtil.randomDouble(min, max *= 1.0 + weightBonus / 100.0);
        return NumberUtil.round2(random);
    }

    public int calculatePrice(FishDefinition definition, double weight, int playerLevel) {
        double rarityMultiplier = this.plugin.getConfig().getDouble("rarity-multiplier." + definition.rarity().name(), 1.0);
        double sellBonusCap = this.plugin.getConfig().getDouble("level-system.bonus.max-sell-price-percent", 20.0);
        double sellBonus = Math.min(sellBonusCap, (double)this.getLevelBonusSteps(playerLevel) * this.plugin.getConfig().getDouble("level-system.bonus.sell-price-percent", 2.0));
        double finalPrice = weight * definition.basePricePerKg() * rarityMultiplier * (1.0 + sellBonus / 100.0);
        return NumberUtil.safeIntPrice(finalPrice);
    }

    public double rarityMultiplier(Rarity rarity) {
        return this.plugin.getConfig().getDouble("rarity-multiplier." + rarity.name(), 1.0);
    }

    public int expFor(Rarity rarity) {
        return this.plugin.getConfig().getInt("level-system.exp-by-rarity." + rarity.name(), 5);
    }

    public int requiredExp(int level) {
        return Math.max(100, level * 100);
    }

    public double getRareChanceBonus(int level) {
        double cap = this.plugin.getConfig().getDouble("level-system.bonus.max-rare-chance-percent", 10.0);
        double perStep = this.plugin.getConfig().getDouble("level-system.bonus.rare-chance-percent", 1.0);
        return Math.min(cap, (double)this.getLevelBonusSteps(level) * perStep);
    }

    private int getLevelBonusSteps(int level) {
        int everyLevels = Math.max(1, this.plugin.getConfig().getInt("level-system.bonus.every-levels", 10));
        return Math.max(0, level / everyLevels);
    }
}

