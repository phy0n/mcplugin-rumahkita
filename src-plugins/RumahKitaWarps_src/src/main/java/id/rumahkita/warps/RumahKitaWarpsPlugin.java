package id.rumahkita.warps;

import org.bukkit.plugin.java.JavaPlugin;

public class RumahKitaWarpsPlugin extends JavaPlugin {
    
    private static RumahKitaWarpsPlugin instance;
    private WarpManager warpManager;
    private RtpManager rtpManager;
    private ServerWarpManager serverWarpManager;

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
        
        serverWarpManager = new ServerWarpManager(this);
        getServer().getPluginManager().registerEvents(serverWarpManager, this);
        ServerWarpCommand swCmd = new ServerWarpCommand(serverWarpManager);
        getCommand("warp").setExecutor(swCmd);
        getCommand("warp").setTabCompleter(swCmd);
        getCommand("setwarp").setExecutor(swCmd);
        getCommand("delwarp").setExecutor(swCmd);
        getCommand("delwarp").setTabCompleter(swCmd);
        getCommand("editwarp").setExecutor(swCmd);
        getCommand("editwarp").setTabCompleter(swCmd);
        
        RkwCommand rkwCmd = new RkwCommand(this);
        getCommand("rkw").setExecutor(rkwCmd);
        getCommand("rkw").setTabCompleter(rkwCmd);
        
        BackManager backManager = new BackManager(this);
        getServer().getPluginManager().registerEvents(backManager, this);
        if (getCommand("back") != null) {
            getCommand("back").setExecutor(new BackCommand(backManager));
        }
        
        getLogger().info("RumahKitaWarps successfully enabled!");
    }

    @Override
    public void onDisable() {
        if (warpManager != null) {
            warpManager.saveWarps();
        }
        if (serverWarpManager != null) {
            serverWarpManager.saveWarps();
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
