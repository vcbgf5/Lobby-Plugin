package com.dziubek.lobbyspawn;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public class PortalParticleTask implements Runnable {

    private final LobbySpawnPlugin plugin;
    private double angle = 0;

    public PortalParticleTask(LobbySpawnPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        angle += 0.35;
        if (angle > Math.PI * 2) {
            angle -= Math.PI * 2;
        }

        for (PortalData portal : plugin.getPortals().getPortals()) {
            boolean online = plugin.getPlayerCounts().isOnline(portal.getTargetServer());
            World world = portal.getPlateLocation().getWorld();
            if (world == null) {
                continue;
            }

            Color color = online ? Color.LIME : Color.RED;
            Particle.DustOptions dust = new Particle.DustOptions(color, 1.0f);

            Location center = portal.getPlateLocation().clone().add(0, 0.15, 0);
            double radius = 0.6;

            // obracający się pierścień dwóch punktów po przeciwnych stronach płytki
            for (int i = 0; i < 2; i++) {
                double a = angle + (i * Math.PI);
                double x = Math.cos(a) * radius;
                double z = Math.sin(a) * radius;
                Location point = center.clone().add(x, 0, z);
                world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, dust);
            }

            // kolumna 4 statycznych punktów nad portalem (~4 bloki w górę) - widoczna "belka" oznaczająca portal
            for (int level = 0; level < 4; level++) {
                double y = 0.6 + level;
                Location point = portal.getPlateLocation().clone().add(0, y, 0);
                if (online) {
                    world.spawnParticle(Particle.END_ROD, point, 1, 0.08, 0.02, 0.08, 0.0);
                } else {
                    world.spawnParticle(Particle.SMOKE, point, 1, 0.08, 0.02, 0.08, 0.004);
                }
            }

            if (online) {
                // dodatkowy punkt "unoszący się" w górę kolumny w pętli - wrażenie energii płynącej przez portal
                double travel = (angle / (Math.PI * 2)) * 4.0;
                Location rising = portal.getPlateLocation().clone().add(0, 0.3 + travel, 0);
                world.spawnParticle(Particle.END_ROD, rising, 3, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }
}
