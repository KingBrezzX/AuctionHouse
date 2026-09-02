package com.kingbrezz.auctionhouse;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AHListener implements Listener {

    private final AuctionHousePlugin plugin;
    private final Map<UUID, String> priceInput = new ConcurrentHashMap<>();

    public AHListener(AuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof GuiHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getRawSlot() < 0
                || event.getRawSlot() >= top.getSize()) {
            return;
        }

        AHGui gui = new AHGui(plugin);

        switch (holder.getType()) {
            case BROWSE -> gui.handleBrowseClick(
                    player,
                    event.getRawSlot(),
                    event.isRightClick(),
                    event.isShiftClick(),
                    event.getClick().isDoubleClick()
            );

            case MY_LISTINGS -> gui.handleMyListingsClick(
                    player,
                    event.getRawSlot()
            );

            case CLAIMS -> gui.handleClaimsClick(
                    player,
                    event.getRawSlot()
            );

            case SELL -> gui.handleSellClick(
                    player,
                    event.getRawSlot()
            );
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder()
                instanceof GuiHolder holder)) {
            return;
        }

        if (holder.getType() == GuiHolder.Type.SELL) {
            // Sell GUI does not automatically remove the player's item.
            // The item remains in the player's inventory unless listed.
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!priceInput.containsKey(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        String input = event.getMessage().trim();

        if (input.equalsIgnoreCase("cancel")) {
            priceInput.remove(player.getUniqueId());

            player.getScheduler().run(
                    plugin,
                    task -> player.sendMessage(color(
                            plugin.getConfig().getString(
                                    "messages.sell.cancel",
                                    "&cSale cancelled."
                            )
                    )),
                    null
            );

            return;
        }

        priceInput.remove(player.getUniqueId());

        player.getScheduler().run(
                plugin,
                task -> {
                    AHGui gui = new AHGui(plugin);
                    gui.createListingFromInput(player, input);
                },
                null
        );
    }

    public void beginPriceInput(Player player) {
        priceInput.put(player.getUniqueId(), "pending");

        player.closeInventory();

        player.sendMessage(color(
                plugin.getConfig().getString(
                        "messages.sell.prompt",
                        "&eEnter the price in chat."
                )
        ));

        player.sendMessage(color(
                plugin.getConfig().getString(
                        "messages.sell.examples",
                        "&7Examples: &f1000, 1k, 1.5m, 2b, 1t"
                )
        ));

        player.sendMessage(color(
                "&7Type &fcancel &7to cancel."
        ));
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                text == null ? "" : text
        );
    }
    }
