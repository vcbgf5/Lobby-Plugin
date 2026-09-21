package com.dziubek.lobbyspawn;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Katalog serwerów pokazywanych w GUI kompasu (ServerSelectorMenu) - NIEZALEŻNY od fizycznych
 * portali (PortalManager/złote płytki, /addserverhologram). Gracz nie musi stać przy żadnym
 * bloku, żeby zobaczyć serwer w menu - wystarczy wpis w config.yml (menu-servers). Przy pierwszym
 * uruchomieniu po aktualizacji (brak sekcji w już istniejącym config.yml) plugin sam dopisuje
 * domyślne dwa wpisy - EcoSMP i BoxPvP.
 */
public class MenuServerManager {

    private final LobbySpawnPlugin plugin;
    private final Map<String, MenuServerData> servers = new LinkedHashMap<>();

    public MenuServerManager(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        FileConfiguration cfg = plugin.getConfig();
        if (cfg.getConfigurationSection("menu-servers") == null) {
            seedDefaults(cfg);
        }

        servers.clear();
        ConfigurationSection section = cfg.getConfigurationSection("menu-servers");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) {
                continue;
            }

            String displayName = entry.getString("display-name", id);
            String targetServer = entry.getString("target-server", id);
            int maxPlayers = entry.getInt("max-players", 100);
            Material icon = parseIcon(entry.getString("icon"));
            List<String> description = entry.getStringList("description");

            MenuServerData data = new MenuServerData(id, displayName, targetServer, maxPlayers, icon, description);
            servers.put(id, data);
            plugin.getQueue().setMaxPlayers(targetServer, maxPlayers);
        }
    }

    private Material parseIcon(String raw) {
        if (raw != null) {
            Material material = Material.matchMaterial(raw);
            if (material != null) {
                return material;
            }
        }
        return Material.COMPASS;
    }

    private void seedDefaults(FileConfiguration cfg) {
        cfg.set("menu-servers.ecosmp.display-name", "&a&lEcoSMP");
        cfg.set("menu-servers.ecosmp.target-server", "EcoSMP");
        cfg.set("menu-servers.ecosmp.max-players", 100);
        cfg.set("menu-servers.ecosmp.icon", "GRASS_BLOCK");
        cfg.set("menu-servers.ecosmp.description", List.of(
                "&7Survival z ekonomią, home'ami",
                "&7i handlem między graczami.",
                "",
                "&eDołącz i zacznij budować!"
        ));

        cfg.set("menu-servers.boxpvp.display-name", "&c&lBoxPvP");
        cfg.set("menu-servers.boxpvp.target-server", "BoxPvP");
        cfg.set("menu-servers.boxpvp.max-players", 100);
        cfg.set("menu-servers.boxpvp.icon", "DIAMOND_SWORD");
        cfg.set("menu-servers.boxpvp.description", List.of(
                "&7Box PvP - pojedynki, generatory,",
                "&7skrzynki i eventy w jednym miejscu.",
                "",
                "&eDołącz do walki!"
        ));

        plugin.saveConfig();
    }

    /** Odpytuje proxy o status (online/offline, liczba graczy) każdego serwera z menu. */
    public void refreshPlayerCounts() {
        for (MenuServerData server : servers.values()) {
            plugin.getPlayerCounts().requestStatus(server.getTargetServer());
        }
    }

    public int getEffectiveMaxPlayers(MenuServerData server) {
        int liveMax = plugin.getPlayerCounts().getMaxPlayers(server.getTargetServer());
        return liveMax >= 0 ? liveMax : server.getMaxPlayers();
    }

    public Collection<MenuServerData> getServers() {
        return servers.values();
    }
}
