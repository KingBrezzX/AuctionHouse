package com.kingbrezz.auctionhouse;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AHListener implements Listener {

    public enum InputMode {
        PRICE,
        SEARCH
    }

    private final AuctionHousePlugin plugin;
    private final Map<UUID, InputState> inputStates =
            new ConcurrentHashMap<>();

    public AHListener(AuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(
            InventoryClickEvent event
    ) {
        if (!(event.getWhoClicked()
                instanceof Player player)) {
            return;
        }

        Inventory top =
                event.getView().getTopInventory();

        if (!(top.getHolder()
                instanceof GuiHolder holder)) {
            return;
        }

        event.setCancelled(true);

        int rawSlot =
                event.getRawSlot();

        if (rawSlot < 0
                || rawSlot >= top.getSize()) {
            return;
        }

        AHGui gui =
                new AHGui(plugin);

        switch (holder.getType()) {
            case BROWSE ->
                    gui.handleBrowseClick(
                            player,
                            rawSlot
                    );

            case MY_LISTINGS ->
                    gui.handleMyListingsClick(
                            player,
                            rawSlot
                    );

            case CLAIMS ->
                    gui.handleClaimsClick(
                            player,
                            rawSlot
                    );

            case SELL ->
                    gui.handleSellClick(
                            player,
                            rawSlot
                    );
        }
    }

    @EventHandler
    public void onInventoryClose(
            InventoryCloseEvent event
    ) {
        // GUI actions are fully handled by click events.
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(
            AsyncChatEvent event
    ) {
        Player player =
                event.getPlayer();

        InputState state =
                inputStates.get(
                        player.getUniqueId()
                );

        if (state == null) {
            return;
        }

        event.setCancelled(true);

        String input = PlainTextComponentSerializer.plainText()
                .serialize(event.message())
                .trim();

        inputStates.remove(
                player.getUniqueId()
        );

        player.getScheduler().run(
                plugin,
                task -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        plugin.getMessages().send(
                                player,
                                "sell.cancelled",
                                "&eAction cancelled."
                        );
                        return;
                    }

                    AHGui gui =
                            new AHGui(plugin);

                    if (state.mode() == InputMode.SEARCH) {
                        gui.openBrowse(
                                player,
                                0,
                                state.sortMode(),
                                input
                        );
                        return;
                    }

                    gui.createListingFromInput(
                            player,
                            input
                    );
                },
                null
        );
    }

    public void beginPriceInput(
            Player player
    ) {
        inputStates.put(
                player.getUniqueId(),
                new InputState(
                        InputMode.PRICE,
                        GuiHolder.SortMode.NEWEST
                )
        );

        player.closeInventory();

        plugin.getMessages().send(
                player,
                "sell.prompt",
                "&eEnter the total selling price in chat."
        );

        plugin.getMessages().send(
                player,
                "sell.examples",
                "&7Examples: &f1k / 1m / 1b / 1t"
        );

        plugin.getMessages().send(
                player,
                "sell.cancel",
                "&7Type &ccancel &7to cancel."
        );
    }

    public void beginSearchInput(
            Player player
    ) {
        GuiHolder holder =
                player.getOpenInventory()
                        .getTopInventory()
                        .getHolder()
                        instanceof GuiHolder guiHolder
                        ? guiHolder
                        : null;

        inputStates.put(
                player.getUniqueId(),
                new InputState(
                        InputMode.SEARCH,
                        holder == null
                                ? GuiHolder.SortMode.NEWEST
                                : holder.getSortMode()
                )
        );

        player.closeInventory();

        player.sendMessage(
                "§eEnter an item name or seller name to search."
        );

        player.sendMessage(
                "§7Type §ccancel §7to cancel."
        );

        if (holder != null
                && !holder.getSearch().isBlank()) {
            player.sendMessage(
                    "§7Current search: §f"
                            + holder.getSearch()
            );
        }
    }

    public void clearInput(
            UUID player
    ) {
        inputStates.remove(player);
    }

    @EventHandler
    public void onQuit(
            PlayerQuitEvent event
    ) {
        clearInput(
                event.getPlayer()
                        .getUniqueId()
        );
    }

    private record InputState(
            InputMode mode,
            GuiHolder.SortMode sortMode
    ) {
    }
}
