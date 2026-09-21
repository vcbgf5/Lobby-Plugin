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
 * GUI wyboru serwera otwierane kompasem z hotbaru (styl Hypixela). Zawsze 3 rzędy (27 slotów) -
 * serwery są wyśrodkowane zamiast upchane od lewej w jednej linii, reszta wypełniona szklaną
 * "ramką" dla wyglądu. Lista serwerów, ich ikony i opisy pochodzą z MenuServerManager
 * (config.yml, menu-servers) - liczba graczy/status na żywo z PlayerCountManager (ten sam ping,
 * co hologramy portali).
 */
public class ServerSelectorMenu {

    private static final String TITLE = "§8§lWybierz serwer";
    private static final int ROWS = 3;
    private static final int SIZE = ROWS * 9;
    private static final int COLUMNS_USABLE = 7; // kolumny 1..7 - kolumny 0 i 8 to ramka

    private final LobbySpawnPlugin plugin;
    private final NamespacedKey targetKey;

    public ServerSelectorMenu(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
        this.targetKey = new NamespacedKey(plugin, "lobbyspawn_menu_target");
    }

    public void open(Player player) {
        List<MenuServerData> servers = new ArrayList<>(plugin.getMenuServers().getServers());
        if (servers.isEmpty()) {
            player.sendMessage("§cBrak skonfigurowanych serwerów.");
            return;
        }

        MenuHolder holder = new MenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, LegacyComponentSerializer.legacySection().deserialize(TITLE));
        holder.setInventory(inventory);

        ItemStack filler = buildFiller();
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        List<Integer> slots = centeredSlots(servers.size());
        for (int i = 0; i < servers.size() && i < slots.size(); i++) {
            inventory.setItem(slots.get(i), buildItem(servers.get(i), player));
        }

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    /**
     * Rozkłada `count` itemów na siatce ROWS x COLUMNS_USABLE, wyśrodkowanych w pionie i w
     * poziomie - dla 1-7 serwerów wszystkie lądują w środkowym rzędzie; więcej - rozkłada się
     * równo na kolejne rzędy (maksymalnie ROWS * COLUMNS_USABLE serwerów naraz).
     */
    private List<Integer> centeredSlots(int count) {
        int rowsNeeded = Math.max(1, Math.min(ROWS, (count + COLUMNS_USABLE - 1) / COLUMNS_USABLE));
        int base = count / rowsNeeded;
        int extra = count % rowsNeeded;
        int startRow = (ROWS - rowsNeeded) / 2;

        List<Integer> slots = new ArrayList<>();
        for (int r = 0; r < rowsNeeded; r++) {
            int itemsInRow = base + (r < extra ? 1 : 0);
            int startCol = 1 + (COLUMNS_USABLE - itemsInRow) / 2;
            for (int c = 0; c < itemsInRow && slots.size() < count; c++) {
                slots.add((startRow + r) * 9 + startCol + c);
            }
        }
        return slots;
    }

    private ItemStack buildFiller() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);
        return filler;
    }

    private ItemStack buildItem(MenuServerData server, Player viewer) {
        String target = server.getTargetServer();
        boolean online = plugin.getPlayerCounts().isOnline(target);
        int count = plugin.getPlayerCounts().getCount(target);
        int max = plugin.getMenuServers().getEffectiveMaxPlayers(server);
        BanCheckManager.BanInfo ban = plugin.getBanChecks().getBan(viewer, target);
        boolean closed = ban == null && plugin.getBanChecks().isClosed(target);

        ItemStack item = new ItemStack(ban != null ? Material.BARRIER : (closed ? Material.ANVIL : (online ? server.getIcon() : Material.GRAY_DYE)));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', server.getDisplayName()));
        meta.setEnchantmentGlintOverride(online && ban == null && !closed);

        List<String> lore = new ArrayList<>();
        List<String> description = server.getDescription();
        for (String line : description) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        if (!description.isEmpty()) {
            lore.add("");
        }
        if (ban != null) {
            for (String line : BanCheckManager.formatBanMessage(ban).split("\n")) {
                lore.add(line);
            }
        } else if (closed) {
            for (String line : BanCheckManager.formatClosedMessage(target).split("\n")) {
                lore.add(line);
            }
        } else if (online) {
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

    /** Kliknięcie w serwer w GUI - ban ma pierwszeństwo, potem offline, potem kolejka gdy pełny. */
    public void handleClick(Player player, ItemStack clicked) {
        String target = readTarget(clicked);
        if (target == null) {
            return;
        }
        player.closeInventory();

        BanCheckManager.BanInfo ban = plugin.getBanChecks().getBan(player, target);
        if (ban != null) {
            player.sendMessage(BanCheckManager.formatBanMessage(ban));
            return;
        }

        if (plugin.getBanChecks().isClosed(target)) {
            player.sendMessage(BanCheckManager.formatClosedMessage(target));
            return;
        }

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
        for (MenuServerData server : plugin.getMenuServers().getServers()) {
            if (server.getTargetServer().equals(targetServer)) {
                return plugin.getMenuServers().getEffectiveMaxPlayers(server);
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
