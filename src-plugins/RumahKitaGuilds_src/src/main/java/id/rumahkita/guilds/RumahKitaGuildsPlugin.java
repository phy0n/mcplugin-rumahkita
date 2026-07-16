/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.GuildChatListener;
import id.rumahkita.guilds.GuildChatManager;
import id.rumahkita.guilds.GuildCommand;
import id.rumahkita.guilds.GuildGui;
import id.rumahkita.guilds.GuildHomeManager;
import id.rumahkita.guilds.GuildManager;
import id.rumahkita.guilds.GuildPlaceholderExpansion;
import id.rumahkita.guilds.GuildWarManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class RumahKitaGuildsPlugin
extends JavaPlugin {
    private GuildManager guildManager;
    private GuildHomeManager homeManager;
    private GuildChatManager chatManager;
    private GuildGui gui;
    private GuildWarManager warManager;
    private GuildPlaceholderExpansion placeholderExpansion;

    public void onEnable() {
        this.saveDefaultConfig();
        this.guildManager = new GuildManager(this);
        this.guildManager.load();
        this.homeManager = new GuildHomeManager(this);
        this.chatManager = new GuildChatManager(this, this.guildManager);
        this.gui = new GuildGui(this, this.guildManager);
        this.warManager = new GuildWarManager(this, this.guildManager);
        GuildCommand guildCommand = new GuildCommand(this, this.guildManager, this.homeManager, this.chatManager, this.gui, this.warManager);
        this.getCommand("guild").setExecutor((CommandExecutor)guildCommand);
        this.getCommand("guild").setTabCompleter((TabCompleter)guildCommand);
        this.getCommand("guildchat").setExecutor((CommandExecutor)guildCommand);
        this.getCommand("guildchat").setTabCompleter((TabCompleter)guildCommand);
        Bukkit.getPluginManager().registerEvents((Listener)this.homeManager, (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)this.gui, (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new GuildChatListener(this, this.guildManager, this.chatManager), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)this.warManager, (Plugin)this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderExpansion = new GuildPlaceholderExpansion(this, this.guildManager);
            this.placeholderExpansion.register();
            this.getLogger().info("PlaceholderAPI hooked. Guild placeholders registered.");
        } else {
            this.getLogger().warning("PlaceholderAPI not found. Guild placeholders will not work until PlaceholderAPI is installed.");
        }
        this.getLogger().info("RumahKitaGuilds v2.3.3 GuildWar enabled.");
    }

    public void onDisable() {
        if (this.placeholderExpansion != null) {
            this.placeholderExpansion.unregister();
        }
        if (this.homeManager != null) {
            this.homeManager.cancelAllTeleports();
        }
        if (this.warManager != null) {
            this.warManager.shutdown();
        }
        if (this.guildManager != null) {
            this.guildManager.save();
        }
    }

    public void reloadAll() {
        this.reloadConfig();
        this.guildManager.load();
        if (this.warManager != null) {
            this.warManager.reload();
        }
    }
}

