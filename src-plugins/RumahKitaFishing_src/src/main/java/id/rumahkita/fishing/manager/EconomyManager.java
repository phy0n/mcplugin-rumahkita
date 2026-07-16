/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.RegisteredServiceProvider
 */
package id.rumahkita.fishing.manager;

import id.rumahkita.fishing.RumahKitaFishingPlugin;
import java.lang.reflect.Method;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyManager {
    private final RumahKitaFishingPlugin plugin;
    private Object vaultEconomy;
    private Class<?> vaultEconomyClass;
    private Method vaultDepositPlayer;

    public EconomyManager(RumahKitaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        this.vaultEconomy = null;
        this.vaultEconomyClass = null;
        this.vaultDepositPlayer = null;
        String mode = this.mode();
        if ("VAULT".equals(mode)) {
            this.setupVault();
        }
    }

    public String mode() {
        return this.plugin.getConfig().getString("economy.mode", "COMMAND").trim().toUpperCase(Locale.ROOT);
    }

    public boolean deposit(Player player, int amount) {
        if (amount <= 0) {
            return false;
        }
        String mode = this.mode();
        if ("NONE".equals(mode)) {
            return true;
        }
        if ("VAULT".equals(mode)) {
            return this.depositVault(player, amount);
        }
        return this.depositCommand(player, amount);
    }

    private void setupVault() {
        try {
            this.vaultEconomyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider registration = Bukkit.getServicesManager().getRegistration(this.vaultEconomyClass);
            if (registration == null) {
                this.plugin.getLogger().warning("Vault economy mode is enabled but no Vault economy provider found.");
                return;
            }
            this.vaultEconomy = registration.getProvider();
            this.vaultDepositPlayer = this.vaultEconomyClass.getMethod("depositPlayer", OfflinePlayer.class, Double.TYPE);
            this.plugin.getLogger().info("Hooked into Vault economy provider.");
        }
        catch (ReflectiveOperationException exception) {
            this.plugin.getLogger().warning("Failed to hook Vault economy: " + exception.getMessage());
        }
    }

    private boolean depositVault(Player player, int amount) {
        if (this.vaultEconomy == null || this.vaultDepositPlayer == null) {
            return false;
        }
        try {
            Object response = this.vaultDepositPlayer.invoke(this.vaultEconomy, player, amount);
            Method success = response.getClass().getMethod("transactionSuccess", new Class[0]);
            Object result = success.invoke(response, new Object[0]);
            return result instanceof Boolean && (Boolean)result != false;
        }
        catch (ReflectiveOperationException exception) {
            this.plugin.getLogger().warning("Vault deposit failed for " + player.getName() + ": " + exception.getMessage());
            return false;
        }
    }

    private boolean depositCommand(Player player, int amount) {
        String command = this.plugin.getConfig().getString("economy.deposit-command", "eco give {player} {amount}");
        command = command.replace("{player}", player.getName()).replace("{uuid}", player.getUniqueId().toString()).replace("{amount}", String.valueOf(amount));
        return Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)command);
    }
}

