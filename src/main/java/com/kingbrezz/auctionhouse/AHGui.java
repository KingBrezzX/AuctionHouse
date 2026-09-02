package com.kingbrezz.auctionhouse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AHGui {

    private final AuctionHousePlugin plugin;
    private final AuctionManager manager;

    public AHGui(AuctionHousePlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getAuctionManager();
    }

    public void openBrowse(Player player, int page) {
        List<AuctionListing> listings = new ArrayList<>(manager.getActiveListings());

        listings.sort(
                Comparator.comparingLong(AuctionListing::getCreatedAt)
                        .reversed()
        );

        int perPage = Math.max(
                1,
                plugin.getConfig().getInt("settings.items-per-page", 45)
        );

        int maxPage = Math.max(
                0,
                (listings.size() - 1) / perPage
        );

        page = Math.max(0, Math.min(page, maxPage));

        Inventory inventory = Bukkit.createInventory(
                new GuiHolder(GuiHolder.Type.BROWSE, page),
                54,
                color("&8Auction House &7- Page " + (page + 1))
        );

        fillBackground(inventory);

        int start = page * perPage;
        int end = Math.min(start + perPage, listings.size());

        int slot = 0;

        for (int i = start; i < end && slot < 45; i++) {
            AuctionListing listing = listings.get(i);

            ItemStack display = createListingItem(listing);
            inventory.setItem(slot, display);

            slot++;
        }

        inventory.setItem(45, button(
                Material.ARROW,
                "&ePrevious Page",
                "&7Click to go to the previous page."
        ));

        inventory.setItem(49, button(
                Material.BOOK,
                "&bAuction House",
                "&7Page: &f" + (page + 1) + "&7/&f" + (maxPage + 1),
                "&7Listings: &f" + listings.size()
        ));

        inventory.setItem(53, button(
                Material.ARROW,
                "&eNext Page",
                "&7Click to go to the next page."
        ));

        player.openInventory(inventory);
    }

    public void openMyListings(Player player, int page) {
        UUID uuid = player.getUniqueId();

        List<AuctionListing> listings = manager.getActiveListings()
                .stream()
                .filter(listing -> listing.getSeller().equals(uuid))
                .sorted(
                        Comparator.comparingLong(AuctionListing::getCreatedAt)
                                .reversed()
                )
                .toList();

        int perPage = Math.max(
                1,
                plugin.getConfig().getInt("settings.items-per-page", 45)
        );

        int maxPage = Math.max(
                0,
                (listings.size() - 1) / perPage
        );

        page = Math.max(0, Math.min(page, maxPage));

        Inventory inventory = Bukkit.createInventory(
                new GuiHolder(GuiHolder.Type.MY_LISTINGS, page),
                54,
                color("&8My Auctions &7- Page " + (page + 1))
        );

        fillBackground(inventory);

        int start = page * perPage;
        int end = Math.min(start + perPage, listings.size());

        int slot = 0;

        for (int i = start; i < end && slot < 45; i++) {
            AuctionListing listing = listings.get(i);

            ItemStack item = createListingItem(listing);

            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                List<String> lore = meta.hasLore()
                        ? new ArrayList<>(meta.getLore())
                        : new ArrayList<>();

                lore.add("");
                lore.add(color("&cClick to cancel this listing."));

                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inventory.setItem(slot, item);
            slot++;
        }

        inventory.setItem(45, button(
                Material.ARROW,
                "&ePrevious Page",
                "&7Go to the previous page."
        ));

        inventory.setItem(49, button(
                Material.CHEST,
                "&bMy Auctions",
                "&7Active listings: &f" + listings.size()
        ));

        inventory.setItem(53, button(
                Material.ARROW,
                "&eNext Page",
                "&7Go to the next page."
        ));

        player.openInventory(inventory);
    }

    public void openSell(Player player) {
        Inventory inventory = Bukkit.createInventory(
                new GuiHolder(GuiHolder.Type.SELL, 0),
                27,
                color("&8Sell Item")
        );

        fillBackground(inventory);

        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType() != Material.AIR) {
            ItemStack item = hand.clone();

            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                List<String> lore = meta.hasLore()
                        ? new ArrayList<>(meta.getLore())
                        : new ArrayList<>();

                lore.add("");
                lore.add(color("&eSelected Item"));
                lore.add(color("&7Amount: &f" + hand.getAmount()));
                lore.add("");
                lore.add(color("&aClick to set price."));

                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inventory.setItem(13, item);
        } else {
            inventory.setItem(13, button(
                    Material.BARRIER,
                    "&cNo Item",
                    "&7Hold an item in your main hand."
            ));
        }

        inventory.setItem(11, button(
                Material.EMERALD,
                "&aSell Item",
                "&7Hold an item and click here.",
                "&7You will be asked for a price."
        ));

        inventory.setItem(15, button(
                Material.ARROW,
                "&cBack",
                "&7Return to Auction House."
        ));

        player.openInventory(inventory);
    }

    public void openClaims(Player player, int page) {
        Inventory inventory = Bukkit.createInventory(
                new GuiHolder(GuiHolder.Type.CLAIMS, page),
                54,
                color("&8Auction Claims")
        );

        fillBackground(inventory);

        inventory.setItem(49, button(
                Material.CHEST,
                "&bAuction Claims",
                "&7Items waiting for collection."
        ));

        inventory.setItem(45, button(
                Material.ARROW,
                "&ePrevious Page",
                "&7Previous page."
        ));

        inventory.setItem(53, button(
                Material.ARROW,
                "&eNext Page",
                "&7Next page."
        ));

        player.openInventory(inventory);
    }

    public void handleBrowseClick(
            Player player,
            int slot,
            boolean rightClick,
            boolean shiftClick,
            boolean doubleClick
    ) {
        if (slot < 0) {
            return;
        }

        if (slot == 45) {
            openBrowse(player, Math.max(
                    0,
                    getCurrentPage(player) - 1
            ));
            return;
        }

        if (slot == 53) {
            openBrowse(player, getCurrentPage(player) + 1);
            return;
        }

        if (slot >= 45) {
            return;
        }

        AuctionListing listing = getListingForSlot(player, slot);

        if (listing == null) {
            return;
        }

        if (listing.isExpired()) {
            player.sendMessage(color("&cThis auction has expired."));
            openBrowse(player, getCurrentPage(player));
            return;
        }

        if (listing.getSeller().equals(player.getUniqueId())
                && !plugin.getConfig().getBoolean(
                "auction.allow-self-purchase",
                false
        )) {
            player.sendMessage(color("&cYou cannot buy your own listing."));
            return;
        }

        if (!manager.removeListing(listing.getId())) {
            player.sendMessage(color("&cThis listing is no longer available."));
            openBrowse(player, getCurrentPage(player));
            return;
        }

        player.getInventory().addItem(listing.getItem().clone());

        player.sendMessage(color(
                "&aPurchased &f"
                        + listing.getItem().getAmount()
                        + "x "
                        + prettyMaterial(listing.getItem().getType())
                        + " &afor &e"
                        + PriceFormatter.format(listing.getPrice())
        ));

        Player seller = Bukkit.getPlayer(listing.getSeller());

        if (seller != null && seller.isOnline()) {
            seller.sendMessage(color(
                    "&aYour auction was purchased for &e"
                            + PriceFormatter.format(listing.getPrice())
            ));
        }

        openBrowse(player, getCurrentPage(player));
    }

    private AuctionListing getListingForSlot(Player player, int slot) {
        List<AuctionListing> listings = new ArrayList<>(
                manager.getActiveListings()
        );

        listings.sort(
                Comparator.comparingLong(AuctionListing::getCreatedAt)
                        .reversed()
        );

        int perPage = Math.max(
                1,
                plugin.getConfig().getInt("settings.items-per-page", 45)
        );

        int page = getCurrentPage(player);
        int index = page * perPage + slot;

        if (index < 0 || index >= listings.size()) {
            return null;
        }

        return listings.get(index);
    }

    private int getCurrentPage(Player player) {
        if (!(player.getOpenInventory().getTopInventory().getHolder()
                instanceof GuiHolder holder)) {
            return 0;
        }

        return holder.getPage();
    }

    private ItemStack createListingItem(AuctionListing listing) {
        ItemStack item = listing.getItem().clone();

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        List<String> lore = meta.hasLore()
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();

        lore.add("");
        lore.add(color("&7Seller: &f" + listing.getSellerName()));
        lore.add(color("&7Price: &e" + PriceFormatter.format(
                listing.getPrice()
        )));
        lore.add(color("&7Amount: &f" + listing.getItem().getAmount()));
        lore.add(color("&7Time Left: &f" + listing.getRemainingFormatted()));

        lore.add("");
        lore.add(color("&aClick to purchase"));

        meta.setLore(lore);

        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS
        );

        item.setItemMeta(meta);

        return item;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

        ItemMeta meta = filler.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }
    }

    private ItemStack button(
            Material material,
            String name,
            String... lore
    ) {
        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(color(name));

            List<String> lines = new ArrayList<>();

            for (String line : lore) {
                lines.add(color(line));
            }

            meta.setLore(lines);
            item.setItemMeta(meta);
        }

        return item;
    }

    private String prettyMaterial(Material material) {
        String raw = material.name().toLowerCase().replace('_', ' ');

        String[] parts = raw.split(" ");

        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(
                    Character.toUpperCase(part.charAt(0))
            );

            if (part.length() > 1) {
                result.append(part.substring(1));
            }
        }

        return result.toString();
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
                           }
