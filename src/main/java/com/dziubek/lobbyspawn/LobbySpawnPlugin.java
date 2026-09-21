package com.dziubek.lobbyspawn;

import io.github.nacvark.hudengine.api.HudEngineProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

public class LobbySpawnPlugin extends JavaPlugin {

    private Location spawnLocation;

    private PlayerCountManager playerCounts;
    private BanCheckManager banChecks;
    private PortalManager portals;
    private ServerQueueManager queue;
    private ChatAdsManager chatAds;
    private MenuServerManager menuServers;
    private ServerSelectorMenu serverSelectorMenu;
    private HotbarItemListener hotbarItems;
    private boolean hudEnginePresent;

    private NamespacedKey wandServerKey;
    private NamespacedKey wandMaxKey;
    private NamespacedKey hotbarItemKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSpawnLocation();

        wandServerKey = new NamespacedKey(this, "lobbyspawn_wand_server");
        wandMaxKey = new NamespacedKey(this, "lobbyspawn_wand_max");
        hotbarItemKey = new NamespacedKey(this, "lobbyspawn_hotbar_item");

        // Wykrywane przed portals.loadAll(), żeby placeholdery portali od razu się zarejestrowały w HUDEngine.
        hudEnginePresent = HudEngineProvider.find().isPresent();

        playerCounts = new PlayerCountManager(this);
        banChecks = new BanCheckManager(this);
        portals = new PortalManager(this);
        queue = new ServerQueueManager(this);
        menuServers = new MenuServerManager(this);
        serverSelectorMenu = new ServerSelectorMenu(this);

        // "BungeeCord" - do teleportacji graczy między serwerami (Connect)
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        // "lobbyspawn:query" - prawdziwy status serwera (ping) od proxy, patrz PlayerCountManager
        getServer().getMessenger().registerOutgoingPluginChannel(this, "lobbyspawn:query");
        getServer().getMessenger().registerIncomingPluginChannel(this, "lobbyspawn:query", playerCounts);
        // "banmanager:query" - czy gracz ma bana (plugin BanManager na proxy), patrz BanCheckManager
        getServer().getMessenger().registerOutgoingPluginChannel(this, "banmanager:query");
        getServer().getMessenger().registerIncomingPluginChannel(this, "banmanager:query", banChecks);

        getCommand("setspawnlobby").setExecutor(new SetSpawnCommand(this));
        getCommand("addserverhologram").setExecutor(new AddServerHologramCommand(this));
        getCommand("removeportal").setExecutor(new RemovePortalCommand(this));
        getCommand("fixmovement").setExecutor(new FixMovementCommand());
        getCommand("leavequeue").setExecutor(new LeaveQueueCommand(this));
        getCommand("reloadlobby").setExecutor(new ReloadLobbyCommand(this));

        getServer().getPluginManager().registerEvents(new JoinTeleportListener(this), this);
        getServer().getPluginManager().registerEvents(new LaunchPadListener(this), this);
        getServer().getPluginManager().registerEvents(new QueueQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new WandInteractListener(this), this);

        hotbarItems = new HotbarItemListener(this);
        getServer().getPluginManager().registerEvents(hotbarItems, this);
        // /reload pluginu (nie serwera) - dogrywa itemy graczom, którzy już byli online.
        for (Player online : getServer().getOnlinePlayers()) {
            hotbarItems.giveItems(online);
        }

        if (hudEnginePresent) {
            getServer().getPluginManager().registerEvents(new HudEngineAdsListener(), this);
            getServer().getPluginManager().registerEvents(new HudAutoHideListener(this), this);
            HudEngineBridge.registerGlobalValues(this);
            getLogger().info("Wykryto HUDEngine - reklamy/HUD beda obslugiwane przez niego (HUD '"
                    + HudEngineBridge.ADS_HUD_KEY + "', popup '" + HudEngineBridge.QUEUE_SPOT_HUD_KEY
                    + "', status kolejki '" + HudEngineBridge.QUEUE_STATUS_HUD_KEY
                    + "', teleport '" + HudEngineBridge.TELEPORT_HUD_KEY + "').");
        }

        portals.loadAll();
        menuServers.loadAll();

