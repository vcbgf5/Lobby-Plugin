package com.dziubek.lobbyspawn;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI wyboru serwera otwierane kompasem z hotbaru (styl Hypixela) - zamiast tylko fizycznych
 * portali. Dane (online/offline, liczba graczy) biorą się z tego samego źródła co hologramy
 * portali (PlayerCountManager, prawdziwy ping z proxy), więc są tak samo "na żywo" jak one -
 * odświeżają się co portal-refresh-seconds razem z resztą pluginu.
 */
public class ServerSelectorMenu {

    private static final String TITLE = "§8§lWybierz serwer";

    private final LobbySpawnPlugin plugin;
    private final NamespacedKey targetKey;

    public ServerSelectorMenu(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
        this.targetKey = new NamespacedKey(plugin, "lobbyspawn_menu_target");
    }

    public void open(Player player) {
        List<PortalData> portals = new ArrayList<>(plugin.getPortals().getPortals());
        if (portals.isEmpty()) {
            player.sendMessage("§cBrak skonfigurowanych serwerów (użyj /addserverhologram).");
            return;
        }

        int size = Math.min(54, Math.max(9, ((portals.size() + 8) / 9) * 9));
        MenuHolder holder = new MenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, size, LegacyComponentSerializer.legacySection().deserialize(TITLE));
        holder.setInventory(inventory);

        int slot = 0;
        for (PortalData portal : portals) {
            if (slot >= size) {
                break;
            }
            inventory.setItem(slot++, buildItem(portal));
        }

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    private ItemStack buildItem(PortalData portal) {
        String target = portal.getTargetServer();
        boolean online = plugin.getPlayerCounts().isOnline(target);
        int count = plugin.getPlayerCounts().getCount(target);
        int max = plugin.getPortals().getEffectiveMaxPlayers(portal);

        ItemStack item = new ItemStack(online ? Material.COMPASS : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', portal.getDisplayName()));

        List<String> lore = new ArrayList<>();
        if (online) {
            lore.add("§7Status: §aOnline");
            lore.add("§7Gracze: §f" + Math.max(0, count) + "§7/§f" + max);
            lore.add("");
            lore.add(count >= max ? "§eSerwer pełny - dołączysz do kolejki" : "§eKliknij, aby dołączyć");
        } else {
            lore.add("§7Status: §cOffline");
            lore.add("");
            lore.add("§cSerwer obecnie niedostępny");
        }
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, target);
        item.setItemMeta(meta);
        return item;
    }

    /** Kliknięcie w serwer w GUI - ta sama logika co launch pad (kolejka gdy pełny, offline = odmowa). */
    public void handleClick(Player player, ItemStack clicked) {
        String target = readTarget(clicked);
        if (target == null) {
            return;
        }
        player.closeInventory();

        if (!plugin.getPlayerCounts().isOnline(target)) {
            player.sendMessage("§cSerwer '" + target + "' jest obecnie niedostępny (offline).");
            return;
        }

        int count = plugin.getPlayerCounts().getCount(target);
        int effectiveMax = effectiveMaxFor(target);
        if (count >= effectiveMax) {
            plugin.getQueue().enqueue(target, player);
            return;
        }

        player.sendMessage("§aTeleportacja na " + target + "...");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.3f);
        if (plugin.isHudEnginePresent()) {
            int ticks = plugin.getConfig().getInt("teleport-popup-ticks", 40);
            HudEngineBridge.showTeleportNotice(player, ticks);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.sendToServer(player, target);
            }
        }, 8L);
    }

    private int effectiveMaxFor(String targetServer) {
        for (PortalData portal : plugin.getPortals().getPortals()) {
            if (portal.getTargetServer().equals(targetServer)) {
                return plugin.getPortals().getEffectiveMaxPlayers(portal);
            }
        }
        return 100;
    }

    private String readTarget(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(targetKey, PersistentDataType.STRING);
    }

    public static class MenuHolder implements InventoryHolder {
        private Inventory inventory;

        void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
