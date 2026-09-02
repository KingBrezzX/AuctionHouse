package com.kingbrezz.auctionhouse;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class AuctionHousePlugin extends JavaPlugin {

    private AuctionManager auctionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        auctionManager = new AuctionManager(this);
        auctionManager.load();

        AHCommand command = new AHCommand(this);

        PluginCommand ah = Objects.requireNonNull(
                getCommand("ah"),
                "Command 'ah' is missing from plugin.yml"
        );

        ah.setExecutor(command);
        ah.setTabCompleter(command);

        getServer().getPluginManager().registerEvents(
                new AHListener(this),
                this
        );

        auctionManager.startExpiryTask();

        getLogger().info("========================================");
        getLogger().info("AuctionHouse enabled");
        getLogger().info("Author: KingBrezz");
        getLogger().info("Paper: 26.2");
        getLogger().info("Java: 25");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (auctionManager != null) {
            auctionManager.shutdown();
        }
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public void reloadPlugin() {
        reloadConfig();

        if (auctionManager != null) {
            auctionManager.reload();
        }
    }
                         }
