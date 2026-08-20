package com.dziubek.lobbyspawn;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class WandInteractListener implements Listener {

    private final LobbySpawnPlugin plugin;

    public WandInteractListener(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(plugin.getWandServerKey(), PersistentDataType.STRING)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null || block.getType() != Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
            player.sendMessage("§cTo nie jest złota płytka naciskowa.");
            return;
        }

        String serverName = meta.getPersistentDataContainer().get(plugin.getWandServerKey(), PersistentDataType.STRING);
        Integer maxPlayers = meta.getPersistentDataContainer().get(plugin.getWandMaxKey(), PersistentDataType.INTEGER);
        if (maxPlayers == null) {
            maxPlayers = 100;
        }

        String displayName = "&6&l" + serverName.toUpperCase();
        plugin.getPortals().addPortal(serverName, block.getLocation(), displayName, serverName, maxPlayers);

        player.sendMessage("§aStworzono portal dla serwera '" + serverName + "'.");

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }
    }
}
