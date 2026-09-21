package com.dziubek.lobbyspawn;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LaunchPadListener implements Listener {

    private final LobbySpawnPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public LaunchPadListener(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlateStep(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("launchpad.enabled", true)) {
            return;
        }
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
            // LIGHT_WEIGHTED_PRESSURE_PLATE = złota płytka naciskowa
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldownMs = plugin.getConfig().getLong("launchpad.cooldown-ms", 500);

        Long last = cooldowns.get(uuid);
        if (last != null && now - last < cooldownMs) {
            return;
        }
        cooldowns.put(uuid, now);

        PortalData portal = plugin.getPortals().findByBlock(block.getLocation());

        if (portal == null) {
            // zwykła złota płytka bez portalu - tylko wyrzut w powietrze do przodu, koniec
            launch(player, false);
            return;
        }

        String target = portal.getTargetServer();

        BanCheckManager.BanInfo ban = plugin.getBanChecks().getBan(player, target);
        if (ban != null) {
            launch(player, true); // zbanowany - wystrzel do TYŁU, jak przy offline
            player.sendMessage(BanCheckManager.formatBanMessage(ban));
            return;
        }

        if (plugin.getBanChecks().isClosed(target)) {
            launch(player, true); // prace techniczne - wystrzel do TYŁU, jak przy offline
            player.sendMessage(BanCheckManager.formatClosedMessage(target));
            return;
        }

        if (!plugin.getPlayerCounts().isOnline(target)) {
            launch(player, true); // offline - wystrzel do TYŁU
            player.sendMessage("§cSerwer '" + target + "' jest obecnie niedostępny (offline).");
            return;
        }

        launch(player, false); // online - normalnie do przodu, jak wcześniej

        int count = plugin.getPlayerCounts().getCount(target);
        int effectiveMax = plugin.getPortals().getEffectiveMaxPlayers(portal);
        if (count >= effectiveMax) {
            plugin.getQueue().enqueue(target, player);
            return;
        }

        player.sendMessage("§aTeleportacja na " + portal.getDisplayName().replaceAll("&.", "") + "...");
        if (plugin.isHudEnginePresent()) {
            int ticks = plugin.getConfig().getInt("teleport-popup-ticks", 40);
            HudEngineBridge.showTeleportNotice(player, ticks);
        }
        // małe opóźnienie, żeby gracz najpierw poczuł wyrzut w powietrze zanim proxy go przełączy
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.sendToServer(player, target);
            }
        }, 8L);
    }

    private void launch(Player player, boolean backwards) {
        double forwardPower = plugin.getConfig().getDouble("launchpad.forward-power", 1.6);
        double upwardPower = plugin.getConfig().getDouble("launchpad.upward-power", 0.9);

        Vector direction = player.getLocation().getDirection().normalize();
        if (backwards) {
            direction = direction.multiply(-1);
        }

        Vector velocity = new Vector(
                direction.getX() * forwardPower,
                upwardPower,
                direction.getZ() * forwardPower
        );

        player.setVelocity(velocity);
        Sound sound = backwards ? Sound.ENTITY_VILLAGER_NO : Sound.ENTITY_FIREWORK_ROCKET_LAUNCH;
        player.getWorld().playSound(player.getLocation(), sound, 0.7f, backwards ? 0.7f : 1.2f);
    }
}
