package com.chocosmp;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class Gui implements InventoryHolder {
    public enum Type { MAIN, CATEGORY, STASH }

    public final Type type;
    public final String data;
    private Inventory inventory;

    public Gui(Type type, String data) {
        this.type = type;
        this.data = data;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
