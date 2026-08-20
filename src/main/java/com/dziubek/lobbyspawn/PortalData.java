package com.dziubek.lobbyspawn;

import org.bukkit.Location;

public class PortalData {

    private final String id;
    private final Location plateLocation;
    private final String displayName;
    private final String targetServer;
    private final int maxPlayers;

    public PortalData(String id, Location plateLocation, String displayName, String targetServer, int maxPlayers) {
        this.id = id;
        this.plateLocation = plateLocation;
        this.displayName = displayName;
        this.targetServer = targetServer;
        this.maxPlayers = maxPlayers;
    }

    public String getId() {
        return id;
    }

    public Location getPlateLocation() {
        return plateLocation;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTargetServer() {
        return targetServer;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean matches(Location blockLocation) {
        return plateLocation.getWorld() != null
                && plateLocation.getWorld().equals(blockLocation.getWorld())
                && plateLocation.getBlockX() == blockLocation.getBlockX()
                && plateLocation.getBlockY() == blockLocation.getBlockY()
                && plateLocation.getBlockZ() == blockLocation.getBlockZ();
    }
}
