package com.kingbrezz.auctionhouse;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyHook {

    private final AuctionHousePlugin plugin;
    private Economy economy;

    public EconomyHook(AuctionHousePlugin plugin) {
        this.plugin = plugin;
        hook();
    }

    public void reload() {
        economy = null;
        hook();
    }

    private void hook() {
        if (plugin.getServer()
                .getPluginManager()
                .getPlugin("Vault") == null) {
            plugin.getLogger().warning(
                    "Vault not found. Economy features are unavailable."
            );
            return;
        }

        RegisteredServiceProvider<Economy> provider =
                plugin.getServer()
                        .getServicesManager()
                        .getRegistration(
                                Economy.class
                        );

        if (provider == null
                || provider.getProvider() == null) {
            plugin.getLogger().warning(
                    "No Vault economy provider found."
            );
            return;
        }

        economy = provider.getProvider();

        plugin.getLogger().info(
                "Economy hooked: "
                        + economy.getName()
        );
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public boolean has(
            OfflinePlayer player,
            double amount
    ) {
        return isAvailable()
                && amount >= 0D
                && economy.has(player, amount);
    }

    public boolean withdraw(
            OfflinePlayer player,
            double amount
    ) {
        return isAvailable()
                && amount >= 0D
                && economy.withdrawPlayer(
                        player,
                        amount
                ).transactionSuccess();
    }

    public boolean deposit(
            OfflinePlayer player,
            double amount
    ) {
        return isAvailable()
                && amount >= 0D
                && economy.depositPlayer(
                        player,
                        amount
                ).transactionSuccess();
    }
}
