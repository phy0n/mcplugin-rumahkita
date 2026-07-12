/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  id.rumahkita.economy.RkePlaceholderExpansion
 *  id.rumahkita.economy.RumahKitaEconomyRupiahPlugin
 *  id.rumahkita.economy.RumahKitaEconomyRupiahPlugin$MarketHolder
 *  id.rumahkita.economy.RumahKitaEconomyRupiahPlugin$MarketItem
 *  id.rumahkita.economy.RumahKitaEconomyRupiahPlugin$VoucherResult
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.util.io.BukkitObjectInputStream
 *  org.bukkit.util.io.BukkitObjectOutputStream
 */
package id.rumahkita.economy;

import id.rumahkita.economy.RkePlaceholderExpansion;
import id.rumahkita.economy.RumahKitaEconomyRupiahPlugin;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public final class RumahKitaEconomyRupiahPlugin
extends JavaPlugin
implements Listener,
TabExecutor {
    private final Map<String, MarketItem> items = new LinkedHashMap();
    private final Map<Material, MarketItem> sellByMaterial = new HashMap();
    private final Map<UUID, String> selectedVoucher = new HashMap();
    private final Map<UUID, Long> cooldowns = new HashMap();
    private final Map<UUID, Long> payAllCooldowns = new HashMap();
    private final Set<String> allowedSellCategories = new HashSet();
    private File marketFile;
    private File messagesFile;
    private File balancesFile;
    private File refundsFile;
    private File pricesFile;
    private File stockFile;
    private File statsFile;
    private File limitsFile;
    private File migrationFile;
    private FileConfiguration marketCfg;
    private FileConfiguration messagesCfg;
    private FileConfiguration balancesCfg;
    private FileConfiguration refundsCfg;
    private FileConfiguration pricesCfg;
    private FileConfiguration stockCfg;
    private FileConfiguration statsCfg;
    private FileConfiguration limitsCfg;
    private FileConfiguration migrationCfg;
    private NamespacedKey keyGuiItem;
    private NamespacedKey keyVoucherId;
    private NamespacedKey keyVoucherPercent;
    private NamespacedKey keyRefundId;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static RumahKitaEconomyRupiahPlugin instance;

    public static RumahKitaEconomyRupiahPlugin getInstance() {
        return instance;
    }

    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        this.saveIfMissing("market.yml");
        this.saveIfMissing("messages.yml");
        this.setupFiles();
        this.keyGuiItem = new NamespacedKey((Plugin)this, "market_item_key");
        this.keyVoucherId = new NamespacedKey((Plugin)this, "voucher_id");
        this.keyVoucherPercent = new NamespacedKey((Plugin)this, "voucher_percent");
        this.keyRefundId = new NamespacedKey((Plugin)this, "refund_id");
        this.reloadAll();
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        for (String cmd : Arrays.asList("market", "sellhand", "sellall", "bal", "pay", "payall", "rke", "baltop", "hidebal")) {
            if (this.getCommand(cmd) != null) {
                this.getCommand(cmd).setExecutor((CommandExecutor)this);
            }
            if (this.getCommand(cmd) == null) continue;
            this.getCommand(cmd).setTabCompleter((TabCompleter)this);
        }
        this.startTasks();
        this.hookPlaceholderAPI();
        if (this.getConfig().getBoolean("settings.auto-refund-old-player-shop-on-first-start", true) && !this.migrationCfg.getBoolean("old-player-shop-refund.done", false)) {
            Bukkit.getScheduler().runTaskLater((Plugin)this, () -> this.runRefundMigration((CommandSender)Bukkit.getConsoleSender(), false), 60L);
        }
        this.setupVault();
        this.getLogger().info("RumahKitaEconomyRupiah v2.1.1 BalanceMigrationFix enabled.");
    }

    private void setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found. Economy hook will not be registered.");
            return;
        }
        getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class, new VaultEconomyProvider(this), this, org.bukkit.plugin.ServicePriority.Highest);
        getLogger().info("Vault Economy Provider registered successfully!");
    }

    public void onDisable() {
        this.saveData();
    }

    private void hookPlaceholderAPI() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                new RkePlaceholderExpansion(this).register();
                this.getLogger().info("PlaceholderAPI expansion rke registered.");
            }
            catch (Throwable t) {
                this.getLogger().warning("PlaceholderAPI hook failed: " + t.getMessage());
            }
        }
    }

    private void saveIfMissing(String name) {
        File f = new File(this.getDataFolder(), name);
        if (!f.exists()) {
            this.saveResource(name, false);
        }
    }

    private void setupFiles() {
        File logsDir;
        File dataDir;
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdirs();
        }
        if (!(dataDir = new File(this.getDataFolder(), "data")).exists()) {
            dataDir.mkdirs();
        }
        if (!(logsDir = new File(this.getDataFolder(), "logs")).exists()) {
            logsDir.mkdirs();
        }
        this.marketFile = new File(this.getDataFolder(), "market.yml");
        this.messagesFile = new File(this.getDataFolder(), "messages.yml");
        
        this.saveIfMissing("data/balances.yml");
        this.saveIfMissing("data/refunds.yml");
        this.saveIfMissing("data/prices.yml");
        this.saveIfMissing("data/stock.yml");
        this.saveIfMissing("data/stats.yml");
        this.saveIfMissing("data/daily-limits.yml");
        this.saveIfMissing("data/migration.yml");
        this.balancesFile = new File(dataDir, "balances.yml");
        this.refundsFile = new File(dataDir, "refunds.yml");
        this.pricesFile = new File(dataDir, "prices.yml");
        this.stockFile = new File(dataDir, "stock.yml");
        this.statsFile = new File(dataDir, "stats.yml");
        this.limitsFile = new File(dataDir, "daily-limits.yml");
        this.migrationFile = new File(dataDir, "migration.yml");
        this.balancesCfg = YamlConfiguration.loadConfiguration((File)this.balancesFile);
        this.refundsCfg = YamlConfiguration.loadConfiguration((File)this.refundsFile);
        this.pricesCfg = YamlConfiguration.loadConfiguration((File)this.pricesFile);
        this.stockCfg = YamlConfiguration.loadConfiguration((File)this.stockFile);
        this.statsCfg = YamlConfiguration.loadConfiguration((File)this.statsFile);
        this.limitsCfg = YamlConfiguration.loadConfiguration((File)this.limitsFile);
        this.migrationCfg = YamlConfiguration.loadConfiguration((File)this.migrationFile);
        int migrated = this.migrateLegacyBalancesIfNeeded(false);
        if (migrated > 0) {
            this.getLogger().info("Migrated " + migrated + " legacy balance entries to balances.<uuid> format.");
        }
        this.cleanUpHiddenBalances();
    }

    private void cleanUpHiddenBalances() {
        if (this.balancesCfg.contains("players")) {
            int unhidden = 0;
            for (String uuidStr : this.balancesCfg.getConfigurationSection("players").getKeys(false)) {
                String path = "players." + uuidStr;
                boolean hidden = this.balancesCfg.getBoolean(path + ".hidden", false);
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                long balance = this.getBalance(uuid);
                if (hidden && balance < 20000000L) {
                    this.balancesCfg.set(path + ".hidden", false);
                    unhidden++;
                }
            }
            if (unhidden > 0) {
                this.trySave(this.balancesCfg, this.balancesFile);
                this.getLogger().info("Force unhid " + unhidden + " players with balance < 20M.");
            }
        }
    }

    public void reloadAll() {
        this.reloadConfig();
        this.marketCfg = YamlConfiguration.loadConfiguration((File)this.marketFile);
        this.messagesCfg = YamlConfiguration.loadConfiguration((File)this.messagesFile);
        this.balancesCfg = YamlConfiguration.loadConfiguration((File)this.balancesFile);
        this.migrateLegacyBalancesIfNeeded(false);
        this.refundsCfg = YamlConfiguration.loadConfiguration((File)this.refundsFile);
        this.pricesCfg = YamlConfiguration.loadConfiguration((File)this.pricesFile);
        this.stockCfg = YamlConfiguration.loadConfiguration((File)this.stockFile);
        this.statsCfg = YamlConfiguration.loadConfiguration((File)this.statsFile);
        this.limitsCfg = YamlConfiguration.loadConfiguration((File)this.limitsFile);
        this.allowedSellCategories.clear();
        for (String c : this.getConfig().getStringList("sell-rules.allowed-categories")) {
            this.allowedSellCategories.add(c.toLowerCase(Locale.ROOT));
        }
        this.loadItems();
    }

    private int migrateLegacyBalancesIfNeeded(boolean force) {
        if (this.balancesCfg == null || this.balancesFile == null) {
            return 0;
        }
        int migrated = 0;
        try {
            if (this.balancesFile.exists()) {
                File backup = new File(this.balancesFile.getParentFile(), "balances-v5-backup-" + System.currentTimeMillis() + ".yml");
                if (!this.migrationCfg.getBoolean("legacy-balances.backup-created", false)) {
                    Files.copy(this.balancesFile.toPath(), backup.toPath(), new CopyOption[0]);
                    this.migrationCfg.set("legacy-balances.backup-created", true);
                    this.migrationCfg.set("legacy-balances.backup-file", backup.getName());
                    this.trySave(this.migrationCfg, this.migrationFile);
                }
            }
        }
        catch (Exception e) {
            this.getLogger().warning("Failed to create old balances backup: " + e.getMessage());
        }
        ConfigurationSection players = this.balancesCfg.getConfigurationSection("players");
        if (players != null) {
            for (String uuid : players.getKeys(false)) {
                long bal = Math.max(0L, players.getLong(uuid + ".balance", 0L));
                String name = players.getString(uuid + ".name", "");
                if (force || !this.balancesCfg.contains("balances." + uuid)) {
                    this.balancesCfg.set("balances." + uuid, bal);
                    ++migrated;
                }
                if (name == null || name.isEmpty()) continue;
                this.balancesCfg.set("names." + uuid, name);
            }
        } else {
            migrated += this.migrateLegacyBalancesFromRawJson(force);
        }
        if (migrated > 0) {
            this.balancesCfg.set("version", 6);
            this.balancesCfg.set("currency", "RUPIAH");
            this.balancesCfg.set("legacy-v5-migrated", true);
            this.balancesCfg.set("legacy-v5-migrated-at", this.nowLine());
            this.trySave(this.balancesCfg, this.balancesFile);
            this.logLine("market_transactions.log", "BALANCE_MIGRATION migrated=" + migrated + " at=" + this.nowLine());
        }
        return migrated;
    }

    private int migrateLegacyBalancesFromRawJson(boolean force) {
        if (this.balancesFile == null || !this.balancesFile.exists()) {
            return 0;
        }
        int migrated = 0;
        try {
            String raw = new String(Files.readAllBytes(this.balancesFile.toPath()), StandardCharsets.UTF_8);
            Pattern pat = Pattern.compile("\\\"([0-9a-fA-F-]{36})\\\"\\s*:\\s*\\{\\s*\\\"name\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"\\s*,\\s*\\\"balance\\\"\\s*:\\s*(-?\\d+)", 32);
            Matcher m = pat.matcher(raw);
            while (m.find()) {
                long bal;
                String uuid = m.group(1);
                String name = m.group(2);
                try {
                    bal = Math.max(0L, Long.parseLong(m.group(3)));
                }
                catch (Exception ex) {
                    bal = 0L;
                }
                if (force || !this.balancesCfg.contains("balances." + uuid)) {
                    this.balancesCfg.set("balances." + uuid, bal);
                    ++migrated;
                }
                if (name == null || name.isEmpty()) continue;
                this.balancesCfg.set("names." + uuid, name);
            }
        }
        catch (Exception e) {
            this.getLogger().warning("Gagal membaca balances JSON lama: " + e.getMessage());
        }
        return migrated;
    }

    private void loadItems() {
        this.items.clear();
        this.sellByMaterial.clear();
        ConfigurationSection sec = this.marketCfg.getConfigurationSection("items");
        if (sec == null) {
            return;
        }
        for (String key : sec.getKeys(false)) {
            String path = "items." + key + ".";
            Material mat = Material.matchMaterial((String)this.marketCfg.getString(path + "material", "STONE"));
            if (mat == null) {
                this.getLogger().warning("Material invalid for market item " + key);
                continue;
            }
            MarketItem mi = new MarketItem();
            mi.key = key;
            mi.enabled = this.marketCfg.getBoolean(path + "enabled", true);
            mi.category = this.marketCfg.getString(path + "category", "servermarket").toLowerCase(Locale.ROOT);
            mi.material = mat;
            mi.displayName = this.marketCfg.getString(path + "display-name", mat.name());
            mi.tradeAmount = Math.max(1, this.marketCfg.getInt(path + "trade-amount", 1));
            mi.buyEnabled = this.marketCfg.getBoolean(path + "buy.enabled", false);
            mi.sellEnabled = this.marketCfg.getBoolean(path + "sell.enabled", false);
            mi.baseBuyPrice = this.marketCfg.getLong(path + "buy.price", 0L);
            mi.baseSellPrice = this.marketCfg.getLong(path + "sell.price", 0L);
            mi.currentBuyPrice = this.pricesCfg.getLong(key + ".buy", mi.baseBuyPrice);
            mi.currentSellPrice = this.pricesCfg.getLong(key + ".sell", mi.baseSellPrice);
            mi.dailyBuyLimit = this.marketCfg.getInt(path + "buy.daily-limit-per-player", 0);
            mi.dailySellLimit = this.marketCfg.getInt(path + "sell.daily-limit-per-player", 0);
            mi.stockEnabled = this.marketCfg.getBoolean(path + "stock.enabled", false);
            mi.stockCurrent = this.stockCfg.getLong(key + ".current", this.marketCfg.getLong(path + "stock.current", -1L));
            mi.stockMax = this.marketCfg.getLong(path + "stock.max", -1L);
            mi.restockAmount = this.marketCfg.getLong(path + "stock.restock-amount", 0L);
            mi.restockIntervalMinutes = this.marketCfg.getLong(path + "stock.restock-interval-minutes", 0L);
            mi.pricingMode = this.marketCfg.getString(path + "pricing.mode", "MANUAL").toUpperCase(Locale.ROOT);
            mi.demandEnabled = this.marketCfg.getBoolean(path + "pricing.demand-enabled", false);
            mi.minBuyPrice = this.marketCfg.getLong(path + "pricing.min-buy-price", Math.max(0L, mi.baseBuyPrice / 2L));
            mi.maxBuyPrice = this.marketCfg.getLong(path + "pricing.max-buy-price", Math.max(mi.baseBuyPrice, mi.baseBuyPrice * 2L));
            mi.minSellPrice = this.marketCfg.getLong(path + "pricing.min-sell-price", Math.max(0L, mi.baseSellPrice / 2L));
            mi.maxSellPrice = this.marketCfg.getLong(path + "pricing.max-sell-price", Math.max(mi.baseSellPrice, mi.baseSellPrice * 2L));
            mi.maxChangeUpdate = this.marketCfg.getDouble(path + "pricing.max-change-percent-per-update", 5.0);
            mi.maxChangeDay = this.marketCfg.getDouble(path + "pricing.max-change-percent-per-day", 15.0);
            mi.adminLocked = this.marketCfg.getBoolean(path + "pricing.admin-locked-price", false);
            this.items.put(key, mi);
            if (!mi.sellEnabled || !this.allowedSellCategories.contains(mi.category)) continue;
            this.sellByMaterial.put(mat, mi);
        }
    }

    private void startTasks() {
        new org.bukkit.scheduler.BukkitRunnable() {
            public void run() { saveData(); }
        }.runTaskTimer((Plugin)this, 6000L, 6000L);
        new org.bukkit.scheduler.BukkitRunnable() {
            public void run() { restockItems(); }
        }.runTaskTimer((Plugin)this, 1200L, 1200L);
        long minutes = Math.max(5L, this.getConfig().getLong("settings.demand-price-update-minutes", 30L));
        new org.bukkit.scheduler.BukkitRunnable() {
            public void run() { updateDemandPrices(false); }
        }.runTaskTimer((Plugin)this, 1200L * minutes, 1200L * minutes);
    }

    private void saveData() {
        this.trySave(this.balancesCfg, this.balancesFile);
        this.trySave(this.refundsCfg, this.refundsFile);
        this.trySave(this.pricesCfg, this.pricesFile);
        this.trySave(this.stockCfg, this.stockFile);
        this.trySave(this.statsCfg, this.statsFile);
        this.trySave(this.limitsCfg, this.limitsFile);
        this.trySave(this.migrationCfg, this.migrationFile);
    }

    private void trySave(FileConfiguration cfg, File file) {
        try {
            cfg.save(file);
        }
        catch (Exception e) {
            this.getLogger().warning("Failed saving " + file.getName() + ": " + e.getMessage());
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("market")) {
            return this.handleMarket(sender, args);
        }
        if (name.equals("sellhand")) {
            return this.handleSellHand(sender);
        }
        if (name.equals("sellall")) {
            return this.handleSellAll(sender);
        }

        if (name.equals("bal")) {
            return this.handleBal(sender, args);
        }
        if (name.equals("baltop")) {
            return this.handleBaltop(sender);
        }
        if (name.equals("hidebal")) {
            return this.handleHidebal(sender);
        }
        if (name.equals("pay")) {
            return this.handlePay(sender, args);
        }
        if (name.equals("payall")) {
            return this.handlePayAll(sender, args);
        }
        if (name.equals("rke")) {
            return this.handleRke(sender, args);
        }
        return true;
    }

    private boolean handleMarket(CommandSender sender, String[] args) {
        if (args.length > 0) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("reload")) {
                if (!this.hasAdmin(sender, "market.admin.reload")) {
                    return this.noPerm(sender);
                }
                this.reloadAll();
                this.msg(sender, this.m("reloaded"));
                return true;
            }
            if (sub.equals("stats")) {
                if (!this.hasAdmin(sender, "market.admin.stats")) {
                    return this.noPerm(sender);
                }
                this.sendStats(sender);
                return true;
            }
            if (sub.equals("refundshop")) {
                if (!this.hasAdmin(sender, "market.admin.refund")) {
                    return this.noPerm(sender);
                }
                this.runRefundMigration(sender, true);
                return true;
            }
            if (sub.equals("admin")) {
                if (!(sender instanceof Player)) {
                    return this.playerOnly(sender);
                }
                if (!this.hasAdmin(sender, "market.admin")) {
                    return this.noPerm(sender);
                }
                this.openAdmin((Player)sender);
                return true;
            }
            if (sub.equals("claim")) {
                if (!(sender instanceof Player)) {
                    return this.playerOnly(sender);
                }
                Player p = (Player)sender;
                if (!this.hasUse((CommandSender)p, "market.claim")) {
                    return this.noPerm(sender);
                }
                this.openClaim(p);
                return true;
            }
        }
        if (!(sender instanceof Player)) {
            return this.playerOnly(sender);
        }
        Player p = (Player)sender;
        if (!this.hasUse((CommandSender)p, "market.use")) {
            return this.noPerm(sender);
        }
        this.openMain(p);
        return true;
    }

    private boolean handleSellHand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return this.playerOnly(sender);
        }
        Player p = (Player)sender;
        if (!this.hasUse((CommandSender)p, "market.sellhand")) {
            return this.noPerm(sender);
        }
        if (this.onCooldown(p)) {
            return true;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            this.msg((CommandSender)p, "&cPegang item yang mau dijual.");
            return true;
        }
        MarketItem mi = (MarketItem)this.sellByMaterial.get(hand.getType());
        if (mi == null || !this.isSellAllowed(mi)) {
            this.msg((CommandSender)p, this.m("item-not-sellable"));
            this.logAbuse(p, hand.getType().name(), "SELLHAND_BLOCKED");
            return true;
        }
        int amount = hand.getAmount();
        if (amount <= 0) {
            this.msg((CommandSender)p, this.m("not-enough-item"));
            return true;
        }
        this.sellMaterial(p, mi, amount, true);
        return true;
    }

    private boolean handleSellAll(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return this.playerOnly(sender);
        }
        Player p = (Player)sender;
        if (!this.hasUse((CommandSender)p, "market.sellall")) {
            return this.noPerm(sender);
        }
        if (this.onCooldown(p)) {
            return true;
        }
        long total = 0L;
        int soldTypes = 0;
        for (MarketItem mi : this.items.values()) {
            long earned;
            int count;
            int amount;
            if (!this.isSellAllowed(mi) || (amount = (count = this.countMaterial(p, mi.material))) <= 0) continue;
            int remainLimit = this.getRemainingDaily(p.getUniqueId(), mi.key, "SELL", mi.dailySellLimit);
            if (mi.dailySellLimit > 0) {
                amount = Math.min(amount, remainLimit);
            }
            if (amount <= 0 || (earned = this.sellMaterial(p, mi, amount, false)) <= 0L) continue;
            total += earned;
            ++soldTypes;
        }
        if (total <= 0L) {
            this.msg((CommandSender)p, "&cNo Mob Drops/Farming items that can be sold, or daily limit has been reached.");
        } else {
            this.msg((CommandSender)p, "&aSellAll finished. &7Item types: &f" + soldTypes + " &7Total: &e" + this.formatRp(total));
        }
        return true;
    }
    private boolean handleBaltop(CommandSender sender) {
        ConfigurationSection sec = this.balancesCfg.getConfigurationSection("balances");
        if (sec == null) {
            this.msg(sender, "&cNo balance data yet.");
            return true;
        }
        
        java.util.List<java.util.Map.Entry<String, Long>> top = new java.util.ArrayList<>();
        for (String uuidStr : sec.getKeys(false)) {
            if (this.balancesCfg.getBoolean("players." + uuidStr + ".hidden", false)) continue;
            
            long bal = this.balancesCfg.getLong("balances." + uuidStr, 0L);
            String name = this.balancesCfg.getString("players." + uuidStr + ".name");
            if (name == null || name.equals("Unknown")) {
                org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuidStr));
                name = op.getName();
                if (name == null) name = "Unknown";
                else this.balancesCfg.set("players." + uuidStr + ".name", name);
            }
            top.add(new java.util.AbstractMap.SimpleEntry<>(name, bal));
        }
        
        top.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        
        sender.sendMessage(this.color("&8&m========================="));
        sender.sendMessage(this.color("&e&lTop Balance"));
        for (int i = 0; i < Math.min(10, top.size()); i++) {
            java.util.Map.Entry<String, Long> entry = top.get(i);
            sender.sendMessage(this.color("&7" + (i + 1) + ". &f" + entry.getKey() + " &8- &a" + this.formatRp(entry.getValue())));
        }
        sender.sendMessage(this.color("&8&m========================="));
        return true;
    }

    private boolean handleHidebal(CommandSender sender) {
        if (!(sender instanceof Player)) return this.playerOnly(sender);
        Player p = (Player)sender;
        long balance = this.getBalance(p.getUniqueId());
        if (balance < 20000000L) {
            this.msg(sender, "&cThis command requires a minimum balance of Rp 20,000,000.");
            return true;
        }
        String path = "players." + p.getUniqueId().toString() + ".hidden";
        boolean current = this.balancesCfg.getBoolean(path, false);
        this.balancesCfg.set(path, !current);
        this.trySave(this.balancesCfg, this.balancesFile);
        if (!current) {
            this.msg(sender, "&aSuccessfully hid your balance from /baltop.");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ajleaderboards removeplayer " + p.getName() + " rke_balance_raw");
        } else {
            this.msg(sender, "&eYour balance is now visible on /baltop.");
        }
        return true;
    }

    private boolean handleBal(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) && args.length == 0) {
            this.msg(sender, "&cUsage: /bal <player>");
            return true;
        }
        if (args.length > 0 && !this.hasAdmin(sender, "market.admin")) {
            if (sender instanceof Player && !args[0].equalsIgnoreCase(sender.getName())) {
                this.msg(sender, "&cYou do not have permission to view other players' balances.");
                return true;
            }
        }
        OfflinePlayer target = args.length > 0 ? Bukkit.getOfflinePlayer((String)args[0]) : (OfflinePlayer)sender;
        this.msg(sender, "&7Balance of &f" + target.getName() + "&7: &a" + this.formatRp(this.getBalance(target.getUniqueId())));
        return true;
    }

    private boolean handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return this.playerOnly(sender);
        }
        Player p = (Player)sender;
        if (!this.hasUse((CommandSender)p, "market.use")) {
            return this.noPerm(sender);
        }
        if (args.length < 2) {
            this.msg((CommandSender)p, "&cGunakan /pay <player> <amount>");
            return true;
        }
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            this.msg((CommandSender)p, "&cPlayer has never played on this server.");
            return true;
        }
        long amount = this.parseLong(args[1], -1L);
        if (amount <= 0L) {
            this.msg((CommandSender)p, this.m("invalid-number"));
            return true;
        }
        if (!this.takeBalance(p.getUniqueId(), amount)) {
            this.msg((CommandSender)p, this.m("not-enough-money").replace("%price%", this.formatNumber(amount)));
            return true;
        }
        this.addBalance(target.getUniqueId(), amount);
        this.msg((CommandSender)p, "&aSuccessfully transferred &e" + this.formatRp(amount) + " &ake &f" + target.getName());
        if (target.isOnline()) {
            this.msg((CommandSender)target.getPlayer(), "&aKamu menerima &e" + this.formatRp(amount) + " &adari &f" + p.getName());
        }
        return true;
    }

    private boolean handlePayAll(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return this.playerOnly(sender);
        }
        Player p = (Player)sender;
        if (!this.hasUse((CommandSender)p, "market.use")) {
            return this.noPerm(sender);
        }
        if (args.length < 1) {
            this.msg((CommandSender)p, "&cGunakan /payall <total_uang> [alasan...]");
            return true;
        }
        long now = System.currentTimeMillis();
        long last = this.payAllCooldowns.getOrDefault(p.getUniqueId(), 0L);
        if (now - last < 300000L) {
            long wait = (300000L - (now - last)) / 1000L;
            this.msg((CommandSender)p, "&cTunggu &e" + wait + " seconds &cbefore using /payall again.");
            return true;
        }
        long totalAmount = this.parseLong(args[0], -1L);
        if (totalAmount < 100000L) {
            this.msg((CommandSender)p, "&cMinimal /payall adalah &eRp 100.000");
            return true;
        }
        String reason = "";
        if (args.length > 1) {
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                reasonBuilder.append(args[i]).append(" ");
            }
            reason = reasonBuilder.toString().trim();
        }
        List<Player> targets = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(p.getUniqueId())) continue;
            targets.add(online);
        }
        if (targets.isEmpty()) {
            this.msg((CommandSender)p, "&cNo other players online to share money.");
            return true;
        }
        long splitAmount = totalAmount / targets.size();
        if (splitAmount <= 0) {
            this.msg((CommandSender)p, "&cAmount is too small to be split among " + targets.size() + " players.");
            return true;
        }
        long actualTotalCost = splitAmount * targets.size();
        if (!this.takeBalance(p.getUniqueId(), actualTotalCost)) {
            this.msg((CommandSender)p, "&cNot enough balance! (You need " + this.formatRp(actualTotalCost) + ")");
            return true;
        }
        for (Player target : targets) {
            this.addBalance(target.getUniqueId(), splitAmount);
            String titleText = "&a+" + this.formatRp(splitAmount);
            String subtitleText = "&fFrom: &e" + p.getName();
            if (!reason.isEmpty()) {
                subtitleText += " &7| &f" + reason;
            }
            target.sendTitle(this.color(titleText), this.color(subtitleText), 10, 70, 20);
            
            String chatMsg = "&aYou received a splash of &e" + this.formatRp(splitAmount) + " &afrom &f" + p.getName();
            if (!reason.isEmpty()) {
                chatMsg += " &7(" + reason + ")";
            }
            target.sendMessage(this.color(chatMsg));
        }
        this.payAllCooldowns.put(p.getUniqueId(), now);
        p.sendMessage(this.color("&aSuccessfully distributed a total of &e" + this.formatRp(actualTotalCost) + " &ato &f" + targets.size() + " &aplayers! (Each received " + this.formatRp(splitAmount) + ")"));
        return true;
    }

    private boolean handleRke(CommandSender sender, String[] args) {
        if (!this.hasAdmin(sender, "market.admin")) {
            return this.noPerm(sender);
        }
        if (args.length == 0) {
            this.msg(sender, "&cType /rke help for help.");
            return true;
        }
        if (args[0].equalsIgnoreCase("help")) {
            this.msg(sender, "&8==== &b&lRumahKita Economy &8====");
            this.msg(sender, "&e--- Admin Commands ---");
            this.msg(sender, "&a /rke give|take|set|balance <player> <amount>");
            this.msg(sender, "&a /rke voucher give <player> <percent> <amount>");
            this.msg(sender, "&a /rke voucher giveall <percent> <amount>");
            this.msg(sender, "&a /rke reload | save | placeholders | demandupdate");
            this.msg(sender, "&a /rke migratebalances");
            this.msg(sender, "&e--- Player Commands ---");
            this.msg(sender, "&a /market atau /shop &7- Buka Menu Toko");
            this.msg(sender, "&a /bal or /money &7- Check Balance");
            this.msg(sender, "&a /baltop &7- Peringkat Orang Terkaya");
            this.msg(sender, "&a /pay <player> <amount> &7- Transfer Money");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            this.reloadAll();
            this.msg(sender, this.m("reloaded"));
            return true;
        }
        if (sub.equals("save")) {
            this.saveData();
            this.msg(sender, "&aData tersimpan.");
            return true;
        }
        if (sub.equals("placeholders")) {
            this.sendPlaceholders(sender);
            return true;
        }
        if (sub.equals("demandupdate")) {
            this.updateDemandPrices(true);
            this.msg(sender, "&aDemand pricing updated.");
            return true;
        }
        if (sub.equals("migratebalances")) {
            int migrated = this.migrateLegacyBalancesIfNeeded(true);
            this.msg(sender, "&aBalance migration complete. Entries processed: &e" + migrated + "&a. Check balance with /bal <player>.");
            return true;
        }
        if (sub.equals("voucher")) {
            return this.handleVoucherCommand(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        if (args.length >= 2 && Arrays.asList("give", "take", "set", "balance").contains(sub)) {
            OfflinePlayer op = Bukkit.getOfflinePlayer((String)args[1]);
            if (sub.equals("balance")) {
                this.msg(sender, "&7Saldo &f" + op.getName() + "&7: &a" + this.formatRp(this.getBalance(op.getUniqueId())));
                return true;
            }
            if (args.length < 3) {
                this.msg(sender, "&cButuh amount.");
                return true;
            }
            long amount = this.parseLong(args[2], -1L);
            if (amount < 0L) {
                this.msg(sender, this.m("invalid-number"));
                return true;
            }
            if (sub.equals("give")) {
                this.addBalance(op.getUniqueId(), amount);
            }
            if (sub.equals("take")) {
                this.takeBalance(op.getUniqueId(), amount);
            }
            if (sub.equals("set")) {
                this.setBalance(op.getUniqueId(), amount);
            }
            this.msg(sender, "&aSaldo &f" + op.getName() + " &asekarang &e" + this.formatRp(this.getBalance(op.getUniqueId())));
            return true;
        }
        this.msg(sender, "&cUnknown RKE command.");
        return true;
    }

    private boolean handleVoucherCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            this.msg(sender, "&c/rke voucher give <player> <percent> <amount>");
            return true;
        }
        if (args[0].equalsIgnoreCase("give") && args.length >= 4) {
            Player target = Bukkit.getPlayerExact((String)args[1]);
            if (target == null) {
                this.msg(sender, "&cPlayer not online.");
                return true;
            }
            int percent = (int)this.parseLong(args[2], -1L);
            int amount = (int)this.parseLong(args[3], -1L);
            if (percent <= 0 || percent > 90 || amount <= 0) {
                this.msg(sender, "&cPercent 1-90, minimum amount 1.");
                return true;
            }
            ItemStack voucher = this.createVoucher(percent, amount);
            HashMap<Integer, ItemStack> left = target.getInventory().addItem(new ItemStack[]{voucher});
            for (ItemStack it : left.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), it);
            }
            this.msg(sender, this.m("voucher-given"));
            return true;
        }
        if (args[0].equalsIgnoreCase("giveall") && args.length >= 3) {
            int percent = (int)this.parseLong(args[1], -1L);
            int amount = (int)this.parseLong(args[2], -1L);
            if (percent <= 0 || percent > 90 || amount <= 0) {
                this.msg(sender, "&cPercent 1-90, minimum amount 1.");
                return true;
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getInventory().addItem(new ItemStack[]{this.createVoucher(percent, amount)});
            }
            this.msg(sender, "&aVoucher diberikan ke semua player online.");
            return true;
        }
        this.msg(sender, "&c/rke voucher give <player> <percent> <amount>");
        return true;
    }

    private void openMain(Player p) {
        int size = this.marketCfg.getInt("gui.main.size", 54);
        String title = this.marketCfg.getString("gui.main.title", "&8Market Categories");
        Inventory inv = Bukkit.createInventory((InventoryHolder)new MarketHolder("main", null), size, this.color(title));
        String fillMatStr = this.marketCfg.getString("gui.main.fill", "GRAY_STAINED_GLASS_PANE");
        Material fillMat = Material.matchMaterial(fillMatStr);
        if (fillMat != null && fillMat != Material.AIR) {
            ItemStack fill = this.icon(fillMat, " ", Collections.emptyList());
            for (int i = 0; i < size; i++) inv.setItem(i, fill);
        }
        org.bukkit.configuration.ConfigurationSection categories = this.marketCfg.getConfigurationSection("categories");
        if (categories != null) {
            for (String key : categories.getKeys(false)) {
                int slot = categories.getInt(key + ".slot", -1);
                if (slot >= 0 && slot < size) {
                    Material mat = Material.matchMaterial(categories.getString(key + ".material", "STONE"));
                    if (mat == null) mat = Material.STONE;
                    String name = categories.getString(key + ".display-name", "&a" + key);
                    java.util.List<String> lore = categories.getStringList(key + ".lore");
                    inv.setItem(slot, this.icon(mat, name, lore));
                }
            }
        }
        inv.setItem(this.marketCfg.getInt("gui.main.slots.voucher", 48), this.icon(Material.PAPER, "&dVoucher Diskon", Arrays.asList("&7Pilih voucher yang mau dipakai.", "&7Voucher is not automatically used.", "&fKlik untuk pilih voucher.")));
        inv.setItem(this.marketCfg.getInt("gui.main.slots.info", 49), this.icon(Material.BOOK, "&bInfo Ekonomi", Arrays.asList("&7Untuk menjaga ekonomi server tetap stabil,", "&7only farm items and mob drops can be sold.")));
        if (this.hasAdmin((CommandSender)p, "market.admin")) {
            inv.setItem(this.marketCfg.getInt("gui.main.slots.admin", 53), this.icon(Material.COMMAND_BLOCK, "&cAdmin Market", Arrays.asList("&7Kelola item, stock, harga, demand,", "&7statistik, refund, dan reload config.")));
        }
        int balSlot = this.marketCfg.getInt("gui.main.slots.balance", 50);
        if (balSlot >= 0 && balSlot < size) {
            String balStr = this.formatRp(this.getBalance(p.getUniqueId()));
            inv.setItem(balSlot, this.icon(Material.EMERALD, "&eYour Money", Arrays.asList("&7Sisa uangmu saat ini:", "&a" + balStr)));
        }
        p.openInventory(inv);
    }

    private void openCategory(Player p, String category, int page) {
        String catName = this.marketCfg.getString("categories." + category + ".display-name", category);
        String title = "&8" + ChatColor.stripColor(this.color(catName));
        if (page > 0) title += " (P." + (page + 1) + ")";
        Inventory inv = Bukkit.createInventory((InventoryHolder)new MarketHolder("category", category, page), (int)54, (String)this.color(title));
        
        java.util.List<MarketItem> catItems = new java.util.ArrayList<>();
        for (MarketItem mi : this.items.values()) {
            if (mi.enabled && mi.category.equalsIgnoreCase(category)) {
                catItems.add(mi);
            }
        }
        
        int totalItems = catItems.size();
        int maxPages = (int) Math.ceil(totalItems / 27.0);
        
        int startIndex = page * 27;
        int slot = 9;
        for (int i = startIndex; i < startIndex + 27 && i < totalItems; i++) {
            inv.setItem(slot++, this.marketIcon(catItems.get(i)));
        }
        
        if (page > 0) {
            inv.setItem(45, this.icon(Material.ARROW, "&aSebelumnya", Collections.singletonList("&7Klik untuk ke halaman sebelumnya.")));
        } else {
            inv.setItem(45, this.icon(Material.CHEST, "&eKe Menu Utama", Collections.singletonList("&7Click to return to category.")));
        }
        
        inv.setItem(49, this.icon(Material.BARRIER, "&cTutup", Collections.singletonList("&7Klik untuk menutup menu.")));
        
        if (page < maxPages && maxPages > 1 && page < maxPages - 1) {
            inv.setItem(53, this.icon(Material.ARROW, "&aSelanjutnya", Collections.singletonList("&7Klik untuk ke halaman selanjutnya.")));
        }
        
        p.openInventory(inv);
    }

    
    private void openQuantity(Player p, String itemKey) {
        MarketItem mi = this.items.get(itemKey);
        if (mi == null) return;
        Inventory inv = Bukkit.createInventory((InventoryHolder)new MarketHolder("quantity", itemKey), (int)36, (String)this.color("&8Select Amount (Buy/Sell)"));
        
        inv.setItem(13, this.marketIcon(mi));
        
        if (mi.buyEnabled) {
            long unitB = (long)Math.ceil((double)mi.currentBuyPrice / (double)mi.tradeAmount);
            inv.setItem(9, this.icon(Material.GREEN_STAINED_GLASS_PANE, "&aBeli 1", java.util.Collections.singletonList("&7Harga: &e" + this.formatRp(unitB * 1))));
            inv.setItem(10, this.icon(Material.GREEN_STAINED_GLASS_PANE, "&aBeli 10", java.util.Collections.singletonList("&7Harga: &e" + this.formatRp(unitB * 10))));
            inv.setItem(11, this.icon(Material.GREEN_STAINED_GLASS_PANE, "&aBeli 64", java.util.Collections.singletonList("&7Harga: &e" + this.formatRp(unitB * 64))));
        }
        
        if (this.isSellAllowed(mi)) {
            long unitS = (long)Math.floor((double)mi.currentSellPrice / (double)mi.tradeAmount);
            inv.setItem(15, this.icon(Material.RED_STAINED_GLASS_PANE, "&cJual 1", java.util.Collections.singletonList("&7Harga: &a" + this.formatRp(unitS * 1))));
            inv.setItem(16, this.icon(Material.RED_STAINED_GLASS_PANE, "&cJual 10", java.util.Collections.singletonList("&7Harga: &a" + this.formatRp(unitS * 10))));
            inv.setItem(17, this.icon(Material.RED_STAINED_GLASS_PANE, "&cJual 64", java.util.Collections.singletonList("&7Harga: &a" + this.formatRp(unitS * 64))));
        }
        
        inv.setItem(31, this.icon(Material.ARROW, "&cKembali", java.util.Collections.singletonList("&7Click to return.")));
        
        p.openInventory(inv);
    }

    private void openVouchers(Player p) {
        Inventory inv = Bukkit.createInventory((InventoryHolder)new MarketHolder("voucher", null), (int)54, (String)this.color("&8Pilih Voucher Diskon"));
        int slot = 0;
        for (ItemStack it : p.getInventory().getContents()) {
            if (it == null || !this.isVoucher(it)) continue;
            if (slot >= 45) break;
            inv.setItem(slot++, it.clone());
        }
        inv.setItem(47, this.icon(Material.REDSTONE, "&cRemove Active Voucher", Collections.singletonList("&7Click to cancel voucher.")));
        inv.setItem(49, this.icon(Material.BARRIER, "&cKembali", Collections.singletonList("&7Click to return.")));
        p.openInventory(inv);
    }

    private void openAdmin(Player p) {
        Inventory inv = Bukkit.createInventory((InventoryHolder)new MarketHolder("admin", null), (int)27, (String)this.color("&8Market Admin"));
        inv.setItem(10, this.icon(Material.CHEST, "&eKelola Item", Arrays.asList("&7Lihat item aktif/nonaktif.", "&7Edit detail lewat market.yml.")));
        inv.setItem(11, this.icon(Material.HOPPER, "&eKelola Stock", Arrays.asList("&7Stock otomatis tersimpan.", "&7Gunakan market.yml + /market reload.")));
        inv.setItem(12, this.icon(Material.GOLD_INGOT, "&eManage Prices", Arrays.asList("&7Current price is in data/prices.yml.", "&7Base price is in market.yml.")));
        inv.setItem(13, this.icon(Material.COMPARATOR, "&eDemand Pricing", Arrays.asList("&7HYBRID/AUTO_DEMAND aman dibatasi.", "&7Klik untuk update demand sekarang.")));
        inv.setItem(14, this.icon(Material.PAPER, "&eLihat Statistik", Arrays.asList("&7Klik untuk menjalankan /market stats.")));
        inv.setItem(15, this.icon(Material.ENDER_CHEST, "&eRefund Pending", Arrays.asList("&7Klik untuk migrasi/refund Player Shop lama.")));
        inv.setItem(16, this.icon(Material.REDSTONE_TORCH, "&aReload Config", Arrays.asList("&7Klik untuk reload market config.")));
        p.openInventory(inv);
    }

    private void openClaim(Player p) {
        Inventory inv = Bukkit.createInventory((InventoryHolder)new MarketHolder("claim", null), (int)54, (String)this.color("&8Market Claim"));
        int slot = 0;
        ConfigurationSection sec = this.refundsCfg.getConfigurationSection("refunds");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                ItemMeta meta;
                String status;
                String base = "refunds." + id + ".";
                if (!p.getUniqueId().toString().equals(this.refundsCfg.getString(base + "player_uuid")) || (status = this.refundsCfg.getString(base + "status", "PENDING")).equals("CLAIMED")) continue;
                ItemStack item = this.decodeItem(this.refundsCfg.getString(base + "item_stack_serialized", ""));
                if (item == null) {
                    item = this.icon(Material.BARRIER, "&cFailed to read item", Arrays.asList("&7Refund ID: &f" + id));
                }
                if ((meta = item.getItemMeta()) != null) {
                    ArrayList<String> lore = meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList();
                    lore.add(this.color("&8Refund ID: " + id));
                    lore.add(this.color("&eKlik untuk claim."));
                    meta.setLore(lore);
                    meta.getPersistentDataContainer().set(this.keyRefundId, PersistentDataType.STRING, id);
                    item.setItemMeta(meta);
                }
                if (slot >= 45) continue;
                inv.setItem(slot++, item);
            }
        }
        inv.setItem(49, this.icon(Material.BARRIER, "&cKembali", Collections.singletonList("&7Click to return.")));
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!(e.getInventory().getHolder() instanceof MarketHolder)) {
            return;
        }
        e.setCancelled(true);
        Player p = (Player)e.getWhoClicked();
        MarketHolder holder = (MarketHolder)e.getInventory().getHolder();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        if (holder.type.equals("main")) {
            if (e.getSlot() == this.marketCfg.getInt("gui.main.slots.voucher", 48)) {
                this.openVouchers(p);
                return;
            } else if (e.getSlot() == this.marketCfg.getInt("gui.main.slots.admin", 53) && this.hasAdmin((CommandSender)p, "market.admin")) {
                this.openAdmin(p);
                return;
            } else if (e.getSlot() == this.marketCfg.getInt("gui.main.slots.balance", 50)) {
                return;
            } else if (e.getSlot() == this.marketCfg.getInt("gui.main.slots.info", 49)) {
                return;
            }
            org.bukkit.configuration.ConfigurationSection categories = this.marketCfg.getConfigurationSection("categories");
            if (categories != null) {
                for (String key : categories.getKeys(false)) {
                    if (e.getSlot() == categories.getInt(key + ".slot", -1)) {
                        this.openCategory(p, key, 0);
                        return;
                    }
                }
            }
            return;
        }
        
        if (holder.type.equals("quantity")) {
            if (e.getSlot() == 31) {
                MarketItem mi = (MarketItem)this.items.get(holder.value);
                if (mi != null) {
                    this.openCategory(p, mi.category, 0);
                } else {
                    this.openMain(p);
                }
                return;
            }
            MarketItem mi = (MarketItem)this.items.get(holder.value);
            if (mi == null) return;
            
            if (e.getSlot() == 9) this.buyItem(p, mi, 1);
            else if (e.getSlot() == 10) this.buyItem(p, mi, 10);
            else if (e.getSlot() == 11) this.buyItem(p, mi, 64);
            else if (e.getSlot() == 15 && this.isSellAllowed(mi)) this.sellMaterial(p, mi, 1, true);
            else if (e.getSlot() == 16 && this.isSellAllowed(mi)) this.sellMaterial(p, mi, 10, true);
            else if (e.getSlot() == 17 && this.isSellAllowed(mi)) this.sellMaterial(p, mi, 64, true);
            
            this.openQuantity(p, holder.value);
            return;
        }

        if (holder.type.equals("category")) {
            if (e.getSlot() == 45) {
                if (holder.page > 0) {
                    this.openCategory(p, holder.value, holder.page - 1);
                } else {
                    this.openMain(p);
                }
                return;
            }
            if (e.getSlot() == 49) {
                p.closeInventory();
                return;
            }
            if (e.getSlot() == 53) {
                if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
                    this.openCategory(p, holder.value, holder.page + 1);
                }
                return;
            }
            String itemKey = this.getPdcString(clicked, this.keyGuiItem);
            if (itemKey == null) {
                return;
            }
            this.openQuantity(p, itemKey);
            return;
        }
        if (holder.type.equals("voucher")) {
            if (e.getSlot() == 49) {
                this.openMain(p);
                return;
            }
            if (e.getSlot() == 47) {
                this.selectedVoucher.remove(p.getUniqueId());
                this.msg((CommandSender)p, this.m("voucher-cleared"));
                this.openMain(p);
                return;
            }
            if (!this.isVoucher(clicked)) {
                return;
            }
            String id = this.getPdcString(clicked, this.keyVoucherId);
            Integer percent = this.getPdcInt(clicked, this.keyVoucherPercent);
            if (id == null || percent == null) {
                return;
            }
            this.selectedVoucher.put(p.getUniqueId(), id);
            this.msg((CommandSender)p, this.m("voucher-selected").replace("%diskon%", String.valueOf(percent)));
            this.openMain(p);
            return;
        }
        if (holder.type.equals("claim")) {
            if (e.getSlot() == 49) {
                this.openMain(p);
                return;
            }
            String id = this.getPdcString(clicked, this.keyRefundId);
            if (id != null) {
                this.claimRefund(p, id, true);
            }
            this.openClaim(p);
            return;
        }
        if (holder.type.equals("admin")) {
            if (!this.hasAdmin((CommandSender)p, "market.admin")) {
                return;
            }
            if (e.getSlot() == 13) {
                this.updateDemandPrices(true);
                this.msg((CommandSender)p, "&aDemand pricing updated.");
            }
            if (e.getSlot() == 14) {
                p.closeInventory();
                this.sendStats((CommandSender)p);
            }
            if (e.getSlot() == 15) {
                p.closeInventory();
                this.runRefundMigration((CommandSender)p, true);
            }
            if (e.getSlot() == 16) {
                this.reloadAll();
                this.msg((CommandSender)p, this.m("reloaded"));
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> this.autoClaimOnLogin(e.getPlayer()), 40L);
    }

    private ItemStack marketIcon(MarketItem mi) {
        ItemStack it = new ItemStack(mi.material, Math.max(1, Math.min(mi.tradeAmount, mi.material.getMaxStackSize())));
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(this.color(mi.displayName));
            ArrayList<String> lore = new ArrayList<String>();
            lore.add(this.color("&8Kategori: &f" + this.niceCategory(mi.category)));
            lore.add(this.color("&7Trade Amount: &f" + mi.tradeAmount));
            if (mi.buyEnabled) {
                long diff = mi.currentBuyPrice - mi.baseBuyPrice;
                String indicator = "";
                if (diff != 0 && mi.baseBuyPrice > 0) {
                    long pct = Math.abs(diff) * 100L / mi.baseBuyPrice;
                    indicator = diff > 0 ? "&a\u2191 +" + pct + "%" : "&c\u2193 -" + pct + "%";
                }
                lore.add(this.color("&7Harga Beli: &a" + this.formatRp(mi.currentBuyPrice) + " " + indicator));
            } else {
                lore.add(this.color("&7Buy Price: &cNot for sale"));
            }
            if (this.isSellAllowed(mi)) {
                long diff = mi.currentSellPrice - mi.baseSellPrice;
                String indicator = "";
                if (diff != 0 && mi.baseSellPrice > 0) {
                    long pct = Math.abs(diff) * 100L / mi.baseSellPrice;
                    indicator = diff > 0 ? "&a\u2191 +" + pct + "%" : "&c\u2193 -" + pct + "%";
                }
                lore.add(this.color("&7Harga Jual: &a" + this.formatRp(mi.currentSellPrice) + " " + indicator));
            } else {
                lore.add(this.color("&7Sell Price: &cCannot be sold"));
            }
            lore.add(this.color("&7Stock: &f" + (String)(mi.stockEnabled ? mi.stockCurrent + "/" + mi.stockMax : "Unlimited")));

            lore.add("");
            lore.add(this.color("&eClick &7to Buy / Sell / Select Amount"));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(this.keyGuiItem, PersistentDataType.STRING, mi.key);
            it.setItemMeta(meta);
        }
        return it;
    }

    private boolean isSellAllowed(MarketItem mi) {
        return mi != null && mi.enabled && mi.sellEnabled && this.allowedSellCategories.contains(mi.category);
    }

    private long sellMaterial(Player p, MarketItem mi, int amount, boolean verbose) {
        long minHours = this.getConfig().getLong("settings.min-playtime-hours-to-sell", 0L);
        if (minHours > 0L) {
            long ticksPlayed = p.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
            long hoursPlayed = ticksPlayed / 20L / 60L / 60L;
            if (hoursPlayed < minHours) {
                this.msg((CommandSender)p, this.m("not-enough-playtime").replace("%hours%", String.valueOf(minHours)));
                return 0L;
            }
        }
        if (!this.isSellAllowed(mi)) {
            this.msg((CommandSender)p, this.m("item-not-sellable"));
            return 0L;
        }
        int sellAmount = amount;
        if (sellAmount <= 0) {
            this.msg((CommandSender)p, this.m("not-enough-item"));
            return 0L;
        }
        int remain = this.getRemainingDaily(p.getUniqueId(), mi.key, "SELL", mi.dailySellLimit);
        if (mi.dailySellLimit > 0 && sellAmount > remain) {
            sellAmount = remain;
        }
        if (sellAmount <= 0) {
            this.msg((CommandSender)p, this.m("daily-limit"));
            return 0L;
        }
        if (this.countMaterial(p, mi.material) < sellAmount) {
            this.msg((CommandSender)p, this.m("not-enough-item"));
            return 0L;
        }
        this.removeMaterial(p, mi.material, sellAmount);
        long unitPrice = (long)Math.floor((double)mi.currentSellPrice / (double)mi.tradeAmount);
        long total = (long)sellAmount * unitPrice;
        this.addBalance(p.getUniqueId(), total);
        this.addDaily(p.getUniqueId(), mi.key, "SELL", sellAmount);
        this.addStat(mi.key, "total_sold", (long)sellAmount);
        this.addStat(mi.key, "window_sell_amount", (long)sellAmount);
        this.addStat(mi.key, "sell_rp", total);
        this.logTransaction(p, mi, sellAmount, "SELL", mi.currentSellPrice, total, mi.stockCurrent, mi.stockCurrent);
        if (verbose) {
            this.msg((CommandSender)p, this.m("sell-success").replace("%amount%", String.valueOf(sellAmount)).replace("%item%", this.plain(mi.displayName)).replace("%price%", this.formatNumber(total)));
        }
        return total;
    }

    private void buyItem(Player p, MarketItem mi, int amount) {
        if (!mi.enabled || !mi.buyEnabled) {
            this.msg((CommandSender)p, "&cThis item cannot be bought.");
            return;
        }
        int buyAmount = amount;
        if (buyAmount <= 0) {
            this.msg((CommandSender)p, this.m("not-enough-item"));
            return;
        }
        int remain = this.getRemainingDaily(p.getUniqueId(), mi.key, "BUY", mi.dailyBuyLimit);
        if (mi.dailyBuyLimit > 0 && buyAmount > remain) {
            buyAmount = remain;
        }
        if (buyAmount <= 0) {
            this.msg((CommandSender)p, this.m("daily-limit"));
            return;
        }
        if (mi.stockEnabled && mi.stockCurrent >= 0L && mi.stockCurrent < (long)buyAmount) {
            this.msg((CommandSender)p, this.m("stock-empty"));
            return;
        }
        long unitPrice = (long)Math.ceil((double)mi.currentBuyPrice / (double)mi.tradeAmount);
        long total = (long)buyAmount * unitPrice;
        VoucherResult vr = this.applySelectedVoucherPreview(p, total);
        long finalTotal = vr.finalPrice;
        if (this.getBalance(p.getUniqueId()) < finalTotal) {
            this.msg((CommandSender)p, this.m("not-enough-money").replace("%price%", this.formatNumber(finalTotal)));
            return;
        }
        this.takeBalance(p.getUniqueId(), finalTotal);
        if (vr.applied) {
            this.consumeVoucher(p, vr.voucherId);
        }
        ItemStack item = new ItemStack(mi.material, buyAmount);
        java.util.HashMap<Integer, ItemStack> left = p.getInventory().addItem(new ItemStack[]{item});
        if (!left.isEmpty()) {
            for (ItemStack leftover : left.values()) {
                p.getWorld().dropItem(p.getLocation(), leftover);
            }
            this.msg((CommandSender)p, "&eInventory penuh! Sisa item dijatuhkan ke lantai.");
        }
        long before = mi.stockCurrent;
        if (mi.stockEnabled && mi.stockCurrent >= 0L) {
            mi.stockCurrent -= (long)buyAmount;
            this.stockCfg.set(mi.key + ".current", mi.stockCurrent);    
        }
        this.addDaily(p.getUniqueId(), mi.key, "BUY", buyAmount);
        this.addStat(mi.key, "total_bought", (long)buyAmount);
        this.addStat(mi.key, "window_buy_amount", (long)buyAmount);
        this.addStat(mi.key, "buy_rp", finalTotal);
        this.logTransaction(p, mi, buyAmount, "BUY", mi.currentBuyPrice, finalTotal, before, mi.stockCurrent);
        this.msg((CommandSender)p, this.m("buy-success").replace("%amount%", String.valueOf(buyAmount)).replace("%item%", this.plain(mi.displayName)).replace("%price%", this.formatNumber(finalTotal)));
    }

    private VoucherResult applySelectedVoucherPreview(Player p, long price) {
        String id = (String)this.selectedVoucher.get(p.getUniqueId());
        if (id == null) {
            return new VoucherResult(false, null, price);
        }
        for (ItemStack it : p.getInventory().getContents()) {
            if (it == null || !this.isVoucher(it)) continue;
            String vid = this.getPdcString(it, this.keyVoucherId);
            Integer percent = this.getPdcInt(it, this.keyVoucherPercent);
            if (!id.equals(vid) || percent == null) continue;
            long discounted = (long)Math.ceil((double)price * (100.0 - (double)percent.intValue()) / 100.0);
            discounted = Math.max(1L, discounted);
            return new VoucherResult(true, id, discounted);
        }
        this.selectedVoucher.remove(p.getUniqueId());
        return new VoucherResult(false, null, price);
    }

    private void consumeVoucher(Player p, String id) {
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; ++i) {
            String vid;
            ItemStack it = contents[i];
            if (it == null || !this.isVoucher(it) || !id.equals(vid = this.getPdcString(it, this.keyVoucherId))) continue;
            if (it.getAmount() > 1) {
                it.setAmount(it.getAmount() - 1);
            } else {
                p.getInventory().setItem(i, null);
            }
            this.selectedVoucher.remove(p.getUniqueId());
            return;
        }
    }

    private ItemStack createVoucher(int percent, int amount) {
        ItemStack it = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(this.color("&dVoucher Diskon " + percent + "%"));
            meta.setLore(Arrays.asList(this.color("&7Pakai voucher ini lewat GUI /market."), this.color("&7Diskon: &d" + percent + "%"), this.color("&8Bisa diberikan ke player lain.")));
            meta.getPersistentDataContainer().set(this.keyVoucherId, PersistentDataType.STRING, UUID.randomUUID().toString().replace("-", ""));
            meta.getPersistentDataContainer().set(this.keyVoucherPercent, PersistentDataType.INTEGER, percent);
            it.setItemMeta(meta);
        }
        return it;
    }

    private boolean isVoucher(ItemStack it) {
        if (it == null || !it.hasItemMeta()) {
            return false;
        }
        return it.getItemMeta().getPersistentDataContainer().has(this.keyVoucherPercent, PersistentDataType.INTEGER);
    }

    private void restockItems() {
        long now = System.currentTimeMillis();
        for (MarketItem mi : this.items.values()) {
            if (!mi.stockEnabled || mi.restockIntervalMinutes <= 0L || mi.restockAmount <= 0L || mi.stockMax < 0L) continue;
            long last = this.stockCfg.getLong(mi.key + ".last_restock", 0L);
            if (last == 0L) {
                this.stockCfg.set(mi.key + ".last_restock", now);
                continue;
            }
            long intervalMs = mi.restockIntervalMinutes * 60000L;
            if (now - last < intervalMs || mi.stockCurrent >= mi.stockMax) continue;
            long before = mi.stockCurrent;
            mi.stockCurrent = Math.min(mi.stockMax, mi.stockCurrent + mi.restockAmount);
            this.stockCfg.set(mi.key + ".current", mi.stockCurrent);
            this.stockCfg.set(mi.key + ".last_restock", now);
            this.logLine("market_transactions.log", this.nowLine() + " RESTOCK item=" + mi.key + " before=" + before + " after=" + mi.stockCurrent);
        }
    }

    private void updateDemandPrices(boolean manual) {
        for (MarketItem mi : this.items.values()) {
            double pct;
            if (!mi.demandEnabled || mi.adminLocked || mi.pricingMode.equals("MANUAL")) continue;
            long wb = this.stat(mi.key, "window_buy_amount");
            long ws = this.stat(mi.key, "window_sell_amount");
            long oldBuy = mi.currentBuyPrice;
            long oldSell = mi.currentSellPrice;
            if (mi.buyEnabled && mi.currentBuyPrice > 0L) {
                pct = wb >= (long)mi.tradeAmount * 20L ? mi.maxChangeUpdate : (wb == 0L ? -Math.min(2.0, mi.maxChangeUpdate) : 0.0);
                mi.currentBuyPrice = this.clampPrice(mi.currentBuyPrice, pct, mi.minBuyPrice, mi.maxBuyPrice);
            }
            if (mi.sellEnabled && mi.currentSellPrice > 0L) {
                pct = ws >= (long)mi.tradeAmount * 32L ? -mi.maxChangeUpdate : (ws == 0L ? Math.min(1.0, mi.maxChangeUpdate) : 0.0);
                mi.currentSellPrice = this.clampPrice(mi.currentSellPrice, pct, mi.minSellPrice, mi.maxSellPrice);
            }
            if (oldBuy != mi.currentBuyPrice || oldSell != mi.currentSellPrice) {
                this.pricesCfg.set(mi.key + ".buy", mi.currentBuyPrice);
                this.pricesCfg.set(mi.key + ".sell", mi.currentSellPrice);
                this.logLine("market_price_changes.log", this.nowLine() + " item=" + mi.key + " buy=" + oldBuy + "->" + mi.currentBuyPrice + " sell=" + oldSell + "->" + mi.currentSellPrice + " manual=" + manual);
            }
            this.statsCfg.set("items." + mi.key + ".window_buy_amount", 0);
            this.statsCfg.set("items." + mi.key + ".window_sell_amount", 0);
        }
    }

    private long clampPrice(long current, double pct, long min, long max) {
        if (pct == 0.0) {
            return current;
        }
        long changed = Math.round((double)current * (1.0 + pct / 100.0));
        return Math.max(min, Math.min(max, changed));
    }

    private void runRefundMigration(CommandSender sender, boolean manual) {
        try {
            int created = 0;
            List<String> paths = this.getConfig().getStringList("old-player-shop.search-files");
            File backupDir = new File(this.getDataFolder(), "data/backups");
            backupDir.mkdirs();
            for (String p : paths) {
                File f = new File(p);
                if (!f.exists()) continue;
                if (this.getConfig().getBoolean("settings.keep-backup-before-migration", true)) {
                    Files.copy(f.toPath(), new File(backupDir, f.getName() + "." + System.currentTimeMillis() + ".bak").toPath(), new CopyOption[0]);
                }
                String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                Pattern pat = Pattern.compile("\"([a-fA-F0-9]{32})\"\\s*:\\s*\\{\\s*\"sellerUuid\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"sellerName\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"item\"\\s*:\\s*\"([^\"]+)\"", 32);
                Matcher m = pat.matcher(text);
                while (m.find()) {
                    String listingId = m.group(1);
                    if (this.refundExistsForSource(listingId)) continue;
                    String refundId = UUID.randomUUID().toString().replace("-", "");
                    String base = "refunds." + refundId + ".";
                    this.refundsCfg.set(base + "refund_id", refundId);
                    this.refundsCfg.set(base + "player_uuid", m.group(2));
                    this.refundsCfg.set(base + "player_name", m.group(3));
                    this.refundsCfg.set(base + "item_stack_serialized", m.group(4));
                    this.refundsCfg.set(base + "amount", 1);
                    this.refundsCfg.set(base + "source", f.getPath());
                    this.refundsCfg.set(base + "source_listing_id", listingId);
                    this.refundsCfg.set(base + "reason", "Economy controlled market update. Player Shop disabled/refunded.");
                    this.refundsCfg.set(base + "status", "PENDING");
                    this.refundsCfg.set(base + "created_at", System.currentTimeMillis());
                    ++created;
                    this.logLine("market_refund.log", this.nowLine() + " REFUND_PENDING listing=" + listingId + " player=" + m.group(3) + " uuid=" + m.group(2));
                }
            }
            this.migrationCfg.set("old-player-shop-refund.done", true);
            this.migrationCfg.set("old-player-shop-refund.last_run", System.currentTimeMillis());
            this.migrationCfg.set("old-player-shop-refund.last_created", created);
            this.saveData();
            this.msg(sender, "&aRefund migration selesai. Pending baru: &f" + created);
        }
        catch (Exception e) {
            this.msg(sender, "&cRefund migration gagal: " + e.getMessage());
            this.getLogger().warning("Refund migration failed: " + e.getMessage());
        }
    }

    private boolean refundExistsForSource(String listingId) {
        ConfigurationSection sec = this.refundsCfg.getConfigurationSection("refunds");
        if (sec == null) {
            return false;
        }
        for (String id : sec.getKeys(false)) {
            if (!listingId.equals(this.refundsCfg.getString("refunds." + id + ".source_listing_id"))) continue;
            return true;
        }
        return false;
    }

    private void autoClaimOnLogin(Player p) {
        int claimed = 0;
        ConfigurationSection sec = this.refundsCfg.getConfigurationSection("refunds");
        if (sec == null) {
            return;
        }
        for (String id : new ArrayList<String>(sec.getKeys(false))) {
            String base = "refunds." + id + ".";
            if (!p.getUniqueId().toString().equals(this.refundsCfg.getString(base + "player_uuid")) || "CLAIMED".equals(this.refundsCfg.getString(base + "status")) || !this.claimRefund(p, id, false)) continue;
            ++claimed;
        }
        if (claimed > 0) {
            this.msg((CommandSender)p, this.m("refund-login").replace("%amount%", String.valueOf(claimed)));
        }
    }

    private boolean claimRefund(Player p, String id, boolean verbose) {
        String base = "refunds." + id + ".";
        if (!this.refundsCfg.contains(base + "item_stack_serialized")) {
            return false;
        }
        if ("CLAIMED".equals(this.refundsCfg.getString(base + "status"))) {
            return false;
        }
        ItemStack item = this.decodeItem(this.refundsCfg.getString(base + "item_stack_serialized", ""));
        if (item == null) {
            this.refundsCfg.set(base + "status", "ERROR");
            return false;
        }
        HashMap<Integer, ItemStack> left = p.getInventory().addItem(new ItemStack[]{item});
        if (!left.isEmpty()) {
            this.refundsCfg.set(base + "status", "PENDING");
            if (verbose) {
                this.msg((CommandSender)p, this.m("claim-full"));
            }
            return false;
        }
        this.refundsCfg.set(base + "status", "CLAIMED");
        this.refundsCfg.set(base + "claimed_at", System.currentTimeMillis());
        this.logLine("market_refund.log", this.nowLine() + " CLAIM player=" + p.getName() + " refund=" + id);
        if (verbose) {
            this.msg((CommandSender)p, this.m("claim-success"));
        }
        return true;
    }

    private ItemStack decodeItem(String base64) {
        try (BukkitObjectInputStream in = new BukkitObjectInputStream((InputStream)new ByteArrayInputStream(Base64.getDecoder().decode(base64)));){
            Object obj = in.readObject();
            if (obj instanceof ItemStack) {
                ItemStack itemStack = (ItemStack)obj;
                return itemStack;
            }
            ItemStack itemStack = null;
            return itemStack;
        }
        catch (Exception e) {
            return null;
        }
    }

    private String encodeItem(ItemStack item) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream out = new BukkitObjectOutputStream((OutputStream)bytes);){
                out.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        }
        catch (Exception e) {
            return "";
        }
    }

    private void sendStats(CommandSender sender) {
        sender.sendMessage(this.color("&6=== &eTop Market Stats &6==="));
        
        java.util.List<MarketItem> topBought = new java.util.ArrayList<>(this.items.values());
        topBought.removeIf(mi -> this.stat(mi.key, "total_bought") == 0);
        topBought.sort((a, b) -> Long.compare(this.stat(b.key, "total_bought"), this.stat(a.key, "total_bought")));
        
        java.util.List<MarketItem> topSold = new java.util.ArrayList<>(this.items.values());
        topSold.removeIf(mi -> this.stat(mi.key, "total_sold") == 0);
        topSold.sort((a, b) -> Long.compare(this.stat(b.key, "total_sold"), this.stat(a.key, "total_sold")));
        
        sender.sendMessage(this.color("&b&lTop 10 Barang Paling Banyak Dibeli:"));
        for (int i = 0; i < Math.min(10, topBought.size()); i++) {
            MarketItem mi = topBought.get(i);
            long count = this.stat(mi.key, "total_bought");
            long rp = this.stat(mi.key, "buy_rp");
            sender.sendMessage(this.color("&f" + (i+1) + ". &e" + mi.displayName + " &7- Terbeli: &a" + count + "x &7(" + this.formatRp(rp) + ")"));
        }
        if (topBought.isEmpty()) sender.sendMessage(this.color("&7- No purchase data yet."));
        
        sender.sendMessage("");
        sender.sendMessage(this.color("&c&lTop 10 Barang Paling Banyak Dijual:"));
        for (int i = 0; i < Math.min(10, topSold.size()); i++) {
            MarketItem mi = topSold.get(i);
            long count = this.stat(mi.key, "total_sold");
            long rp = this.stat(mi.key, "sell_rp");
            sender.sendMessage(this.color("&f" + (i+1) + ". &e" + mi.displayName + " &7- Terjual: &c" + count + "x &7(" + this.formatRp(rp) + ")"));
        }
        if (topSold.isEmpty()) sender.sendMessage(this.color("&7- No sales data yet."));
    }

    private void sendPlaceholders(CommandSender sender) {
        this.msg(sender, "&aPlaceholderAPI:");
        this.msg(sender, "&f%rke_balance% &7= balance in Rp format");
        this.msg(sender, "&f%rke_balance_raw% &7= raw balance number");
        this.msg(sender, "&f%rke_balance_short% &7= short balance");
        this.msg(sender, "&f%rke_voucher% &7= active voucher");
    }

    public long getBalance(UUID uuid) {
        return this.balancesCfg.getLong("balances." + String.valueOf(uuid), this.getConfig().getLong("currency.starting-balance", 0L));
    }

    public void setBalance(UUID uuid, long amount) {
        this.balancesCfg.set("balances." + String.valueOf(uuid), Math.max(0L, amount));
    }

    public void addBalance(UUID uuid, long amount) {
        this.setBalance(uuid, this.getBalance(uuid) + amount);
    }

    public boolean takeBalance(UUID uuid, long amount) {
        long bal = this.getBalance(uuid);
        if (bal < amount) {
            return false;
        }
        long newBal = bal - amount;
        this.setBalance(uuid, newBal);
        
        if (newBal < 20000000L) {
            String path = "players." + uuid.toString() + ".hidden";
            if (this.balancesCfg != null && this.balancesCfg.getBoolean(path, false)) {
                this.balancesCfg.set(path, false);
                this.trySave(this.balancesCfg, this.balancesFile);
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    this.msg((CommandSender)p, "&cYour balance dropped below Rp 20,000,000, your hidebal status is disabled.");
                }
            }
        }
        
        return true;
    }

    public String getBalanceFormatted(OfflinePlayer p) {
        return this.formatRp(this.getBalance(p.getUniqueId()));
    }

    public String getBalanceRaw(OfflinePlayer p) {
        if (p != null) {
            String path = "players." + p.getUniqueId().toString() + ".hidden";
            if (this.balancesCfg != null && this.balancesCfg.getBoolean(path, false)) {
                return "";
            }
        }
        return String.valueOf(this.getBalance(p.getUniqueId()));
    }

    public String getBalanceShort(OfflinePlayer p) {
        long v = this.getBalance(p.getUniqueId());
        if (v >= 1000000L) {
            return "Rp " + v / 1000000L + "jt";
        }
        if (v >= 1000L) {
            return "Rp " + v / 1000L + "rb";
        }
        return this.formatRp(v);
    }

    public String getVoucherPlaceholder(OfflinePlayer p) {
        if (p == null) {
            return "-";
        }
        String id = (String)this.selectedVoucher.get(p.getUniqueId());
        if (id == null || !(p instanceof Player)) {
            return "-";
        }
        Player pl = (Player)p;
        for (ItemStack it : pl.getInventory().getContents()) {
            if (it == null || !this.isVoucher(it) || !id.equals(this.getPdcString(it, this.keyVoucherId))) continue;
            Integer pct = this.getPdcInt(it, this.keyVoucherPercent);
            return pct == null ? "-" : pct + "%";
        }
        return "-";
    }

    private int getRemainingDaily(UUID uuid, String item, String action, int limit) {
        if (limit <= 0) {
            return Integer.MAX_VALUE;
        }
        String key = this.dailyKey(uuid, item, action);
        int used = this.limitsCfg.getInt(key, 0);
        return Math.max(0, limit - used);
    }

    private void addDaily(UUID uuid, String item, String action, int amount) {
        String key = this.dailyKey(uuid, item, action);
        this.limitsCfg.set(key, (this.limitsCfg.getInt(key, 0) + amount));
    }

    private String dailyKey(UUID uuid, String item, String action) {
        return "limits." + String.valueOf(LocalDate.now()) + "." + String.valueOf(uuid) + "." + item + "." + action;
    }

    private int countMaterial(Player p, Material mat) {
        int c = 0;
        for (ItemStack it : p.getInventory().getContents()) {
            if (it == null || it.getType() != mat || it.hasItemMeta()) continue;
            c += it.getAmount();
        }
        return c;
    }

    private void removeMaterial(Player p, Material mat, int amount) {
        int left = amount;
        ItemStack[] cont = p.getInventory().getContents();
        for (int i = 0; i < cont.length && left > 0; ++i) {
            ItemStack it = cont[i];
            if (it == null || it.getType() != mat || it.hasItemMeta()) continue;
            int take = Math.min(left, it.getAmount());
            it.setAmount(it.getAmount() - take);
            if (it.getAmount() <= 0) {
                p.getInventory().setItem(i, null);
            }
            left -= take;
        }
    }

    private void addStat(String item, String field, long amount) {
        this.statsCfg.set("items." + item + "." + field, (this.stat(item, field) + amount));
    }

    private long stat(String item, String field) {
        return this.statsCfg.getLong("items." + item + "." + field, 0L);
    }

    private boolean hasUse(CommandSender s, String perm) {
        return s.hasPermission(perm) || s.hasPermission("rumahkita.economy.use");
    }

    private boolean hasAdmin(CommandSender s, String perm) {
        return s.hasPermission(perm) || s.hasPermission("market.admin") || s.hasPermission("rumahkita.economy.admin");
    }

    private boolean noPerm(CommandSender s) {
        this.msg(s, this.m("no-permission"));
        return true;
    }

    private boolean playerOnly(CommandSender s) {
        this.msg(s, this.m("player-only"));
        return true;
    }

    private boolean onCooldown(Player p) {
        long cd = Math.max(0L, this.getConfig().getLong("settings.transaction-cooldown-ms", 700L));
        long now = System.currentTimeMillis();
        Long last = (Long)this.cooldowns.get(p.getUniqueId());
        if (last != null && now - last < cd) {
            return true;
        }
        this.cooldowns.put(p.getUniqueId(), now);
        return false;
    }

    private ItemStack icon(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(this.color(name));
            ArrayList<String> out = new ArrayList<String>();
            for (String l : lore) {
                out.add(this.color(l));
            }
            meta.setLore(out);
            it.setItemMeta(meta);
        }
        return it;
    }

    private String getPdcString(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private Integer getPdcInt(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
    }

    private void logTransaction(Player p, MarketItem mi, int amount, String action, long priceEach, long total, long stockBefore, long stockAfter) {
        this.logLine("market_transactions.log", this.nowLine() + " player_uuid=" + String.valueOf(p.getUniqueId()) + " player_name=" + p.getName() + " item=" + mi.key + " amount=" + amount + " action=" + action + " price_each=" + priceEach + " total_price=" + total + " stock_before=" + stockBefore + " stock_after=" + stockAfter);
    }

    private void logAbuse(Player p, String item, String action) {
        this.logLine("market_abuse_warning.log", this.nowLine() + " player_uuid=" + String.valueOf(p.getUniqueId()) + " player_name=" + p.getName() + " item=" + item + " action=" + action);
    }

    private void logLine(String file, String line) {
        try {
            File f = new File(this.getDataFolder(), "logs/" + file);
            f.getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(f, true);){
                fw.write(line + System.lineSeparator());
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private String nowLine() {
        return LocalDateTime.now().format(this.timeFmt);
    }

    private String m(String key) {
        return this.messagesCfg.getString(key, key);
    }

    private void msg(CommandSender s, String message) {
        String prefix = this.messagesCfg.getString("prefix", "");
        for (String line : message.split("\\n")) {
            s.sendMessage(this.color(prefix + line));
        }
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)(s == null ? "" : s));
    }

    private String plain(String s) {
        return ChatColor.stripColor((String)this.color(s));
    }

    private String niceCategory(String cat) {
        if (cat.equals("mobdrops")) {
            return "Mob Drops";
        }
        if (cat.equals("farming")) {
            return "Farming";
        }
        if (cat.equals("servermarket")) {
            return "Server Market";
        }
        return cat;
    }

    private long parseLong(String s, long def) {
        try {
            return Long.parseLong(s.replace(".", ""));
        }
        catch (Exception e) {
            return def;
        }
    }

    public String formatRp(long amount) {
        return this.getConfig().getString("currency.prefix", "Rp") + " " + this.formatNumber(amount);
    }

    private String formatNumber(long amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(this.getConfig().getString("currency.thousands-separator", ".").charAt(0));
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format(amount);
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("market") && args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("claim", "stats"));
            if (sender.hasPermission("market.admin")) {
                options.addAll(Arrays.asList("admin", "reload", "refundshop"));
            }
            return this.filter(options, args[0]);
        }
        if (name.equals("bal") || name.equals("pay")) {
            if (args.length == 1) {
                List<String> players = new ArrayList<>();
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    players.add(p.getName());
                }
                return this.filter(players, args[0]);
            }
        }
        if (name.equals("payall")) {
            if (args.length == 1) {
                return Collections.singletonList("<total_uang>");
            } else if (args.length >= 2) {
                return Collections.singletonList("<alasan...>");
            }
            return Collections.emptyList();
        }
        if (name.equals("rke")) {
            if (!sender.hasPermission("rumahkita.economy.admin")) {
                return Collections.emptyList();
            }
            if (args.length == 1) {
                return this.filter(Arrays.asList("give", "take", "set", "balance", "voucher", "reload", "save", "placeholders", "demandupdate", "migratebalances"), args[0]);
            }
            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("voucher")) {
                    return this.filter(Arrays.asList("give", "giveall"), args[1]);
                } else if (Arrays.asList("give", "take", "set", "balance").contains(args[0].toLowerCase())) {
                    List<String> players = new ArrayList<>();
                    for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                        players.add(p.getName());
                    }
                    return this.filter(players, args[1]);
                }
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("voucher") && args[1].equalsIgnoreCase("give")) {
                List<String> players = new ArrayList<>();
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    players.add(p.getName());
                }
                return this.filter(players, args[2]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> in, String prefix) {
        ArrayList<String> out = new ArrayList<String>();
        for (String s : in) {
            if (!s.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) continue;
            out.add(s);
        }
        return out;
    }

    public static class MarketItem {
        public String key;
        public boolean enabled;
        public String category;
        public Material material;
        public String displayName;
        public int tradeAmount;
        public boolean buyEnabled;
        public boolean sellEnabled;
        public long baseBuyPrice;
        public long baseSellPrice;
        public long currentBuyPrice;
        public long currentSellPrice;
        public int dailyBuyLimit;
        public int dailySellLimit;
        public boolean stockEnabled;
        public long stockCurrent;
        public long stockMax;
        public long restockAmount;
        public long restockIntervalMinutes;
        public String pricingMode;
        public boolean demandEnabled;
        public long minBuyPrice;
        public long maxBuyPrice;
        public long minSellPrice;
        public long maxSellPrice;
        public double maxChangeUpdate;
        public double maxChangeDay;
        public boolean adminLocked;
    }

    public static class MarketHolder implements InventoryHolder {
        public String type;
        public String value;
        public int page;
        public MarketHolder(String type, String value, int page) {
            this.type = type;
            this.value = value;
            this.page = page;
        }
        public MarketHolder(String type, String value) {
            this(type, value, 0);
        }
        public Inventory getInventory() { return Bukkit.createInventory(this, 9, "Market"); }
    }

    public static class VoucherResult {
        public boolean applied;
        public String voucherId;
        public long finalPrice;
        public VoucherResult(boolean applied, String voucherId, long finalPrice) {
            this.applied = applied;
            this.voucherId = voucherId;
            this.finalPrice = finalPrice;
        }
    }
}