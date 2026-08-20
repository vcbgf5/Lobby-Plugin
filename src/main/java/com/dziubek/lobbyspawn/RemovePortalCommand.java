package com.dziubek.lobbyspawn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RemovePortalCommand implements CommandExecutor {

    private final LobbySpawnPlugin plugin;

    public RemovePortalCommand(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUżycie: /removeportal <nazwaServera>");
            return true;
        }

        boolean removed = plugin.getPortals().removePortal(args[0]);
        if (removed) {
            sender.sendMessage("§aUsunięto portal '" + args[0] + "'.");
        } else {
            sender.sendMessage("§cNie znaleziono portalu o nazwie '" + args[0] + "'.");
        }
        return true;
    }
}
