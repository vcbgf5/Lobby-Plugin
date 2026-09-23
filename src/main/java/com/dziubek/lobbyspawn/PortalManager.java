package com.dziubek.lobbyspawn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class PortalManager {

    private final LobbySpawnPlugin plugin;
    private final Map<String, PortalData> portals = new LinkedHashMap<>();

    public PortalManager(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        portals.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("portals");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection portalSection = section.getConfigurationSection(id);
            if (portalSection == null) {
                continue;
            }

            String worldName = portalSection.getString("world");
            World world = worldName != null ? Bukkit.getWorld(worldName) : null;
            if (world == null) {
                plugin.getLogger().warning("Portal '" + id + "': świat '" + worldName + "' nie istnieje, pomijam.");
                continue;
            }

            double x = portalSection.getDouble("x");
            double y = portalSection.getDouble("y");
            double z = portalSection.getDouble("z");
            String displayName = portalSection.getString("display-name", id);
            String targetServer = portalSection.getString("target-server", id);
            int maxPlayers = portalSection.getInt("max-players", 100);

            Location loc = new Location(world, x, y, z);
            PortalData portal = new PortalData(id, loc, displayName, targetServer, maxPlayers);
            portals.put(id, portal);
            plugin.getQueue().setMaxPlayers(targetServer, maxPlayers);
        }

        if (plugin.isHudEnginePresent()) {
            HudEngineBridge.registerPortalValues(plugin, portals.values());
        }
    }

    /**
     * Odpytuje proxy o aktualny status (online/offline, liczba graczy) każdego serwera z portalem.
     * Dane trafiają do PlayerCountManager - korzystają z nich kolejka (ServerQueueManager),
     * launch pad (LaunchPadListener) i cząsteczki portalu (PortalParticleTask).
     */
    public void refreshPlayerCounts() {
        for (PortalData portal : portals.values()) {
            plugin.getPlayerCounts().requestStatus(portal.getTargetServer());
        }
    }

    public int getEffectiveMaxPlayers(PortalData portal) {
        int liveMax = plugin.getPlayerCounts().getMaxPlayers(portal.getTargetServer());
        return liveMax >= 0 ? liveMax : portal.getMaxPlayers();
    }

    public PortalData findByBlock(Location blockLocation) {
        for (PortalData portal : portals.values()) {
            if (portal.matches(blockLocation)) {
                return portal;
            }
        }
        return null;
    }

    public void addPortal(String id, Location plateLocation, String displayName, String targetServer, int maxPlayers) {
        FileConfiguration cfg = plugin.getConfig();
        String path = "portals." + id;
        cfg.set(path + ".world", plateLocation.getWorld().getName());
        cfg.set(path + ".x", plateLocation.getBlockX() + 0.5);
        cfg.set(path + ".y", (double) plateLocation.getBlockY());
        cfg.set(path + ".z", plateLocation.getBlockZ() + 0.5);
        cfg.set(path + ".display-name", displayName);
        cfg.set(path + ".target-server", targetServer);
        cfg.set(path + ".max-players", maxPlayers);
        plugin.saveConfig();

        loadAll();
        plugin.getPortalHolograms().reload();
    }

    public boolean removePortal(String id) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.contains("portals." + id)) {
            return false;
        }
        cfg.set("portals." + id, null);
        plugin.saveConfig();
        portals.remove(id);
        plugin.getPortalHolograms().remove(id);
        return true;
    }

    public Collection<PortalData> getPortals() {
        return portals.values();
    }
}
