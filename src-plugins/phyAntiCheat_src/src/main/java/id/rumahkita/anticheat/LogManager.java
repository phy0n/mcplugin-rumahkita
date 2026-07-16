package id.rumahkita.anticheat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager {

    private final PhyAntiCheat plugin;
    private File logFile;

    public LogManager(PhyAntiCheat plugin) {
        this.plugin = plugin;
        initLogFile();
    }

    private void initLogFile() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File logsDir = new File(dataFolder, "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }

        logFile = new File(logsDir, "violations.log");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create violations.log!");
            }
        }
    }

    public void logViolation(String playerName, String module, String details) {
        if (!plugin.getConfig().getBoolean("logging.file-logging", true)) return;

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logEntry = String.format("[%s] [%s] Player: %s | Details: %s", timestamp, module, playerName, details);

        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(logEntry);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write to violations.log: " + e.getMessage());
        }
    }
}
