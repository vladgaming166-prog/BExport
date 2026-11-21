package network.blazemc.bexport.export;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import network.blazemc.bexport.BExportPlugin;
import network.blazemc.bexport.itemsadder.ItemsAdderAccess;
import network.blazemc.bexport.itemsadder.ItemsAdderIdentity;
import network.blazemc.bexport.skcrates.SkCratesAccess;
import network.blazemc.bexport.skcrates.SkCratesFormat;
import network.blazemc.bexport.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class ExportService {

    private static final Map<EquipmentSlot, String> ARMOR_SLOTS = new LinkedHashMap<>();

    static {
        ARMOR_SLOTS.put(EquipmentSlot.HEAD, "helmet");
        ARMOR_SLOTS.put(EquipmentSlot.CHEST, "chestplate");
        ARMOR_SLOTS.put(EquipmentSlot.LEGS, "leggings");
        ARMOR_SLOTS.put(EquipmentSlot.FEET, "boots");
    }

    private final BExportPlugin plugin;
    private final Messages messages;
    private final ItemsAdderAccess itemsAdderAccess;
    private final SkCratesAccess skCratesAccess;
    private final PaperItemCodec codec = new PaperItemCodec();
    private final ItemInspector inspector = new ItemInspector();
    private final ExportWriter writer = new ExportWriter();

    public ExportService(
        BExportPlugin plugin,
        Messages messages,
        ItemsAdderAccess itemsAdderAccess,
        SkCratesAccess skCratesAccess
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.itemsAdderAccess = itemsAdderAccess;
        this.skCratesAccess = skCratesAccess;
    }

    public ExportResult exportHand(Player player, String name) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
            messages.send(player, "empty-hand", Map.of());
            return ExportResult.failed("empty-hand");
        }
        try {
            SerializedItem serialized = serialize(item, true);
            if (serialized == null) {
                messages.send(player, "validation-failed", Map.of());
                return ExportResult.failed("validation-failed");
            }
            Path exportsDir = exportsDirectory();
            Files.createDirectories(exportsDir);
            SkCratesFormat skFormat = currentSkCrates();
            Path yaml = writer.writeItem(
                exportsDir,
                name,
                player.getName(),
                serialized.item(),
                serialized.base64(),
                serialized.info(),
                serialized.identity(),
                skFormat,
                serialized.validated()
            );
            writer.updateIndex(indexFile(), name, "SINGLE_ITEM", yaml, serialized.identity(), serialized.item().getType().name());
            List<Path> files = List.of(
                yaml,
                exportsDir.resolve(name + ".txt"),
                exportsDir.resolve(name + ".skcrates.yml"),
                indexFile()
            );
            notifySuccess(player, name, "item", serialized.identity(), files, skFormat);
            plugin.getLogger().info(player.getName() + " exported item '" + name + "' to " + yaml.toAbsolutePath()
                + (serialized.identity() != null ? " ia=" + serialized.identity().namespacedId() : ""));
            return new ExportResult(true, name, "SINGLE_ITEM", files, serialized.identity(), null);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to write item export " + name, exception);
            messages.send(player, "io-error", Map.of());
            return ExportResult.failed("io-error");
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to serialize held item for " + name, exception);
            messages.send(player, "validation-failed", Map.of());
            return ExportResult.failed("serialize-error");
        }
    }

    public ExportResult exportArmor(Player player, String name) {
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) {
            messages.send(player, "empty-armor", Map.of());
            return ExportResult.failed("empty-armor");
        }
        Map<String, ItemStack> pieces = new LinkedHashMap<>();
        Map<String, String> pieceBase64 = new LinkedHashMap<>();
        Map<String, Map<String, Object>> pieceInfo = new LinkedHashMap<>();
        Map<String, ItemsAdderIdentity> pieceIds = new LinkedHashMap<>();
        List<ItemStack> ordered = new ArrayList<>();
        int present = 0;
        ItemsAdderIdentity firstIdentity = null;

        for (Map.Entry<EquipmentSlot, String> slot : ARMOR_SLOTS.entrySet()) {
            ItemStack piece = equipment.getItem(slot.getKey());
            String key = slot.getValue();
            if (piece == null || piece.getType().isAir()) {
                pieces.put(key, null);
                ordered.add(ItemStack.empty());
                continue;
            }
            SerializedItem serialized = serialize(piece, true);
            if (serialized == null) {
                messages.send(player, "validation-failed", Map.of());
                return ExportResult.failed("validation-failed");
            }
            pieces.put(key, serialized.item());
            pieceBase64.put(key, serialized.base64());
            pieceInfo.put(key, serialized.info());
            if (serialized.identity() != null) {
                pieceIds.put(key, serialized.identity());
                if (firstIdentity == null) {
                    firstIdentity = serialized.identity();
                }
            }
            ordered.add(serialized.item());
            present++;
        }
        if (present == 0) {
            messages.send(player, "empty-armor", Map.of());
            return ExportResult.failed("empty-armor");
        }

        try {
            String setBase64 = codec.encodeSet(ordered.toArray(ItemStack[]::new));
            Path exportsDir = exportsDirectory();
            Files.createDirectories(exportsDir);
            SkCratesFormat skFormat = currentSkCrates();
            Path yaml = writer.writeArmor(
                exportsDir,
                name,
                player.getName(),
                pieces,
                pieceBase64,
                pieceInfo,
                pieceIds,
                setBase64,
                skFormat,
                true
            );
            String materialSummary = present + "-piece-set";
            writer.updateIndex(indexFile(), name, "ARMOR_SET", yaml, firstIdentity, materialSummary);
            List<Path> files = List.of(
                yaml,
                exportsDir.resolve(name + ".txt"),
                exportsDir.resolve(name + ".skcrates.yml"),
                indexFile()
            );
            notifySuccess(player, name, "armor set", firstIdentity, files, skFormat);
            plugin.getLogger().info(player.getName() + " exported armor set '" + name + "' (" + present + " pieces) to " + yaml.toAbsolutePath());
            return new ExportResult(true, name, "ARMOR_SET", files, firstIdentity, null);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to write armor export " + name, exception);
            messages.send(player, "io-error", Map.of());
            return ExportResult.failed("io-error");
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to serialize armor for " + name, exception);
            messages.send(player, "validation-failed", Map.of());
            return ExportResult.failed("serialize-error");
        }
    }

    public List<String> knownExports() {
        return writer.listedExports(indexFile());
    }

    public Path exportsDirectory() {
        String folder = plugin.getConfig().getString("settings.exports-folder", "exports");
        return plugin.getDataFolder().toPath().resolve(folder);
    }

    public Path indexFile() {
        return plugin.getDataFolder().toPath().resolve("exports.yml");
    }

    private SerializedItem serialize(ItemStack original, boolean validate) {
        ItemStack clone = original.clone();
        String base64 = codec.encode(clone);
        boolean validated = false;
        if (validate && plugin.getConfig().getBoolean("settings.validate-exports", true)) {
            PaperItemCodec.ValidationResult result = codec.validate(clone, base64);
            if (!result.success()) {
                plugin.getLogger().severe("Base64 validation failed: " + result.reason());
                return null;
            }
            validated = true;
        }
        ItemsAdderIdentity identity = itemsAdderAccess.identify(clone).orElse(null);
        return new SerializedItem(clone, base64, inspector.describe(clone), identity, validated);
    }

    private SkCratesFormat currentSkCrates() {
        if (!plugin.getConfig().getBoolean("settings.detect-skcrates", true)) {
            return SkCratesFormat.notInstalled();
        }
        return skCratesAccess.inspect();
    }

    private void notifySuccess(
        Player player,
        String name,
        String type,
        ItemsAdderIdentity identity,
        List<Path> files,
        SkCratesFormat skFormat
    ) {
        messages.send(player, "export-success", Map.of("name", name, "type", type));
        if (identity != null) {
            messages.send(player, "itemsadder-id", Map.of("id", identity.namespacedId()));
        } else {
            messages.send(player, "not-itemsadder", Map.of());
        }
        messages.send(player, "export-files", Map.of());
        Path dataFolder = plugin.getDataFolder().toPath();
        for (Path file : files) {
            String relative = "plugins/" + plugin.getName() + "/" + dataFolder.relativize(file).toString().replace('\\', '/');
            messages.send(player, "export-file-line", Map.of("path", relative));
            player.sendMessage(net.kyori.adventure.text.Component.text("    " + file.toAbsolutePath()));
        }
        if (skFormat.pluginPresent()) {
            messages.send(player, "skcrates-detected", Map.of("plugin", String.valueOf(skFormat.pluginName()), "format", skFormat.label()));
        } else {
            messages.send(player, "skcrates-missing", Map.of());
        }
    }

    private record SerializedItem(
        ItemStack item,
        String base64,
        Map<String, Object> info,
        ItemsAdderIdentity identity,
        boolean validated
    ) {
    }
}
