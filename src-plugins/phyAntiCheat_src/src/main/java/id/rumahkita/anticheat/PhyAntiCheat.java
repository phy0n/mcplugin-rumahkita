package id.rumahkita.anticheat;

import org.bukkit.plugin.java.JavaPlugin;

public class PhyAntiCheat extends JavaPlugin {

    private LogManager logManager;

    @Override
    public void onEnable() {
        // Load Configuration
        saveDefaultConfig();
        
        // Initialize LogManager
        this.logManager = new LogManager(this);

        // Register Client Detector (Plugin Messaging)
        if (getConfig().getBoolean("modules.client-detector.enabled", true)) {
            new ClientDetector(this);
        }
        
        // Register Commands
        if (getCommand("pac") != null) {
            getCommand("pac").setExecutor(new PACCommand(this));
        }
        
        // Register Listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new MovementListener(this), this);
        getServer().getPluginManager().registerEvents(new FreezeListener(), this);
        getServer().getPluginManager().registerEvents(new IllegalItemListener(this), this);

        getLogger().info("phyAntiCheat successfully enabled! Full suite active.");
    }

    @Override
    public void onDisable() {
        getLogger().info("phyAntiCheat disabled.");
    }

    public LogManager getLogManager() {
        return logManager;
    }
}
