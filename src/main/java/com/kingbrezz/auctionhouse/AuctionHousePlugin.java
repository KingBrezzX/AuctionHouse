package com.kingbrezz.auctionhouse;

import org.bukkit.plugin.java.JavaPlugin;

public final class AuctionHousePlugin extends JavaPlugin {

    private ClaimManager claimManager;
    private AuctionManager auctionManager;
    private EconomyHook economyHook;
    private MessageService messages;
    private AHListener auctionListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");

        messages =
                new MessageService(this);

        claimManager =
                new ClaimManager(this);

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

            getCommand("ah")
                    .setTabCompleter(command);
        }

        auctionManager.startExpiryTask();

        getLogger().info(
                "AuctionHouse enabled."
        );
        getLogger().info(
                "Author: KingBrezz"
        );
        getLogger().info(
                "Target: Paper 26.2 / Java 25"
        );
    }

    @Override
    public void onDisable() {
        if (auctionManager != null) {
            auctionManager.shutdown();
        }

        if (claimManager != null) {
            claimManager.save();
        }
    }

    public void reloadPlugin() {
        reloadConfig();

        if (messages != null) {
            messages.reload();
        }

        if (claimManager != null) {
            claimManager.load();
        }

        if (auctionManager != null) {
            auctionManager.stopExpiryTask();
            auctionManager.load();
        }

        if (economyHook != null) {
            economyHook.reload();
        }

        if (auctionManager != null) {
            auctionManager.startExpiryTask();
        }
    }

    public boolean isAuctionHouseEnabled() {
        return getConfig().getBoolean(
                "settings.enabled",
                true
        );
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public EconomyHook getEconomyHook() {
        return economyHook;
    }

    public MessageService getMessages() {
        return messages;
    }

    public AHListener getAuctionListener() {
        return auctionListener;
    }

    private void saveResourceIfMissing(
            String resource
    ) {
        java.io.File file =
                new java.io.File(
                        getDataFolder(),
                        resource
                );

        if (!file.exists()) {
            saveResource(
                    resource,
                    false
            );
        }
    }
}
