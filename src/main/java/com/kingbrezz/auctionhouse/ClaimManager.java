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

    public void load() {
        file = new File(
                plugin.getDataFolder(),
                "claims.yml"
        );

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().severe(
                        "Could not create claims.yml: "
                                + exception.getMessage()
                );
            }
        }

        data =
                YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        if (data == null) {
            data = new YamlConfiguration();
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

    public void addItem(
            UUID player,
            ItemStack item,
            String reason
    ) {
        if (player == null
                || item == null
                || item.getType().isAir()) {
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

        data.set(
                path + ".item",
                item.clone()
        );

        data.set(
                path + ".reason",
                reason
        );

        data.set(
                path + ".created",
                System.currentTimeMillis()
        );

        save();
    }

    public List<ClaimEntry> getClaims(
            UUID player
    ) {
        List<ClaimEntry> result =
                new ArrayList<>();

        ConfigurationSection section =
                data.getConfigurationSection(
                        "claims."
                                + player
                );

        if (section == null) {
            return result;
        }

        for (String id :
                section.getKeys(false)) {

            ConfigurationSection node =
                    section.getConfigurationSection(id);

            if (node == null) {
                continue;
            }

            ItemStack item =
                    node.getItemStack("item");

            if (item == null
                    || item.getType().isAir()) {
                continue;
            }

            String reason =
                    node.getString(
                            "reason",
                            "Auction"
                    );

            long created =
                    node.getLong(
                            "created",
                            System.currentTimeMillis()
                    );

            result.add(
                    new ClaimEntry(
                            id,
                            item,
                            reason,
                            created
                    )
            );
        }

        return result;
    }

    public boolean claim(
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
                || item.getType().isAir()) {
            return false;
        }

        if (!hasSpace(player, item)) {
            return false;
        }

        player.getInventory().addItem(
                item.clone()
        );

        data.set(path, null);

        save();

        return true;
    }

    public int count(UUID player) {
        return getClaims(player).size();
    }

    private boolean hasSpace(
            Player player,
            ItemStack item
    ) {
        int remaining =
                item.getAmount();

        for (ItemStack current :
                player.getInventory().getStorageContents()) {

            if (current == null
                    || current.getType().isAir()) {
                remaining -=
                        item.getMaxStackSize();
            } else if (current.isSimilar(item)) {
                remaining -=
                        Math.max(
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

    public record ClaimEntry(
            String id,
            ItemStack item,
            String reason,
            long createdAt
    ) {
    }
            }
