package com.dziubek.lobbyspawn;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class BossBarAdsManager implements Listener {

    private final LobbySpawnPlugin plugin;
    private BossBar bossBar;
    private List<String> messages;
    private int currentIndex = 0;
    private double progress = 1.0;
    private int intervalSeconds;

    public BossBarAdsManager(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("bossbar-ads.enabled", true)) {
            return;
        }

        messages = plugin.getConfig().getStringList("bossbar-ads.messages");
        if (messages.isEmpty()) {
            plugin.getLogger().warning("bossbar-ads.messages jest puste - bossbar wyłączony.");
            return;
        }

        intervalSeconds = Math.max(1, plugin.getConfig().getInt("bossbar-ads.interval-seconds", 8));

        BarColor color = parseColor(plugin.getConfig().getString("bossbar-ads.color", "YELLOW"));
        BarStyle style = parseStyle(plugin.getConfig().getString("bossbar-ads.style", "SEGMENTED_10"));

        bossBar = Bukkit.createBossBar(ChatColor.translateAlternateColorCodes('&', messages.get(0)), color, style);
        bossBar.setProgress(1.0);

        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        if (bossBar == null || messages.isEmpty()) {
            return;
        }

        double step = 1.0 / intervalSeconds;
        progress -= step;

        if (progress <= 0) {
            currentIndex = (currentIndex + 1) % messages.size();
            progress = 1.0;
            bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', messages.get(currentIndex)));
        }

        bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (bossBar != null) {
            bossBar.addPlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (bossBar != null) {
            bossBar.removePlayer(event.getPlayer());
        }
    }

    private BarColor parseColor(String raw) {
        try {
            return BarColor.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.YELLOW;
        }
    }

    private BarStyle parseStyle(String raw) {
        try {
            return BarStyle.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarStyle.SEGMENTED_10;
        }
    }
}
