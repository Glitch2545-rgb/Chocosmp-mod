package com.chocosmp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;

public final class GuiListener implements Listener {
    private final ChocoSMP plugin;

    public GuiListener(ChocoSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof Gui gui)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(top)) return;

        final int slot = e.getSlot();
        final ClickType click = e.getClick();
        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (gui.type) {
                case MAIN -> plugin.shop().handleMainClick(p, slot);
                case CATEGORY -> plugin.shop().handleCategoryClick(p, gui.data, slot, click);
                case STASH -> plugin.stash().handleClick(p, slot);
            }
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Gui) e.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int n = plugin.stash().count(p.getUniqueId());
            if (p.isOnline() && n > 0) {
                plugin.msg(p, "<yellow>Stash'inde <white>" + n + "<yellow> item var! <light_purple>/stash");
            }
        }, 40L);
    }
}
