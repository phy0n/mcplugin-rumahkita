/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.Guild;
import id.rumahkita.guilds.GuildRole;
import id.rumahkita.guilds.RumahKitaGuildsPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class GuildManager {
    private final RumahKitaGuildsPlugin plugin;
    private final Map<String, Guild> guildsByTag = new LinkedHashMap<String, Guild>();
    private final Map<UUID, String> playerGuild = new HashMap<UUID, String>();
    private final File dataFile;

    public GuildManager(RumahKitaGuildsPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "guilds.yml");
    }

    public void load() {
        this.guildsByTag.clear();
        this.playerGuild.clear();
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }
        if (!this.dataFile.exists()) {
            this.save();
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration((File)this.dataFile);
        ConfigurationSection guildsSection = data.getConfigurationSection("guilds");
        if (guildsSection == null) {
            return;
        }
        for (String key : guildsSection.getKeys(false)) {
            Guild guild = Guild.loadFrom(guildsSection.getConfigurationSection(key));
            if (guild == null) continue;
            String normalized = this.normalizeTag(guild.getTag());
            this.guildsByTag.put(normalized, guild);
            for (UUID member : guild.getMembers().keySet()) {
                this.playerGuild.put(member, normalized);
            }
        }
    }

    public void save() {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }
        YamlConfiguration data = new YamlConfiguration();
        ConfigurationSection guildsSection = data.createSection("guilds");
        for (Guild guild : this.guildsByTag.values()) {
            guild.saveTo(guildsSection.createSection(this.normalizeTag(guild.getTag()).toLowerCase(Locale.ROOT)));
        }
        try {
            data.save(this.dataFile);
        }
        catch (IOException ex) {
            this.plugin.getLogger().severe("Could not save guilds.yml: " + ex.getMessage());
        }
    }

    public Guild getGuildByTag(String tag) {
        return this.guildsByTag.get(this.normalizeTag(tag));
    }

    public Guild getGuild(Player player) {
        return player == null ? null : this.getGuild(player.getUniqueId());
    }

    public Guild getGuild(UUID uuid) {
        String tag = this.playerGuild.get(uuid);
        return tag == null ? null : this.guildsByTag.get(tag);
    }

    public Guild getGuildByName(String name) {
        if (name == null) {
            return null;
        }
        for (Guild guild : this.guildsByTag.values()) {
            if (!guild.getName().equalsIgnoreCase(name)) continue;
            return guild;
        }
        return null;
    }

    public boolean hasGuild(UUID uuid) {
        return this.playerGuild.containsKey(uuid);
    }

    public Collection<Guild> getGuilds() {
        return Collections.unmodifiableCollection(this.guildsByTag.values());
    }

    public CreateResult createGuild(Player player, String tag, String name) {
        if (this.hasGuild(player.getUniqueId())) {
            return CreateResult.ALREADY_IN_GUILD;
        }
        if (!this.isValidTag(tag)) {
            return CreateResult.INVALID_TAG;
        }
        if (!this.isValidName(name)) {
            return CreateResult.INVALID_NAME;
        }
        if (this.getGuildByTag(tag) != null) {
            return CreateResult.DUPLICATE_TAG;
        }
        if (this.getGuildByName(name) != null) {
            return CreateResult.DUPLICATE_NAME;
        }
        if (this.plugin.getConfig().getBoolean("settings.create-cost.enabled", true)) {
            Material material = Material.matchMaterial((String)this.plugin.getConfig().getString("settings.create-cost.material", "DIAMOND"));
            int amount = Math.max(0, this.plugin.getConfig().getInt("settings.create-cost.amount", 5));
            if (material == null) {
                material = Material.DIAMOND;
            }
            if (this.countItem(player, material) < amount) {
                return CreateResult.NOT_ENOUGH_COST;
            }
            this.removeItem(player, material, amount);
        }
        Guild guild = new Guild(tag.toUpperCase(Locale.ROOT), name, player.getUniqueId(), player.getName(), System.currentTimeMillis());
        this.guildsByTag.put(this.normalizeTag(tag), guild);
        this.playerGuild.put(player.getUniqueId(), this.normalizeTag(tag));
        this.save();
        return CreateResult.SUCCESS;
    }

    public void addMember(Guild guild, Player player) {
        guild.addMember(player.getUniqueId(), player.getName(), GuildRole.MEMBER, System.currentTimeMillis());
        this.playerGuild.put(player.getUniqueId(), this.normalizeTag(guild.getTag()));
        this.save();
    }

    public void removeMember(Guild guild, UUID uuid) {
        guild.removeMember(uuid);
        this.playerGuild.remove(uuid);
        this.save();
    }

    public void disband(Guild guild) {
        for (UUID member : new ArrayList<UUID>(guild.getMembers().keySet())) {
            this.playerGuild.remove(member);
        }
        this.guildsByTag.remove(this.normalizeTag(guild.getTag()));
        this.save();
    }

    public boolean renameGuild(Guild guild, String newName) {
        if (!this.isValidName(newName)) {
            return false;
        }
        Guild existing = this.getGuildByName(newName);
        if (existing != null && existing != guild) {
            return false;
        }
        guild.setName(newName);
        this.save();
        return true;
    }

    public boolean changeTag(Guild guild, String newTag) {
        if (!this.isValidTag(newTag)) {
            return false;
        }
        String normalized = this.normalizeTag(newTag);
        if (this.guildsByTag.containsKey(normalized)) {
            return false;
        }
        String old = this.normalizeTag(guild.getTag());
        this.guildsByTag.remove(old);
        guild.setTag(newTag.toUpperCase(Locale.ROOT));
        this.guildsByTag.put(normalized, guild);
        for (UUID member : guild.getMembers().keySet()) {
            this.playerGuild.put(member, normalized);
        }
        this.save();
        return true;
    }

    public int getMaxMembers() {
        return Math.max(1, this.plugin.getConfig().getInt("settings.max-members-per-guild", 25));
    }

    public boolean isValidTag(String tag) {
        if (tag == null) {
            return false;
        }
        int min = this.plugin.getConfig().getInt("settings.tag.min-length", 2);
        int max = this.plugin.getConfig().getInt("settings.tag.max-length", 5);
        String pattern = this.plugin.getConfig().getString("settings.tag.pattern", "^[A-Za-z0-9]+$");
        return tag.length() >= min && tag.length() <= max && Pattern.matches(pattern, tag);
    }

    public boolean isValidName(String name) {
        if (name == null) {
            return false;
        }
        int min = this.plugin.getConfig().getInt("settings.name.min-length", 3);
        int max = this.plugin.getConfig().getInt("settings.name.max-length", 24);
        String clean = name.trim();
        return clean.length() >= min && clean.length() <= max;
    }

    public String normalizeTag(String tag) {
        return tag == null ? "" : tag.toUpperCase(Locale.ROOT);
    }

    public String getOfflineName(UUID uuid) {
        Guild guild = this.getGuild(uuid);
        if (guild != null && guild.getStoredName(uuid) != null && !guild.getStoredName(uuid).isBlank()) {
            return guild.getStoredName(uuid);
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer((UUID)uuid);
        return player.getName() == null ? uuid.toString().substring(0, 8) : player.getName();
    }

    public UUID findMember(Guild guild, String name) {
        for (UUID uuid : guild.getMembers().keySet()) {
            if (!this.getOfflineName(uuid).equalsIgnoreCase(name)) continue;
            return uuid;
        }
        Player online = Bukkit.getPlayerExact((String)name);
        if (online != null && guild.isMember(online.getUniqueId())) {
            return online.getUniqueId();
        }
        return null;
    }

    private int countItem(Player player, Material material) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) continue;
            total += item.getAmount();
        }
        return total;
    }

    private void removeItem(Player player, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) continue;
            int take = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - take);
            if ((remaining -= take) <= 0) break;
        }
        player.updateInventory();
    }

    public static enum CreateResult {
        SUCCESS,
        ALREADY_IN_GUILD,
        INVALID_TAG,
        INVALID_NAME,
        DUPLICATE_TAG,
        DUPLICATE_NAME,
        NOT_ENOUGH_COST;

    }
}

