package com.kingbrezz.auctionhouse;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;

public final class MessageService {

    private final AuctionHousePlugin plugin;

    private File file;
    private YamlConfiguration messages;

    public MessageService(AuctionHousePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        file = new File(
                plugin.getDataFolder(),
                "messages.yml"
        );

        if (!file.exists()) {
            plugin.saveResource(
                    "messages.yml",
                    false
            );
        }

        messages =
                YamlConfiguration.loadConfiguration(file);
    }

    public String get(
            String path,
            String fallback
    ) {
        String value =
                messages.getString(
                        path,
                        fallback
                );

        return value == null
                ? fallback
                : value;
    }

    public String text(
            String path,
            String fallback
    ) {
        return color(
                get(
                        path,
                        fallback
                )
        );
    }

    public String text(
            String path,
            String fallback,
            Map<String, ?> placeholders
    ) {
        String value =
                get(
                        path,
                        fallback
                );

        if (placeholders != null) {
            for (Map.Entry<String, ?> entry :
                    placeholders.entrySet()) {

                String key =
                        "{" + entry.getKey() + "}";

                String replacement =
                        String.valueOf(
                                entry.getValue()
                        );

                value =
                        value.replace(
                                key,
                                replacement
                        );
            }
        }

        return color(value);
    }

    public void send(
            Player player,
            String path,
            String fallback
    ) {
        if (player == null) {
            return;
        }

        player.sendMessage(
                text(
                        path,
                        fallback
                )
        );
    }

    public void send(
            Player player,
            String path,
            String fallback,
            Map<String, ?> placeholders
    ) {
        if (player == null) {
            return;
        }

        player.sendMessage(
                text(
                        path,
                        fallback,
                        placeholders
                )
        );
    }

    private String color(String value) {
        if (value == null) {
            return "";
        }

        return value.replace(
                '&',
                '§'
        );
    }
}
