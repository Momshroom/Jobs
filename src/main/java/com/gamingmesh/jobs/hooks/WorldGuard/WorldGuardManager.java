package com.gamingmesh.jobs.hooks.WorldGuard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.RestrictedArea;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

public class WorldGuardManager {

    private WorldGuardPlugin wg;

    public WorldGuardManager() {
        Plugin pl = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        if (pl instanceof WorldGuardPlugin) {
            wg = (WorldGuardPlugin) pl;
        }
    }

    public WorldGuardPlugin getPlugin() {
        return wg;
    }

    public List<RestrictedArea> getArea(Location loc) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(loc.getWorld()));

            if (regions != null) {
                for (ProtectedRegion one : regions.getRegions().values()) {
                    List<RestrictedArea> rest = Jobs.getRestrictedAreaManager().getRestrictedAreasByName(one.getId());

                    if (!rest.isEmpty())
                        return rest;
                }
            }
        } catch (Throwable e) {
        }

        return new ArrayList<>();
    }

    public boolean inArea(Location loc, String name) {

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(loc.getWorld()));

        if (regions != null) {
            for (ProtectedRegion one : regions.getRegions().values()) {
                if (one.getId().equalsIgnoreCase(name) && one.contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()))
                    return true;
            }
        }

        return false;
    }

    public ProtectedRegion getProtectedRegionByName(String name) {
        for (World one : Bukkit.getServer().getWorlds()) {
            Map<String, ProtectedRegion> regions = null;

            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager manager = container.get(BukkitAdapter.adapt(one));

            if (manager != null)
                regions = manager.getRegions();

            if (regions != null) {
                for (Entry<String, ProtectedRegion> map : regions.entrySet()) {
                    if (map.getKey().equalsIgnoreCase(name))
                        return map.getValue();
                }
            }
        }

        return null;
    }
}
