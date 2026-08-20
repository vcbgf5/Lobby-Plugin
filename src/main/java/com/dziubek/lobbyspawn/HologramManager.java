package com.dziubek.lobbyspawn;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hologramy z ArmorStandów. Każda LINIA ma unikalny tag (np. "survival1", "survival2").
 *
 * Identyfikacja istniejącego ArmorStanda działa dwuetapowo:
 *  1. Sprawdzamy UUID zapisany w config.yml pod "hologram-entities.<tag>" - jeśli encja
 *     o tym UUID nadal istnieje, używamy jej bezpośrednio (najpewniejsza metoda).
 *  2. Jeśli nie ma zapisanego UUID (albo encja zniknęła), wymuszamy załadowanie chunka
 *     i skanujemy go po tagu PDC - bo przy starcie serwera chunk z hologramem może być
 *     jeszcze nieaktywny (0 graczy online) i normalny getEntitiesByClass() nic wtedy nie widzi,
 *     co wcześniej powodowało tworzenie duplikatów przy każdym restarcie.
 *
 * Dopiero gdy OBIE metody zawiodą, spawnujemy nowego ArmorStanda i zapisujemy jego UUID.
 */
public class HologramManager {

    private final LobbySpawnPlugin plugin;
    private final NamespacedKey tagKey;

    private final Map<String, List<ArmorStand>> holograms = new HashMap<>();

    private static final double LINE_SPACING = 0.28;

    public HologramManager(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
        this.tagKey = new NamespacedKey(plugin, "lobbyspawn_hologram_tag");
    }

    public void create(String id, Location topLocation, List<String> lines) {
        List<ArmorStand> stands = new ArrayList<>();
        Location current = topLocation.clone();

        // wymuszamy załadowanie chunka ZANIM zaczniemy czegokolwiek szukać/tworzyć
        current.getWorld().getChunkAt(current).load();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String tag = id + (i + 1); // np. "survival1", "survival2"...

            ArmorStand stand = findExisting(tag, current);

            if (stand == null) {
                stand = (ArmorStand) current.getWorld().spawnEntity(current, EntityType.ARMOR_STAND);
                stand.setVisible(false);
                stand.setGravity(false);
                stand.setMarker(true);
                stand.setSmall(true);
                stand.setInvulnerable(true);
                stand.setCustomNameVisible(true);
                stand.setPersistent(true);
                stand.getPersistentDataContainer().set(tagKey, PersistentDataType.STRING, tag);
                saveEntityUuid(tag, stand.getUniqueId());
            } else if (!sameSpot(stand.getLocation(), current)) {
                stand.teleport(current);
            }

            stand.setCustomName(ChatColor.translateAlternateColorCodes('&', line));
            stands.add(stand);

            current = current.clone().subtract(0, LINE_SPACING, 0);
        }

        holograms.put(id, stands);
    }

    private ArmorStand findExisting(String tag, Location near) {
        String savedUuid = plugin.getConfig().getString("hologram-entities." + tag);
        if (savedUuid != null) {
            try {
                Entity entity = Bukkit.getEntity(UUID.fromString(savedUuid));
                if (entity instanceof ArmorStand && !entity.isDead()) {
                    return (ArmorStand) entity;
                }
            } catch (IllegalArgumentException ignored) {
                // zepsuty/niepoprawny UUID w configu - lecimy do fallbacku niżej
            }
        }

        // fallback - skan chunka (już załadowanego) po tagu PDC
        for (Entity entity : near.getWorld().getChunkAt(near).getEntities()) {
            if (entity instanceof ArmorStand) {
                String value = entity.getPersistentDataContainer().get(tagKey, PersistentDataType.STRING);
                if (tag.equals(value)) {
                    saveEntityUuid(tag, entity.getUniqueId()); // odzyskujemy UUID do configu na przyszłość
                    return (ArmorStand) entity;
                }
            }
        }
        return null;
    }

    private void saveEntityUuid(String tag, UUID uuid) {
        plugin.getConfig().set("hologram-entities." + tag, uuid.toString());
        plugin.saveConfig();
    }

    private boolean sameSpot(Location a, Location b) {
        return a.getWorld().equals(b.getWorld())
                && Math.abs(a.getX() - b.getX()) < 0.01
                && Math.abs(a.getY() - b.getY()) < 0.01
                && Math.abs(a.getZ() - b.getZ()) < 0.01;
    }

    public void updateLine(String id, int lineIndex, String text) {
        List<ArmorStand> stands = holograms.get(id);
        if (stands == null || lineIndex < 0 || lineIndex >= stands.size()) {
            return;
        }
        ArmorStand stand = stands.get(lineIndex);
        if (stand != null && !stand.isDead()) {
            stand.setCustomName(ChatColor.translateAlternateColorCodes('&', text));
        }
    }

    public void remove(String id) {
        List<ArmorStand> stands = holograms.remove(id);
        if (stands != null) {
            for (int i = 0; i < stands.size(); i++) {
                ArmorStand stand = stands.get(i);
                if (stand != null && !stand.isDead()) {
                    stand.remove();
                }
                plugin.getConfig().set("hologram-entities." + id + (i + 1), null);
            }
            plugin.saveConfig();
        }
    }

    public void removeAll() {
        for (String id : new ArrayList<>(holograms.keySet())) {
            remove(id);
        }
    }

    public boolean exists(String id) {
        return holograms.containsKey(id);
    }
}
