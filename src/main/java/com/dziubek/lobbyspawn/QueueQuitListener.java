package com.dziubek.lobbyspawn;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QueueQuitListener implements Listener {

    private final LobbySpawnPlugin plugin;

    public QueueQuitListener(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getQueue().removeFromAll(event.getPlayer().getUniqueId());
    }
}
