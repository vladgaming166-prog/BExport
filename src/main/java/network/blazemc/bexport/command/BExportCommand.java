package network.blazemc.bexport.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import network.blazemc.bexport.BExportPlugin;
import network.blazemc.bexport.export.ExportService;
import network.blazemc.bexport.util.ExportNames;
import network.blazemc.bexport.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BExportCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT = List.of("export", "exportarmor", "reload", "help");

    private final BExportPlugin plugin;
    private final ExportService exportService;
    private final Messages messages;

    public BExportCommand(BExportPlugin plugin, ExportService exportService, Messages messages) {
        this.plugin = plugin;
        this.exportService = exportService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bexport.admin")) {
            messages.send(sender, "no-permission", Map.of());
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            messages.sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                plugin.reloadPlugin();
                messages.send(sender, "reload-success", Map.of());
                return true;
            }
            case "export", "exportarmor" -> {
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "player-only", Map.of());
                    return true;
                }
                if (args.length < 2) {
                    messages.sendHelp(sender);
                    return true;
                }
                var name = ExportNames.normalize(args[1]);
                if (name.isEmpty()) {
                    messages.send(player, "invalid-name", Map.of("name", args[1]));
                    return true;
                }
                if (sub.equals("exportarmor")) {
                    exportService.exportArmor(player, name.get());
                } else {
                    exportService.exportHand(player, name.get());
                }
                return true;
            }
            default -> {
                messages.sendHelp(sender);
                return true;
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String alias,
        @NotNull String[] args
    ) {
        if (!sender.hasPermission("bexport.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return prefix(ROOT, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("export") || args[0].equalsIgnoreCase("exportarmor"))) {
            List<String> names = new ArrayList<>(exportService.knownExports());
            if (sender instanceof Player) {
                names.add("held_item");
                names.add("armor_set");
            }
            return prefix(names, args[1]);
        }
        return List.of();
    }

    private static List<String> prefix(List<String> values, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(value);
            }
        }
        return matches;
    }
}
