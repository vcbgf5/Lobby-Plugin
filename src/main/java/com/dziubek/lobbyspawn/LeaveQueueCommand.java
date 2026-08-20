package com.dziubek.lobbyspawn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveQueueCommand implements CommandExecutor {

    private final LobbySpawnPlugin plugin;

    public LeaveQueueCommand(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Tej komendy może użyć tylko gracz.");
            return true;
        }

        boolean removed = plugin.getQueue().leaveQueue(player);
        if (removed) {
            player.sendMessage("§aOpuściłeś kolejkę.");
        } else {
            player.sendMessage("§cNie jesteś w żadnej kolejce.");
        }
        return true;
    }
}
