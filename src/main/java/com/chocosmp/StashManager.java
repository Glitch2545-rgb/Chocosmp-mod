package com.chocosmp;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StashManager {
    private final ChocoSMP plugin;
    private final File file;
    private final Map<UUID, List<ItemStack>> data = new HashMap<>();

    public StashManager(ChocoSMP plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stash.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String key : y.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            List<ItemStack> list = new ArrayList<>();
            for (String s : y.getStringList(key)) {
                try {
                    list.add(ItemStack.deserializeBytes(Base64.getDecoder().decode(s)));
                } catch (Exception ex) {
                    plugin.getLogger().warning("Stash'ten bir item okunamadı (" + key + ")");
                }
            }
            data.put(id, list);
        }
    }

    private synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, List<ItemStack>> e : data.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            List<String> out = new ArrayList<>();
            for (ItemStack it : e.getValue()) {
                out.add(Base64.getEncoder().encodeToString(it.serializeAsBytes()));
            }
            y.set(e.getKey().toString(), out);
        }
        try {
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("stash.yml kaydedilemedi: " + ex.getMessage());
        }
    }

    public int count(UUID id) {
        return data.getOrDefault(id, List.of()).size();
    }

    public void add(UUID id, ItemStack item) {
        data.computeIfAbsent(id, k -> new ArrayList<>()).add(item.clone());
        save();
    }

    public void open(Player p) {
        Gui gui = new Gui(Gui.Type.STASH, "");
        Inventory inv = Bukkit.createInventory(gui, 54, Text.menu(true, "<light_purple><bold>Stash"));
        gui.setInventory(inv);
        List<ItemStack> list = data.getOrDefault(p.getUniqueId(), List.of());
        for (int i = 0; i < list.size() && i < 54; i++) {
            ItemStack it = list.get(i).clone();
            ItemMeta meta = it.getItemMeta();
            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Text.c(" "));
            lore.add(Text.c("<yellow>Tıkla: envanterine al"));
            meta.lore(lore);
            it.setItemMeta(meta);
            inv.setItem(i, it);
        }
        p.openInventory(inv);
    }

    public void handleClick(Player p, int slot) {
        List<ItemStack> list = data.get(p.getUniqueId());
        if (list == null || slot < 0 || slot >= list.size()) return;
        ItemStack it = list.remove(slot);
        Map<Integer, ItemStack> left = p.getInventory().addItem(it);
        if (!left.isEmpty()) {
            list.add(slot, left.values().iterator().next());
            plugin.msg(p, "<red>Envanterin dolu!");
        }
        save();
        open(p);
    }

    /** Yönetici: eldeki itemi bir oyuncunun stash'ine koyar. */
    public boolean giveHeld(Player admin, Player target) {
        ItemStack hand = admin.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) return false;
        add(target.getUniqueId(), hand);
        admin.getInventory().setItemInMainHand(null);
        return true;
    }
}
