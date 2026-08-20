package com.dziubek.lobbyspawn;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prosi proxy (SpawnPlugin, kanał "lobbyspawn:query") o zapingowanie konkretnego serwera.
 * Proxy odsyła PRAWDZIWY status z pingu: online/offline, dokładna liczba graczy i dokładny max -
 * nie zgadujemy niczego, to źródło prawdy takie samo jak zwykły ping w liście serwerów w kliencie.
 */
public class PlayerCountManager implements PluginMessageListener {

    private static final String CHANNEL = "lobbyspawn:query";

    private final LobbySpawnPlugin plugin;
    private final Map<String, Boolean> onlineStatus = new ConcurrentHashMap<>();
    private final Map<String, Integer> counts = new ConcurrentHashMap<>();
    private final Map<String, Integer> maxPlayers = new ConcurrentHashMap<>();

    public PlayerCountManager(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isOnline(String serverName) {
        return onlineStatus.getOrDefault(serverName, false);
    }

    public int getCount(String serverName) {
        return counts.getOrDefault(serverName, -1);
    }

    /**
     * Prawdziwy max graczy z serwera (z pingu). -1 jeśli jeszcze nie znamy.
     */
    public int getMaxPlayers(String serverName) {
        return maxPlayers.getOrDefault(serverName, -1);
    }

    public void requestStatus(String serverName) {
        Player any = plugin.getServer().getOnlinePlayers().stream().findAny().orElse(null);
        if (any == null) {
            return;
        }
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteOut);
            out.writeUTF(serverName);
            any.sendPluginMessage(plugin, CHANNEL, byteOut.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("Nie udało się wysłać zapytania o status: " + e.getMessage());
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) {
            return;
        }
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String server = in.readUTF();
            boolean online = in.readBoolean();
            int count = in.readInt();
            int max = in.readInt();

            onlineStatus.put(server, online);
            if (online) {
                counts.put(server, count);
                maxPlayers.put(server, max);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Błąd odczytu odpowiedzi statusu: " + e.getMessage());
        }
    }
}
