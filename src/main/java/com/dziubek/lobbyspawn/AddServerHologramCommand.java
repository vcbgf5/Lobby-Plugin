package com.dziubek.lobbyspawn;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class AddServerHologramCommand implements CommandExecutor {

    private final LobbySpawnPlugin plugin;

    public AddServerHologramCommand(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tej komendy może użyć tylko gracz.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§cUżycie: /addserverhologram <nazwaServera> [maxGraczy]");
            return true;
        }

        Player player = (Player) sender;
        String serverName = args[0];
        int maxPlayers = 100;
        if (args.length >= 2) {
            try {
                maxPlayers = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cmaxGraczy musi być liczbą.");
                return true;
            }
        }

        ItemStack wand = new ItemStack(Material.STICK);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName("§e§lRóżdżka portalu: §f" + serverName);

        List<String> lore = new ArrayList<>();
        lore.add("§7Kliknij PRAWYM na złotą płytkę");
        lore.add("§7naciskową, aby stworzyć tam");
        lore.add("§7portal do serwera.");
        lore.add("§8Limit graczy: " + maxPlayers);
        meta.setLore(lore);

        NamespacedKey serverKey = plugin.getWandServerKey();
        NamespacedKey maxKey = plugin.getWandMaxKey();
        meta.getPersistentDataContainer().set(serverKey, PersistentDataType.STRING, serverName);
        meta.getPersistentDataContainer().set(maxKey, PersistentDataType.INTEGER, maxPlayers);

        wand.setItemMeta(meta);
        player.getInventory().addItem(wand);

        player.sendMessage("§aDostałeś różdżkę. Kliknij nią prawym na złotą płytkę naciskową.");
        return true;
    }
}
