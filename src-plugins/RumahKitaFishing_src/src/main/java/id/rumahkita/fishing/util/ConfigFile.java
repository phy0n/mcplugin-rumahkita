/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.plugin.java.JavaPlugin
 */
package id.rumahkita.fishing.util;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigFile {
    private final JavaPlugin plugin;
    private final String fileName;
    private File file;
    private FileConfiguration configuration;

    public ConfigFile(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
    }

    public void setup() {
        this.file = new File(this.plugin.getDataFolder(), this.fileName);
        if (!this.file.exists()) {
            this.plugin.saveResource(this.fileName, false);
        }
        this.configuration = YamlConfiguration.loadConfiguration((File)this.file);
    }

    public void reload() {
        if (this.file == null) {
            this.setup();
            return;
        }
        this.configuration = YamlConfiguration.loadConfiguration((File)this.file);
    }

    public void save() {
        if (this.configuration == null || this.file == null) {
            return;
        }
        try {
            this.configuration.save(this.file);
        }
        catch (IOException exception) {
            this.plugin.getLogger().warning("Failed to save " + this.fileName + ": " + exception.getMessage());
        }
    }

    public FileConfiguration get() {
        return this.configuration;
    }
}

