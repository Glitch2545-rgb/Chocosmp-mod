package com.chocosmp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Commands implements CommandExecutor {
    private final ChocoSMP plugin;

    public Commands(ChocoSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Bu komut sadece oyuncular içindir.");
            return true;
        }
        switch (cmd.getName().toLowerCase()) {
            case "shop" -> plugin.shop().openMain(p);
            case "sell" -> plugin.shop().sellAll(p, args.length > 0 && args[0].equalsIgnoreCase("hand"));
            case "stash" -> {
                if (args.length >= 2 && args[0].equalsIgnoreCase("give")) {
                    if (!p.hasPermission("chocosmp.admin")) {
                        plugin.msg(p, "<red>Yetkin yok.");
                        return true;
                    }
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        plugin.msg(p, "<red>Oyuncu çevrimiçi değil.");
                    } else if (plugin.stash().giveHeld(p, target)) {
                        plugin.msg(p, "<green>Eldeki item " + target.getName() + " oyuncusunun stash'ine gönderildi.");
                    } else {
                        plugin.msg(p, "<red>Elinde item yok.");
                    }
                } else {
                    plugin.stash().open(p);
                }
            }
            case "spawn" -> spawn(p);
            case "setspawn" -> {
                if (!p.hasPermission("chocosmp.admin")) {
                    plugin.msg(p, "<red>Yetkin yok.");
                    return true;
                }
                plugin.getConfig().set("spawn", p.getLocation());
                plugin.saveConfig();
                plugin.msg(p, "<green>Spawn noktası ayarlandı.");
            }
            default -> { }
        }
        return true;
    }

    private void spawn(Player p) {
        Location found = plugin.getConfig().getLocation("spawn");
        final Location dest = found != null ? found : Bukkit.getWorlds().get(0).getSpawnLocation();
        int delay = plugin.getConfig().getInt("spawn-delay-seconds", 3);

        if (delay <= 0 || p.hasPermission("chocosmp.spawn.bypass")) {
            p.teleportAsync(dest);
            plugin.msg(p, "<green>Spawn'a ışınlandın.");
            return;
        }

        final Location start = p.getLocation();
        plugin.msg(p, "<yellow>" + delay + " saniye içinde ışınlanacaksın, hareket etme!");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            Location now = p.getLocation();
            if (now.getBlockX() != start.getBlockX() || now.getBlockY() != start.getBlockY()
                    || now.getBlockZ() != start.getBlockZ()) {
                plugin.msg(p, "<red>Hareket ettin, ışınlanma iptal edildi.");
                return;
            }
            p.teleportAsync(dest);
            plugin.msg(p, "<green>Spawn'a ışınlandın.");
        }, delay * 20L);
    }
}
