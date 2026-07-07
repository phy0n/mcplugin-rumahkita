package id.rumahkita.warps;

import org.bukkit.plugin.java.JavaPlugin;

public class RumahKitaWarpsPlugin extends JavaPlugin {
    
    private static RumahKitaWarpsPlugin instance;
    private WarpManager warpManager;
    private RtpManager rtpManager;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        getLogger().info("RumahKitaWarps v1.0.0 is enabling...");
        
        warpManager = new WarpManager(this);
        warpManager.loadWarps();
        
        getServer().getPluginManager().registerEvents(warpManager, this);
        WarpCommand warpCmd = new WarpCommand(this);
        getCommand("pwarp").setExecutor(warpCmd);
        getCommand("pwarp").setTabCompleter(warpCmd);
        
        rtpManager = new RtpManager(this);
        RtpCommand rtpCmd = new RtpCommand(rtpManager);
        getCommand("rtp").setExecutor(rtpCmd);
        
        RkwCommand rkwCmd = new RkwCommand(this);
        getCommand("rkw").setExecutor(rkwCmd);
        getCommand("rkw").setTabCompleter(rkwCmd);
        
        getLogger().info("RumahKitaWarps successfully enabled!");
    }

    @Override
    public void onDisable() {
        if (warpManager != null) {
            warpManager.saveWarps();
        }
        getLogger().info("RumahKitaWarps disabled.");
    }

    public static RumahKitaWarpsPlugin getInstance() {
        return instance;
    }
    
    public WarpManager getWarpManager() {
        return warpManager;
    }
}
