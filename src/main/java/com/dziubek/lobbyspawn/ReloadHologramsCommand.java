package com.dziubek.lobbyspawn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** Usuwa WSZYSTKIE hologramy portali (DecentHolograms) i stawia je od nowa wg aktualnej listy portali. */
public class ReloadHologramsCommand implements CommandExecutor {

    private final LobbySpawnPlugin plugin;

    public ReloadHologramsCommand(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getPortalHolograms().isAvailable()) {
            sender.sendMessage("§cDecentHolograms nie jest zainstalowany na tym serwerze.");
            return true;
        }
        plugin.getPortalHolograms().reload();
        sender.sendMessage("§aHologramy portali przeładowane (" + plugin.getPortals().getPortals().size() + ").");
        return true;
    }
}
