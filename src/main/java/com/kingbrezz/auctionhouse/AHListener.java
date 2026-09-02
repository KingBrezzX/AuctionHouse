package com.kingbrezz.auctionhouse;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryView;

public final class AHListener implements Listener {

    private final AuctionHousePlugin plugin;

    public AHListener(AuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();

        String title = view.getTitle();

        if (title.equals(color("&8Auction House"))
                || title.equals(color("&8My Auctions"))
                || title.equals(color("&8Auction Claims"))
                || title.equals(color("&8Sell Item"))) {

            /*
             * Auction GUI is controlled by AHGui.
             *
             * Inventory interaction is cancelled here first
             * so players cannot take GUI buttons/items directly.
             */
            event.setCancelled(true);

            if (!(event.getWhoClicked()
                    instanceof org.bukkit.entity.Player player)) {
                return;
            }

            if (title.equals(color("&8Auction House"))) {
                new AHGui(plugin).handleBrowseClick(
                        player,
                        event.getRawSlot(),
                        event.isShiftClick(),
                        event.isLeftClick(),
                        event.isRightClick()
                );
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();

        if (title.equals(color("&8Auction House"))
                || title.equals(color("&8My Auctions"))
                || title.equals(color("&8Auction Claims"))
                || title.equals(color("&8Sell Item"))) {

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        /*
         * Reserved for future sell-menu state cleanup.
         */
    }

    private String color(String message) {
        return message.replace("&", "§");
    }
                               }
