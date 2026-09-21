package com.dziubek.lobbyspawn;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Hotbar Lobby w stylu Hypixela: kompas wyboru serwera (slot 1), papier z info o serwerze
 * (slot 2, obok kompasu), barwnik ukrywania graczy (slot 8) i kompas powrotu na spawn Lobby
 * (slot 9). "Ukrywanie graczy" to lokalny toggle (jak /hide na hubach) - działa TYLKO na
 * widok klikającego gracza, nikogo innego nie robi niewidzialnym, i resetuje się przy wyjściu.
 */
public class HotbarItemListener implements Listener {

    public static final int SLOT_SELECTOR = 0;
    public static final int SLOT_INFO = 1;
    public static final int SLOT_VISIBILITY = 7;
    public static final int SLOT_LOBBY = 8;

    private final LobbySpawnPlugin plugin;
    private final ServerSelectorMenu menu;
    private final NamespacedKey itemKey;
    private final Set<UUID> hidingOthers = new HashSet<>();

    public HotbarItemListener(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
        this.menu = plugin.getServerSelectorMenu();
        this.itemKey = plugin.getHotbarItemKey();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 2 ticki po JoinTeleportListener (1 tick) - żeby dawać itemy po teleporcie na spawn.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            giveItems(player);
            for (UUID hiderUuid : hidingOthers) {
                Player hider = plugin.getServer().getPlayer(hiderUuid);
                if (hider != null) {
                    hider.hidePlayer(plugin, player);
                }
            }
        }, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        hidingOthers.remove(event.getPlayer().getUniqueId());
    }

    public void giveItems(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.setItem(SLOT_SELECTOR, buildCompassSelector());
        inv.setItem(SLOT_INFO, buildInfoPaper());
        inv.setItem(SLOT_VISIBILITY, buildVisibilityDye(hidingOthers.contains(player.getUniqueId())));
        inv.setItem(SLOT_LOBBY, buildLobbyCompass());
    }

    private ItemStack tagged(Material material, String name, String tag, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, tag);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildCompassSelector() {
        return tagged(Material.COMPASS, "§b§l⚡ Wybierz serwer", "selector",
                "§7Kliknij, aby zobaczyć listę serwerów", "§7i dołączyć do wybranego.");
    }

    private ItemStack buildInfoPaper() {
        return tagged(Material.PAPER, "§e§lInfo serwera", "info",
                "§7Kliknij, aby zobaczyć informacje", "§7o tym serwerze.");
    }

    private ItemStack buildLobbyCompass() {
        return tagged(Material.COMPASS, "§7§l🏠 Lobby", "lobby",
                "§7Kliknij, aby wrócić na spawn Lobby.");
    }

    private ItemStack buildVisibilityDye(boolean hiding) {
        if (hiding) {
            return tagged(Material.GRAY_DYE, "§7§lGracze: ukryci", "visibility",
                    "§7Kliknij, aby pokazać graczy.");
        }
        return tagged(Material.LIME_DYE, "§a§lGracze: widoczni", "visibility",
                "§7Kliknij, aby ukryć graczy.");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // druga ręka wywołałaby ten sam handler drugi raz
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK
                && action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        String tag = readTag(event.getItem());
        if (tag == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();

        switch (tag) {
            case "selector" -> menu.open(player);
            case "lobby" -> teleportToLobby(player);
            case "info" -> sendServerInfo(player);
            case "visibility" -> toggleVisibility(player);
            default -> {
            }
        }
    }

    private void teleportToLobby(Player player) {
        if (!plugin.hasSpawn()) {
            player.sendMessage(plugin.msg("spawn-not-set", "&cSpawn Lobby nie jest jeszcze ustawiony. Użyj /setspawnlobby."));
            return;
        }
        player.teleport(plugin.getSpawnLocation());
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.4f);
        player.sendMessage("§aTeleportowano na spawn Lobby.");
    }

    private void sendServerInfo(Player player) {
        int online = plugin.getServer().getOnlinePlayers().size();
        int max = plugin.getServer().getMaxPlayers();
        double tps = Math.min(20.0, plugin.getServer().getTPS()[0]);

        player.sendMessage("§8§m--------------------§r §b§lINFO SERWERA §8§m--------------------");
        player.sendMessage("§7Gracze online: §f" + online + "§7/§f" + max);
        player.sendMessage("§7TPS: " + tpsColor(tps) + String.format("%.1f", tps));
        player.sendMessage("§7Wersja: §f" + plugin.getServer().getVersion());
        player.sendMessage("§8§m------------------------------------------------");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
    }

    private String tpsColor(double tps) {
        if (tps >= 19.0) {
            return "§a";
        }
        if (tps >= 15.0) {
            return "§e";
        }
        return "§c";
    }

    private void toggleVisibility(Player player) {
        UUID uuid = player.getUniqueId();
        boolean nowHiding = !hidingOthers.contains(uuid);

        if (nowHiding) {
            hidingOthers.add(uuid);
            for (Player other : plugin.getServer().getOnlinePlayers()) {
                if (!other.equals(player)) {
                    player.hidePlayer(plugin, other);
                }
            }
            player.sendMessage("§7Gracze zostali ukryci.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 1.0f);
        } else {
            hidingOthers.remove(uuid);
            for (Player other : plugin.getServer().getOnlinePlayers()) {
                if (!other.equals(player)) {
                    player.showPlayer(plugin, other);
                }
            }
            player.sendMessage("§aGracze są teraz widoczni.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.4f);
        }
        player.getInventory().setItem(SLOT_VISIBILITY, buildVisibilityDye(nowHiding));
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (readTag(event.getItemDrop().getItemStack()) != null) {
            event.setCancelled(true);
        }
    }

    /** Blokuje przenoszenie itemów hotbaru (do skrzyni itd.) i obsługuje kliknięcia w GUI wyboru serwera. */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ServerSelectorMenu.MenuHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() != null
                    && event.getClickedInventory().getHolder() instanceof ServerSelectorMenu.MenuHolder
                    && event.getWhoClicked() instanceof Player player) {
                menu.handleClick(player, event.getCurrentItem());
            }
            return;
        }
        if (readTag(event.getCurrentItem()) != null) {
            event.setCancelled(true);
        }
    }

    private String readTag(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }
}