        int refreshTicks = getConfig().getInt("portal-refresh-seconds", 5) * 20;
        getServer().getScheduler().runTaskTimer(this, () -> {
            portals.refreshPlayerCounts();
            menuServers.refreshPlayerCounts();
            banChecks.refreshAll(allTargetServers());
        }, 40L, refreshTicks);

        int queueTicks = getConfig().getInt("queue-check-seconds", 3) * 20;
        getServer().getScheduler().runTaskTimer(this, () -> queue.tick(), 60L, queueTicks);

        chatAds = new ChatAdsManager(this);
        chatAds.start();

        getServer().getScheduler().runTaskTimer(this, new PortalParticleTask(this), 20L, 2L);

        getLogger().info("LobbySpawn włączony! Spawn ustawiony: " + (spawnLocation != null) + ", portali: " + portals.getPortals().size());
    }

    /**
     * Przeładowuje config.yml i to co z niego zależy na żywo (bez restartu serwera):
     * spawn, listę portali i reklamy na czacie. Interwały pętli tick (portal-refresh-seconds,
     * queue-check-seconds) wymagają nadal restartu, bo są ustawione raz przy planowaniu zadań.
     */
    public void reloadLobbyConfig() {
        reloadConfig();
        loadSpawnLocation();
        portals.loadAll();
        menuServers.loadAll();
        chatAds.reload();
    }

    public boolean isHudEnginePresent() {
        return hudEnginePresent;
    }

    public boolean hasSpawn() {
        return spawnLocation != null;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location location) {
        this.spawnLocation = location;

        FileConfiguration cfg = getConfig();
        cfg.set("spawn.world", location.getWorld().getName());
        cfg.set("spawn.x", location.getX());
        cfg.set("spawn.y", location.getY());
        cfg.set("spawn.z", location.getZ());
        cfg.set("spawn.yaw", location.getYaw());
        cfg.set("spawn.pitch", location.getPitch());
        saveConfig();
    }

    private void loadSpawnLocation() {
        FileConfiguration cfg = getConfig();
        String worldName = cfg.getString("spawn.world");
        if (worldName == null) {
            spawnLocation = null;
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            getLogger().warning("Świat '" + worldName + "' zapisany jako spawn nie istnieje (jeszcze?).");
            spawnLocation = null;
            return;
        }

        double x = cfg.getDouble("spawn.x");
        double y = cfg.getDouble("spawn.y");
        double z = cfg.getDouble("spawn.z");
        float yaw = (float) cfg.getDouble("spawn.yaw");
        float pitch = (float) cfg.getDouble("spawn.pitch");

        spawnLocation = new Location(world, x, y, z, yaw, pitch);
    }

    public String msg(String key, String def) {
        String raw = getConfig().getString("messages." + key, def);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', raw);
    }

    public void sendToServer(Player player, String serverName) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteOut);
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(this, "BungeeCord", byteOut.toByteArray());
        } catch (IOException e) {
            getLogger().warning("Nie udało się wysłać gracza na serwer '" + serverName + "': " + e.getMessage());
        }
    }

    public PlayerCountManager getPlayerCounts() {
        return playerCounts;
    }

    public BanCheckManager getBanChecks() {
        return banChecks;
    }

    /** Suma docelowych serwerów z fizycznych portali i z menu kompasu - do cyklicznego sprawdzania banów. */
    private Set<String> allTargetServers() {
        Set<String> set = new LinkedHashSet<>();
        for (PortalData portal : portals.getPortals()) {
            set.add(portal.getTargetServer());
        }
        for (MenuServerData server : menuServers.getServers()) {
            set.add(server.getTargetServer());
        }
        return set;
    }

    public PortalManager getPortals() {
        return portals;
    }

    public ServerQueueManager getQueue() {
        return queue;
    }

    public NamespacedKey getWandServerKey() {
        return wandServerKey;
    }

    public NamespacedKey getWandMaxKey() {
        return wandMaxKey;
    }

    public NamespacedKey getHotbarItemKey() {
        return hotbarItemKey;
    }

    public ServerSelectorMenu getServerSelectorMenu() {
        return serverSelectorMenu;
    }

    public MenuServerManager getMenuServers() {
        return menuServers;
    }
}
