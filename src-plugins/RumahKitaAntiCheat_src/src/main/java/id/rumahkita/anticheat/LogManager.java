/*
 * Decompiled with CFR 0.152.
 */
package id.rumahkita.anticheat;

import id.rumahkita.anticheat.RumahKitaAntiCheatPlugin;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class LogManager {
    private final RumahKitaAntiCheatPlugin plugin;
    private final File logFile;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LogManager(RumahKitaAntiCheatPlugin plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "logs");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.logFile = new File(dir, plugin.getConfig().getString("logs.file-name", "violations.log"));
    }

    public void log(String message) {
        String line = "[" + this.formatter.format(LocalDateTime.now()) + "] " + message;
        if (this.plugin.getConfig().getBoolean("actions.log-to-console", true)) {
            this.plugin.getLogger().warning(message);
        }
        if (this.plugin.getConfig().getBoolean("actions.log-to-file", true)) {
            try (FileWriter writer = new FileWriter(this.logFile, true);){
                writer.write(line + System.lineSeparator());
            }
            catch (IOException ex) {
                this.plugin.getLogger().warning("Could not write log: " + ex.getMessage());
            }
        }
    }
}

