package com.kingbrezz.auctionhouse;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClaimManager {

    private final AuctionHousePlugin plugin;

    private File file;
    private YamlConfiguration data;

    public ClaimManager(AuctionHousePlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public synchronized void load() {
        file = new File(
                plugin.getDataFolder(),
                "claims.yml"
        );

        if (!plugin.getDataFolder().exists()
                && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning(
                    "Could not create plugin data folder."
            );
        }

        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().warning(
                            "Could not create claims.yml."
                    );
                }
            } catch (IOException exception) {
                plugin.getLogger().severe(
                        "Could not create claims.yml: "
                                + exception.getMessage()
                );
            }
        }

        data = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save() {
        if (data == null || file == null) {
            return;
        }

        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe(
                    "Could not save claims.yml: "
                            + exception.getMessage()
            );
        }
    }

    public synchronized void addItem(
            UUID player,
            ItemStack item,
            String reason
    ) {
        if (player == null
                || item == null
                || item.getType().isAir()
                || item.getAmount() <= 0) {
            return;
        }

        String id = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        String path =
                "claims."
                        + player
                        + "."
                        + id;

        data.set(path + ".item", item.clone());
        data.set(path + ".reason",
                reason == null ? "Auction" : reason);
        data.set(path + ".created",
                System.currentTimeMillis());

        save();
    }

    public synchronized List<ClaimEntry> getClaims(
            UUID player
    ) {
        List<ClaimEntry> result =
                new ArrayList<>();

        if (player == null || data == null) {
            return result;
        }

        ConfigurationSection section =
                data.getConfigurationSection(
                        "claims." + player
                );

        if (section == null) {
            return result;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection node =
                    section.getConfigurationSection(id);

            if (node == null) {
                continue;
            }

            ItemStack item =
                    node.getItemStack("item");

            if (item == null
                    || item.getType().isAir()
                    || item.getAmount() <= 0) {
                continue;
            }

            result.add(
                    new ClaimEntry(
                            id,
                            item,
                            node.getString(
                                    "reason",
                                    "Auction"
                            ),
                            node.getLong(
                                    "created",
                                    System.currentTimeMillis()
                            )
                    )
            );
        }

        return result;
    }

    public synchronized boolean claim(
            Player player,
            String id
    ) {
        if (player == null || id == null) {
            return false;
        }

        String path =
                "claims."
                        + player.getUniqueId()
                        + "."
                        + id;

        ItemStack item =
                data.getItemStack(
                        path + ".item"
                );

        if (item == null
                || item.getType().isAir()
                || item.getAmount() <= 0) {
            return false;
        }

        if (!hasSpace(player, item)) {
            return false;
        }

        MapGiveResult result =
                giveItem(player, item);

        if (!result.success()) {
            return false;
        }

        data.set(path, null);
        save();

        return true;
    }

    public synchronized int count(UUID player) {
        return getClaims(player).size();
    }

    private boolean hasSpace(
            Player player,
            ItemStack item
    ) {
        int remaining = item.getAmount();

        for (ItemStack current :
                player.getInventory().getStorageContents()) {

            if (current == null
                    || current.getType().isAir()) {
                remaining -= item.getMaxStackSize();
            } else if (current.isSimilar(item)) {
                remaining -= Math.max(
                        0,
                        current.getMaxStackSize()
                                - current.getAmount()
                );
            }

            if (remaining <= 0) {
                return true;
            }
        }

        return false;
    }

    private MapGiveResult giveItem(
            Player player,
            ItemStack item
    ) {
        var leftovers =
                player.getInventory().addItem(
                        item.clone()
                );

        if (!leftovers.isEmpty()) {
            return new MapGiveResult(false);
        }

        return new MapGiveResult(true);
    }

    public record ClaimEntry(
            String id,
            ItemStack item,
            String reason,
            long createdAt
    ) {
    }

    private record MapGiveResult(
            boolean success
    ) {
    }
}
