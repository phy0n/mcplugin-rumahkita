package id.rumahkita.games;

import org.bukkit.plugin.java.JavaPlugin;

public class RumahKitaGamesPlugin extends JavaPlugin {
    
    private static RumahKitaGamesPlugin instance;
    private CoinflipManager coinflipManager;
    private RpsManager rpsManager;

    @Override
    public void onEnable() {
        instance = this;
        
        getLogger().info("RumahKitaGames v1.0.0 is enabling...");
        
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        
        coinflipManager = new CoinflipManager(this);
        getServer().getPluginManager().registerEvents(coinflipManager, this);
        CoinflipCommand cfCmd = new CoinflipCommand(this);
        getCommand("coinflip").setExecutor(cfCmd);
        getCommand("coinflip").setTabCompleter(cfCmd);
        
        rpsManager = new RpsManager(this);
        getServer().getPluginManager().registerEvents(rpsManager, this);
        RpsCommand rpsCmd = new RpsCommand(this);
        getCommand("rps").setExecutor(rpsCmd);
        getCommand("rps").setTabCompleter(rpsCmd);
        
        RkgCommand rkgCmd = new RkgCommand(this);
        getCommand("rkg").setExecutor(rkgCmd);
        getCommand("rkg").setTabCompleter(rkgCmd);
        
        getLogger().info("RumahKitaGames successfully hooked to RumahKitaEconomyV2!");
    }

    @Override
    public void onDisable() {
        getLogger().info("RumahKitaGames disabled.");
    }

    public static RumahKitaGamesPlugin getInstance() {
        return instance;
    }
    
    public CoinflipManager getCoinflipManager() {
        return coinflipManager;
    }

    public RpsManager getRpsManager() {
        return rpsManager;
    }
}
