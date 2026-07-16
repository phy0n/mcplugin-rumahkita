package id.rumahkita.trade;

import org.bukkit.plugin.java.JavaPlugin;

public class RumahKitaTradePlugin extends JavaPlugin {
    
    private static RumahKitaTradePlugin instance;
    private TradeManager tradeManager;

    @Override
    public void onEnable() {
        instance = this;
        tradeManager = new TradeManager(this);
        
        TradeCommand cmd = new TradeCommand(tradeManager);
        getCommand("trade").setExecutor(cmd);
        getCommand("trade").setTabCompleter(cmd);
        
        getServer().getPluginManager().registerEvents(tradeManager, this);
        
        getLogger().info("RumahKitaTrade v1.0.0 is enabling. Anti-Dupe protections active.");
    }

    @Override
    public void onDisable() {
        if (tradeManager != null) {
            tradeManager.cancelAllActiveTrades();
        }
        getLogger().info("RumahKitaTrade disabled.");
    }

    public static RumahKitaTradePlugin getInstance() {
        return instance;
    }
}
