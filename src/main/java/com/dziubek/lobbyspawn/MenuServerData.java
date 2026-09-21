package com.dziubek.lobbyspawn;

import org.bukkit.Material;

import java.util.List;

/** Jeden wpis serwera w GUI kompasu (ServerSelectorMenu) - patrz MenuServerManager. */
public class MenuServerData {

    private final String id;
    private final String displayName;
    private final String targetServer;
    private final int maxPlayers;
    private final Material icon;
    private final List<String> description;

    public MenuServerData(String id, String displayName, String targetServer, int maxPlayers,
                           Material icon, List<String> description) {
        this.id = id;
        this.displayName = displayName;
        this.targetServer = targetServer;
        this.maxPlayers = maxPlayers;
        this.icon = icon;
        this.description = description;
    }

    public String getId() {
        return id;
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

    public Material getIcon() {
        return icon;
    }

    public List<String> getDescription() {
        return description;
    }
}
