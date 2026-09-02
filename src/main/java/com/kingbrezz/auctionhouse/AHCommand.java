package com.kingbrezz.auctionhouse;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AHCommand implements CommandExecutor {

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
        if (args.length > 0
                && args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("auctionhouse.admin")) {
                sender.sendMessage(color("&cYou do not have permission."));
                return true;
            }

            plugin.reloadPluginConfig();

            sender.sendMessage(color(
                    "&aAuctionHouse configuration reloaded."
            ));

            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("auctionhouse.use")) {
            player.sendMessage(color(
                    "&cYou do not have permission to use Auction House."
            ));
            return true;
        }

        new AHGui(plugin).openBrowse(player, 0);
        return true;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
