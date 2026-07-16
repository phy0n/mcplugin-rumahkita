/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.Guild;
import id.rumahkita.guilds.GuildManager;
import id.rumahkita.guilds.GuildRole;
import id.rumahkita.guilds.RumahKitaGuildsPlugin;
import id.rumahkita.guilds.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GuildGui
implements Listener {
    private final RumahKitaGuildsPlugin plugin;
    private final GuildManager guildManager;

    public GuildGui(RumahKitaGuildsPlugin plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory((InventoryHolder)new Holder("main", ""), (int)this.plugin.getConfig().getInt("gui.size", 27), (String)Text.color(this.plugin.getConfig().getString("gui.title", "&8RumahKita Guild")));
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            inv.setItem(11, this.item(Material.DIAMOND, "&aBuat Guild", List.of("&7Gunakan:", "&e/guild create <TAG> <Nama>")));
            inv.setItem(13, this.item(Material.BOOK, "&bGuild List", List.of("&7Klik untuk melihat semua guild.")));
            inv.setItem(15, this.item(Material.PAPER, "&eInfo", List.of("&7Kamu belum punya guild.", "&7Join guild teman atau buat sendiri.")));
        } else {
            GuildRole role = guild.getRole(player.getUniqueId());
            inv.setItem(10, this.item(Material.PAPER, "&bInfo Guild", List.of("&7Nama: &f" + guild.getName(), "&7Tag: &b" + guild.getTag(), "&7Role: &e" + role.displayName(this.plugin), "&7Member: &f" + guild.size() + "/" + this.guildManager.getMaxMembers())));
            inv.setItem(11, this.item(Material.COMPASS, "&aGuild Home", List.of("&7Klik untuk teleport.", "&e/guild home")));
            inv.setItem(12, this.item(Material.PLAYER_HEAD, "&eMembers", List.of("&7Klik untuk lihat anggota guild.")));
            inv.setItem(13, this.item(Material.BOOK, "&bGuild List", List.of("&7Klik untuk melihat semua guild.")));
            inv.setItem(14, this.item(Material.WRITABLE_BOOK, "&dGuild Chat", List.of("&7Klik untuk toggle guild chat.", "&e/guild chat <pesan>", "&e/guildchat")));
            if (role.atLeast(GuildRole.ADMIN)) {
                inv.setItem(15, this.item(Material.EMERALD, "&6Manage Guild", List.of("&e/guild invite <player>", "&e/guild sethome", "&e/guild kick <player>")));
            }
            if (role == GuildRole.LEADER) {
                inv.setItem(16, this.item(Material.NAME_TAG, "&dRename / Tag", List.of("&e/guild rename <nama>", "&e/guild settag <tag>", "&e/guild transfer <player>")));
            }
            inv.setItem(22, this.item(Material.OAK_DOOR, "&cLeave Guild", List.of("&e/guild leave")));
        }
        player.openInventory(inv);
    }

    public void openGuildList(Player player) {
        Inventory inv = Bukkit.createInventory((InventoryHolder)new Holder("list", ""), (int)54, (String)Text.color(this.plugin.getConfig().getString("gui.list-title", "&8Guild List")));
        int slot = 0;
        for (Guild guild : this.guildManager.getGuilds()) {
            if (slot >= 45) break;
            inv.setItem(slot++, this.item(Material.WHITE_BANNER, "&b" + guild.getName() + " &8[&f" + guild.getTag() + "&8]", List.of("&7Leader: &f" + this.guildManager.getOfflineName(guild.getLeader()), "&7Members: &f" + guild.size() + "/" + this.guildManager.getMaxMembers(), "", "&eKlik untuk lihat member")));
        }
        inv.setItem(49, this.item(Material.ARROW, "&cKembali", List.of("&7Kembali ke menu guild.")));
        player.openInventory(inv);
    }

    public void openMembers(Player player, Guild guild) {
        Inventory inv = Bukkit.createInventory((InventoryHolder)new Holder("members", guild.getTag()), (int)54, (String)Text.color(this.plugin.getConfig().getString("gui.members-title", "&8Guild Members")));
        int slot = 0;
        for (Map.Entry<UUID, GuildRole> entry : guild.getMembers().entrySet()) {
            if (slot >= 45) break;
            String name = this.guildManager.getOfflineName(entry.getKey());
            inv.setItem(slot++, this.item(Material.PLAYER_HEAD, "&f" + name, List.of("&7Role: &e" + entry.getValue().displayName(this.plugin), "&7Guild: &b" + guild.getTag())));
        }
        inv.setItem(49, this.item(Material.ARROW, "&cKembali", List.of("&7Kembali ke menu guild.")));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder inventoryHolder = event.getInventory().getHolder();
        if (!(inventoryHolder instanceof Holder)) {
            return;
        }
        Holder holder = (Holder)inventoryHolder;
        event.setCancelled(true);
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }
        int slot = event.getRawSlot();
        if (holder.type.equals("main")) {
            player.closeInventory();
            if (slot == 11 && this.guildManager.getGuild(player) != null) {
                Bukkit.dispatchCommand((CommandSender)player, (String)"guild home");
            } else if (slot == 12 && this.guildManager.getGuild(player) != null) {
                this.openMembers(player, this.guildManager.getGuild(player));
            } else if (slot == 13) {
                this.openGuildList(player);
            } else if (slot == 14 && this.guildManager.getGuild(player) != null) {
                Bukkit.dispatchCommand((CommandSender)player, (String)"guild chat");
            }
        } else if (holder.type.equals("list")) {
            if (slot == 49) {
                this.open(player);
                return;
            }
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) {
                return;
            }
            String display = clicked.getItemMeta().getDisplayName();
            for (Guild guild : this.guildManager.getGuilds()) {
                if (!Text.color("&b" + guild.getName() + " &8[&f" + guild.getTag() + "&8]").equals(display)) continue;
                this.openMembers(player, guild);
                return;
            }
        } else if (holder.type.equals("members") && slot == 49) {
            this.open(player);
        }
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            ArrayList<String> out = new ArrayList<String>();
            for (String line : lore) {
                out.add(Text.color(line));
            }
            meta.setLore(out);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static final class Holder
    implements InventoryHolder {
        private final String type;
        private final String tag;

        private Holder(String type, String tag) {
            this.type = type;
            this.tag = tag;
        }

        public Inventory getInventory() {
            return null;
        }
    }
}

