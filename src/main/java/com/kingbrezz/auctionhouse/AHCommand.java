package com.kingbrezz.auctionhouse;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AHCommand
        implements CommandExecutor, TabCompleter {

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

            if (!sender.hasPermission(
                    "auctionhouse.admin"
            )) {
                sender.sendMessage(
                        color("&cYou do not have permission.")
                );
                return true;
            }

            plugin.reloadPlugin();

            sender.sendMessage(
                    plugin.getMessages().text(
                            "command.reloaded",
                            "&aAuction House configuration reloaded."
                    )
            );

            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    "Only players can use this command."
            );
            return true;
        }

        if (!plugin.isAuctionHouseEnabled()) {
            plugin.getMessages().send(
                    player,
                    "general.disabled",
                    "&cAuction House is currently disabled."
            );
            return true;
        }

        if (!player.hasPermission(
                "auctionhouse.use"
        )) {
            plugin.getMessages().send(
                    player,
                    "general.no-permission",
                    "&cYou do not have permission."
            );
            return true;
        }

        AHGui gui =
                new AHGui(plugin);

        if (args.length == 0) {
            gui.openBrowse(
                    player,
                    0
            );
            return true;
        }

        switch (
                args[0].toLowerCase(Locale.ROOT)
        ) {
            case "sell" ->
                    gui.openSell(player);

            case "my", "listings" ->
                    gui.openMyListings(
                            player,
                            0
                    );

            case "claims" ->
                    gui.openClaims(
                            player,
                            0
                    );

            case "help" ->
                    sendHelp(player);

            case "search" -> {
                if (args.length < 2) {
                    plugin.getMessages().send(
                            player,
                            "command.search-usage",
                            "&eUsage: /ah search <item|seller>"
                    );
                    return true;
                }

                String search =
                        String.join(
                                " ",
                                java.util.Arrays.copyOfRange(
                                        args,
                                        1,
                                        args.length
                                )
                        );

                gui.openBrowse(
                        player,
                        0,
                        GuiHolder.SortMode.NEWEST,
                        search
                );
            }

            default -> {
                plugin.getMessages().send(
                        player,
                        "command.usage",
                        "&eUsage: /ah [sell|my|claims|search|reload]"
                );
            }
        }

        return true;
    }

    private void sendHelp(
            Player player
    ) {
        player.sendMessage(
                color("&8&m-----------------------------")
        );
        player.sendMessage(
                color("&b&lAuction House")
        );
        player.sendMessage(
                color("&7/ah &f- Browse auctions")
        );
        player.sendMessage(
                color("&7/ah sell &f- Sell an item")
        );
        player.sendMessage(
                color("&7/ah my &f- Manage your auctions")
        );
        player.sendMessage(
                color("&7/ah claims &f- Collect expired items")
        );
        player.sendMessage(
                color("&7/ah search <text> &f- Search auctions")
        );
        player.sendMessage(
                color("&7/ah reload &f- Reload configuration")
        );
        player.sendMessage(
                color("&8&m-----------------------------")
        );
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (args.length != 1) {
            return List.of();
        }

        String input =
                args[0].toLowerCase(Locale.ROOT);

        List<String> values =
                new ArrayList<>();

        addIfMatches(
                values,
                "sell",
                input
        );
        addIfMatches(
                values,
                "my",
                input
        );
        addIfMatches(
                values,
                "claims",
                input
        );
        addIfMatches(
                values,
                "search",
                input
        );
        addIfMatches(
                values,
                "help",
                input
        );

        if (sender.hasPermission(
                "auctionhouse.admin"
        )) {
            addIfMatches(
                    values,
                    "reload",
                    input
            );
        }

        return values;
    }

    private void addIfMatches(
            List<String> values,
            String value,
            String input
    ) {
        if (value.startsWith(input)) {
            values.add(value);
        }
    }

    private String color(
            String text
    ) {
        return text == null
                ? ""
                : text.replace('&', '§');
    }
}
