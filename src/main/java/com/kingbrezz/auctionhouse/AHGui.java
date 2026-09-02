package com.kingbrezz.auctionhouse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class AHGui {

    private final AuctionHousePlugin plugin;
    private final AuctionManager manager;

    public AHGui(AuctionHousePlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getAuctionManager();
    }

    public void openBrowse(Player player, int page) {
        List<AuctionListing> listings = new ArrayList<>(
                manager.getActiveListings()
        );

        listings.sort(
                Comparator.comparingLong(
                        AuctionListing::getCreatedAt
                ).reversed()
        );

        int perPage = 45;

        int maxPage = Math.max(
                0,
                (listings.size() - 1) / perPage
        );

        page = Math.max(0, Math.min(page, maxPage));

        Inventory inv = Bukkit.createInventory(
                new GuiHolder(GuiHolder.Type.BROWSE, page),
                54,
                color("&8Auction House")
        );

        fillBottom(inv);

        int start = page * perPage;
        int end = Math.min(start + perPage, listings.size());

        int slot = 0;

        for (int i = start; i < end; i++) {
            inv.setItem(
                    slot++,
                    createListingItem(listings.get(i))
            );
        }

        inv.setItem(
                45,
                button(
                        Material.ARROW,
                        "&ePrevious Page",
                        "&7Page " + Math.max(1, page)
                )
        );

        inv.setItem(
                49,
                button(
                        Material.BOOK,
                        "&bAuction House",
                        "&7Page: &f" + (page + 1),
                        "&7Listings: &f" + listings.size(),
                        "",
                        "&7Use &f/ah &7to reopen."
                )
        );

        inv.setItem(
                53,
                button(
                        Material.ARROW,
                        "&eNext Page",
                        "&7Page " + (page + 2)
                )
        );

        player.openInventory(inv);
    }

    public void openMyListings(Player player, int page) {
        List<AuctionListing> listings =
                manager.getActiveListings()
                        .stream()
                        .filter(
                                listing -> listing.getSeller()
                                        .equals(player.getUniqueId())
                        )
                        .sorted(
                                Comparator.comparingLong(
                                        AuctionListing::getCreatedAt
                                ).reversed()
                        )
                        .toList();

        Inventory inv = Bukkit.createInventory(
                new GuiHolder(
                        GuiHolder.Type.MY_LISTINGS,
                        page
                ),
                54,
                color("&8My Auctions")
        );

        fillBottom(inv);

        for (int i = 0; i < listings.size() && i < 45; i++) {
            ItemStack item =
                    createListingItem(listings.get(i));

            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                List<String> lore =
                        meta.hasLore()
                                ? new ArrayList<>(meta.getLore())
                                : new ArrayList<>();

                lore.add("");
                lore.add(color("&cRight-click to cancel."));

                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inv.setItem(i, item);
        }

        inv.setItem(
                49,
                button(
                        Material.CHEST,
                        "&bMy Auctions",
                        "&7Active listings: &f" + listings.size(),
                        "",
                        "&7Your listing limit: &f"
                                + manager.getListingLimit(player)
                )
        );

        player.openInventory(inv);
    }

    public void openSell(Player player) {
        Inventory inv = Bukkit.createInventory(
                new GuiHolder(GuiHolder.Type.SELL, 0),
                27,
                color("&8Sell Item")
        );

        fillFull(inv);

        ItemStack hand =
                player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            inv.setItem(
                    13,
                    button(
                            Material.BARRIER,
                            "&cNo Item",
                            "&7Hold an item in your main hand."
                    )
            );
        } else {
            ItemStack display = hand.clone();

            ItemMeta meta = display.getItemMeta();

            if (meta != null) {
                List<String> lore =
                        meta.hasLore()
                                ? new ArrayList<>(meta.getLore())
                                : new ArrayList<>();

                lore.add("");
                lore.add(color("&7Amount: &f" + hand.getAmount()));
                lore.add("");
                lore.add(color("&aClick to set a price."));

                meta.setLore(lore);
                display.setItemMeta(meta);
            }

            inv.setItem(13, display);
        }

        inv.setItem(
                11,
                button(
                        Material.EMERALD,
                        "&aCreate Listing",
                        "&7Hold an item in your hand.",
                        "&7Then click here."
                )
        );

        inv.setItem(
                15,
                button(
                        Material.ARROW,
                        "&cBack",
                        "&7Return to Auction House."
                )
        );

        player.openInventory(inv);
    }

    public void openClaims(Player player, int page) {
        Inventory inv = Bukkit.createInventory(
                new GuiHolder(
                        GuiHolder.Type.CLAIMS,
                        page
                ),
                54,
                color("&8Auction Claims")
        );

        fillBottom(inv);

        inv.setItem(
                49,
                button(
                        Material.CHEST,
                        "&bAuction Claims",
                        "&7Items from completed auctions.",
                        "",
                        "&eClaim items here."
                )
        );

        player.openInventory(inv);
    }

    public void handleBrowseClick(
            Player player,
            int slot,
            boolean rightClick,
            boolean shiftClick,
            boolean doubleClick
    ) {
        if (slot == 45) {
            GuiHolder holder = getHolder(player);

            int page = holder == null
                    ? 0
                    : holder.getPage();

            openBrowse(
                    player,
                    Math.max(0, page - 1)
            );

            return;
        }

        if (slot == 53) {
            GuiHolder holder = getHolder(player);

            int page = holder == null
                    ? 0
                    : holder.getPage();

            openBrowse(
                    player,
                    page + 1
            );

            return;
        }

        if (slot >= 45) {
            return;
        }

        AuctionListing listing =
                getListingForSlot(player, slot);

        if (listing == null) {
            return;
        }

        if (listing.isExpired()) {
            player.sendMessage(
                    color("&cThis auction has expired.")
            );
            openBrowse(player, 0);
            return;
        }

        if (listing.getSeller().equals(
                player.getUniqueId()
        )) {
            player.sendMessage(
                    color("&cYou cannot buy your own auction.")
            );
            return;
        }

        double price = listing.getPrice();

        EconomyHook economy =
                plugin.getEconomyHook();

        if (!economy.isAvailable()) {
            player.sendMessage(
                    color("&cEconomy is not available.")
            );
            return;
        }

        if (!economy.has(player, price)) {
            player.sendMessage(
                    color(
                            "&cYou need &e"
                                    + PriceFormatter.format(price)
                                    + " &cto buy this item."
                    )
            );
            return;
        }

        if (!economy.withdraw(player, price)) {
            player.sendMessage(
                    color("&cPayment failed.")
            );
            return;
        }

        manager.removeListing(listing.getId());

        MapGiveResult result =
                giveItem(player, listing.getItem());

        if (!result.success()) {
            economy.deposit(player, price);

            player.sendMessage(
                    color(
                            "&cYour inventory is full. "
                                    + "Purchase cancelled."
                    )
            );

            return;
        }

        Player seller =
                Bukkit.getPlayer(listing.getSeller());

        double tax =
                plugin.getConfig().getDouble(
                        "settings.tax-percent",
                        5.0
                );

        double sellerAmount =
                price - (price * tax / 100.0);

        if (seller != null && seller.isOnline()) {
            economy.deposit(
                    seller,
                    sellerAmount
            );

            seller.sendMessage(
                    color(
                            "&aYour auction sold for &e"
                                    + PriceFormatter.format(price)
                                    + "&a."
                    )
            );
        } else {
            economy.deposit(
                    Bukkit.getOfflinePlayer(
                            listing.getSeller()
                    ),
                    sellerAmount
            );
        }

        player.sendMessage(
                color(
                        "&aPurchased &f"
                                + listing.getItem().getAmount()
                                + "x "
                                + pretty(
                                listing.getItem().getType()
                        )
                                + " &afor &e"
                                + PriceFormatter.format(price)
                )
        );

        openBrowse(player, 0);
    }

    public void handleMyListingsClick(
            Player player,
            int slot
    ) {
        if (slot >= 45) {
            return;
        }

        List<AuctionListing> listings =
                manager.getActiveListings()
                        .stream()
                        .filter(
                                listing -> listing.getSeller()
                                        .equals(player.getUniqueId())
                        )
                        .sorted(
                                Comparator.comparingLong(
                                        AuctionListing::getCreatedAt
                                ).reversed()
                        )
                        .toList();

        if (slot >= listings.size()) {
            return;
        }

        AuctionListing listing =
                listings.get(slot);

        if (!manager.removeListing(
                listing.getId()
        )) {
            player.sendMessage(
                    color("&cThis listing is no longer available.")
            );
            return;
        }

        player.getInventory().addItem(
                listing.getItem().clone()
        );

        player.sendMessage(
                color(
                        "&aAuction cancelled. "
                                + "Your item has been returned."
                )
        );

        openMyListings(player, 0);
    }

    public void handleClaimsClick(
            Player player,
            int slot
    ) {
        player.sendMessage(
                color("&eThere are currently no claim items.")
        );
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
            player.sendMessage(
                    color("&cYou do not have permission to sell.")
            );
            return;
        }

        if (!manager.canList(player)) {
            player.sendMessage(
                    color(
                            "&cYou reached your auction limit: &f"
                                    + manager.getListingLimit(player)
                    )
            );
            return;
        }

        ItemStack hand =
                player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            player.sendMessage(
                    color("&cHold an item first.")
            );
            return;
        }

        if (isBlocked(hand)) {
            player.sendMessage(
                    color("&cThis item cannot be auctioned.")
            );
            return;
        }

        player.closeInventory();

        AHListener listener =
                plugin.getAuctionListener();

        listener.beginPriceInput(player);
    }

    public void createListingFromInput(
            Player player,
            String input
    ) {
        Double price =
                PriceFormatter.parse(input);

        if (price == null) {
            player.sendMessage(
                    color(
                            "&cInvalid price. Examples: "
                                    + "&f1k, 1.5m, 10m, 1b, 1t"
                    )
            );
            return;
        }

        if (!manager.isPriceAllowed(price)) {
            double min =
                    plugin.getConfig().getDouble(
                            "pricing.minimum-price",
                            100
                    );

            double max =
                    plugin.getConfig().getDouble(
                            "pricing.maximum-price",
                            100000000
                    );

            player.sendMessage(
                    color(
                            "&cPrice must be between &e"
                                    + PriceFormatter.format(min)
                                    + " &cand &e"
                                    + PriceFormatter.format(max)
                    )
            );
            return;
        }

        if (!manager.canList(player)) {
            player.sendMessage(
                    color("&cYou reached your auction limit.")
            );
            return;
        }

        ItemStack hand =
                player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            player.sendMessage(
                    color("&cThe item is no longer in your hand.")
            );
            return;
        }

        if (isBlocked(hand)) {
            player.sendMessage(
                    color("&cThis item cannot be auctioned.")
            );
            return;
        }

        ItemStack listingItem = hand.clone();

        player.getInventory().setItemInMainHand(
                new ItemStack(Material.AIR)
        );

        boolean created =
                manager.addListing(
                        player,
                        listingItem,
                        price
                );

        if (!created) {
            player.getInventory().addItem(
                    listingItem
            );

            player.sendMessage(
                    color("&cCould not create the auction.")
            );
            return;
        }

        player.sendMessage(
                color(
                        "&aAuction created for &e"
                                + PriceFormatter.format(price)
                                + "&a."
                )
        );

        openBrowse(player, 0);
    }

    private AuctionListing getListingForSlot(
            Player player,
            int slot
    ) {
        List<AuctionListing> listings =
                new ArrayList<>(
                        manager.getActiveListings()
                );

        listings.sort(
                Comparator.comparingLong(
                        AuctionListing::getCreatedAt
                ).reversed()
        );

        GuiHolder holder = getHolder(player);

        int page =
                holder == null
                        ? 0
                        : holder.getPage();

        int index =
                (page * 45) + slot;

        if (index < 0
                || index >= listings.size()) {
            return null;
        }

        return listings.get(index);
    }

    private GuiHolder getHolder(Player player) {
        if (player.getOpenInventory()
                .getTopInventory()
                .getHolder() instanceof GuiHolder holder) {
            return holder;
        }

        return null;
    }

    private boolean isBlocked(ItemStack item) {
        List<String> blocked =
                plugin.getConfig().getStringList(
                        "restrictions.blocked-materials"
                );

        return blocked.contains(
                item.getType().name()
        );
    }

    private MapGiveResult giveItem(
            Player player,
            ItemStack item
    ) {
        Map<Integer, ItemStack> leftover =
                player.getInventory().addItem(
                        item.clone()
                );

        if (leftover.isEmpty()) {
            return new MapGiveResult(true);
        }

        for (ItemStack stack : leftover.values()) {
            player.getWorld().dropItemNaturally(
                    player.getLocation(),
                    stack
            );
        }

        return new MapGiveResult(true);
    }

    private void fillBottom(Inventory inv) {
        ItemStack filler =
                button(
                        Material.GRAY_STAINED_GLASS_PANE,
                        " "
                );

        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }
    }

    private void fillFull(Inventory inv) {
        ItemStack filler =
                button(
                        Material.GRAY_STAINED_GLASS_PANE,
                        " "
                );

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    private ItemStack createListingItem(
            AuctionListing listing
    ) {
        ItemStack item =
                listing.getItem().clone();

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        List<String> lore =
                meta.hasLore()
                        ? new ArrayList<>(meta.getLore())
                        : new ArrayList<>();

        lore.add("");
        lore.add(
                color(
                        "&7Seller: &f"
                                + listing.getSellerName()
                )
        );
        lore.add(
                color(
                        "&7Amount: &f"
                                + listing.getItem().getAmount()
                )
        );
        lore.add(
                color(
                        "&7Price: &e"
                                + PriceFormatter.format(
                                listing.getPrice()
                        )
                )
        );
        lore.add(
                color(
                        "&7Time Left: &f"
                                + listing.getRemainingFormatted()
                )
        );
        lore.add("");
        lore.add(
                color("&aClick to purchase")
        );

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
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
                lines.add(color(line));
            }

            meta.setLore(lines);
            item.setItemMeta(meta);
        }

        return item;
    }

    private String pretty(Material material) {
        String[] parts =
                material.name()
                        .toLowerCase()
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

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                text == null ? "" : text
        );
    }

    private record MapGiveResult(boolean success) {
    }
            }
