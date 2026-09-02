package com.kingbrezz.auctionhouse;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class AHGui {

    private final AuctionHousePlugin plugin;
    private final AuctionManager manager;

    public AHGui(AuctionHousePlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getAuctionManager();
    }

    public void openBrowse(
            Player player,
            int page
    ) {
        openBrowse(
                player,
                page,
                GuiHolder.SortMode.NEWEST,
                ""
        );
    }

    public void openBrowse(
            Player player,
            int page,
            GuiHolder.SortMode sortMode,
            String search
    ) {
        if (!player.hasPermission(
                "auctionhouse.use"
        )) {
            plugin.getMessages().send(
                    player,
                    "general.no-permission",
                    "&cYou do not have permission."
            );
            return;
        }

        List<AuctionListing> listings =
                manager.getSortedListings(
                        sortMode,
                        search
                );

        List<Integer> itemSlots =
                getItemSlots();

        int perPage =
                itemSlots.size();

        if (perPage <= 0) {
            perPage = 29;
        }

        int pages =
                Math.max(
                        1,
                        (int) Math.ceil(
                                listings.size()
                                        / (double) perPage
                        )
                );

        page =
                clampPage(
                        page,
                        pages
                );

        String title =
                plugin.getConfig().getString(
                        "gui.browse.title",
                        "&8Auction House"
                );

        Inventory inventory =
                Bukkit.createInventory(
                        new GuiHolder(
                                GuiHolder.Type.BROWSE,
                                page,
                                sortMode,
                                search
                        ),
                        54,
                        componentTitle(title)
                );

        fillBottom(inventory);

        int start =
                page * perPage;

        int end =
                Math.min(
                        start + perPage,
                        listings.size()
                );

        int slotIndex = 0;

        for (int index = start;
             index < end;
             index++) {

            inventory.setItem(
                    itemSlots.get(slotIndex++),
                    createListingItem(
                            listings.get(index)
                    )
            );
        }

        inventory.setItem(
                45,
                button(
                        Material.ARROW,
                        "&ePrevious Page"
                )
        );

        inventory.setItem(
                46,
                button(
                        Material.COMPARATOR,
                        "&bSort",
                        "&7Current: &f"
                                + sortMode.displayName(),
                        "&7Click to change."
                )
        );

        inventory.setItem(
                47,
                button(
                        Material.OAK_SIGN,
                        "&eSearch",
                        search == null || search.isBlank()
                                ? "&7Search: &fAll"
                                : "&7Search: &f" + search,
                        "",
                        "&eClick to search."
                )
        );

        inventory.setItem(
                48,
                button(
                        Material.CHEST,
                        "&eMy Auctions"
                )
        );

        inventory.setItem(
                49,
                button(
                        Material.BUNDLE,
                        "&6Claims",
                        "&7Items waiting: &f"
                                + plugin.getClaimManager()
                                .count(
                                        player.getUniqueId()
                                )
                )
        );

        inventory.setItem(
                50,
                button(
                        Material.EMERALD,
                        "&aSell Item"
                )
        );

        inventory.setItem(
                51,
                button(
                        Material.NETHER_STAR,
                        "&bRefresh",
                        "&7Reload current auction data."
                )
        );

        inventory.setItem(
                52,
                button(
                        Material.BARRIER,
                        "&cClose"
                )
        );

        inventory.setItem(
                53,
                button(
                        Material.ARROW,
                        "&eNext Page"
                )
        );

        if (listings.isEmpty()) {
            inventory.setItem(
                    22,
                    button(
                            Material.BARRIER,
                            "&cNo Auctions",
                            search == null || search.isBlank()
                                    ? "&7There are no active auctions."
                                    : "&7No auction matched:",
                            search == null || search.isBlank()
                                    ? "&7"
                                    : "&f" + search
                    )
            );
        }

        player.openInventory(inventory);
    }

    public void openMyListings(
            Player player,
            int page
    ) {
        List<AuctionListing> listings =
                manager.getActiveListings()
                        .stream()
                        .filter(
                                listing ->
                                        listing.getSeller()
                                                .equals(
                                                        player.getUniqueId()
                                                )
                        )
                        .sorted(
                                java.util.Comparator
                                        .comparingLong(
                                                AuctionListing::getCreatedAt
                                        )
                                        .reversed()
                        )
                        .toList();

        List<Integer> itemSlots =
                getItemSlots();

        int perPage =
                itemSlots.size();

        int pages =
                Math.max(
                        1,
                        (int) Math.ceil(
                                listings.size()
                                        / (double) perPage
                        )
                );

        page =
                clampPage(page, pages);

        Inventory inventory =
                Bukkit.createInventory(
                        new GuiHolder(
                                GuiHolder.Type.MY_LISTINGS,
                                page
                        ),
                        54,
                        componentTitle(
                        plugin.getConfig().getString(
                                "gui.my-listings.title",
                                "&8My Auctions"
                        )
                ));

        fillBottom(inventory);

        int start =
                page * perPage;

        int end =
                Math.min(
                        start + perPage,
                        listings.size()
                );

        int slotIndex = 0;

        for (int index = start;
             index < end;
             index++) {

            inventory.setItem(
                    itemSlots.get(slotIndex++),
                    createMyListingItem(
                            listings.get(index)
                    )
            );
        }

        inventory.setItem(
                45,
                button(
                        Material.ARROW,
                        "&ePrevious Page"
                )
        );

        inventory.setItem(
                48,
                button(
                        Material.CHEST,
                        "&eAuction House"
                )
        );

        inventory.setItem(
                49,
                button(
                        Material.CHEST,
                        "&bMy Auctions",
                        "&7Active: &f"
                                + listings.size(),
                        "&7Limit: &f"
                                + manager.getListingLimit(
                                player
                        )
                )
        );

        inventory.setItem(
                52,
                button(
                        Material.BARRIER,
                        "&cClose"
                )
        );

        inventory.setItem(
                53,
                button(
                        Material.ARROW,
                        "&eNext Page"
                )
        );

        if (listings.isEmpty()) {
            inventory.setItem(
                    22,
                    button(
                            Material.BARRIER,
                            "&cNo Active Listings",
                            "&7You currently have no auctions."
                    )
            );
        }

        player.openInventory(inventory);
    }

    public void openClaims(
            Player player,
            int page
    ) {
        List<ClaimManager.ClaimEntry> claims =
                plugin.getClaimManager()
                        .getClaims(
                                player.getUniqueId()
                        );

        List<Integer> itemSlots =
                getItemSlots();

        int perPage =
                itemSlots.size();

        int pages =
                Math.max(
                        1,
                        (int) Math.ceil(
                                claims.size()
                                        / (double) perPage
                        )
                );

        page =
                clampPage(page, pages);

        Inventory inventory =
                Bukkit.createInventory(
                        new GuiHolder(
                                GuiHolder.Type.CLAIMS,
                                page
                        ),
                        54,
                        componentTitle(
                        plugin.getConfig().getString(
                                "gui.claims.title",
                                "&8Auction Claims"
                        )
                ));

        fillBottom(inventory);

        int start =
                page * perPage;

        int end =
                Math.min(
                        start + perPage,
                        claims.size()
                );

        int slotIndex = 0;

        for (int index = start;
             index < end;
             index++) {

            ClaimManager.ClaimEntry claim =
                    claims.get(index);

            inventory.setItem(
                    itemSlots.get(slotIndex++),
                    createClaimItem(claim)
            );
        }

        inventory.setItem(
                45,
                button(
                        Material.ARROW,
                        "&ePrevious Page"
                )
        );

        inventory.setItem(
                48,
                button(
                        Material.CHEST,
                        "&eAuction House"
                )
        );

        inventory.setItem(
                49,
                button(
                        Material.BUNDLE,
                        "&6Auction Claims",
                        "&7Waiting: &f"
                                + claims.size()
                )
        );

        inventory.setItem(
                52,
                button(
                        Material.BARRIER,
                        "&cClose"
                )
        );

        inventory.setItem(
                53,
                button(
                        Material.ARROW,
                        "&eNext Page"
                )
        );

        if (claims.isEmpty()) {
            inventory.setItem(
                    22,
                    button(
                            Material.BARRIER,
                            "&cNo Claims",
                            "&7You have no items waiting."
                    )
            );
        }

        player.openInventory(inventory);
    }

    public void openSell(
            Player player
    ) {
        if (!player.hasPermission(
                "auctionhouse.sell"
        )) {
            plugin.getMessages().send(
                    player,
                    "general.no-permission",
                    "&cYou do not have permission."
            );
            return;
        }

        Inventory inventory =
                Bukkit.createInventory(
                        new GuiHolder(
                                GuiHolder.Type.SELL,
                                0
                        ),
                        27,
                        componentTitle(
                        plugin.getConfig().getString(
                                "gui.sell.title",
                                "&8Sell Item"
                        )
                ));

        fillAll(inventory);

        ItemStack hand =
                player.getInventory()
                        .getItemInMainHand();

        if (hand.getType().isAir()) {
            inventory.setItem(
                    13,
                    button(
                            Material.BARRIER,
                            "&cNo Item",
                            "&7Hold an item in your",
                            "&7main hand."
                    )
            );
        } else {
            inventory.setItem(
                    13,
                    hand.clone()
            );
        }

        inventory.setItem(
                11,
                button(
                        Material.EMERALD,
                        "&aCreate Listing",
                        "&7Click to enter a price.",
                        "",
                        "&7Examples:",
                        "&f1k &7| &f1m &7| &f1b &7| &f1t"
                )
        );

        inventory.setItem(
                15,
                button(
                        Material.ARROW,
                        "&cBack"
                )
        );

        player.openInventory(inventory);
    }

    public void handleBrowseClick(
            Player player,
            int slot
    ) {
        GuiHolder holder =
                getHolder(player);

        if (holder == null
                || holder.getType()
                != GuiHolder.Type.BROWSE) {
            return;
        }

        int page =
                holder.getPage();

        switch (slot) {
            case 45 ->
                    openBrowse(
                            player,
                            Math.max(0, page - 1),
                            holder.getSortMode(),
                            holder.getSearch()
                    );

            case 46 ->
                    openBrowse(
                            player,
                            0,
                            holder.getSortMode().next(),
                            holder.getSearch()
                    );

            case 47 ->
                    plugin.getAuctionListener()
                            .beginSearchInput(player);

            case 48 ->
                    openMyListings(
                            player,
                            0
                    );

            case 49 ->
                    openClaims(
                            player,
                            0
                    );

            case 50 ->
                    openSell(player);

            case 51 ->
                    openBrowse(
                            player,
                            page,
                            holder.getSortMode(),
                            holder.getSearch()
                    );

            case 52 ->
                    player.closeInventory();

            case 53 ->
                    openBrowse(
                            player,
                            page + 1,
                            holder.getSortMode(),
                            holder.getSearch()
                    );

            default -> {
                if (!getItemSlots().contains(slot)) {
                    return;
                }

                AuctionListing listing =
                        getBrowseListing(
                                holder,
                                slot
                        );

                if (listing == null) {
                    return;
                }

                buy(
                        player,
                        listing
                );
            }
        }
    }

    public void handleMyListingsClick(
            Player player,
            int slot
    ) {
        GuiHolder holder =
                getHolder(player);

        if (holder == null) {
            return;
        }

        switch (slot) {
            case 45 ->
                    openMyListings(
                            player,
                            Math.max(
                                    0,
                                    holder.getPage() - 1
                            )
                    );

            case 48 ->
                    openBrowse(player, 0);

            case 52 ->
                    player.closeInventory();

            case 53 ->
                    openMyListings(
                            player,
                            holder.getPage() + 1
                    );

            default -> {
                if (!getItemSlots().contains(slot)) {
                    return;
                }

                List<AuctionListing> listings =
                        getMyListings(player);

                int index =
                        holder.getPage()
                                * getItemSlots().size()
                                + getItemSlots()
                                .indexOf(slot);

                if (index < 0
                        || index >= listings.size()) {
                    return;
                }

                cancelListing(
                        player,
                        listings.get(index)
                );
            }
        }
    }

    public void handleClaimsClick(
            Player player,
            int slot
    ) {
        GuiHolder holder =
                getHolder(player);

        if (holder == null) {
            return;
        }

        switch (slot) {
            case 45 ->
                    openClaims(
                            player,
                            Math.max(
                                    0,
                                    holder.getPage() - 1
                            )
                    );

            case 48 ->
                    openBrowse(player, 0);

            case 52 ->
                    player.closeInventory();

            case 53 ->
                    openClaims(
                            player,
                            holder.getPage() + 1
                    );

            default -> {
                if (!getItemSlots().contains(slot)) {
                    return;
                }

                List<ClaimManager.ClaimEntry> claims =
                        plugin.getClaimManager()
                                .getClaims(
                                        player.getUniqueId()
                                );

                int index =
                        holder.getPage()
                                * getItemSlots().size()
                                + getItemSlots()
                                .indexOf(slot);

                if (index < 0
                        || index >= claims.size()) {
                    return;
                }

                ClaimManager.ClaimEntry claim =
                        claims.get(index);

                if (!plugin.getClaimManager().claim(
                        player,
                        claim.id()
                )) {
                    plugin.getMessages().send(
                            player,
                            "claims.inventory-full",
                            "&cYour inventory is full."
                    );
                    return;
                }

                plugin.getMessages().send(
                        player,
                        "claims.item-collected",
                        "&aItem collected."
                );

                openClaims(
                        player,
                        holder.getPage()
                );
            }
        }
    }

    public void handleSellClick(
            Player player,
            int slot
    ) {
        if (slot == 15) {
            openBrowse(player, 0);
            return;
        }

        if (slot != 11) {
            return;
        }

        if (!player.hasPermission(
                "auctionhouse.sell"
        )) {
            plugin.getMessages().send(
                    player,
                    "general.no-permission",
                    "&cYou do not have permission."
            );
            return;
        }

        if (!manager.canList(player)) {
            plugin.getMessages().send(
                    player,
                    "sell.maximum-listings",
                    "&cYou have reached your maximum active listings.",
                    java.util.Map.of(
                            "limit",
                            manager.getListingLimit(player)
                    )
            );
            return;
        }

        ItemStack hand =
                player.getInventory()
                        .getItemInMainHand();

        if (hand.getType().isAir()) {
            plugin.getMessages().send(
                    player,
                    "sell.no-item",
                    "&cHold an item in your main hand first."
            );
            return;
        }

        if (isBlocked(hand)) {
            plugin.getMessages().send(
                    player,
                    "sell.blocked-item",
                    "&cThis item cannot be listed."
            );
            return;
        }

        if (hasBlockedLore(hand)) {
            plugin.getMessages().send(
                    player,
                    "sell.blocked-item",
                    "&cThis item cannot be listed."
            );
            return;
        }

        player.closeInventory();

        plugin.getAuctionListener()
                .beginPriceInput(player);
    }

    public void createListingFromInput(
            Player player,
            String input
    ) {
        final double price;

        try {
            price =
                    PriceFormatter.parse(input);
        } catch (IllegalArgumentException exception) {
            plugin.getMessages().send(
                    player,
                    "sell.invalid-price",
                    "&cInvalid price."
            );
            return;
        }

        double minimum =
                plugin.getConfig().getDouble(
                        "pricing.minimum-price",
                        100D
                );

        double maximum =
                plugin.getConfig().getDouble(
                        "pricing.maximum-price",
                        100_000_000D
                );

        if (!PriceFormatter.isValidRange(
                price,
                minimum,
                maximum
        )) {
            if (price < minimum) {
                plugin.getMessages().send(
                        player,
                        "sell.minimum-price",
                        "&cMinimum price: &e{price}",
                        java.util.Map.of(
                                "price",
                                PriceFormatter.format(minimum)
                        )
                );
            } else {
                plugin.getMessages().send(
                        player,
                        "sell.maximum-price",
                        "&cMaximum price: &e{price}",
                        java.util.Map.of(
                                "price",
                                PriceFormatter.format(maximum)
                        )
                );
            }
            return;
        }

        if (!manager.canList(player)) {
            plugin.getMessages().send(
                    player,
                    "sell.maximum-listings",
                    "&cYou have reached your maximum active listings.",
                    java.util.Map.of(
                            "limit",
                            manager.getListingLimit(player)
                    )
            );
            return;
        }

        ItemStack hand =
                player.getInventory()
                        .getItemInMainHand();

        if (hand.getType().isAir()) {
            plugin.getMessages().send(
                    player,
                    "sell.no-item",
                    "&cHold an item in your main hand first."
            );
            return;
        }

        if (isBlocked(hand)
                || hasBlockedLore(hand)) {
            plugin.getMessages().send(
                    player,
                    "sell.blocked-item",
                    "&cThis item cannot be listed."
            );
            return;
        }

        EconomyHook economy =
                plugin.getEconomyHook();

        double fee =
                Math.max(
                        0D,
                        plugin.getConfig().getDouble(
                                "pricing.listing-fee",
                                0D
                        )
                );

        if (fee > 0D) {
            if (!economy.isAvailable()) {
                plugin.getMessages().send(
                        player,
                        "errors.economy",
                        "&cEconomy is unavailable."
                );
                return;
            }

            if (!economy.has(
                    player,
                    fee
            )) {
                plugin.getMessages().send(
                        player,
                        "sell.not-enough-for-fee",
                        "&cYou cannot afford the listing fee."
                );
                return;
            }

            if (!economy.withdraw(
                    player,
                    fee
            )) {
                plugin.getMessages().send(
                        player,
                        "errors.economy",
                        "&cEconomy transaction failed."
                );
                return;
            }
        }

        ItemStack listingItem =
                hand.clone();

        player.getInventory()
                .setItemInMainHand(
                        new ItemStack(Material.AIR)
                );

        if (!manager.addListing(
                player,
                listingItem,
                price
        )) {
            player.getInventory().addItem(
                    listingItem
            );

            if (fee > 0D) {
                economy.deposit(
                        player,
                        fee
                );
            }

            plugin.getMessages().send(
                    player,
                    "errors.internal",
                    "&cCould not create the auction."
            );
            return;
        }

        plugin.getMessages().text(
                "sell.created",
                "&aAuction created: &f{item} x{amount} &afor &e{price}",
                java.util.Map.of(
                        "item",
                        pretty(listingItem.getType()),
                        "amount",
                        listingItem.getAmount(),
                        "price",
                        PriceFormatter.format(price)
                )
        );

        player.sendMessage(
                plugin.getMessages().text(
                        "sell.created",
                        "&aAuction created: &f{item} x{amount} &afor &e{price}",
                        java.util.Map.of(
                                "item",
                                pretty(listingItem.getType()),
                                "amount",
                                listingItem.getAmount(),
                                "price",
                                PriceFormatter.format(price)
                        )
                )
        );

        if (fee > 0D) {
            player.sendMessage(
                    plugin.getMessages().text(
                            "sell.listing-fee",
                            "&7Listing fee: &e{fee}",
                            java.util.Map.of(
                                    "fee",
                                    PriceFormatter.format(fee)
                            )
                    )
            );
        }

        openBrowse(player, 0);
    }

    private void buy(
            Player player,
            AuctionListing listing
    ) {
        if (!player.hasPermission(
                "auctionhouse.buy"
        )) {
            plugin.getMessages().send(
                    player,
                    "general.no-permission",
                    "&cYou do not have permission."
            );
            return;
        }

        plugin.getMessages().send(
                player,
                "buy.processing",
                "&eProcessing purchase..."
        );

        AuctionManager.PurchaseOutcome outcome =
                manager.purchase(
                        player,
                        listing.getId()
                );

        switch (outcome.result()) {
            case SUCCESS -> {
                AuctionListing sold =
                        Objects.requireNonNull(
                                outcome.listing()
                        );

                plugin.getMessages().text(
                        "buy.success",
                        "&aYou bought &f{item} x{amount} &afor &e{price}&a.",
                        java.util.Map.of(
                                "item",
                                pretty(
                                        sold.getItem().getType()
                                ),
                                "amount",
                                sold.getItem().getAmount(),
                                "price",
                                PriceFormatter.format(
                                        sold.getPrice()
                                )
                        )
                );

                player.sendMessage(
                        plugin.getMessages().text(
                                "buy.success",
                                "&aYou bought &f{item} x{amount} &afor &e{price}&a.",
                                java.util.Map.of(
                                        "item",
                                        pretty(
                                                sold.getItem()
                                                        .getType()
                                        ),
                                        "amount",
                                        sold.getItem()
                                                .getAmount(),
                                        "price",
                                        PriceFormatter.format(
                                                sold.getPrice()
                                        )
                                )
                        )
                );

                Player seller =
                        Bukkit.getPlayer(
                                sold.getSeller()
                        );

                if (seller != null
                        && seller.isOnline()) {
                    seller.getScheduler().run(
                            plugin,
                            task -> seller.sendMessage(
                                    plugin.getMessages().text(
                                            "listing.sold",
                                            "&aYour item has been sold for &e{price}&a.",
                                            java.util.Map.of(
                                                    "price",
                                                    PriceFormatter.format(
                                                            sold.getPrice()
                                                    )
                                            )
                                    )
                            ),
                            null
                    );
                }

                GuiHolder holder =
                        getHolder(player);

                if (holder != null) {
                    openBrowse(
                            player,
                            holder.getPage(),
                            holder.getSortMode(),
                            holder.getSearch()
                    );
                } else {
                    openBrowse(
                            player,
                            0
                    );
                }
            }

            case NOT_FOUND ->
                    plugin.getMessages().send(
                            player,
                            "buy.unavailable",
                            "&cThis auction is no longer available."
                    );

            case EXPIRED ->
                    plugin.getMessages().send(
                            player,
                            "listing.expired",
                            "&eThis auction expired."
                    );

            case OWN_LISTING ->
                    plugin.getMessages().send(
                            player,
                            "buy.own-listing",
                            "&cYou cannot purchase your own listing."
                    );

            case NO_ECONOMY ->
                    plugin.getMessages().send(
                            player,
                            "errors.economy",
                            "&cEconomy is unavailable."
                    );

            case NOT_ENOUGH_MONEY ->
                    plugin.getMessages().send(
                            player,
                            "buy.not-enough-money",
                            "&cYou don't have enough money."
                    );

            case PAYMENT_FAILED,
                 INTERNAL_ERROR ->
                    plugin.getMessages().send(
                            player,
                            "buy.failed",
                            "&cThe payment could not be completed."
                    );

            case INVENTORY_FULL ->
                    plugin.getMessages().send(
                            player,
                            "buy.inventory-full",
                            "&cYour inventory is full."
                    );
        }
    }

    private void cancelListing(
            Player player,
            AuctionListing listing
    ) {
        if (!hasInventorySpace(
                player,
                listing.getItem()
        )) {
            player.sendMessage(
                    color("&cYour inventory does not have enough space to return this item.")
            );
            return;
        }

        if (!manager.removeListing(
                listing.getId()
        )) {
            plugin.getMessages().send(
                    player,
                    "buy.unavailable",
                    "&cThis listing is no longer available."
            );
            return;
        }

        player.getInventory().addItem(
                listing.getItem()
        );

        player.sendMessage(
                plugin.getMessages().text(
                        "listing.cancelled",
                        "&eYour listing has been cancelled."
                )
        );

        openMyListings(
                player,
                0
        );
    }

    private AuctionListing getBrowseListing(
            GuiHolder holder,
            int slot
    ) {
        List<AuctionListing> listings =
                manager.getSortedListings(
                        holder.getSortMode(),
                        holder.getSearch()
                );

        List<Integer> slots =
                getItemSlots();

        int position =
                slots.indexOf(slot);

        if (position < 0) {
            return null;
        }

        int index =
                holder.getPage()
                        * slots.size()
                        + position;

        if (index < 0
                || index >= listings.size()) {
            return null;
        }

        return listings.get(index);
    }

    private List<AuctionListing> getMyListings(
            Player player
    ) {
        return manager.getActiveListings()
                .stream()
                .filter(
                        listing ->
                                listing.getSeller()
                                        .equals(
                                                player.getUniqueId()
                                        )
                )
                .sorted(
                        java.util.Comparator
                                .comparingLong(
                                        AuctionListing::getCreatedAt
                                )
                                .reversed()
                )
                .toList();
    }

    private ItemStack createListingItem(
            AuctionListing listing
    ) {
        ItemStack item =
                listing.getItem();

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        List<String> lore =
                meta.hasLore()
                        ? new ArrayList<>(
                                meta.getLore()
                        )
                        : new ArrayList<>();

        lore.add("");
        lore.add(
                color("&7Seller: &f"
                        + listing.getSellerName())
        );
        lore.add(
                color("&7Amount: &f"
                        + listing.getItem().getAmount())
        );
        lore.add(
                color("&7Price: &e"
                        + PriceFormatter.format(
                        listing.getPrice()
                ))
        );
        lore.add(
                color("&7Time Left: &f"
                        + listing.getRemainingFormatted())
        );
        lore.add(
                color("&7Listing ID: &f"
                        + listing.getId())
        );
        lore.add("");
        lore.add(
                color("&aClick to purchase")
        );

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createMyListingItem(
            AuctionListing listing
    ) {
        ItemStack item =
                createListingItem(
                        listing
                );

        ItemMeta meta =
                item.getItemMeta();

        if (meta != null) {
            List<String> lore =
                    meta.hasLore()
                            ? new ArrayList<>(
                                    meta.getLore()
                            )
                            : new ArrayList<>();

            lore.add(
                    color("&cClick to cancel")
            );

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createClaimItem(
            ClaimManager.ClaimEntry claim
    ) {
        ItemStack item =
                claim.item().clone();

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        List<String> lore =
                meta.hasLore()
                        ? new ArrayList<>(
                                meta.getLore()
                        )
                        : new ArrayList<>();

        lore.add("");
        lore.add(
                color("&7Reason: &f"
                        + claim.reason())
        );
        lore.add(
                color("&7Claim ID: &f"
                        + claim.id())
        );
        lore.add("");
        lore.add(
                color("&aClick to collect")
        );

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private boolean isBlocked(
            ItemStack item
    ) {
        for (String material :
                plugin.getConfig()
                        .getStringList(
                                "restrictions.blocked-materials"
                        )) {

            if (material.equalsIgnoreCase(
                    item.getType().name()
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean hasBlockedLore(
            ItemStack item
    ) {
        List<String> blocked =
                plugin.getConfig()
                        .getStringList(
                                "restrictions.blocked-lore"
                        );

        if (blocked.isEmpty()) {
            return false;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null
                || !meta.hasLore()) {
            return false;
        }

        List<String> lore =
                meta.getLore();

        if (lore == null) {
            return false;
        }

        for (String line : lore) {
            for (String needle : blocked) {
                if (stripLegacyColors(line).toLowerCase(Locale.ROOT)
                        .contains(
                                stripLegacyColors(needle).toLowerCase(Locale.ROOT)
                        )) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasInventorySpace(
            Player player,
            ItemStack item
    ) {
        int remaining =
                item.getAmount();

        for (ItemStack current :
                player.getInventory()
                        .getStorageContents()) {

            if (current == null
                    || current.getType().isAir()) {
                remaining -=
                        item.getMaxStackSize();
            } else if (current.isSimilar(item)) {
                remaining -= Math.max(
                        0,
                        current.getMaxStackSize()
                                - current.getAmount()
                );
            }

            if (remaining <= 0) {
                return true;
            }
        }

        return false;
    }

    private List<Integer> getItemSlots() {
        List<Integer> slots =
                plugin.getConfig()
                        .getIntegerList(
                                "gui.browse.item-slots"
                        );

        if (slots.isEmpty()) {
            return List.of(
                    10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30, 31, 32, 33, 34,
                    37, 38, 39, 40, 41, 42, 43
            );
        }

        return slots.stream()
                .filter(slot -> slot >= 0 && slot < 45)
                .distinct()
                .toList();
    }

    private int clampPage(
            int page,
            int pages
    ) {
        return Math.max(
                0,
                Math.min(
                        page,
                        pages - 1
                )
        );
    }

    private GuiHolder getHolder(
            Player player
    ) {
        if (player.getOpenInventory()
                .getTopInventory()
                .getHolder()
                instanceof GuiHolder holder) {
            return holder;
        }

        return null;
    }

    private void fillBottom(
            Inventory inventory
    ) {
        ItemStack filler =
                button(
                        Material.GRAY_STAINED_GLASS_PANE,
                        " "
                );

        for (int slot = 45;
             slot < 54;
             slot++) {
            inventory.setItem(
                    slot,
                    filler
            );
        }
    }

    private void fillAll(
            Inventory inventory
    ) {
        ItemStack filler =
                button(
                        Material.GRAY_STAINED_GLASS_PANE,
                        " "
                );

        for (int slot = 0;
             slot < inventory.getSize();
             slot++) {
            inventory.setItem(
                    slot,
                    filler
            );
        }
    }

    private ItemStack button(
            Material material,
            String name,
            String... lore
    ) {
        ItemStack item =
                new ItemStack(material);

        ItemMeta meta =
                item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(
                    color(name)
            );

            List<String> lines =
                    new ArrayList<>();

            for (String line : lore) {
                lines.add(
                        color(line)
                );
            }

            meta.setLore(lines);

            item.setItemMeta(meta);
        }

        return item;
    }

    private String pretty(
            Material material
    ) {
        String[] parts =
                material.name()
                        .toLowerCase(Locale.ROOT)
                        .replace('_', ' ')
                        .split(" ");

        StringBuilder result =
                new StringBuilder();

        for (String part : parts) {
            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(
                    Character.toUpperCase(
                            part.charAt(0)
                    )
            );

            if (part.length() > 1) {
                result.append(
                        part.substring(1)
                );
            }
        }

        return result.toString();
    }

    private Component componentTitle(
            String text
    ) {
        return LegacyComponentSerializer
                .legacySection()
                .deserialize(color(text));
    }

    private String stripLegacyColors(
            String text
    ) {
        if (text == null) {
            return "";
        }

        return text.replaceAll(
                "(?i)§[0-9A-FK-OR]",
                ""
        );
    }

    private String color(
            String text
    ) {
        return text == null
                ? ""
                : text.replace('&', '§');
    }
}
