package com.kingbrezz.auctionhouse;

import org.bukkit.plugin.java.JavaPlugin;

public final class AuctionHousePlugin extends JavaPlugin {

    private AuctionManager auctionManager;
    private EconomyHook economyHook;
    private AHListener auctionListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        saveResourceIfMissing(
                "messages.yml"
        );

        auctionManager =
                new AuctionManager(this);

        economyHook =
                new EconomyHook(this);

        auctionListener =
                new AHListener(this);

        getServer()
                .getPluginManager()
                .registerEvents(
                        auctionListener,
                        this
                );

        AHCommand command =
                new AHCommand(this);

        if (getCommand("ah") != null) {
            getCommand("ah")
                    .setExecutor(command);
        }

        auctionManager.startExpiryTask();

        getLogger().info(
                "AuctionHouse enabled."
        );
        getLogger().info(
                "Author: KingBrezz"
        );
    }

    @Override
    public void onDisable() {
        if (auctionManager != null) {
            auctionManager.shutdown();
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();

        if (auctionManager != null) {
            auctionManager.load();
            auctionManager.startExpiryTask();
        }
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public EconomyHook getEconomyHook() {
        return economyHook;
    }

    public AHListener getAuctionListener() {
        return auctionListener;
    }

    private void saveResourceIfMissing(
            String resource
    ) {
        if (!new java.io.File(
                getDataFolder(),
                resource
        ).exists()) {
            saveResource(resource, false);
        }
    }
}
