package com.kingbrezz.auctionhouse;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class AuctionManager {

    public enum PurchaseResult {
        SUCCESS,
        NOT_FOUND,
        EXPIRED,
        OWN_LISTING,
        NO_ECONOMY,
        NOT_ENOUGH_MONEY,
        PAYMENT_FAILED,
        INVENTORY_FULL,
        INTERNAL_ERROR
    }

    public record PurchaseOutcome(
            PurchaseResult result,
            AuctionListing listing,
            double sellerAmount
    ) {
        public static PurchaseOutcome fail(
                PurchaseResult result
        ) {
            return new PurchaseOutcome(
                    result,
                    null,
                    0D
            );
        }
    }

    private final AuctionHousePlugin plugin;
    private final Map<String, AuctionListing> listings =
            new LinkedHashMap<>();

    private File file;
    private YamlConfiguration data;
    private ScheduledTask expiryTask;

    public AuctionManager(AuctionHousePlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public synchronized void load() {
        file = new File(
                plugin.getDataFolder(),
                "listings.yml"
        );

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().severe(
                        "Could not create listings.yml: "
                                + exception.getMessage()
                );
            }
        }

        data =
                YamlConfiguration.loadConfiguration(file);

        listings.clear();

        ConfigurationSection section =
                data.getConfigurationSection(
                        "listings"
                );

        if (section == null) {
            return;
        }

        boolean changed = false;

        for (String id : section.getKeys(false)) {
            ConfigurationSection node =
                    section.getConfigurationSection(id);

            if (node == null) {
                continue;
            }

            try {
                String sellerText =
                        node.getString("seller");

                if (sellerText == null) {
                    continue;
                }

                UUID seller =
                        UUID.fromString(sellerText);

                ItemStack item =
                        node.getItemStack("item");

                double price =
                        node.getDouble("price");

                long created =
                        node.getLong("created");

                long expires =
                        node.getLong("expires");

                if (item == null
                        || item.getType().isAir()
                        || item.getAmount() <= 0
                        || !Double.isFinite(price)
                        || created <= 0
                        || expires <= created) {
                    plugin.getLogger().warning(
                            "Skipping invalid auction: " + id
                    );
                    continue;
                }

                AuctionListing listing =
                        new AuctionListing(
                                id,
                                seller,
                                node.getString(
                                        "seller-name",
                                        "Unknown"
                                ),
                                item,
                                price,
                                created,
                                expires
                        );

                if (listing.isExpired()) {
                    moveExpiredToClaims(listing);
                    changed = true;
                    continue;
                }

                listings.put(id, listing);

            } catch (Exception exception) {
                plugin.getLogger().warning(
                        "Could not load auction "
                                + id
                                + ": "
                                + exception.getMessage()
                );
            }
        }

        if (changed) {
            save();
        }
    }

    public synchronized void save() {
        if (data == null || file == null) {
            return;
        }

        data.set("listings", null);

        for (AuctionListing listing :
                listings.values()) {

            String path =
                    "listings."
                            + listing.getId();

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

    public synchronized boolean addListing(
            Player seller,
            ItemStack item,
            double price
    ) {
        if (seller == null
                || item == null
                || item.getType().isAir()
                || item.getAmount() <= 0) {
            return false;
        }

        if (!plugin.isAuctionHouseEnabled()) {
            return false;
        }

        if (!isPriceAllowed(price)
                || !canList(seller)) {
            return false;
        }

        long now =
                System.currentTimeMillis();

        long durationHours =
                Math.max(
                        1L,
                        plugin.getConfig().getLong(
                                "settings.listing-duration-hours",
                                48L
                        )
                );

        long durationMillis =
                Math.multiplyExact(
                        Math.multiplyExact(
                                durationHours,
                                60_000L
                        ),
                        60L
                );

        String id =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12);

        AuctionListing listing =
                new AuctionListing(
                        id,
                        seller.getUniqueId(),
                        seller.getName(),
                        item,
                        price,
                        now,
                        now + durationMillis
                );

        listings.put(id, listing);
        save();

        return true;
    }

    public synchronized boolean removeListing(
            String id
    ) {
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

    public synchronized AuctionListing getListing(
            String id
    ) {
        return id == null
                ? null
                : listings.get(id);
    }

    public synchronized Collection<AuctionListing>
    getActiveListings() {
        return new ArrayList<>(
                listings.values()
        );
    }

    public synchronized List<AuctionListing>
    getSortedListings(
            GuiHolder.SortMode sortMode,
            String search
    ) {
        String query =
                search == null
                        ? ""
                        : search.trim()
                        .toLowerCase(Locale.ROOT);

        List<AuctionListing> result =
                new ArrayList<>();

        for (AuctionListing listing :
                listings.values()) {

            if (listing.isExpired()) {
                continue;
            }

            if (!query.isEmpty()
                    && !listing.getSellerName()
                    .toLowerCase(Locale.ROOT)
                    .contains(query)
                    && !listing.getItem()
                    .getType()
                    .name()
                    .toLowerCase(Locale.ROOT)
                    .contains(query)) {
                continue;
            }

            result.add(listing);
        }

        GuiHolder.SortMode mode =
                sortMode == null
                        ? GuiHolder.SortMode.NEWEST
                        : sortMode;

        Comparator<AuctionListing> comparator;

        switch (mode) {
            case PRICE_LOWEST ->
                    comparator =
                            Comparator.comparingDouble(
                                    AuctionListing::getPrice
                            );

            case PRICE_HIGHEST ->
                    comparator =
                            Comparator.comparingDouble(
                                    AuctionListing::getPrice
                            ).reversed();

            case EXPIRING_SOON ->
                    comparator =
                            Comparator.comparingLong(
                                    AuctionListing::getExpiresAt
                            );

            case OLDEST ->
                    comparator =
                            Comparator.comparingLong(
                                    AuctionListing::getCreatedAt
                            );

            default ->
                    comparator =
                            Comparator.comparingLong(
                                    AuctionListing::getCreatedAt
                            ).reversed();
        }

        result.sort(comparator);
        return result;
    }

    public synchronized int getPlayerListingCount(
            UUID uuid
    ) {
        if (uuid == null) {
            return 0;
        }

        int count = 0;

        for (AuctionListing listing :
                listings.values()) {

            if (listing.getSeller().equals(uuid)
                    && !listing.isExpired()) {
                count++;
            }
        }

        return count;
    }

    public synchronized boolean canList(
            Player player
    ) {
        return player != null
                && getPlayerListingCount(
                player.getUniqueId()
        ) < getListingLimit(player);
    }

    public int getListingLimit(
            Player player
    ) {
        if (player == null) {
            return 0;
        }

        ConfigurationSection limits =
                plugin.getConfig()
                        .getConfigurationSection(
                                "settings.listing-limits"
                        );

        int result =
                plugin.getConfig().getInt(
                        "settings.default-listing-limit",
                        5
                );

        if (limits == null) {
            return result;
        }

        for (String permission :
                limits.getKeys(false)) {

            if (player.hasPermission(permission)) {
                result = Math.max(
                        result,
                        limits.getInt(permission)
                );
            }
        }

        return Math.max(0, result);
    }

    public boolean isPriceAllowed(
            double price
    ) {
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

        return PriceFormatter.isValidRange(
                price,
                minimum,
                maximum
        );
    }

    public synchronized PurchaseOutcome purchase(
            Player buyer,
            String id
    ) {
        if (buyer == null || id == null) {
            return PurchaseOutcome.fail(
                    PurchaseResult.INTERNAL_ERROR
            );
        }

        AuctionListing listing = listings.get(id);

        if (listing == null) {
            return PurchaseOutcome.fail(
                    PurchaseResult.NOT_FOUND
            );
        }

        if (listing.isExpired()) {
            listings.remove(id);
            moveExpiredToClaims(listing);
            save();
            notifyExpiredSeller(listing);

            return PurchaseOutcome.fail(
                    PurchaseResult.EXPIRED
            );
        }

        if (!plugin.getConfig().getBoolean(
                "auction.allow-self-purchase",
                false
        )
                && listing.getSeller()
                .equals(buyer.getUniqueId())) {
            return PurchaseOutcome.fail(
                    PurchaseResult.OWN_LISTING
            );
        }

        EconomyHook economy =
                plugin.getEconomyHook();

        if (!economy.isAvailable()) {
            return PurchaseOutcome.fail(
                    PurchaseResult.NO_ECONOMY
            );
        }

        ItemStack item =
                listing.getItem();

        if (!hasInventorySpace(
                buyer,
                item
        )) {
            return PurchaseOutcome.fail(
                    PurchaseResult.INVENTORY_FULL
            );
        }

        double price =
                listing.getPrice();

        if (!economy.has(
                buyer,
                price
        )) {
            return PurchaseOutcome.fail(
                    PurchaseResult.NOT_ENOUGH_MONEY
            );
        }

        if (!economy.withdraw(
                buyer,
                price
        )) {
            return PurchaseOutcome.fail(
                    PurchaseResult.PAYMENT_FAILED
            );
        }

        double tax =
                Math.max(
                        0D,
                        plugin.getConfig().getDouble(
                                "settings.tax-percent",
                                5D
                        )
                );

        double sellerAmount =
                Math.max(
                        0D,
                        price - (price * tax / 100D)
                );

        /*
         * Reserve the listing before payout so another buyer cannot
         * purchase the same item while this transaction is in progress.
         */
        listings.remove(id);

        OfflinePlayer seller =
                Bukkit.getOfflinePlayer(
                        listing.getSeller()
                );

        boolean sellerPaid =
                sellerAmount <= 0D
                        || economy.deposit(
                        seller,
                        sellerAmount
                );

        if (!sellerPaid) {
            economy.deposit(
                    buyer,
                    price
            );

            listings.put(
                    id,
                    listing
            );

            save();

            plugin.getLogger().severe(
                    "Could not pay seller for auction " + id
            );

            return PurchaseOutcome.fail(
                    PurchaseResult.INTERNAL_ERROR
            );
        }

        try {
            if (!giveItem(
                    buyer,
                    item
            )) {
                /*
                 * Best-effort rollback: refund buyer and reverse seller
                 * payout. The listing is restored before returning.
                 */
                boolean sellerReversed =
                        sellerAmount <= 0D
                                || economy.withdraw(
                                seller,
                                sellerAmount
                        );

                economy.deposit(
                        buyer,
                        price
                );

                listings.put(
                        id,
                        listing
                );

                save();

                if (!sellerReversed) {
                    plugin.getLogger().severe(
                            "Could not reverse seller payout for auction "
                                    + id
                                    + ". Manual economy correction may be required."
                    );
                }

                return PurchaseOutcome.fail(
                        PurchaseResult.INVENTORY_FULL
                );
            }

            save();

            return new PurchaseOutcome(
                    PurchaseResult.SUCCESS,
                    listing,
                    sellerAmount
            );

        } catch (Exception exception) {
            boolean sellerReversed =
                    sellerAmount <= 0D
                            || economy.withdraw(
                            seller,
                            sellerAmount
                    );

            economy.deposit(
                    buyer,
                    price
            );

            listings.put(
                    id,
                    listing
            );

            save();

            if (!sellerReversed) {
                plugin.getLogger().severe(
                        "Could not reverse seller payout after exception for auction "
                                + id
                );
            }

            plugin.getLogger().severe(
                    "Purchase failed for auction "
                            + id
                            + ": "
                            + exception.getMessage()
            );

            return PurchaseOutcome.fail(
                    PurchaseResult.INTERNAL_ERROR
            );
        }
    }

    public void startExpiryTask() {
        stopExpiryTask();

        if (!plugin.isAuctionHouseEnabled()) {
            return;
        }

        long seconds =
                Math.max(
                        5L,
                        plugin.getConfig().getLong(
                                "settings.expire-check-seconds",
                                30L
                        )
                );

        expiryTask =
                plugin.getServer()
                        .getGlobalRegionScheduler()
                        .runAtFixedRate(
                                plugin,
                                task -> removeExpired(),
                                seconds * 20L,
                                seconds * 20L
                        );
    }

    public void stopExpiryTask() {
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }
    }

    private synchronized void removeExpired() {
        boolean changed = false;

        for (AuctionListing listing :
                new ArrayList<>(
                        listings.values()
                )) {

            if (!listing.isExpired()) {
                continue;
            }

            listings.remove(
                    listing.getId()
            );

            moveExpiredToClaims(
                    listing
            );

            notifyExpiredSeller(
                    listing
            );

            changed = true;
        }

        if (changed) {
            save();
        }
    }

    private void moveExpiredToClaims(
            AuctionListing listing
    ) {
        if (!plugin.getConfig().getBoolean(
                "auction.expired-to-claims",
                true
        )) {
            return;
        }

        ClaimManager claimManager =
                plugin.getClaimManager();

        if (claimManager != null) {
            claimManager.addItem(
                    listing.getSeller(),
                    listing.getItem(),
                    "Expired auction"
            );
        }
    }

    private void notifyExpiredSeller(
            AuctionListing listing
    ) {
        Player seller =
                Bukkit.getPlayer(
                        listing.getSeller()
                );

        if (seller == null || !seller.isOnline()) {
            return;
        }

        seller.getScheduler().run(
                plugin,
                task -> plugin.getMessages().send(
                        seller,
                        "listing.expired",
                        "&eYour auction expired and the item was moved to Claims."
                ),
                null
        );
    }

    private boolean hasInventorySpace(
            Player player,
            ItemStack item
    ) {
        int remaining =
                item.getAmount();

        for (ItemStack current :
                player.getInventory().getStorageContents()) {

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

    private boolean giveItem(
            Player player,
            ItemStack item
    ) {
        return player.getInventory()
                .addItem(
                        item.clone()
                )
                .isEmpty();
    }

    public synchronized void shutdown() {
        stopExpiryTask();
        save();
    }
}
