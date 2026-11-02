package com.chocosmp;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShopManager {
    public record ShopItem(Material material, double buy, double sell) {}
    public record Category(String id, String name, Material icon, List<ShopItem> items) {}

    private static final int[] CAT_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private static final int STASH_SLOT = 18;
    private static final int SELLALL_SLOT = 26;
    private static final int BACK_SLOT = 49;

    private final ChocoSMP plugin;
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final Map<Material, Double> sellPrices = new EnumMap<>(Material.class);

    public ShopManager(ChocoSMP plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        categories.clear();
        sellPrices.clear();
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "shop.yml"));
        ConfigurationSection cats = cfg.getConfigurationSection("categories");
        if (cats == null) return;
        for (String id : cats.getKeys(false)) {
            ConfigurationSection sec = cats.getConfigurationSection(id);
            if (sec == null) continue;
            Material icon = Material.matchMaterial(sec.getString("icon", "CHEST"));
            if (icon == null) icon = Material.CHEST;
            List<ShopItem> items = new ArrayList<>();
            ConfigurationSection is = sec.getConfigurationSection("items");
            if (is != null) {
                for (String key : is.getKeys(false)) {
                    Material m = Material.matchMaterial(key);
                    if (m == null || !m.isItem()) {
                        plugin.getLogger().warning("shop.yml: bilinmeyen item: " + key);
                        continue;
                    }
                    double buy = is.getDouble(key + ".buy", -1);
                    double sell = is.getDouble(key + ".sell", -1);
                    items.add(new ShopItem(m, buy, sell));
                    if (sell > 0) sellPrices.put(m, sell);
                }
            }
            categories.put(id, new Category(id, sec.getString("name", id), icon, items));
        }
    }

    // ---------- GUI ----------

    private static ItemStack filler() {
        return Text.item(Material.BLACK_STAINED_GLASS_PANE, " ", null);
    }

    public void openMain(Player p) {
        Gui gui = new Gui(Gui.Type.MAIN, "");
        Inventory inv = Bukkit.createInventory(gui, 27, Text.menu(false, "<gold><bold>ChocoSMP Market"));
        gui.setInventory(inv);
        int n = 0;
        for (Category c : categories.values()) {
            if (n >= CAT_SLOTS.length) break;
            inv.setItem(CAT_SLOTS[n++], Text.item(c.icon(), c.name(), List.of("<gray>Tıkla ve aç")));
        }
        inv.setItem(STASH_SLOT, Text.item(Material.ENDER_CHEST, "<light_purple>Stash",
                List.of("<gray>Sığmayan itemlerin burada durur.", "<yellow>Tıkla: stash'i aç")));
        inv.setItem(SELLALL_SLOT, Text.item(Material.GOLD_INGOT, "<yellow>Hepsini Sat",
                List.of("<gray>Envanterindeki satılabilir", "<gray>tüm itemleri satar.", "<yellow>Tıkla: sat")));
        p.openInventory(inv);
    }

    public void openCategory(Player p, Category c) {
        Gui gui = new Gui(Gui.Type.CATEGORY, c.id());
        Inventory inv = Bukkit.createInventory(gui, 54, Text.menu(true, c.name()));
        gui.setInventory(inv);
        for (int i = 0; i < c.items().size() && i < 45; i++) {
            inv.setItem(i, shopIcon(c.items().get(i)));
        }
        inv.setItem(BACK_SLOT, Text.item(Material.ARROW, "<red>Geri", null));
        p.openInventory(inv);
    }

    private ItemStack shopIcon(ShopItem si) {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        if (si.buy() > 0) lore.add("<gray>Alış fiyatı: <green>" + Text.money(si.buy()));
        if (si.sell() > 0) lore.add("<gray>Satış fiyatı: <red>" + Text.money(si.sell()));
        lore.add(" ");
        if (si.buy() > 0) {
            lore.add("<yellow>Sol tık: <white>1 al");
            lore.add("<yellow>Shift + Sol tık: <white>" + si.material().getMaxStackSize() + " al");
        }
        if (si.sell() > 0) {
            lore.add("<yellow>Sağ tık: <white>1 sat");
            lore.add("<yellow>Shift + Sağ tık: <white>hepsini sat");
        }
        return Text.withLore(si.material(), lore);
    }

    public void handleMainClick(Player p, int slot) {
        if (slot == STASH_SLOT) {
            plugin.stash().open(p);
            return;
        }
        if (slot == SELLALL_SLOT) {
            sellAll(p, false);
            return;
        }
        for (int i = 0; i < CAT_SLOTS.length; i++) {
            if (CAT_SLOTS[i] == slot) {
                List<Category> list = new ArrayList<>(categories.values());
                if (i < list.size()) openCategory(p, list.get(i));
                return;
            }
        }
    }

    public void handleCategoryClick(Player p, String id, int slot, ClickType click) {
        Category c = categories.get(id);
        if (c == null) return;
        if (slot == BACK_SLOT) {
            openMain(p);
            return;
        }
        if (slot < 0 || slot >= 45 || slot >= c.items().size()) return;
        ShopItem si = c.items().get(slot);
        switch (click) {
            case LEFT -> buy(p, si, 1);
            case SHIFT_LEFT -> buy(p, si, si.material().getMaxStackSize());
            case RIGHT -> sell(p, si, 1);
            case SHIFT_RIGHT -> sell(p, si, -1);
            default -> { }
        }
    }

    // ---------- Alış / Satış ----------

    public void buy(Player p, ShopItem si, int amount) {
        if (si.buy() <= 0) {
            plugin.msg(p, "<red>Bu item satın alınamaz.");
            return;
        }
        Economy eco = plugin.economy();
        double cost = si.buy() * amount;
        if (!eco.has(p, cost)) {
            plugin.msg(p, "<red>Yetersiz bakiye! Gerekli: <white>" + Text.money(cost));
            return;
        }
        EconomyResponse r = eco.withdrawPlayer(p, cost);
        if (!r.transactionSuccess()) {
            plugin.msg(p, "<red>İşlem başarısız oldu.");
            return;
        }
        Map<Integer, ItemStack> left = p.getInventory().addItem(new ItemStack(si.material(), amount));
        int overflow = 0;
        for (ItemStack l : left.values()) {
            plugin.stash().add(p.getUniqueId(), l);
            overflow += l.getAmount();
        }
        plugin.msg(p, "<green>" + amount + "x " + Text.pretty(si.material()) + " aldın <gray>(" + Text.money(cost) + ")");
        if (overflow > 0) {
            plugin.msg(p, "<yellow>Envanterin doldu, " + overflow + " item <light_purple>/stash<yellow>'e gönderildi.");
        }
    }

    private static boolean isPlain(ItemStack it, Material m) {
        return it != null && it.getType() == m && !it.hasItemMeta();
    }

    public void sell(Player p, ShopItem si, int amount) {
        if (si.sell() <= 0) {
            plugin.msg(p, "<red>Bu item satılamaz.");
            return;
        }
        PlayerInventory inv = p.getInventory();
        int have = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack it = inv.getItem(i);
            if (isPlain(it, si.material())) have += it.getAmount();
        }
        int n = amount < 0 ? have : Math.min(amount, have);
        if (n <= 0) {
            plugin.msg(p, "<red>Envanterinde satılacak item yok.");
            return;
        }
        int left = n;
        for (int i = 0; i < 36 && left > 0; i++) {
            ItemStack it = inv.getItem(i);
            if (!isPlain(it, si.material())) continue;
            int take = Math.min(left, it.getAmount());
            if (take == it.getAmount()) inv.setItem(i, null);
            else it.setAmount(it.getAmount() - take);
            left -= take;
        }
        double total = n * si.sell();
        plugin.economy().depositPlayer(p, total);
        plugin.msg(p, "<green>" + n + "x " + Text.pretty(si.material()) + " sattın <gray>(+" + Text.money(total) + ")");
    }

    public void sellAll(Player p, boolean handOnly) {
        PlayerInventory inv = p.getInventory();
        double total = 0;
        int count = 0;
        if (handOnly) {
            int slot = inv.getHeldItemSlot();
            ItemStack it = inv.getItem(slot);
            if (it != null && !it.hasItemMeta()) {
                Double pr = sellPrices.get(it.getType());
                if (pr != null) {
                    total = pr * it.getAmount();
                    count = it.getAmount();
                    inv.setItem(slot, null);
                }
            }
        } else {
            for (int i = 0; i < 36; i++) {
                ItemStack it = inv.getItem(i);
                if (it == null || it.hasItemMeta()) continue;
                Double pr = sellPrices.get(it.getType());
                if (pr == null) continue;
                total += pr * it.getAmount();
                count += it.getAmount();
                inv.setItem(i, null);
            }
        }
        if (count == 0) {
            plugin.msg(p, "<red>Satılacak item bulunamadı.");
            return;
        }
        plugin.economy().depositPlayer(p, total);
        plugin.msg(p, "<green>" + count + " item sattın <gray>(+" + Text.money(total) + ")");
    }
}
