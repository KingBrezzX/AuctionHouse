package com.kingbrezz.auctionhouse;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class AuctionListing {

    private final long id;
    private final UUID seller;
    private final String sellerName;

    private final ItemStack item;
    private final double price;

    private final long createdAt;
    private final long expiresAt;

    public AuctionListing(
            long id,
            UUID seller,
            String sellerName,
            ItemStack item,
            double price,
            long createdAt,
            long expiresAt
    ) {
        this.id = id;
        this.seller = seller;
        this.sellerName = sellerName;
        this.item = item.clone();
        this.price = price;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public long getId() {
        return id;
    }

    public UUID getSeller() {
        return seller;
    }

    public String getSellerName() {
        return sellerName;
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public int getAmount() {
        return item.getAmount();
    }

    public double getPrice() {
        return price;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }

    public long getRemainingMillis() {
        return Math.max(
                0L,
                expiresAt - System.currentTimeMillis()
        );
    }

    public String getFormattedPrice() {
        return PriceFormatter.format(price);
    }

    public String getFormattedRemainingTime() {
        long seconds = getRemainingMillis() / 1000L;

        long days = seconds / 86_400L;
        seconds %= 86_400L;

        long hours = seconds / 3_600L;
        seconds %= 3_600L;

        long minutes = seconds / 60L;
        seconds %= 60L;

        if (days > 0) {
            return days + "d " + hours + "h";
        }

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }

        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }

        return seconds + "s";
    }

    public AuctionListing copy() {
        return new AuctionListing(
                id,
                seller,
                sellerName,
                item,
                price,
                createdAt,
                expiresAt
        );
    }
  }
