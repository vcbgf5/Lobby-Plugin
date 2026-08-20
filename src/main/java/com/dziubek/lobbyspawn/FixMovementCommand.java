package com.dziubek.lobbyspawn;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FixMovementCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUżycie: /fixmovement <gracz>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§cGracz '" + args[0] + "' nie jest online na TYM serwerze.");
            return true;
        }

        boolean isSelf = sender instanceof Player && sender.equals(target);
        if (!isSelf && !sender.hasPermission("lobbyspawn.fixmovement.others")) {
            sender.sendMessage("§cNie masz uprawnień do naprawiania ruchu innych graczy.");
            return true;
        }

        // domyślne wartości vanilla - jeśli jakiś plugin (np. auth) je wyzerował, to je przywraca
        target.setWalkSpeed(0.2f);
        target.setFlySpeed(0.1f);
        target.setAllowFlight(true);

        target.sendMessage("§aTwoja prędkość chodzenia/latania została zresetowana!");
        if (!sender.equals(target)) {
            sender.sendMessage("§aZresetowano prędkość gracza " + target.getName() + ".");
        }
        return true;
    }
}
