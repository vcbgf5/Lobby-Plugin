package com.dziubek.lobbyspawn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final LobbySpawnPlugin plugin;

    public SetSpawnCommand(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tej komendy może użyć tylko gracz.");
            return true;
        }

        Player player = (Player) sender;
        plugin.setSpawnLocation(player.getLocation());
        player.sendMessage(plugin.msg("spawn-set", "&aSpawn Lobby ustawiony w tym miejscu!"));
        return true;
    }
}
