package com.kingbrezz.auctionhouse;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyHook {

    private final AuctionHousePlugin plugin;
    private Economy economy;

    public EconomyHook(AuctionHousePlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found. Economy features are unavailable.");
            return;
        }

        RegisteredServiceProvider<Economy> provider =
                plugin.getServer()
                        .getServicesManager()
                        .getRegistration(Economy.class);

        if (provider != null) {
            economy = provider.getProvider();
            plugin.getLogger().info("Economy hooked: " + economy.getName());
        } else {
            plugin.getLogger().warning("No Vault economy provider found.");
        }
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public double balance(OfflinePlayer player) {
        if (!isAvailable()) {
            return 0D;
        }

        return economy.getBalance(player);
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (!isAvailable()) {
            return false;
        }

        return economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isAvailable() || amount < 0D) {
            return false;
        }

        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!isAvailable() || amount < 0D) {
            return false;
        }

        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        if (!isAvailable()) {
            return PriceFormatter.format(amount);
        }

        return economy.format(amount);
    }
}
