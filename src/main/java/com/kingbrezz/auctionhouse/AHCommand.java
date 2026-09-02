package com.kingbrezz.auctionhouse;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public final class AHCommand implements CommandExecutor, TabCompleter {

    private final AuctionHousePlugin plugin;

    public AHCommand(AuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(
                        color("&cOnly players can open the Auction House.")
                );
                return true;
            }

            if (!player.hasPermission("auctionhouse.use")) {
                player.sendMessage(
                        message("no-permission")
                );
                return true;
            }

            new AHGui(plugin).openBrowse(player, 0);
            return true;
        }

        if (args.length == 1 &&
                args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("auctionhouse.admin")) {
                sender.sendMessage(
                        message("no-permission")
                );
                return true;
            }

            try {
                plugin.reloadPlugin();

                sender.sendMessage(
                        message("reloaded")
                );
            } catch (Exception exception) {
                sender.sendMessage(
                        message("reload-error")
                );

                plugin.getLogger().warning(
                        "Reload failed: "
                                + exception.getMessage()
                );
            }

            return true;
        }

        sender.sendMessage(
                color("&eUsage: &f/ah &7or &f/ah reload")
        );

        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (args.length == 1 &&
                sender.hasPermission("auctionhouse.admin")) {

            return List.of("reload");
        }

        return List.of();
    }

    private String message(String path) {
        String value = plugin.getConfig().getString(
                "messages." + path,
                "&cMessage not configured."
        );

        return color(value);
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                message
        );
    }
  }
