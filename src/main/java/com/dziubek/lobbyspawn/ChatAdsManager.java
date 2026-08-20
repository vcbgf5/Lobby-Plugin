package com.dziubek.lobbyspawn;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Boss bar/HUD nie obsługują kliknięć w vanilla Minecraft - to ograniczenie protokołu.
 * Ta klasa rozgłasza rzadziej te same typy reklam jako prawdziwie klikalną wiadomość
 * na czacie (otwarcie linku albo wykonanie komendy).
 */
public class ChatAdsManager {

    private record Ad(Component text, String url, String command) {
    }

    private final LobbySpawnPlugin plugin;
    private final List<Ad> ads = new ArrayList<>();
    private int currentIndex = 0;
    private BukkitTask task;

    public ChatAdsManager(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        reload();
    }

    /**
     * Zatrzymuje pętlę i wczytuje reklamy od nowa z configu - używane też przez /reloadlobby.
     */
    public void reload() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        ads.clear();
        currentIndex = 0;

        if (!plugin.getConfig().getBoolean("chat-ads.enabled", true)) {
            return;
        }

        for (Map<?, ?> entry : plugin.getConfig().getMapList("chat-ads.messages")) {
            Object rawText = entry.get("text");
            if (rawText == null) {
                continue;
            }
            Component text = LegacyComponentSerializer.legacySection().deserialize(rawText.toString());
            Object rawUrl = entry.get("url");
            Object rawCommand = entry.get("command");
            String url = rawUrl != null ? rawUrl.toString() : null;
            String command = rawCommand != null ? rawCommand.toString() : null;
            ads.add(new Ad(text, url, command));
        }

        if (ads.isEmpty()) {
            return;
        }

        int intervalSeconds = Math.max(30, plugin.getConfig().getInt("chat-ads.interval-seconds", 300));
        long ticks = 20L * intervalSeconds;
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::broadcast, ticks, ticks);
    }

    private void broadcast() {
        if (ads.isEmpty()) {
            return;
        }

        Ad ad = ads.get(currentIndex);
        currentIndex = (currentIndex + 1) % ads.size();

        Component message = ad.text();
        if (ad.url() != null) {
            message = message.clickEvent(ClickEvent.openUrl(ad.url()))
                    .hoverEvent(HoverEvent.showText(Component.text("Kliknij, aby otworzyć")));
        } else if (ad.command() != null) {
            message = message.clickEvent(ClickEvent.runCommand(ad.command()))
                    .hoverEvent(HoverEvent.showText(Component.text("Kliknij, aby wykonać: " + ad.command())));
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }
}
