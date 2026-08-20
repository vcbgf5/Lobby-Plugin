package com.dziubek.lobbyspawn;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Pokazuje/chowa graczowi HUD reklam zdefiniowany po stronie BetterHud (BetterHudBridge.ADS_HUD_NAME).
 * Rejestrowany tylko gdy LobbySpawnPlugin.isBetterHudPresent() - patrz LobbySpawnPlugin.onEnable().
 */
public class BetterHudAdsListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        BetterHudBridge.showAdsHud(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BetterHudBridge.hideAdsHud(event.getPlayer());
    }
}
