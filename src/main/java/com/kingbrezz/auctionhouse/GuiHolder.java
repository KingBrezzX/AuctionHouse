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

    private final Type type;
    private final int page;

    public GuiHolder(Type type, int page) {
        this.type = type;
        this.page = page;
    }

    public Type getType() {
        return type;
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
