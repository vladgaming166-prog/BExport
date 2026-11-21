package network.blazemc.bexport.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class Messages {

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private String prefix;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.prefix = plugin.getConfig().getString("messages.prefix", "<dark_gray>[<gold>BExport</gold>]</dark_gray> ");
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        String template = plugin.getConfig().getString("messages." + path, "<red>Missing message: " + path);
        sender.sendMessage(deserialize(prefix + template, placeholders));
    }

    public void sendRaw(CommandSender sender, String path, Map<String, String> placeholders) {
        String template = plugin.getConfig().getString("messages." + path, "");
        if (template == null || template.isBlank()) {
            return;
        }
        sender.sendMessage(deserialize(prefix + template, placeholders));
    }

    public void sendHelp(CommandSender sender) {
        List<String> lines = plugin.getConfig().getStringList("messages.usage");
        if (lines.isEmpty()) {
            sender.sendMessage(deserialize(prefix + "<yellow>/bexport export <name>", Map.of()));
            return;
        }
        for (String line : lines) {
            sender.sendMessage(deserialize(line, Map.of()));
        }
    }

    public List<Component> helpLines() {
        List<Component> components = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("messages.usage")) {
            components.add(deserialize(line, Map.of()));
        }
        return components;
    }

    private Component deserialize(String template, Map<String, String> placeholders) {
        TagResolver[] resolvers = placeholders.entrySet().stream()
            .map(entry -> Placeholder.unparsed(entry.getKey(), entry.getValue()))
            .toArray(TagResolver[]::new);
        return miniMessage.deserialize(template, resolvers);
    }
}
