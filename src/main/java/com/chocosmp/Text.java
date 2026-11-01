package com.chocosmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Text {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {}

    public static Component c(String mini) {
        return MM.deserialize(mini).decoration(TextDecoration.ITALIC, false);
    }

    private static List<Component> lines(List<String> lore) {
        List<Component> out = new ArrayList<>();
        for (String s : lore) out.add(c(s));
        return out;
    }

    public static ItemStack item(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(c(name));
        if (lore != null && !lore.isEmpty()) meta.lore(lines(lore));
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack withLore(Material m, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.lore(lines(lore));
        it.setItemMeta(meta);
        return it;
    }

    public static String money(double v) {
        return String.format(Locale.US, "$%,.2f", v);
    }

    public static String pretty(Material m) {
        return m.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
