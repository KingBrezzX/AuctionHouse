package com.kingbrezz.auctionhouse;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public final class AuctionManager {

    private final JavaPlugin plugin;
    private final List<AuctionListing> listings = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(0);

    private File file;
    private YamlConfiguration data;

    public AuctionManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        file = new File(plugin.getDataFolder(), "listings.yml");

        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().warning("Could not create listings.yml");
                }
            } catch (IOException exception) {
                plugin.getLogger().severe("Could not create listings.yml: "
                        + exception.getMessage());
            }
        }

        data = YamlConfiguration.loadConfiguration(file);
        listings.clear();

        ConfigurationSection section = data.getConfigurationSection("listings");

        if (section == null) {
            return;
        }

        long highestId = 0;

        for (String key : section.getKeys(false)) {
            try {
                long id = Long.parseLong(key);

                UUID seller = UUID.fromString(
                        section.getString(key + ".seller")
                );

                String sellerName = section.getString(
                        key + ".seller-name",
                        "Unknown"
                );

                ItemStack item = section.getItemStack(key + ".item");

                double price = section.getDouble(
                        key + ".price"
                );

                long createdAt = section.getLong(
                        key + ".created-at"
                );

                long expiresAt = section.getLong(
                        key + ".expires-at"
                );

                if (item == null || item.getType().isAir()) {
                    continue;
                }

                AuctionListing listing = new AuctionListing(
                        id,
                        seller,
                        sellerName,
                        item,
                        price,
                        createdAt,
                        expiresAt
                );

                if (!listing.isExpired()) {
                    listings.add(listing);
                    highestId = Math.max(highestId, id);
                }
            } catch (Exception exception) {
                plugin.getLogger().warning(
                        "Failed to load auction " + key + ": "
                                + exception.getMessage()
                );
            }
        }

        idCounter.set(highestId);

        save();
    }

    public synchronized void reload() {
        load();
    }

    public synchronized void save() {
        if (data == null) {
            data = new YamlConfiguration();
        }

        data.set("listings", null);

        for (AuctionListing listing : listings) {
            String path = "listings." + listing.getId();

            data.set(path + ".seller",
                    listing.getSeller().toString());

            data.set(path + ".seller-name",
                    listing.getSellerName());

            data.set(path + ".item",
                    listing.getItem());

            data.set(path + ".price",
                    listing.getPrice());

            data.set(path + ".created-at",
                    listing.getCreatedAt());

            data.set(path + ".expires-at",
                    listing.getExpiresAt());
        }

        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe(
                    "Could not save listings.yml: "
                            + exception.getMessage()
            );
        }
    }

    public synchronized AuctionListing createListing(
            Player seller,
            ItemStack item,
            double price
    ) {
        long now = System.currentTimeMillis();

        long durationHours = plugin.getConfig()
                .getLong("settings.listing-duration-hours", 48L);

        long durationMillis = durationHours * 60L * 60L * 1000L;

        long id = idCounter.incrementAndGet();

        AuctionListing listing = new AuctionListing(
                id,
                seller.getUniqueId(),
                seller.getName(),
                item,
                price,
                now,
                now + durationMillis
        );

        listings.add(listing);
        save();

        return listing;
    }

    public synchronized boolean removeListing(long id) {
        boolean removed = listings.removeIf(
                listing -> listing.getId() == id
        );

        if (removed) {
            save();
        }

        return removed;
    }

    public synchronized AuctionListing getListing(long id) {
        return listings.stream()
                .filter(listing -> listing.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public synchronized List<AuctionListing> getListings() {
        return listings.stream()
                .map(AuctionListing::copy)
                .toList();
    }

    public synchronized List<AuctionListing> getPlayerListings(
            UUID uuid
    ) {
        return listings.stream()
                .filter(listing ->
                        listing.getSeller().equals(uuid)
                )
                .map(AuctionListing::copy)
                .toList();
    }

    public synchronized int getPlayerListingCount(UUID uuid) {
        int count = 0;

        for (AuctionListing listing : listings) {
            if (listing.getSeller().equals(uuid)) {
                count++;
            }
        }

        return count;
    }

    public int getListingLimit(Player player) {
        ConfigurationSection section =
                plugin.getConfig().getConfigurationSection(
                        "settings.listing-limits"
                );

        if (section == null) {
            return plugin.getConfig().getInt(
                    "settings.default-listing-limit",
                    5
            );
        }

        int highest = plugin.getConfig().getInt(
                "settings.default-listing-limit",
                5
        );

        for (String permission : section.getKeys(false)) {
            if (player.hasPermission(permission)) {
                highest = Math.max(
                        highest,
                        section.getInt(permission)
                );
            }
        }

        return highest;
    }

    public boolean canCreateListing(Player player) {
        return getPlayerListingCount(player.getUniqueId())
                < getListingLimit(player);
    }

    public double getMinimumPrice() {
        return plugin.getConfig().getDouble(
                "pricing.minimum-price",
                100D
        );
    }

    public double getMaximumPrice() {
        return plugin.getConfig().getDouble(
                "pricing.maximum-price",
                100_000_000D
        );
    }

    public synchronized void expireListings() {
        boolean changed = false;

        Iterator<AuctionListing> iterator = listings.iterator();

        while (iterator.hasNext()) {
            AuctionListing listing = iterator.next();

            if (!listing.isExpired()) {
                continue;
            }

            iterator.remove();
            changed = true;

            Player player = Bukkit.getPlayer(
                    listing.getSeller()
            );

            if (player != null) {
                player.sendMessage(
                        color(
                                plugin.getConfig().getString(
                                        "messages.expired",
                                        "&eYour auction expired."
                                )
                        )
                );
            }

            // Claims system will receive expired items
            // when the ClaimsManager is added.
        }

        if (changed) {
            save();
        }
    }

    public void startExpiryTask() {
        long period = 20L * 60L;

        Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::expireListings,
                period,
                period
        );
    }

    public void shutdown() {
        save();
    }

    private String color(String message) {
        return message.replace("&", "§");
    }
                  }
