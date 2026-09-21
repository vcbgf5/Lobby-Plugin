package com.dziubek.lobbyspawn;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pyta proxy (plugin BanManager, kanal "banmanager:query") czy dany gracz ma ban - globalny albo
 * na konkretny serwer - odswiezane cyklicznie dla wszystkich online graczy x wszystkie
 * skonfigurowane serwery (ten sam wzorzec co PlayerCountManager dla statusu/liczby graczy), zeby
 * lobby moglo od razu pokazac powod/czas/kto zbanowal, zamiast po cichu wyslac gracza dalej i
 * dac mu sie zablokowac/przekierowac dopiero przez proxy.
 */
public class BanCheckManager implements PluginMessageListener {

    private static final String CHANNEL = "banmanager:query";

    public static final class BanInfo {
        public final String scope;
        public final String reason;
        public final String by;
        public final long expiresAt;

        BanInfo(String scope, String reason, String by, long expiresAt) {
            this.scope = scope;
            this.reason = reason;
            this.by = by;
            this.expiresAt = expiresAt;
        }

        public boolean isPermanent() {
            return expiresAt < 0;
        }
    }

    private final LobbySpawnPlugin plugin;
    private final Map<String, BanInfo> results = new ConcurrentHashMap<>();
    private final Map<String, Boolean> closedServers = new ConcurrentHashMap<>();

    public BanCheckManager(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    /** Wysyla zapytania o ban dla kazdego online gracza x kazdy podany serwer. */
    public void refreshAll(Collection<String> servers) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            for (String server : servers) {
                requestCheck(player, server);
            }
        }
    }

    private void requestCheck(Player player, String targetServer) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteOut);
            out.writeUTF(targetServer);
            player.sendPluginMessage(plugin, CHANNEL, byteOut.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("Nie udalo sie zapytac o ban: " + e.getMessage());
        }
    }

    /** null = brak znanego bana (jeszcze nie sprawdzono albo faktycznie niezbanowany). */
    public BanInfo getBan(Player player, String targetServer) {
        return results.get(key(player.getUniqueId(), targetServer));
    }

    /** Czy dany serwer jest aktualnie zamknięty (/shutdown - "prace techniczne"). Domyślnie false, dopóki nie przyjdzie odpowiedź. */
    public boolean isClosed(String targetServer) {
        return closedServers.getOrDefault(targetServer, false);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) {
            return;
        }
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String server = in.readUTF();
            boolean banned = in.readBoolean();
            String key = key(player.getUniqueId(), server);
            if (!banned) {
                results.remove(key);
            } else {
                String scope = in.readUTF();
                String reason = in.readUTF();
                String by = in.readUTF();
                long expiresAt = in.readLong();
                results.put(key, new BanInfo(scope, reason, by, expiresAt));
            }
            closedServers.put(server, in.readBoolean());
        } catch (IOException e) {
            plugin.getLogger().warning("Blad odczytu odpowiedzi o banie: " + e.getMessage());
        }
    }

    private String key(UUID uuid, String server) {
        return uuid + "|" + server;
    }

    /** "Jesteś zbanowany na ... - na X - Przez: Y, Powód: Z" - do wiadomości dla gracza. */
    public static String formatBanMessage(BanInfo info) {
        String scope = info.scope.equalsIgnoreCase("GLOBAL") ? "całym serwerze (wszystkie tryby)" : "serwerze " + info.scope;
        String time = info.isPermanent() ? "na zawsze" : formatDuration(info.expiresAt);
        return "§4§lZOSTAŁEŚ ZBANOWANY\n"
                + "§7Zasięg: §f" + scope + "\n"
                + "§7Czas: §f" + time + "\n"
                + "§7Przez: §f" + info.by + "\n"
                + "§7Powód: §f" + info.reason;
    }

    /** "Serwer zamknięty (prace techniczne)" - do wiadomości dla gracza, gdy cel jest w trakcie /shutdown. */
    public static String formatClosedMessage(String server) {
        return "§4§lSERWER ZAMKNIĘTY\n"
                + "§7Serwer: §f" + server + "\n"
                + "§7Powód: §fPrace techniczne\n"
                + "§7Spróbuj ponownie za chwilę.";
    }

    private static String formatDuration(long expiresAt) {
        long remaining = Math.max(0, expiresAt - System.currentTimeMillis());
        long days = remaining / 86_400_000L;
        long hours = (remaining % 86_400_000L) / 3_600_000L;
        long minutes = (remaining % 3_600_000L) / 60_000L;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (days == 0 && minutes > 0) {
            sb.append(minutes).append("min");
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? "< 1min" : result;
    }
}
