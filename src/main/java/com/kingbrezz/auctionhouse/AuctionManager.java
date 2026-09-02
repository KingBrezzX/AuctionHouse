package com.kingbrezz.auctionhouse;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class AuctionManager {

    private final AuctionHousePlugin plugin;

    private final Map<String, AuctionListing> listings =
            new LinkedHashMap<>();

    private File file;
    private YamlConfiguration data;
    private BukkitTask expiryTask;

    public AuctionManager(AuctionHousePlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "listings.yml");

        if (!file.exists()) {
            try {
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }

                file.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().severe(
                        "Could not create listings.yml: "
                                + exception.getMessage()
                );
            }
        }

        data = YamlConfiguration.loadConfiguration(file);
        listings.clear();

        ConfigurationSection section =
                data.getConfigurationSection("listings");

        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection node =
                    section.getConfigurationSection(id);

            if (node == null) {
                continue;
            }

            try {
                UUID seller = UUID.fromString(
                        node.getString("seller")
                );

                String sellerName =
                        node.getString("seller-name", "Unknown");

                ItemStack item =
                        node.getItemStack("item");

                if (item == null || item.getType().isAir()) {
                    continue;
                }

                double price =
                        node.getDouble("price");

                long created =
                        node.getLong("created");

                long expires =
                        node.getLong("expires");

                AuctionListing listing =
                        new AuctionListing(
                                id,
                                seller,
                                sellerName,
                                item,
                                price,
                                created,
                                expires
                        );

                if (!listing.isExpired()) {
                    listings.put(id, listing);
                }

            } catch (Exception exception) {
                plugin.getLogger().warning(
                        "Failed to load listing "
                                + id
                                + ": "
                                + exception.getMessage()
                );
            }
        }

        save();
    }

    public void save() {
        if (data == null) {
            data = new YamlConfiguration();
        }

        data.set("listings", null);

        for (AuctionListing listing : listings.values()) {
            String path = "listings." + listing.getId();

            data.set(
                    path + ".seller",
                    listing.getSeller().toString()
            );

            data.set(
                    path + ".seller-name",
                    listing.getSellerName()
            );

            data.set(
                    path + ".item",
                    listing.getItem()
            );

            data.set(
                    path + ".price",
                    listing.getPrice()
            );

            data.set(
                    path + ".created",
                    listing.getCreatedAt()
            );

            data.set(
                    path + ".expires",
                    listing.getExpiresAt()
            );
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

    public boolean addListing(
            Player seller,
            ItemStack item,
            double price
    ) {
        if (seller == null
                || item == null
                || item.getType().isAir()) {
            return false;
        }

        if (!isPriceAllowed(price)) {
            return false;
        }

        if (!canList(seller)) {
            return false;
        }

        long now = System.currentTimeMillis();

        long hours = plugin.getConfig().getLong(
                "settings.listing-duration-hours",
                48
        );

        long duration = hours * 60L * 60L * 1000L;

        String id = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        AuctionListing listing =
                new AuctionListing(
                        id,
                        seller.getUniqueId(),
                        seller.getName(),
                        item.clone(),
                        price,
                        now,
                        now + duration
                );

        listings.put(id, listing);
        save();

        return true;
    }

    public boolean removeListing(String id) {
        if (id == null) {
            return false;
        }

        AuctionListing removed =
                listings.remove(id);

        if (removed == null) {
            return false;
        }

        save();
        return true;
    }

    public AuctionListing getListing(String id) {
        return listings.get(id);
    }

    public Collection<AuctionListing> getActiveListings() {
        return new ArrayList<>(listings.values());
    }

    public int getPlayerListingCount(UUID uuid) {
        int count = 0;

        for (AuctionListing listing : listings.values()) {
            if (listing.getSeller().equals(uuid)
                    && !listing.isExpired()) {
                count++;
            }
        }

        return count;
    }

    public boolean canList(Player player) {
        return getPlayerListingCount(
                player.getUniqueId()
        ) < getListingLimit(player);
    }

    public int getListingLimit(Player player) {
        ConfigurationSection limits =
                plugin.getConfig()
                        .getConfigurationSection(
                                "settings.listing-limits"
                        );

        int result = plugin.getConfig().getInt(
                "settings.default-listing-limit",
                5
        );

        if (limits == null) {
            return result;
        }

        for (String permission : limits.getKeys(false)) {
            if (player.hasPermission(permission)) {
                result = Math.max(
                        result,
                        limits.getInt(permission)
                );
            }
        }

        return result;
    }

    public boolean isPriceAllowed(double price) {
        double minimum =
                plugin.getConfig().getDouble(
                        "pricing.minimum-price",
                        100
                );

        double maximum =
                plugin.getConfig().getDouble(
                        "pricing.maximum-price",
                        100000000
                );

        return price >= minimum
                && price <= maximum
                && !Double.isNaN(price)
                && !Double.isInfinite(price);
    }

    public void startExpiryTask() {
        if (expiryTask != null) {
            expiryTask.cancel();
        }

        long seconds = Math.max(
                5,
                plugin.getConfig().getLong(
                        "settings.expire-check-seconds",
                        30
                )
        );

        expiryTask = plugin.getServer()
                .getGlobalRegionScheduler()
                .runAtFixedRate(
                        plugin,
                        task -> removeExpired(),
                        seconds * 20L,
                        seconds * 20L
                );
    }

    private void removeExpired() {
        boolean changed = false;

        for (String id : new ArrayList<>(listings.keySet())) {
            AuctionListing listing =
                    listings.get(id);

            if (listing == null) {
                continue;
            }

            if (listing.isExpired()) {
                listings.remove(id);
                changed = true;
            }
        }

        if (changed) {
            save();
        }
    }

    public void shutdown() {
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }

        save();
    }
                }
