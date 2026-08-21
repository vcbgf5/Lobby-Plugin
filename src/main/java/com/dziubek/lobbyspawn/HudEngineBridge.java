package com.dziubek.lobbyspawn;

import io.github.nacvark.hudengine.api.HudEngineProvider;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Cały kod odwołujący się do klas HUDEngine jest odizolowany w tej jednej klasie.
 * Java ładuje klasę (a z nią te importy) dopiero przy pierwszym realnym wywołaniu jednej
 * z jej metod - a wołamy je wyłącznie gdy LobbySpawnPlugin.isHudEnginePresent() zwraca true,
 * więc na serwerze bez zainstalowanego HUDEngine nic się nie wysypuje (brak NoClassDefFoundError).
 *
 * Klucze HUD-ów ("lobbyspawn_ads" / "lobbyspawn_queue_spot" / "lobbyspawn_queue_status") trzeba
 * zdefiniować po stronie configu samego HUDEngine (plugins/HUDEngine/huds/...) - tam projektuje
 * się wygląd. Jeśli taki HUD nie istnieje w skompilowanym configu HUDEngine, show()/showFor() po
 * prostu zwraca false i nic się nie dzieje.
 *
 * W przeciwieństwie do BetterHud, wartości HUDEngine to statyczne klucze bez argumentów
 * ([namespace:klucz], patrz HudValues), więc dla kolejki/statusu portalu rejestrujemy osobny
 * klucz per serwer docelowy - registerPortalValues() jest wołane przez PortalManager za każdym
 * razem gdy lista portali się zmienia (start, /reloadlobby, /addserverhologram). Wartości, które
 * nie zależą od konkretnego portalu (rotująca reklama, aktywna kolejka gracza, puls koloru) są
 * rejestrowane raz przy starcie przez registerGlobalValues().
 */
public final class HudEngineBridge {

    public static final String ADS_HUD_KEY = "lobbyspawn_ads";
    public static final String QUEUE_SPOT_HUD_KEY = "lobbyspawn_queue_spot";
    public static final String QUEUE_STATUS_HUD_KEY = "lobbyspawn_queue_status";

    // co ile ms zmienia sie tekst reklamy na HUD-zie (osobno od interwalu reklam na czacie -
    // tu chodzi o wrazenie "animacji", wiec krocej)
    private static final long ADS_TICKER_INTERVAL_MS = 4000L;
    // co ile ms zmienia sie faza pulsowania koloru popupu kolejki
    private static final long PULSE_INTERVAL_MS = 400L;

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

    /**
     * Rejestrowane raz przy starcie (tylko gdy HUDEngine jest wykryty) - wartości niezależne
     * od konkretnego portalu.
     */
    public static void registerGlobalValues(LobbySpawnPlugin plugin) {
        HudEngineProvider.find().ifPresent(hud -> {
            // [lobbyspawn:ads_text] - rotujacy tekst reklamy, ta sama lista co chat-ads.messages w config.yml
            hud.values().register("lobbyspawn:ads_text", player -> currentAdText(plugin));

            // [lobbyspawn:queue_active_server] / [lobbyspawn:queue_active_position] - pierwsza kolejka
            // w jakiej aktualnie czeka gracz (zazwyczaj jest w co najwyzej jednej)
            hud.values().register("lobbyspawn:queue_active_server", player -> {
                String server = plugin.getQueue().getActiveQueueServer(player.getUniqueId());
                return server != null ? server : "-";
            });
            hud.values().register("lobbyspawn:queue_active_position", player -> {
                String server = plugin.getQueue().getActiveQueueServer(player.getUniqueId());
                if (server == null) {
                    return "-";
                }
                int position = plugin.getQueue().getQueuePosition(server, player.getUniqueId());
                return position > 0 ? String.valueOf(position) : "-";
            });

            // [lobbyspawn:pulse] - "a"/"b" na zmiane co PULSE_INTERVAL_MS, do animacji koloru (color-by w layouts/)
            hud.values().register("lobbyspawn:pulse", player ->
                    (System.currentTimeMillis() / PULSE_INTERVAL_MS) % 2 == 0 ? "a" : "b");
        });
    }

    private static String currentAdText(LobbySpawnPlugin plugin) {
        List<Map<?, ?>> messages = plugin.getConfig().getMapList("chat-ads.messages");
        if (messages.isEmpty()) {
            return "";
        }
        int index = (int) ((System.currentTimeMillis() / ADS_TICKER_INTERVAL_MS) % messages.size());
        Object rawText = messages.get(index).get("text");
        if (rawText == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', rawText.toString());
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

    public static void showQueueStatusHud(Player player) {
        HudEngineProvider.find().ifPresent(hud -> hud.player(player).show(QUEUE_STATUS_HUD_KEY));
    }

    public static void hideQueueStatusHud(Player player) {
        HudEngineProvider.find().ifPresent(hud -> hud.player(player).hide(QUEUE_STATUS_HUD_KEY));
    }
}
