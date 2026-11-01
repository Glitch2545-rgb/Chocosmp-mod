package com.chocosmp;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChocoSMP extends JavaPlugin {
    private static final String PREFIX = "<gold><bold>ChocoSMP</bold> <dark_gray>» <gray>";

    private Economy economy;
    private ShopManager shop;
    private StashManager stash;
    private SidebarManager sidebar;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("shop.yml", false);

        if (!setupEconomy()) {
            getLogger().severe("Vault ekonomisi bulunamadı! EssentialsX ve Vault kurulu olmalı.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        shop = new ShopManager(this);
        stash = new StashManager(this);

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        sidebar = new SidebarManager(this);
        getServer().getPluginManager().registerEvents(sidebar, this);
        sidebar.start();

        Commands cmds = new Commands(this);
        for (String name : new String[]{"shop", "sell", "stash", "spawn", "setspawn"}) {
            if (getCommand(name) != null) getCommand(name).setExecutor(cmds);
        }
        getLogger().info("ChocoSMP açıldı.");
    }

    @Override
    public void onDisable() {
        if (sidebar != null) sidebar.stop();
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public void msg(CommandSender to, String mini) {
        to.sendMessage(Text.c(PREFIX + mini));
    }

    public Economy economy() { return economy; }
    public ShopManager shop() { return shop; }
    public StashManager stash() { return stash; }
}
