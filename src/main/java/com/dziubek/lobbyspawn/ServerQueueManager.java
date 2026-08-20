package com.dziubek.lobbyspawn;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public class ServerQueueManager {

    private final LobbySpawnPlugin plugin;

    // nazwa serwera -> kolejka UUID graczy czekających
    private final Map<String, Queue<UUID>> queues = new LinkedHashMap<>();
    // nazwa serwera -> maksymalna liczba graczy (ustawiona przy tworzeniu portalu)
    private final Map<String, Integer> maxPlayers = new LinkedHashMap<>();

    public ServerQueueManager(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    public void setMaxPlayers(String serverName, int max) {
        maxPlayers.put(serverName, max);
    }

    public boolean isInQueue(String serverName, UUID uuid) {
        Queue<UUID> q = queues.get(serverName);
        return q != null && q.contains(uuid);
    }

    public void enqueue(String serverName, Player player) {
        Queue<UUID> q = queues.computeIfAbsent(serverName, k -> new LinkedList<>());
        if (q.contains(player.getUniqueId())) {
            player.sendMessage("§eJuż jesteś w kolejce do serwera " + serverName + " (pozycja " + (position(serverName, player.getUniqueId()) + 1) + "). §7Wpisz /leavequeue aby zrezygnować.");
            return;
        }
        q.add(player.getUniqueId());
        player.sendMessage("§eSerwer " + serverName + " jest pełny. Dodano Cię do kolejki (pozycja " + q.size() + "). §7Wpisz /leavequeue aby zrezygnować.");
    }

    /**
     * Usuwa gracza z kolejki na jego własne żądanie (/leavequeue).
     * @return true jeśli gracz faktycznie w jakiejś kolejce czekał
     */
    public boolean leaveQueue(Player player) {
        boolean removed = false;
        for (Queue<UUID> q : queues.values()) {
            if (q.remove(player.getUniqueId())) {
                removed = true;
            }
        }
        return removed;
    }

    public void removeFromAll(UUID uuid) {
        for (Queue<UUID> q : queues.values()) {
            q.remove(uuid);
        }
    }

    private int position(String serverName, UUID uuid) {
        Queue<UUID> q = queues.get(serverName);
        if (q == null) {
            return -1;
        }
        int i = 0;
        for (UUID u : q) {
            if (u.equals(uuid)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /**
     * Pozycja gracza w kolejce liczona od 1 (1 = następny w kolejce), albo -1 gdy nie czeka.
     * Publiczna wersja position() do użytku poza tą klasą (np. placeholder BetterHud).
     */
    public int getQueuePosition(String serverName, UUID uuid) {
        int index = position(serverName, uuid);
        return index >= 0 ? index + 1 : -1;
    }

    /**
     * Wywoływane cyklicznie - sprawdza czy na pełnych serwerach zwolniło się miejsce
     * i jeśli tak, wpuszcza pierwszego gracza z kolejki.
     */
    public void tick() {
        for (Map.Entry<String, Queue<UUID>> entry : queues.entrySet()) {
            String serverName = entry.getKey();
            Queue<UUID> q = entry.getValue();
            if (q.isEmpty()) {
                continue;
            }

            int current = plugin.getPlayerCounts().getCount(serverName);
            int max = maxPlayers.getOrDefault(serverName, 100);
            if (current < 0 || current >= max) {
                announcePositions(serverName, q);
                continue; // nadal pełny albo nie znamy stanu
            }

            UUID next = q.poll();
            Player player = plugin.getServer().getPlayer(next);
            if (player != null && player.isOnline()) {
                player.sendMessage("§aZwolniło się miejsce! Teleportacja na " + serverName + "...");
                if (plugin.isBetterHudPresent()) {
                    BetterHudBridge.showQueueSpotPopup(player);
                }
                plugin.sendToServer(player, serverName);
            }
            announcePositions(serverName, q);
        }
    }

    /**
     * Pokazuje każdemu czekającemu graczowi jego aktualną pozycję na action-barze,
     * żeby nie musiał zgadywać czy kolejka w ogóle się rusza.
     */
    private void announcePositions(String serverName, Queue<UUID> q) {
        int total = q.size();
        int i = 1;
        for (UUID uuid : q) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Component actionBar = LegacyComponentSerializer.legacySection()
                        .deserialize("§eKolejka do " + serverName + ": §f#" + i + " §7z §f" + total);
                player.sendActionBar(actionBar);
            }
            i++;
        }
    }
}
