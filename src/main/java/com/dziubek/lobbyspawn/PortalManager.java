package com.dziubek.lobbyspawn;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PortalManager {

    private final LobbySpawnPlugin plugin;
    private final Map<String, PortalData> portals = new LinkedHashMap<>();

    public PortalManager(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.getHolograms().removeAll();
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

            createHologram(portal);
        }
    }

    private void createHologram(PortalData portal) {
        // plateLocation to JUŻ środek bloku (zapisane w addPortal jako blockX+0.5 itd.) -
        // więc tutaj dodajemy TYLKO wysokość, bez ponownego +0.5 na x/z (to był powód
        // przesunięcia hologramu "obok" płytki zamiast idealnie nad nią)
        Location holoLocation = portal.getPlateLocation().clone().add(0, 1.9, 0);

        List<String> lines = new ArrayList<>();
        lines.add(portal.getDisplayName());
        lines.add("&7Status: &8ładowanie...");
        lines.add("&7Graczy: &8?&7/&a" + portal.getMaxPlayers());
        lines.add("&7Wersja: &b" + shortVersion());

        plugin.getHolograms().create("portal-" + portal.getId(), holoLocation, lines);
    }

    public void refreshPlayerCounts() {
        for (PortalData portal : portals.values()) {
            plugin.getPlayerCounts().requestStatus(portal.getTargetServer());

            boolean online = plugin.getPlayerCounts().isOnline(portal.getTargetServer());
            int count = plugin.getPlayerCounts().getCount(portal.getTargetServer());

            // wolimy prawdziwy max z pingu; jak jeszcze go nie znamy, używamy tego z configu jako fallback
            int liveMax = plugin.getPlayerCounts().getMaxPlayers(portal.getTargetServer());
            int displayMax = liveMax >= 0 ? liveMax : portal.getMaxPlayers();

            String statusLine = online ? "&7Status: &a&lONLINE" : "&7Status: &c&lOFFLINE";
            String countLine = online
                    ? "&7Graczy: &a" + count + "&7/&a" + displayMax
                    : "&7Graczy: &8-&7/&a" + displayMax;

            plugin.getHolograms().updateLine("portal-" + portal.getId(), 1, statusLine);
            plugin.getHolograms().updateLine("portal-" + portal.getId(), 2, countLine);
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
    }

    public boolean removePortal(String id) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.contains("portals." + id)) {
            return false;
        }
        cfg.set("portals." + id, null);
        plugin.saveConfig();
        plugin.getHolograms().remove("portal-" + id);
        portals.remove(id);
        return true;
    }

    public Collection<PortalData> getPortals() {
        return portals.values();
    }

    private String shortVersion() {
        // np. "1.20.4" z pełnego stringa Bukkita
        String full = Bukkit.getBukkitVersion();
        int dash = full.indexOf('-');
        return dash > 0 ? full.substring(0, dash) : full;
    }
}
