package com.dziubek.lobbyspawn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadLobbyCommand implements CommandExecutor {

    private final LobbySpawnPlugin plugin;

    public ReloadLobbyCommand(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.reloadLobbyConfig();
        sender.sendMessage("§aLobbySpawn: config.yml przeładowany (spawn, portale, bossbar).");
        return true;
    }
}
