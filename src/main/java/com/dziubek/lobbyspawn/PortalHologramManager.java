package com.dziubek.lobbyspawn;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Prawdziwe hologramy (DecentHolograms) nad każdym portalem - pokazują nazwę serwera, status
 * (online/offline/prace techniczne) i liczbę graczy. W przeciwieństwie do HUDEngine (osobny
 * plugin, pozycję hologramu trzeba ustawiać ręcznie w JEGO configu) - tu lobby-plugin sam
 * decyduje gdzie hologram stoi (nad płytką portalu) i sam nim zarządza od początku do końca.
 * Miękka integracja - jeśli DecentHolograms nie jest zainstalowany, wszystkie metody nic nie robią.
 */
public class PortalHologramManager {

    private static final String ID_PREFIX = "lobbyspawn_portal_";
    private static final double HEIGHT_OFFSET = 1.6;

    private final LobbySpawnPlugin plugin;
    private final boolean available;

    public PortalHologramManager(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
        this.available = plugin.getServer().getPluginManager().getPlugin("DecentHolograms") != null;
        if (available) {
            plugin.getLogger().info("Wykryto DecentHolograms - hologramy portali aktywne.");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /** Usuwa WSZYSTKIE hologramy portali (po ID prefiksie) i stawia od nowa wg aktualnej listy portali. */
    public void reload() {
        if (!available) {
            return;
        }
        removeAll();
        for (PortalData portal : plugin.getPortals().getPortals()) {
            create(portal);
        }
    }

    public void create(PortalData portal) {
        if (!available) {
            return;
        }
        String id = hologramId(portal.getId());
        Hologram existing = DHAPI.getHologram(id);
        if (existing != null) {
            existing.delete();
        }
        Location loc = portal.getPlateLocation().clone().add(0, HEIGHT_OFFSET, 0);
        DHAPI.createHologram(id, loc, true, buildLines(portal));
    }

    public void remove(String portalId) {
        if (!available) {
            return;
        }
        Hologram existing = DHAPI.getHologram(hologramId(portalId));
        if (existing != null) {
            existing.delete();
        }
    }

    private void removeAll() {
        for (PortalData portal : plugin.getPortals().getPortals()) {
            remove(portal.getId());
        }
    }

    /** Wołane cyklicznie (ten sam interwał co refreshPlayerCounts) - podmienia tekst linii bez usuwania/tworzenia hologramu na nowo. */
    public void refreshContent() {
        if (!available) {
            return;
        }
        for (PortalData portal : plugin.getPortals().getPortals()) {
            Hologram hologram = DHAPI.getHologram(hologramId(portal.getId()));
            if (hologram != null) {
                DHAPI.setHologramLines(hologram, buildLines(portal));
            }
        }
    }

    private List<String> buildLines(PortalData portal) {
        String target = portal.getTargetServer();
        List<String> lines = new ArrayList<>();
        lines.add(portal.getDisplayName());

        if (plugin.getBanChecks().isClosed(target)) {
            lines.add("§6§lPRACE TECHNICZNE");
        } else if (plugin.getPlayerCounts().isOnline(target)) {
            int count = plugin.getPlayerCounts().getCount(target);
            int max = plugin.getPortals().getEffectiveMaxPlayers(portal);
            lines.add("§aOnline §7- §f" + Math.max(0, count) + "§7/§f" + max);
        } else {
            lines.add("§cOffline");
        }
        return lines;
    }

    private String hologramId(String portalId) {
        return ID_PREFIX + portalId;
    }
}
