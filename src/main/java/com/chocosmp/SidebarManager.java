package com.chocosmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SidebarManager implements Listener {
    private static final String DEFAULT_TITLE = "<white>\uE000";
    private static final List<String> DEFAULT_LINES = List.of(
            " ",
            "<white>%rank% <yellow>%name%",
            "<white>\uE001 <gray>Para: <green>$%money%",
            "<white>\uE002 <gray>Oyuncular: <aqua>%online%",
            "<white>\uE003 <gray>Gecikme: <green>%ping%ms",
            " ",
            "<gray>chocosmp.funserver.top"
    );
    private static final String ENTRY_CHARS = "0123456789abcdef";

    private final ChocoSMP plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public SidebarManager(ChocoSMP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("sidebar.enabled", true)) return;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (boards.containsKey(p.getUniqueId())) {
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
        boards.clear();
    }

    private List<String> lines() {
        List<String> l = plugin.getConfig().getStringList("sidebar.lines");
        if (l.isEmpty()) l = DEFAULT_LINES;
        return l.size() > 15 ? l.subList(0, 15) : l;
    }

    private void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) update(p);
    }

    private Scoreboard create(int lineCount) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        String title = plugin.getConfig().getString("sidebar.title", DEFAULT_TITLE);
        Objective obj = sb.registerNewObjective("choco", Criteria.DUMMY, Text.c(title));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (int i = 0; i < lineCount; i++) {
            String entry = "\u00a7" + ENTRY_CHARS.charAt(i);
            Team t = sb.registerNewTeam("l" + i);
            t.addEntry(entry);
            obj.getScore(entry).setScore(lineCount - i);
        }
        return sb;
    }

    private void update(Player p) {
        List<String> lines = lines();
        Scoreboard sb = boards.get(p.getUniqueId());
        if (sb == null || sb.getTeams().size() != lines.size()) {
            sb = create(lines.size());
            boards.put(p.getUniqueId(), sb);
        }
        if (p.getScoreboard() != sb) p.setScoreboard(sb);

        String money = Text.shortNum(plugin.economy().getBalance(p));
        String online = String.valueOf(Bukkit.getOnlinePlayers().size());
        String ping = String.valueOf(p.getPing());

        for (int i = 0; i < lines.size(); i++) {
            Team t = sb.getTeam("l" + i);
            if (t == null) continue;
            String line = lines.get(i)
                    .replace("%money%", money)
                    .replace("%online%", online)
                    .replace("%ping%", ping)
                    .replace("%rank%", Text.rankIcon(p))
                    .replace("%name%", p.getName());
            t.prefix(Text.c(line.isEmpty() ? " " : line));
        }

        if (plugin.getConfig().getBoolean("tab.enabled", true)) {
            p.sendPlayerListHeaderAndFooter(
                    Component.text("\uE000", NamedTextColor.WHITE),
                    Text.c("<gray>Oyuncu: <white>" + online + " <dark_gray>| <yellow>chocosmp.funserver.top"));
            String icon = Text.rankIcon(p);
            Component name = Component.text(p.getName(), NamedTextColor.WHITE);
            p.playerListName(icon.isEmpty() ? name
                    : Component.text(icon + " ", NamedTextColor.WHITE).append(name));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        boards.remove(e.getPlayer().getUniqueId());
    }
}
