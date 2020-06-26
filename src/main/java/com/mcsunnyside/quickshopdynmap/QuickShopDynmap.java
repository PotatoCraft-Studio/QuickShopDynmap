package com.mcsunnyside.quickshopdynmap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.maxgamer.quickshop.api.QuickShopAPI;
import org.maxgamer.quickshop.event.ShopCreateEvent;
import org.maxgamer.quickshop.event.ShopDeleteEvent;
import org.maxgamer.quickshop.event.ShopPriceChangeEvent;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.util.Util;

import java.util.UUID;

public final class QuickShopDynmap extends JavaPlugin implements Listener {
    private DynmapAPI api;
    private MarkerAPI markerAPI;
    private MarkerSet set;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        Plugin dynmap = Bukkit.getPluginManager().getPlugin("dynmap");
        Plugin quickshop = Bukkit.getPluginManager().getPlugin("QuickShop");
        if (dynmap == null || quickshop == null) {
            getLogger().severe("Plugin won't work because dynmap or quickshop not setup correctly.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (!dynmap.isEnabled() || !quickshop.isEnabled()) {
            getLogger().severe("Dynmap or QuickShop not enabled!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        api = (DynmapAPI) dynmap;
        markerAPI = api.getMarkerAPI();
        set = markerAPI.getMarkerSet("quickshop");

        new BukkitRunnable() {
            @Override
            public void run() {
                updateMarkers();
            }
        }.runTaskTimer(this, 1, 20 * 120 * 60);

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        MarkerAPI markerAPI = api.getMarkerAPI();
        if (markerAPI == null) {
            getLogger().warning("Dynmap marker api not ready, skipping...");
            return;
        }
        MarkerSet set = markerAPI.getMarkerSet("quickshop");
        if (set != null) {
            set.getMarkers().forEach(Marker::deleteMarker);
            set.deleteMarkerSet();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onShopRemoved(ShopDeleteEvent event) {
        updateMarkers();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onShopCreated(ShopCreateEvent event) {
        updateMarkers();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onShopPriceChanged(ShopPriceChangeEvent event) {
        updateMarkers();
    }


    private void updateMarkers() {
        if (markerAPI == null) {
            getLogger().warning("Dynmap marker api not ready, skipping...");
            return;
        }
        if (set == null) {
            set = markerAPI.createMarkerSet("quickshop", getConfig().getString("marker-name"), null, false);
        }
        set.getMarkers().forEach(Marker::deleteMarker);
        for (Shop shop : QuickShopAPI.getShopAPI().getAllShops()) {
            if (!shop.isValid() || shop.isDeleted()) {
                return;
            }
            Marker marker = set.createMarker(UUID.randomUUID().toString(),
                    Util.getItemStackName(shop.getItem()) + " - " + (shop.isSelling() ? getConfig().getString("lang.selling") : getConfig().getString("lang.buying")) + " - " + shop.getPrice()
                    , shop.getLocation().getWorld().getName()
                    , shop.getLocation().getBlockX()
                    , shop.getLocation().getBlockY()
                    , shop.getLocation().getBlockZ()
                    , markerAPI.getMarkerIcon("chest"), false);
            if (marker == null) {
                return;
            }
            StringBuilder builder = new StringBuilder();

            builder.append(getConfig().getString("lang.item")).append(ChatColor.stripColor(Util.getItemStackName(shop.getItem()))).append("<br />");
            builder.append(getConfig().getString("lang.owner")).append(Bukkit.getOfflinePlayer(shop.getOwner()).getName()).append("<br />");
            builder.append(getConfig().getString("lang.type")).append(shop.isSelling() ? getConfig().getString("lang.selling") : getConfig().getString("lang.buying")).append("<br />");
            if (shop.isSelling()) {
                builder.append(getConfig().getString("lang.stock")).append(shop.getRemainingStock()).append("<br />");
            } else {
                builder.append(getConfig().getString("lang.space")).append(shop.getRemainingSpace()).append("<br />");
            }
            builder.append(getConfig().getString("lang.price")).append(shop.getPrice()).append("<br />");
            builder.append("------------").append("<br />");
            builder.append(getConfig().getString("lang.x")).append(shop.getLocation().getBlockX()).append("<br />");
            builder.append(getConfig().getString("lang.y")).append(shop.getLocation().getBlockY()).append("<br />");
            builder.append(getConfig().getString("lang.z")).append(shop.getLocation().getBlockZ()).append("<br />");

            marker.setDescription(builder.toString());
        }


    }
}
