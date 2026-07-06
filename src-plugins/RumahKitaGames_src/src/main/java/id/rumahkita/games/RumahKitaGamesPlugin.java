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
        
        coinflipManager = new CoinflipManager(this);
        getServer().getPluginManager().registerEvents(coinflipManager, this);
        getCommand("coinflip").setExecutor(new CoinflipCommand(this));
        
        rpsManager = new RpsManager(this);
        getServer().getPluginManager().registerEvents(rpsManager, this);
        getCommand("rps").setExecutor(new RpsCommand(this));
        
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
