package com.dziubek.lobbyspawn;

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

public class LobbySpawnPlugin extends JavaPlugin {

    private Location spawnLocation;

    private HologramManager holograms;
    private PlayerCountManager playerCounts;
    private PortalManager portals;
    private ServerQueueManager queue;

    private NamespacedKey wandServerKey;
    private NamespacedKey wandMaxKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSpawnLocation();

        wandServerKey = new NamespacedKey(this, "lobbyspawn_wand_server");
        wandMaxKey = new NamespacedKey(this, "lobbyspawn_wand_max");

        holograms = new HologramManager(this);
        playerCounts = new PlayerCountManager(this);
        portals = new PortalManager(this);
        queue = new ServerQueueManager(this);

        // "BungeeCord" - do teleportacji graczy między serwerami (Connect)
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        // "lobbyspawn:query" - prawdziwy status serwera (ping) od proxy, patrz PlayerCountManager
        getServer().getMessenger().registerOutgoingPluginChannel(this, "lobbyspawn:query");
        getServer().getMessenger().registerIncomingPluginChannel(this, "lobbyspawn:query", playerCounts);

        getCommand("setspawnlobby").setExecutor(new SetSpawnCommand(this));
        getCommand("addserverhologram").setExecutor(new AddServerHologramCommand(this));
        getCommand("removeportal").setExecutor(new RemovePortalCommand(this));
        getCommand("fixmovement").setExecutor(new FixMovementCommand());

        getServer().getPluginManager().registerEvents(new JoinTeleportListener(this), this);
        getServer().getPluginManager().registerEvents(new LaunchPadListener(this), this);
        getServer().getPluginManager().registerEvents(new QueueQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new WandInteractListener(this), this);

        // hologramy tworzone/aktualizowane idempotentnie - reużywają istniejące ArmorStandy po tagu,
        // więc nie trzeba już nic czyścić przed wczytaniem
        portals.loadAll();

        int refreshTicks = getConfig().getInt("portal-refresh-seconds", 5) * 20;
        getServer().getScheduler().runTaskTimer(this, () -> portals.refreshPlayerCounts(), 40L, refreshTicks);

        int queueTicks = getConfig().getInt("queue-check-seconds", 3) * 20;
        getServer().getScheduler().runTaskTimer(this, () -> queue.tick(), 60L, queueTicks);

        BossBarAdsManager bossBarAds = new BossBarAdsManager(this);
        getServer().getPluginManager().registerEvents(bossBarAds, this);
        bossBarAds.start();

        getServer().getScheduler().runTaskTimer(this, new PortalParticleTask(this), 20L, 2L);

        getLogger().info("LobbySpawn włączony! Spawn ustawiony: " + (spawnLocation != null) + ", portali: " + portals.getPortals().size());
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

    public HologramManager getHolograms() {
        return holograms;
    }

    public PlayerCountManager getPlayerCounts() {
        return playerCounts;
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
}
