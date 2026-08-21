package com.dziubek.lobbyspawn;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

/**
 * Chowa HUD-y LobbySpawn (reklama, status kolejki), gdy gracz ma otwarty jakikolwiek ekran
 * (ekwipunek, skrzynia, kowadlo, kraft, ...), żeby się na niego nie nakładały, i przywraca je
 * po zamknięciu. Rejestrowany tylko gdy LobbySpawnPlugin.isHudEnginePresent().
 */
public class HudAutoHideListener implements Listener {

    private final LobbySpawnPlugin plugin;

    public HudAutoHideListener(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        HudEngineBridge.hideAdsHud(player);
        HudEngineBridge.hideQueueStatusHud(player);
        HudEngineBridge.hideTeleportNotice(player);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !player.isOnline()) {
            return;
        }
        HudEngineBridge.showAdsHud(player);
        if (plugin.getQueue().getActiveQueueServer(player.getUniqueId()) != null) {
            HudEngineBridge.showQueueStatusHud(player);
        }
    }
}
