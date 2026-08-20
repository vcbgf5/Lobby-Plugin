package com.dziubek.lobbyspawn;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinTeleportListener implements Listener {

    private final LobbySpawnPlugin plugin;

    public JoinTeleportListener(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.hasSpawn()) {
            return;
        }

        Player player = event.getPlayer();
        // 1 tick opóźnienia, żeby gracz był w pełni załadowany zanim go teleportujemy
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.teleport(plugin.getSpawnLocation());
            }
        });
    }
}
