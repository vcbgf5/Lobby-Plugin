package com.dziubek.lobbyspawn;

import io.github.nacvark.hudengine.api.HudEngineProvider;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Cały kod odwołujący się do klas HUDEngine jest odizolowany w tej jednej klasie.
 * Java ładuje klasę (a z nią te importy) dopiero przy pierwszym realnym wywołaniu jednej
 * z jej metod - a wołamy je wyłącznie gdy LobbySpawnPlugin.isHudEnginePresent() zwraca true,
 * więc na serwerze bez zainstalowanego HUDEngine nic się nie wysypuje (brak NoClassDefFoundError).
 *
 * Klucze HUD-ów ("lobbyspawn:ads" / "lobbyspawn:queue_spot") trzeba zdefiniować po stronie configu
 * samego HUDEngine (plugins/HUDEngine/huds/...) - tam projektuje się wygląd. Jeśli taki HUD nie
 * istnieje w skompilowanym configu HUDEngine, show()/showFor() po prostu zwraca false i nic się
 * nie dzieje.
 *
 * W przeciwieństwie do BetterHud, wartości HUDEngine to statyczne klucze bez argumentów
 * ([namespace:klucz], patrz HudValues), więc dla kolejki/statusu portalu rejestrujemy osobny
 * klucz per serwer docelowy - registerPortalValues() jest wołane przez PortalManager za każdym
 * razem gdy lista portali się zmienia (start, /reloadlobby, /addserverhologram).
 */
public final class HudEngineBridge {

    public static final String ADS_HUD_KEY = "lobbyspawn:ads";
    public static final String QUEUE_SPOT_HUD_KEY = "lobbyspawn:queue_spot";

    private HudEngineBridge() {
    }

    public static void registerPortalValues(LobbySpawnPlugin plugin, Collection<PortalData> portals) {
        HudEngineProvider.find().ifPresent(hud -> {
            for (PortalData portal : portals) {
                String server = portal.getTargetServer();

                // [lobbyspawn:queue_position_<serwer>] - pozycja gracza w kolejce do danego serwera (albo "-" gdy nie czeka)
                hud.values().register("lobbyspawn:queue_position_" + server, player -> {
                    int position = plugin.getQueue().getQueuePosition(server, player.getUniqueId());
                    return position > 0 ? String.valueOf(position) : "-";
                });

                // [lobbyspawn:portal_status_<serwer>] - "online" albo "offline"
                hud.values().register("lobbyspawn:portal_status_" + server, player ->
                        plugin.getPlayerCounts().isOnline(server) ? "online" : "offline");
            }
        });
    }

    public static void showAdsHud(Player player) {
        HudEngineProvider.find().ifPresent(hud -> hud.player(player).show(ADS_HUD_KEY));
    }

    public static void hideAdsHud(Player player) {
        HudEngineProvider.find().ifPresent(hud -> hud.player(player).hide(ADS_HUD_KEY));
    }

    public static void showQueueSpotPopup(Player player, int ticks) {
        HudEngineProvider.find().ifPresent(hud -> hud.player(player).showFor(QUEUE_SPOT_HUD_KEY, ticks));
    }
}
