package com.dziubek.lobbyspawn;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Pokazuje/chowa graczowi HUD reklam zdefiniowany po stronie HUDEngine (HudEngineBridge.ADS_HUD_KEY).
 * Rejestrowany tylko gdy LobbySpawnPlugin.isHudEnginePresent() - patrz LobbySpawnPlugin.onEnable().
 */
public class HudEngineAdsListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        HudEngineBridge.showAdsHud(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        HudEngineBridge.hideAdsHud(event.getPlayer());
    }
}
