package com.kingbrezz.auctionhouse;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class GuiHolder implements InventoryHolder {

    public enum Type {
        BROWSE,
        MY_LISTINGS,
        CLAIMS,
        SELL
    }

    public enum SortMode {
        NEWEST,
        PRICE_LOWEST,
        PRICE_HIGHEST,
        EXPIRING_SOON,
        OLDEST;

        public SortMode next() {
            return switch (this) {
                case NEWEST -> PRICE_LOWEST;
                case PRICE_LOWEST -> PRICE_HIGHEST;
                case PRICE_HIGHEST -> EXPIRING_SOON;
                case EXPIRING_SOON -> OLDEST;
                case OLDEST -> NEWEST;
            };
        }

        public String displayName() {
            return switch (this) {
                case NEWEST -> "Newest";
                case PRICE_LOWEST -> "Price Lowest";
                case PRICE_HIGHEST -> "Price Highest";
                case EXPIRING_SOON -> "Expiring Soon";
                case OLDEST -> "Oldest";
            };
        }
    }

    private final Type type;
    private final int page;
    private final SortMode sortMode;
    private final String search;

    public GuiHolder(
            Type type,
            int page
    ) {
        this(type, page, SortMode.NEWEST, "");
    }

    public GuiHolder(
            Type type,
            int page,
            SortMode sortMode,
            String search
    ) {
        this.type = type;
        this.page = Math.max(0, page);
        this.sortMode = sortMode == null
                ? SortMode.NEWEST
                : sortMode;
        this.search = search == null
                ? ""
                : search;
    }

    public Type getType() {
        return type;
    }

    public int getPage() {
        return page;
    }

    public SortMode getSortMode() {
        return sortMode;
    }

    public String getSearch() {
        return search;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
