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

        messages =
                YamlConfiguration.loadConfiguration(file);
    }

    public String get(
            String path,
            String fallback
    ) {
        return messages.getString(path, fallback);
    }

    public String text(
            String path,
            String fallback,
            Map<String, ?> placeholders
    ) {
        String value = get(path, fallback);

        for (Map.Entry<String, ?> entry :
                placeholders.entrySet()) {
            value = value.replace(
                    "{" + entry.getKey() + "}",
                    String.valueOf(entry.getValue())
            );
        }

        return color(value);
    }

    public String text(
            String path,
            String fallback
    ) {
        return color(get(path, fallback));
    }

    public void send(
            Player player,
            String path,
            String fallback
    ) {
        player.sendMessage(text(path, fallback));
    }

    private String color(String value) {
        return value == null
                ? ""
                : value.replace('&', '§');
    }
}
