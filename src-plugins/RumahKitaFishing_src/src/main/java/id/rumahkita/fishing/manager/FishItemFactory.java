/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.NamespacedKey
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 */
package id.rumahkita.fishing.manager;

import id.rumahkita.fishing.RumahKitaFishingPlugin;
import id.rumahkita.fishing.model.CaughtFish;
import id.rumahkita.fishing.model.FishDefinition;
import id.rumahkita.fishing.model.PlayerFishingData;
import id.rumahkita.fishing.util.NumberUtil;
import id.rumahkita.fishing.util.Text;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class FishItemFactory {
    private final RumahKitaFishingPlugin plugin;
    private final NamespacedKey uniqueIdKey;
    private final NamespacedKey fishIdKey;
    private final NamespacedKey rarityKey;
    private final NamespacedKey weightKey;
    private final NamespacedKey priceKey;
    private final NamespacedKey caughtByUuidKey;
    private final NamespacedKey caughtByNameKey;
    private final NamespacedKey caughtAtKey;
    private final NamespacedKey biomeKey;
    private final NamespacedKey worldKey;

    public FishItemFactory(RumahKitaFishingPlugin plugin) {
        this.plugin = plugin;
        this.uniqueIdKey = new NamespacedKey((Plugin)plugin, "fish_unique_id");
        this.fishIdKey = new NamespacedKey((Plugin)plugin, "fish_id");
        this.rarityKey = new NamespacedKey((Plugin)plugin, "rarity");
        this.weightKey = new NamespacedKey((Plugin)plugin, "weight");
        this.priceKey = new NamespacedKey((Plugin)plugin, "price");
        this.caughtByUuidKey = new NamespacedKey((Plugin)plugin, "caught_by_uuid");
        this.caughtByNameKey = new NamespacedKey((Plugin)plugin, "caught_by_name");
        this.caughtAtKey = new NamespacedKey((Plugin)plugin, "caught_at");
        this.biomeKey = new NamespacedKey((Plugin)plugin, "biome");
        this.worldKey = new NamespacedKey((Plugin)plugin, "world");
    }

    public ItemStack create(Player player, FishDefinition definition, double weight, String biome, String world) {
        PlayerFishingData data = this.plugin.playerDataManager().get(player);
        int price = this.plugin.fishManager().calculatePrice(definition, weight, data.level());
        CaughtFish caughtFish = new CaughtFish(UUID.randomUUID(), definition, weight, price, player.getUniqueId(), player.getName(), System.currentTimeMillis(), biome, world);
        return this.create(caughtFish);
    }

    public ItemStack create(CaughtFish caughtFish) {
        FishDefinition definition = caughtFish.definition();
        ItemStack item = new ItemStack(definition.material(), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(Text.color(definition.rarity().color() + definition.displayName()));
        if (this.plugin.getConfig().getBoolean("item.use-custom-model-data", true) && definition.customModelData() > 0) {
            meta.setCustomModelData(Integer.valueOf(definition.customModelData()));
        }
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS});
        meta.setLore(this.buildLore(caughtFish));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(this.uniqueIdKey, PersistentDataType.STRING, (Object)caughtFish.uniqueId().toString());
        container.set(this.fishIdKey, PersistentDataType.STRING, (Object)definition.id());
        container.set(this.rarityKey, PersistentDataType.STRING, (Object)definition.rarity().name());
        container.set(this.weightKey, PersistentDataType.DOUBLE, (Object)caughtFish.weight());
        container.set(this.priceKey, PersistentDataType.INTEGER, (Object)caughtFish.price());
        container.set(this.caughtByUuidKey, PersistentDataType.STRING, (Object)caughtFish.caughtByUuid().toString());
        container.set(this.caughtByNameKey, PersistentDataType.STRING, (Object)caughtFish.caughtByName());
        container.set(this.caughtAtKey, PersistentDataType.LONG, (Object)caughtFish.caughtAt());
        container.set(this.biomeKey, PersistentDataType.STRING, (Object)caughtFish.biome());
        container.set(this.worldKey, PersistentDataType.STRING, (Object)caughtFish.world());
        item.setItemMeta(meta);
        return item;
    }

    private List<String> buildLore(CaughtFish caughtFish) {
        FishDefinition definition = caughtFish.definition();
        List<String> template = definition.lore().isEmpty() ? this.defaultLore() : definition.lore();
        ArrayList<String> lore = new ArrayList<String>();
        Map<String, String> placeholders = this.placeholders(caughtFish);
        for (String line : template) {
            lore.add(Text.format(line, placeholders));
        }
        return lore;
    }

    private List<String> defaultLore() {
        return List.of("&8Custom Fish", "&r", "&7Jenis: &f{fish_name}", "&7Rarity: {rarity_color}{rarity}", "&7Berat: &f{weight} kg", "&7Harga / Kg: &aRp {base_price_per_kg}", "&7Multiplier: &e{x_multiplier}", "&7Harga Jual: &aRp {final_price}", "&7Ditangkap oleh: &f{player}", "&7Lokasi: &f{biome}", "&7Tanggal: &f{date}", "&r", "&eJual di &f/fish sell hand &eatau &f/fish sell all");
    }

    public Map<String, String> placeholders(CaughtFish caughtFish) {
        SimpleDateFormat format = new SimpleDateFormat(this.plugin.getConfig().getString("settings.date-format", "dd/MM/yyyy HH:mm"), Locale.US);
        format.setTimeZone(TimeZone.getTimeZone(this.plugin.getConfig().getString("daily-sell-limit.reset-timezone", "Asia/Jakarta")));
        FishDefinition definition = caughtFish.definition();
        HashMap<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("fish_name", Text.stripColor(definition.displayName()));
        placeholders.put("rarity", definition.rarity().name());
        placeholders.put("rarity_color", definition.rarity().color());
        placeholders.put("weight", NumberUtil.weight(caughtFish.weight()));
        placeholders.put("base_price_per_kg", NumberUtil.money(definition.basePricePerKg()));
        placeholders.put("x_multiplier", "x" + NumberUtil.weight(this.plugin.fishManager().rarityMultiplier(definition.rarity())));
        placeholders.put("final_price", NumberUtil.money(caughtFish.price()));
        placeholders.put("price", NumberUtil.money(caughtFish.price()));
        placeholders.put("player", caughtFish.caughtByName());
        placeholders.put("biome", caughtFish.biome());
        placeholders.put("world", caughtFish.world());
        placeholders.put("date", format.format(new Date(caughtFish.caughtAt())));
        return placeholders;
    }

    public Optional<CaughtFish> read(ItemStack item, int playerLevel) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String uniqueIdRaw = (String)container.get(this.uniqueIdKey, PersistentDataType.STRING);
        String fishId = (String)container.get(this.fishIdKey, PersistentDataType.STRING);
        Double weight = (Double)container.get(this.weightKey, PersistentDataType.DOUBLE);
        String caughtByUuidRaw = (String)container.get(this.caughtByUuidKey, PersistentDataType.STRING);
        String caughtByName = (String)container.get(this.caughtByNameKey, PersistentDataType.STRING);
        Long caughtAt = (Long)container.get(this.caughtAtKey, PersistentDataType.LONG);
        String biome = (String)container.get(this.biomeKey, PersistentDataType.STRING);
        String world = (String)container.get(this.worldKey, PersistentDataType.STRING);
        if (uniqueIdRaw == null || fishId == null || weight == null || weight <= 0.0) {
            return Optional.empty();
        }
        Optional<FishDefinition> optionalDefinition = this.plugin.fishManager().find(fishId);
        if (optionalDefinition.isEmpty()) {
            return Optional.empty();
        }
        try {
            UUID uniqueId = UUID.fromString(uniqueIdRaw);
            UUID caughtByUuid = caughtByUuidRaw == null ? new UUID(0L, 0L) : UUID.fromString(caughtByUuidRaw);
            FishDefinition definition = optionalDefinition.get();
            int price = this.plugin.fishManager().calculatePrice(definition, weight, playerLevel);
            return Optional.of(new CaughtFish(uniqueId, definition, NumberUtil.round2(weight), price, caughtByUuid, caughtByName == null ? "Unknown" : caughtByName, caughtAt == null ? System.currentTimeMillis() : caughtAt, biome == null ? "UNKNOWN" : biome, world == null ? "UNKNOWN" : world));
        }
        catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}

