package com.dziubek.lobbyspawn;

import kr.toxicity.hud.api.BetterHudAPI;
import kr.toxicity.hud.api.hud.Hud;
import kr.toxicity.hud.api.manager.PlaceholderManager;
import kr.toxicity.hud.api.placeholder.HudPlaceholder;
import kr.toxicity.hud.api.player.HudPlayer;
import kr.toxicity.hud.api.popup.Popup;
import kr.toxicity.hud.api.update.UpdateEvent;
import org.bukkit.entity.Player;

/**
 * Cały kod odwołujący się do klas BetterHud jest odizolowany w tej jednej klasie.
 * Java ładuje klasę (a z nią te importy) dopiero przy pierwszym realnym wywołaniu jednej
 * z jej metod - a wołamy je wyłącznie gdy LobbySpawnPlugin.isBetterHudPresent() zwraca true,
 * więc na serwerze bez zainstalowanego BetterHud nic się nie wysypuje (brak NoClassDefFoundError).
 *
 * Nazwy HUD-a i popupu ("lobbyspawn_ads" / "lobbyspawn_queue_spot") trzeba zdefiniować
 * po stronie configu samego BetterHud (plugins/BetterHud/...) - tam projektuje się wygląd.
 * Jeśli taki obiekt nie istnieje w configu BetterHud, wywołania po prostu nic nie robią.
 */
public final class BetterHudBridge {

    public static final String ADS_HUD_NAME = "lobbyspawn_ads";
    public static final String QUEUE_SPOT_POPUP_NAME = "lobbyspawn_queue_spot";

    private BetterHudBridge() {
    }

    public static void registerPlaceholders(LobbySpawnPlugin plugin) {
        PlaceholderManager placeholders = BetterHudAPI.inst().getPlaceholderManager();

        // %lobbyspawn_queue_position_<serwer>% - pozycja gracza w kolejce do danego serwera (albo "-" gdy nie czeka)
        HudPlaceholder.<String>builder()
                .requiredArgsLength(1)
                .function((args, event) -> hudPlayer -> {
                    int position = plugin.getQueue().getQueuePosition(args.get(0), hudPlayer.uuid());
                    return position > 0 ? String.valueOf(position) : "-";
                })
                .add("lobbyspawn_queue_position", placeholders.getStringContainer());

        // %lobbyspawn_portal_status_<serwer>% - "online" albo "offline"
        HudPlaceholder.<String>builder()
                .requiredArgsLength(1)
                .function((args, event) -> hudPlayer ->
                        plugin.getPlayerCounts().isOnline(args.get(0)) ? "online" : "offline")
                .add("lobbyspawn_portal_status", placeholders.getStringContainer());
    }

    public static void showAdsHud(Player player) {
        Hud hud = BetterHudAPI.inst().getHudManager().getHud(ADS_HUD_NAME);
        HudPlayer hudPlayer = BetterHudAPI.inst().getPlayerManager().getHudPlayer(player.getUniqueId());
        if (hud != null && hudPlayer != null) {
            hud.add(hudPlayer);
        }
    }

    public static void hideAdsHud(Player player) {
        Hud hud = BetterHudAPI.inst().getHudManager().getHud(ADS_HUD_NAME);
        HudPlayer hudPlayer = BetterHudAPI.inst().getPlayerManager().getHudPlayer(player.getUniqueId());
        if (hud != null && hudPlayer != null) {
            hud.remove(hudPlayer);
        }
    }

    public static void showQueueSpotPopup(Player player) {
        Popup popup = BetterHudAPI.inst().getPopupManager().getPopup(QUEUE_SPOT_POPUP_NAME);
        HudPlayer hudPlayer = BetterHudAPI.inst().getPlayerManager().getHudPlayer(player.getUniqueId());
        if (popup != null && hudPlayer != null) {
            popup.show(UpdateEvent.EMPTY, hudPlayer);
        }
    }
}
