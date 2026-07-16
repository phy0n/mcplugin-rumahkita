/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.configuration.ConfigurationSection
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.GuildRole;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public final class Guild {
    private String tag;
    private String name;
    private UUID leader;
    private final Map<UUID, GuildRole> members = new LinkedHashMap<UUID, GuildRole>();
    private final Map<UUID, String> memberNames = new LinkedHashMap<UUID, String>();
    private final Map<UUID, Long> joinedAt = new LinkedHashMap<UUID, Long>();
    private long createdAt;
    private Location home;
    private int emeraldWallet;

    public Guild(String tag, String name, UUID leader, String leaderName, long createdAt) {
        this.tag = tag;
        this.name = name;
        this.leader = leader;
        this.createdAt = createdAt <= 0L ? System.currentTimeMillis() : createdAt;
        this.addMember(leader, leaderName, GuildRole.LEADER, this.createdAt);
    }

    public String getTag() {
        return this.tag;
    }

    public String getName() {
        return this.name;
    }

    public UUID getLeader() {
        return this.leader;
    }

    public Map<UUID, GuildRole> getMembers() {
        return this.members;
    }

    public Location getHome() {
        return this.home == null ? null : this.home.clone();
    }

    public int size() {
        return this.members.size();
    }

    public int getEmeraldWallet() {
        return this.emeraldWallet;
    }

    public void addEmeraldWallet(int amount) {
        this.emeraldWallet = Math.max(0, this.emeraldWallet + amount);
    }

    public boolean withdrawEmeraldWallet(int amount) {
        if (amount <= 0 || this.emeraldWallet < amount) {
            return false;
        }
        this.emeraldWallet -= amount;
        return true;
    }

    public void setHome(Location home) {
        this.home = home == null ? null : home.clone();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean isMember(UUID uuid) {
        return this.members.containsKey(uuid);
    }

    public GuildRole getRole(UUID uuid) {
        return this.members.getOrDefault(uuid, GuildRole.MEMBER);
    }

    public String getStoredName(UUID uuid) {
        return this.memberNames.get(uuid);
    }

    public void addMember(UUID uuid, String name, GuildRole role, long joinTime) {
        this.members.put(uuid, role == null ? GuildRole.MEMBER : role);
        if (name != null && !name.isBlank()) {
            this.memberNames.put(uuid, name);
        }
        this.joinedAt.put(uuid, joinTime <= 0L ? System.currentTimeMillis() : joinTime);
        if (role == GuildRole.LEADER) {
            this.leader = uuid;
        }
    }

    public void removeMember(UUID uuid) {
        this.members.remove(uuid);
        this.memberNames.remove(uuid);
        this.joinedAt.remove(uuid);
    }

    public void setRole(UUID uuid, GuildRole role) {
        if (!this.members.containsKey(uuid)) {
            return;
        }
        if (role == GuildRole.LEADER) {
            for (UUID member : this.members.keySet()) {
                if (this.members.get(member) != GuildRole.LEADER) continue;
                this.members.put(member, GuildRole.ADMIN);
            }
            this.leader = uuid;
        }
        this.members.put(uuid, role);
    }

    public void setStoredName(UUID uuid, String name) {
        if (name != null && !name.isBlank()) {
            this.memberNames.put(uuid, name);
        }
    }

    public void saveTo(ConfigurationSection section) {
        section.set("tag", (Object)this.tag);
        section.set("name", (Object)this.name);
        section.set("owner", (Object)this.leader.toString());
        section.set("leader", (Object)this.leader.toString());
        section.set("owner-name", (Object)this.memberNames.getOrDefault(this.leader, ""));
        section.set("created-at", (Object)this.createdAt);
        section.set("wallet.emerald", (Object)this.emeraldWallet);
        section.set("members", null);
        ConfigurationSection memberSection = section.createSection("members");
        for (Map.Entry<UUID, GuildRole> entry : this.members.entrySet()) {
            UUID uuid = entry.getKey();
            ConfigurationSection m = memberSection.createSection(uuid.toString());
            m.set("name", (Object)this.memberNames.getOrDefault(uuid, ""));
            m.set("role", (Object)entry.getValue().name());
            m.set("joined-at", (Object)this.joinedAt.getOrDefault(uuid, this.createdAt));
        }
        if (this.home != null && this.home.getWorld() != null) {
            section.set("home.world", (Object)this.home.getWorld().getName());
            section.set("home.x", (Object)this.home.getX());
            section.set("home.y", (Object)this.home.getY());
            section.set("home.z", (Object)this.home.getZ());
            section.set("home.yaw", (Object)Float.valueOf(this.home.getYaw()));
            section.set("home.pitch", (Object)Float.valueOf(this.home.getPitch()));
        } else {
            section.set("home", null);
        }
    }

    public static Guild loadFrom(ConfigurationSection section) {
        String worldName;
        ConfigurationSection homeSection;
        UUID leader;
        if (section == null) {
            return null;
        }
        String tag = section.getString("tag");
        String name = section.getString("name");
        String leaderString = section.getString("leader", section.getString("owner"));
        String leaderName = section.getString("owner-name", "");
        long createdAt = section.getLong("created-at", System.currentTimeMillis());
        if (tag == null || name == null || leaderString == null) {
            return null;
        }
        try {
            leader = UUID.fromString(leaderString);
        }
        catch (IllegalArgumentException ex) {
            return null;
        }
        Guild guild = new Guild(tag, name, leader, leaderName, createdAt);
        guild.members.clear();
        guild.memberNames.clear();
        guild.joinedAt.clear();
        guild.emeraldWallet = Math.max(0, section.getInt("wallet.emerald", section.getInt("wallet-emerald", 0)));
        ConfigurationSection membersSection = section.getConfigurationSection("members");
        if (membersSection != null) {
            for (String key : membersSection.getKeys(false)) {
                try {
                    GuildRole role2;
                    UUID uuid = UUID.fromString(key);
                    Object raw = membersSection.get(key);
                    String storedName = "";
                    long joined = createdAt;
                    if (raw instanceof String) {
                        role2 = GuildRole.fromString((String)raw);
                    } else {
                        ConfigurationSection m = membersSection.getConfigurationSection(key);
                        role2 = GuildRole.fromString(m == null ? null : m.getString("role"));
                        storedName = m == null ? "" : m.getString("name", "");
                        long l = joined = m == null ? createdAt : m.getLong("joined-at", createdAt);
                        if (m == null || !m.contains("role")) {
                            role2 = uuid.equals(leader) ? GuildRole.LEADER : GuildRole.MEMBER;
                        }
                    }
                    guild.addMember(uuid, storedName, role2, joined);
                }
                catch (IllegalArgumentException illegalArgumentException) {}
            }
        }
        if (!guild.members.containsKey(leader)) {
            guild.addMember(leader, leaderName, GuildRole.LEADER, createdAt);
        }
        if (guild.members.values().stream().noneMatch(role -> role == GuildRole.LEADER)) {
            guild.setRole(leader, GuildRole.LEADER);
        }
        if ((homeSection = section.getConfigurationSection("home")) != null && (worldName = homeSection.getString("world")) != null && Bukkit.getWorld((String)worldName) != null) {
            guild.home = new Location(Bukkit.getWorld((String)worldName), homeSection.getDouble("x"), homeSection.getDouble("y"), homeSection.getDouble("z"), (float)homeSection.getDouble("yaw"), (float)homeSection.getDouble("pitch"));
        }
        return guild;
    }
}

